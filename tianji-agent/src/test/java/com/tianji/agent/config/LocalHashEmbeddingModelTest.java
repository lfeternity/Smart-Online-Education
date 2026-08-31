package com.tianji.agent.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalHashEmbeddingModelTest {

    private final LocalHashEmbeddingModel model = new LocalHashEmbeddingModel(1536);

    @Test
    void producesDeterministicNormalizedVectorsWithConfiguredDimension() {
        float[] first = model.embed("Java 并发与异步编排").content().vector();
        float[] second = model.embed("Java 并发与异步编排").content().vector();

        assertEquals(1536, first.length);
        assertArrayEquals(first, second);
        assertEquals(1D, norm(first), 0.00001D);
        assertEquals(LocalHashEmbeddingModel.MODEL_NAME, model.modelName());
    }

    @Test
    void relatedChineseAndEnglishTextScoresAboveUnrelatedText() {
        float[] query = model.embed("Java CompletableFuture 异步并发").content().vector();
        float[] related = model.embed("CompletableFuture 支持 Java 异步任务组合和并发执行").content().vector();
        float[] unrelated = model.embed("摄影课程讲解光圈快门与构图方法").content().vector();

        assertTrue(cosine(query, related) > cosine(query, unrelated));
        assertTrue(cosine(query, related) > 0.05D);
    }

    private double norm(float[] vector) {
        double sum = 0D;
        for (float value : vector) sum += value * value;
        return Math.sqrt(sum);
    }

    private double cosine(float[] left, float[] right) {
        double score = 0D;
        for (int index = 0; index < left.length; index++) score += left[index] * right[index];
        return score;
    }
}
