package com.tianji.agent.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tianji.agent.api.AgentException;
import com.tianji.agent.config.AgentProperties;
import com.tianji.agent.domain.*;
import com.tianji.agent.persistence.KnowledgeChunkRepository;
import com.tianji.agent.persistence.KnowledgeDocumentRepository;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.Pattern;
import java.util.function.Consumer;

@Service
public class KnowledgeService {

    public record KnowledgeInput(Long courseId, Long chapterId, Long sectionId, KnowledgeSourceType sourceType,
                                 String sourceId, String title, String content, String visibility, String sourceUrl) {}

    public record SearchHit(String chunkId, String sourceType, String sourceId, Long courseId, Long chapterId,
                            Long sectionId, Integer startMoment, Integer endMoment, String title,
                            String content, double score) {
        public SearchHit withScore(double value) {
            return new SearchHit(chunkId, sourceType, sourceId, courseId, chapterId, sectionId,
                    startMoment, endMoment, title, content, value);
        }
    }

    private static final Pattern WORD_SPLIT = Pattern.compile("[^\\p{L}\\p{N}]+");
    private final KnowledgeDocumentRepository documents;
    private final KnowledgeChunkRepository chunks;
    private final AgentProperties properties;
    private final EmbeddingModel embeddingModel;
    private final RerankService rerankService;
    private final TranscriptDocumentCodec transcriptCodec;
    private final WebClient qdrant;
    private final ObjectMapper objectMapper;

    public KnowledgeService(KnowledgeDocumentRepository documents, KnowledgeChunkRepository chunks,
                            AgentProperties properties, ObjectProvider<EmbeddingModel> embeddingProvider,
                            RerankService rerankService, TranscriptDocumentCodec transcriptCodec,
                            WebClient.Builder webClientBuilder,
                            ObjectMapper objectMapper) {
        this.documents = documents;
        this.chunks = chunks;
        this.properties = properties;
        this.embeddingModel = embeddingProvider.getIfAvailable();
        this.rerankService = rerankService;
        this.transcriptCodec = transcriptCodec;
        this.qdrant = webClientBuilder.clone().baseUrl(properties.getQdrant().getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).build();
        this.objectMapper = objectMapper;
    }

    @Transactional
    public KnowledgeDocumentEntity create(KnowledgeInput input) {
        validate(input);
        String hash = digest(input.content());
        return documents.findFirstByCourseIdAndSourceTypeAndSourceIdAndContentHash(
                        input.courseId(), input.sourceType(), input.sourceId(), hash)
                .orElseGet(() -> {
                    KnowledgeDocumentEntity entity = new KnowledgeDocumentEntity();
                    entity.setCourseId(input.courseId()); entity.setChapterId(input.chapterId());
                    entity.setSectionId(input.sectionId()); entity.setSourceType(input.sourceType());
                    entity.setSourceId(input.sourceId()); entity.setTitle(input.title().trim());
                    entity.setContent(clean(input.content())); entity.setContentHash(hash);
                    int version = documents.findTopByCourseIdAndSourceTypeAndSourceIdOrderByVersionDesc(
                                    input.courseId(), input.sourceType(), input.sourceId())
                            .map(KnowledgeDocumentEntity::getVersion).orElse(0) + 1;
                    entity.setVersion(version);
                    entity.setVisibility(input.visibility() == null ? "ENROLLED" : input.visibility().toUpperCase());
                    entity.setSourceUrl(input.sourceUrl());
                    return documents.save(entity);
                });
    }

    @Transactional
    public KnowledgeDocumentEntity createNextVersion(String id) {
        KnowledgeDocumentEntity source = documents.findById(id)
                .orElseThrow(() -> AgentException.notFound("知识文档不存在"));
        KnowledgeInput input = new KnowledgeInput(source.getCourseId(), source.getChapterId(), source.getSectionId(),
                source.getSourceType(), source.getSourceId(), source.getTitle(), source.getContent(),
                source.getVisibility(), source.getSourceUrl());
        KnowledgeDocumentEntity copy = new KnowledgeDocumentEntity();
        copy.setCourseId(input.courseId()); copy.setChapterId(input.chapterId()); copy.setSectionId(input.sectionId());
        copy.setSourceType(input.sourceType()); copy.setSourceId(input.sourceId()); copy.setTitle(input.title());
        copy.setContent(input.content()); copy.setContentHash(source.getContentHash()); copy.setVisibility(input.visibility());
        copy.setSourceUrl(input.sourceUrl());
        int version = documents.findTopByCourseIdAndSourceTypeAndSourceIdOrderByVersionDesc(
                        source.getCourseId(), source.getSourceType(), source.getSourceId())
                .map(KnowledgeDocumentEntity::getVersion).orElse(source.getVersion()) + 1;
        copy.setVersion(version);
        return documents.save(copy);
    }

    @Transactional
    public KnowledgeDocumentEntity publish(String id) {
        return publish(id, stage -> { });
    }

    @Transactional
    public KnowledgeDocumentEntity publish(String id, Consumer<IngestionStage> progress) {
        progress.accept(IngestionStage.VALIDATING);
        KnowledgeDocumentEntity document = documents.findById(id)
                .orElseThrow(() -> AgentException.notFound("知识文档不存在"));
        document.setStatus(KnowledgeStatus.PROCESSING);
        documents.save(document);
        chunks.deleteByDocumentId(document.getId());
        if (canUseQdrant()) {
            ensureCollection();
            deleteDocumentPoints(document.getId());
        }
        progress.accept(IngestionStage.SPLITTING);
        List<KnowledgeChunkEntity> created = split(document);
        chunks.saveAll(created);
        if (canUseQdrant()) {
            try {
                progress.accept(IngestionStage.EMBEDDING);
                progress.accept(IngestionStage.INDEXING);
                upsert(created, document.getVisibility());
            } catch (RuntimeException exception) {
                document.setStatus(KnowledgeStatus.FAILED);
                documents.save(document);
                throw AgentException.unavailable("知识向量索引失败");
            }
        }
        List<KnowledgeDocumentEntity> previous = documents
                .findByCourseIdAndSourceTypeAndSourceIdAndStatus(document.getCourseId(), document.getSourceType(),
                        document.getSourceId(), KnowledgeStatus.ACTIVE);
        progress.accept(IngestionStage.ACTIVATING);
        for (KnowledgeDocumentEntity old : previous) {
            if (old.getId().equals(document.getId())) continue;
            old.setStatus(KnowledgeStatus.ARCHIVED);
            documents.save(old);
            List<KnowledgeChunkEntity> oldChunks = chunks.findByDocumentIdOrderByChunkIndexAsc(old.getId());
            oldChunks.forEach(chunk -> chunk.setActive(false));
            chunks.saveAll(oldChunks);
        }
        created.forEach(chunk -> chunk.setActive(true));
        chunks.saveAll(created);
        document.setStatus(KnowledgeStatus.ACTIVE);
        KnowledgeDocumentEntity active = documents.save(document);
        progress.accept(IngestionStage.COMPLETED);
        return active;
    }

    @Transactional
    public void archiveCourse(Long courseId) {
        for (KnowledgeDocumentEntity document : documents.findByCourseIdAndStatus(courseId, KnowledgeStatus.ACTIVE)) {
            document.setStatus(KnowledgeStatus.ARCHIVED);
            documents.save(document);
            List<KnowledgeChunkEntity> values = chunks.findByDocumentIdOrderByChunkIndexAsc(document.getId());
            values.forEach(chunk -> chunk.setActive(false));
            chunks.saveAll(values);
        }
    }

    @Transactional(readOnly = true)
    public List<KnowledgeDocumentEntity> list() {
        return documents.findTop100ByOrderByUpdateTimeDesc();
    }

    public List<SearchHit> search(Long userId, String requestId, Long courseId, Long sectionId, String query) {
        if (query == null || query.isBlank()) throw AgentException.badRequest("检索问题不能为空");
        int candidateK = properties.getAi().getRetrievalCandidateK();
        List<SearchHit> dense = List.of();
        if (canUseQdrant()) {
            try {
                dense = qdrantSearch(courseId, sectionId, query, candidateK);
            } catch (RuntimeException ignored) {
                // Local keyword retrieval keeps the learning assistant available during vector-store incidents.
            }
        }
        List<SearchHit> lexical = localSearch(courseId, sectionId, query, candidateK);
        List<SearchHit> candidates = reciprocalRankFusion(dense, lexical, sectionId, candidateK);
        return rerankService.rerank(query, candidates, properties.getAi().getRetrievalTopK());
    }

    private List<SearchHit> localSearch(Long courseId, Long sectionId, String query, int limit) {
        Set<String> terms = terms(query);
        return chunks.findTop500ByCourseIdAndActiveTrue(courseId).stream()
                .map(chunk -> Map.entry(chunk, lexicalScore(terms, chunk.getTitle() + " " + chunk.getContent(), sectionId)))
                .filter(entry -> entry.getValue() > 0)
                .sorted(Map.Entry.<KnowledgeChunkEntity, Double>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> hit(entry.getKey(), entry.getValue())).toList();
    }

    private List<SearchHit> qdrantSearch(Long courseId, Long sectionId, String query, int limit) {
        List<String> activeDocuments = documents.findByCourseIdAndStatus(courseId, KnowledgeStatus.ACTIVE)
                .stream().map(KnowledgeDocumentEntity::getId).toList();
        if (activeDocuments.isEmpty()) return List.of();
        List<Float> vector = embeddingModel.embed(query).content().vectorAsList();
        ObjectNode body = objectMapper.createObjectNode();
        body.set("vector", objectMapper.valueToTree(vector));
        body.put("limit", limit);
        body.put("score_threshold", properties.getAi().getRetrievalMinScore());
        body.put("with_payload", true);
        ArrayNode must = body.putObject("filter").putArray("must");
        must.add(match("courseId", courseId)); must.add(matchAny("documentId", activeDocuments));
        if (sectionId != null) {
            ArrayNode should = body.withObject("filter").putArray("should");
            should.add(match("sectionId", sectionId));
        }
        JsonNode response = qdrant.post().uri("/collections/" + properties.getQdrant().getCollection() + "/points/search")
                .headers(this::qdrantAuth).bodyValue(body).retrieve().bodyToMono(JsonNode.class)
                .timeout(properties.getAi().getTimeout()).block();
        if (response == null) return List.of();
        List<SearchHit> hits = new ArrayList<>();
        for (JsonNode item : response.path("result")) {
            JsonNode payload = item.path("payload");
            hits.add(new SearchHit(payload.path("chunkId").asText(), payload.path("sourceType").asText(),
                    textOrNull(payload, "sourceId"), payload.path("courseId").asLong(), longOrNull(payload, "chapterId"),
                    longOrNull(payload, "sectionId"), intOrNull(payload, "startMoment"), intOrNull(payload, "endMoment"),
                    payload.path("title").asText(), payload.path("content").asText(), item.path("score").asDouble()));
        }
        return hits;
    }

    List<SearchHit> reciprocalRankFusion(List<SearchHit> dense, List<SearchHit> lexical,
                                         Long sectionId, int limit) {
        Map<String, SearchHit> values = new LinkedHashMap<>();
        Map<String, Double> scores = new HashMap<>();
        addRanks(dense, values, scores);
        addRanks(lexical, values, scores);
        return values.values().stream()
                .map(hit -> hit.withScore(scores.getOrDefault(hit.chunkId(), 0D)
                        * (sectionId != null && sectionId.equals(hit.sectionId()) ? 1.08D : 1D)))
                .sorted(Comparator.comparingDouble(SearchHit::score).reversed()
                        .thenComparing(SearchHit::chunkId))
                .limit(limit)
                .toList();
    }

    private void addRanks(List<SearchHit> ranked, Map<String, SearchHit> values, Map<String, Double> scores) {
        for (int index = 0; index < ranked.size(); index++) {
            SearchHit hit = ranked.get(index);
            if (hit.chunkId() == null || hit.chunkId().isBlank()) continue;
            values.putIfAbsent(hit.chunkId(), hit);
            scores.merge(hit.chunkId(), 1D / (60D + index + 1D), Double::sum);
        }
    }

    private void ensureCollection() {
        JsonNode existing = qdrant.get().uri("/collections/" + properties.getQdrant().getCollection())
                .headers(this::qdrantAuth).exchangeToMono(response -> {
                    if (response.statusCode().value() == 404) return Mono.empty();
                    if (response.statusCode().isError()) return response.createException().flatMap(Mono::error);
                    return response.bodyToMono(JsonNode.class);
                }).timeout(properties.getAi().getTimeout()).block();
        if (existing != null) {
            int actualDimension = existing.path("result").path("config").path("params")
                    .path("vectors").path("size").asInt(-1);
            if (actualDimension != properties.getQdrant().getDimension()) {
                throw new IllegalStateException("Qdrant collection dimension mismatch: " + actualDimension);
            }
            return;
        }
        ObjectNode vectors = objectMapper.createObjectNode();
        vectors.put("size", properties.getQdrant().getDimension()); vectors.put("distance", "Cosine");
        qdrant.put().uri("/collections/" + properties.getQdrant().getCollection())
                .headers(this::qdrantAuth).bodyValue(objectMapper.createObjectNode().set("vectors", vectors))
                .retrieve().toBodilessEntity().timeout(properties.getAi().getTimeout()).block();
    }

    private void upsert(List<KnowledgeChunkEntity> values, String visibility) {
        ArrayNode points = objectMapper.createArrayNode();
        for (KnowledgeChunkEntity chunk : values) {
            ObjectNode point = points.addObject(); point.put("id", chunk.getId());
            point.set("vector", objectMapper.valueToTree(embeddingModel.embed(chunk.getContent()).content().vectorAsList()));
            ObjectNode payload = point.putObject("payload");
            payload.put("chunkId", chunk.getId()); payload.put("documentId", chunk.getDocumentId());
            payload.put("courseId", chunk.getCourseId());
            put(payload, "chapterId", chunk.getChapterId()); put(payload, "sectionId", chunk.getSectionId());
            payload.put("sourceType", chunk.getSourceType()); put(payload, "sourceId", chunk.getSourceId());
            payload.put("title", chunk.getTitle()); payload.put("content", chunk.getContent());
            payload.put("active", true); payload.put("visibility", visibility);
            put(payload, "startMoment", chunk.getStartMoment()); put(payload, "endMoment", chunk.getEndMoment());
        }
        ObjectNode body = objectMapper.createObjectNode(); body.set("points", points);
        qdrant.put().uri("/collections/" + properties.getQdrant().getCollection() + "/points?wait=true")
                .headers(this::qdrantAuth).bodyValue(body).retrieve().toBodilessEntity()
                .timeout(properties.getAi().getTimeout().multipliedBy(4)).block();
    }

    private void deleteDocumentPoints(String documentId) {
        ObjectNode body = objectMapper.createObjectNode();
        body.set("filter", objectMapper.createObjectNode().set("must",
                objectMapper.createArrayNode().add(match("documentId", documentId))));
        qdrant.post().uri("/collections/" + properties.getQdrant().getCollection() + "/points/delete?wait=true")
                .headers(this::qdrantAuth).bodyValue(body).retrieve().toBodilessEntity()
                .timeout(properties.getAi().getTimeout()).block();
    }

    private List<KnowledgeChunkEntity> split(KnowledgeDocumentEntity document) {
        if (document.getSourceType() == KnowledgeSourceType.TRANSCRIPT) {
            List<KnowledgeChunkEntity> timeline = new ArrayList<>();
            int index = 0;
            for (TranscriptDocumentCodec.TimelineChunk part : transcriptCodec.chunks(document.getContent(), 1800)) {
                KnowledgeChunkEntity chunk = chunk(document, part.content(), index++);
                chunk.setStartMoment(part.startMoment());
                chunk.setEndMoment(part.endMoment());
                timeline.add(chunk);
            }
            return timeline;
        }
        String content = clean(document.getContent());
        int target = 1800, overlap = 220, index = 0, start = 0;
        List<KnowledgeChunkEntity> result = new ArrayList<>();
        while (start < content.length()) {
            int end = Math.min(content.length(), start + target);
            if (end < content.length()) {
                int boundary = Math.max(content.lastIndexOf('\n', end), content.lastIndexOf('。', end));
                if (boundary > start + target / 2) end = boundary + 1;
            }
            String part = content.substring(start, end).trim();
            if (!part.isBlank()) {
                result.add(chunk(document, part, index++));
            }
            if (end >= content.length()) break;
            start = Math.max(start + 1, end - overlap);
        }
        return result;
    }

    private KnowledgeChunkEntity chunk(KnowledgeDocumentEntity document, String content, int index) {
        KnowledgeChunkEntity chunk = new KnowledgeChunkEntity();
        chunk.setDocumentId(document.getId()); chunk.setCourseId(document.getCourseId());
        chunk.setChapterId(document.getChapterId()); chunk.setSectionId(document.getSectionId());
        chunk.setSourceType(document.getSourceType().name()); chunk.setSourceId(document.getSourceId());
        chunk.setTitle(document.getTitle()); chunk.setContent(content); chunk.setContentHash(digest(content));
        chunk.setChunkIndex(index); chunk.setEmbeddingModel(embeddingModel == null
                ? properties.getAi().getEmbeddingModel() : embeddingModel.modelName());
        return chunk;
    }

    private double lexicalScore(Set<String> queryTerms, String text, Long sectionId) {
        String lower = text.toLowerCase(Locale.ROOT); int matches = 0;
        for (String term : queryTerms) if (lower.contains(term)) matches++;
        return queryTerms.isEmpty() ? 0 : (double) matches / queryTerms.size();
    }

    private Set<String> terms(String value) {
        String lower = value.toLowerCase(Locale.ROOT); Set<String> result = new LinkedHashSet<>();
        for (String word : WORD_SPLIT.split(lower)) {
            if (word.length() >= 2) result.add(word);
            if (word.codePoints().anyMatch(cp -> Character.UnicodeScript.of(cp) == Character.UnicodeScript.HAN)) {
                for (int i = 0; i + 2 <= word.length(); i++) result.add(word.substring(i, i + 2));
            }
        }
        return result;
    }

    private SearchHit hit(KnowledgeChunkEntity chunk, double score) {
        return new SearchHit(chunk.getId(), chunk.getSourceType(), chunk.getSourceId(), chunk.getCourseId(),
                chunk.getChapterId(), chunk.getSectionId(), chunk.getStartMoment(), chunk.getEndMoment(),
                chunk.getTitle(), chunk.getContent(), score);
    }

    private ObjectNode match(String key, Object value) {
        ObjectNode condition = objectMapper.createObjectNode(); condition.put("key", key);
        condition.set("match", objectMapper.createObjectNode().set("value", objectMapper.valueToTree(value)));
        return condition;
    }

    private ObjectNode matchAny(String key, List<String> values) {
        ObjectNode condition = objectMapper.createObjectNode(); condition.put("key", key);
        condition.set("match", objectMapper.createObjectNode().set("any", objectMapper.valueToTree(values)));
        return condition;
    }

    private boolean canUseQdrant() { return properties.getQdrant().isEnabled() && embeddingModel != null; }
    private void qdrantAuth(HttpHeaders headers) { if (!properties.getQdrant().getApiKey().isBlank()) headers.set("api-key", properties.getQdrant().getApiKey()); }
    private void put(ObjectNode node, String name, Object value) { if (value != null) node.set(name, objectMapper.valueToTree(value)); }
    private Long longOrNull(JsonNode node, String name) { return node.hasNonNull(name) ? node.path(name).asLong() : null; }
    private Integer intOrNull(JsonNode node, String name) { return node.hasNonNull(name) ? node.path(name).asInt() : null; }
    private String textOrNull(JsonNode node, String name) { return node.hasNonNull(name) ? node.path(name).asText() : null; }
    private String clean(String text) { return text.replaceAll("(?is)<script.*?>.*?</script>", " ").replaceAll("<[^>]+>", " ").replace("\\u0000", "").trim(); }
    private void validate(KnowledgeInput input) {
        if (input.courseId() == null || input.courseId() <= 0 || input.sourceType() == null || input.title() == null || input.title().isBlank() || input.content() == null || input.content().isBlank()) {
            throw AgentException.badRequest("知识文档的课程、类型、标题和内容不能为空");
        }
    }
    private String digest(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception exception) { throw new IllegalStateException(exception); }
    }
}
