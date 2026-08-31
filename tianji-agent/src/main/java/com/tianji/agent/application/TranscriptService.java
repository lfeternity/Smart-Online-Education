package com.tianji.agent.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.tianji.agent.api.AgentException;
import com.tianji.agent.config.AgentProperties;
import com.tianji.agent.domain.KnowledgeDocumentEntity;
import com.tianji.agent.domain.KnowledgeSourceType;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class TranscriptService {
    public record TranscriptInput(Long courseId, Long chapterId, Long sectionId, String sourceId,
                                  String title, String visibility,
                                  List<TranscriptDocumentCodec.Segment> segments) { }
    public record TranscriptMetadata(Long courseId, Long chapterId, Long sectionId, String sourceId,
                                     String title, String visibility) { }

    private final KnowledgeService knowledge;
    private final TranscriptDocumentCodec codec;
    private final AgentProperties properties;
    private final WebClient asr;

    public TranscriptService(KnowledgeService knowledge, TranscriptDocumentCodec codec,
                             AgentProperties properties, WebClient.Builder builder) {
        this.knowledge = knowledge;
        this.codec = codec;
        this.properties = properties;
        String configured = properties.getAsr().getBaseUrl();
        String baseUrl = configured == null || configured.isBlank() ? properties.getAi().getBaseUrl() : configured;
        this.asr = builder.clone().baseUrl(baseUrl).build();
    }

    public KnowledgeDocumentEntity create(TranscriptInput input) {
        validate(input.courseId(), input.title(), input.sourceId());
        return knowledge.create(new KnowledgeService.KnowledgeInput(input.courseId(), input.chapterId(), input.sectionId(),
                KnowledgeSourceType.TRANSCRIPT, input.sourceId(), input.title(), codec.encode(input.segments()),
                input.visibility(), null));
    }

    public KnowledgeDocumentEntity upload(TranscriptMetadata metadata, String filename, byte[] bytes) {
        validateSize(bytes);
        List<TranscriptDocumentCodec.Segment> segments = codec.parseSubtitle(filename,
                new String(bytes, StandardCharsets.UTF_8));
        return create(new TranscriptInput(metadata.courseId(), metadata.chapterId(), metadata.sectionId(),
                metadata.sourceId(), metadata.title(), metadata.visibility(), segments));
    }

    public KnowledgeDocumentEntity transcribe(TranscriptMetadata metadata, String filename, byte[] bytes) {
        if (!properties.getAsr().isEnabled()) throw AgentException.unavailable("ASR 服务未启用");
        validate(metadata.courseId(), metadata.title(), metadata.sourceId());
        validateSize(bytes);
        MultipartBodyBuilder body = new MultipartBodyBuilder();
        body.part("model", properties.getAsr().getModel());
        body.part("response_format", "verbose_json");
        body.part("timestamp_granularities[]", "segment");
        body.part("file", new NamedByteArrayResource(bytes, safeFilename(filename)))
                .contentType(MediaType.APPLICATION_OCTET_STREAM);
        JsonNode response = asr.post().uri(properties.getAsr().getEndpoint())
                .headers(this::authorization).contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(body.build())).retrieve().bodyToMono(JsonNode.class)
                .timeout(properties.getAsr().getTimeout()).block();
        if (response == null) throw AgentException.unavailable("ASR 服务未返回结果");
        List<TranscriptDocumentCodec.Segment> segments = new ArrayList<>();
        for (JsonNode segment : response.path("segments")) {
            String text = segment.path("text").asText("").strip();
            if (text.isBlank()) continue;
            segments.add(new TranscriptDocumentCodec.Segment(
                    Math.max(0, (int) Math.floor(segment.path("start").asDouble(0))),
                    Math.max(0, (int) Math.ceil(segment.path("end").asDouble(0))), text));
        }
        if (segments.isEmpty() && !response.path("text").asText("").isBlank()) {
            segments.add(new TranscriptDocumentCodec.Segment(0, null, response.path("text").asText()));
        }
        if (segments.isEmpty()) throw AgentException.unavailable("ASR 服务未返回有效转写文本");
        return create(new TranscriptInput(metadata.courseId(), metadata.chapterId(), metadata.sectionId(),
                metadata.sourceId(), metadata.title(), metadata.visibility(), segments));
    }

    private void authorization(HttpHeaders headers) {
        String key = properties.getAsr().getApiKey();
        if (key == null || key.isBlank()) key = properties.getAi().getApiKey();
        if (key == null || key.isBlank()) throw AgentException.unavailable("ASR API key 未配置");
        headers.setBearerAuth(key);
    }

    private void validateSize(byte[] bytes) {
        if (bytes == null || bytes.length == 0) throw AgentException.badRequest("上传文件不能为空");
        if (bytes.length > properties.getAsr().getMaxFileBytes()) throw AgentException.badRequest("上传文件超过大小限制");
    }

    private void validate(Long courseId, String title, String sourceId) {
        if (courseId == null || courseId <= 0 || title == null || title.isBlank()
                || sourceId == null || sourceId.isBlank()) {
            throw AgentException.badRequest("课程、媒资标识和标题不能为空");
        }
    }

    private String safeFilename(String filename) {
        String value = filename == null ? "audio.bin" : filename.replace('\\', '/');
        value = value.substring(value.lastIndexOf('/') + 1).replaceAll("[^a-zA-Z0-9._-]", "_");
        return value.isBlank() ? "audio.bin" : value;
    }

    private static final class NamedByteArrayResource extends ByteArrayResource {
        private final String filename;
        private NamedByteArrayResource(byte[] bytes, String filename) { super(bytes); this.filename = filename; }
        @Override public String getFilename() { return filename; }
    }
}
