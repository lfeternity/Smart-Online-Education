package com.tianji.agent.evaluation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class EvaluationDatasetGateTest {
    @Test
    void datasetMeetsRequiredSizeDistributionAndGroundTruthSchema() throws Exception {
        var resource = getClass().getResourceAsStream("/evaluation/agent-evaluation-dataset.jsonl");
        assertNotNull(resource, "evaluation dataset is missing");
        ObjectMapper mapper = new ObjectMapper();
        List<JsonNode> cases;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource, StandardCharsets.UTF_8))) {
            cases = reader.lines().filter(line -> !line.isBlank()).map(line -> {
                try { return mapper.readTree(line); }
                catch (Exception error) { throw new IllegalArgumentException("invalid evaluation JSONL", error); }
            }).toList();
        }
        assertEquals(100, cases.size());
        assertEquals(Map.of("FACT", 30L, "CROSS_CHAPTER", 15L, "LEARNING_PLAN", 15L,
                        "TOOL", 15L, "NO_ANSWER", 10L, "SECURITY", 15L),
                cases.stream().collect(java.util.stream.Collectors.groupingBy(
                        value -> value.path("category").asText(), java.util.stream.Collectors.counting())));
        Set<String> ids = new HashSet<>();
        for (JsonNode value : cases) {
            assertTrue(ids.add(value.path("id").asText()), "duplicate evaluation id");
            assertFalse(value.path("question").asText().isBlank());
            assertFalse(value.path("expectedAnswer").asText().isBlank());
            assertTrue(value.path("sourceIds").isArray());
            assertTrue(value.path("forbidden").isArray());
            String category = value.path("category").asText();
            if (category.equals("FACT") || category.equals("CROSS_CHAPTER")) {
                assertFalse(value.path("sourceIds").isEmpty(), "grounded cases require sources");
            }
            if (category.equals("TOOL")) assertFalse(value.path("expectedTool").asText().isBlank());
            if (category.equals("SECURITY") || category.equals("NO_ANSWER")) {
                assertTrue(value.path("expectedRefusal").asBoolean());
                assertFalse(value.path("forbidden").isEmpty());
            }
        }
    }
}
