package com.tianji.agent.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tianji.agent.config.AgentProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class RerankService {
    private final AgentProperties properties;
    private final ObjectMapper objectMapper;
    private final WebClient client;

    public RerankService(AgentProperties properties, ObjectMapper objectMapper, WebClient.Builder builder) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        String baseUrl = properties.getRerank().getBaseUrl();
        this.client = builder.clone().baseUrl(baseUrl == null || baseUrl.isBlank() ? "http://localhost" : baseUrl).build();
    }

    public List<KnowledgeService.SearchHit> rerank(String query, List<KnowledgeService.SearchHit> candidates, int topK) {
        if (!properties.getRerank().isEnabled() || candidates.isEmpty()) return candidates.stream().limit(topK).toList();
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", properties.getRerank().getModel());
            body.put("query", query); body.put("top_n", topK);
            ArrayNode documents = body.putArray("documents");
            candidates.forEach(hit -> documents.add(hit.title() + "\n" + hit.content()));
            JsonNode response = client.post().uri(properties.getRerank().getEndpoint())
                    .contentType(MediaType.APPLICATION_JSON).headers(this::authorization)
                    .bodyValue(body).retrieve().bodyToMono(JsonNode.class)
                    .timeout(properties.getRerank().getTimeout()).block();
            if (response == null || !response.path("results").isArray()) return candidates.stream().limit(topK).toList();
            List<KnowledgeService.SearchHit> result = new ArrayList<>();
            for (JsonNode item : response.path("results")) {
                int index = item.path("index").asInt(-1);
                if (index < 0 || index >= candidates.size()) continue;
                KnowledgeService.SearchHit hit = candidates.get(index);
                result.add(hit.withScore(item.path("relevance_score").asDouble(hit.score())));
            }
            result.sort(Comparator.comparingDouble(KnowledgeService.SearchHit::score).reversed());
            return result.stream().limit(topK).toList();
        } catch (RuntimeException ignored) {
            return candidates.stream().limit(topK).toList();
        }
    }

    private void authorization(HttpHeaders headers) {
        String apiKey = properties.getRerank().getApiKey();
        if (apiKey != null && !apiKey.isBlank()) headers.setBearerAuth(apiKey);
    }
}
