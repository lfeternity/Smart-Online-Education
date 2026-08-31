package com.tianji.agent.config;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A deterministic, low-memory lexical embedding for deployments without an embedding endpoint. */
public final class LocalHashEmbeddingModel implements EmbeddingModel {

    public static final String MODEL_NAME = "local-hash-v1";
    private static final Pattern TOKEN = Pattern.compile("[\\p{IsHan}]+|[\\p{L}\\p{N}]+");
    private final int dimension;

    public LocalHashEmbeddingModel(int dimension) {
        if (dimension < 1) throw new IllegalArgumentException("Embedding dimension must be positive");
        this.dimension = dimension;
    }

    @Override
    public Response<List<Embedding>> embedAll(List<TextSegment> segments) {
        if (segments == null || segments.isEmpty()) return Response.from(List.of());
        return Response.from(segments.stream().map(segment -> embedText(segment.text())).toList());
    }

    @Override
    public int dimension() {
        return dimension;
    }

    @Override
    public String modelName() {
        return MODEL_NAME;
    }

    private Embedding embedText(String value) {
        String normalized = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT);
        Map<String, Float> features = new LinkedHashMap<>();
        Matcher matcher = TOKEN.matcher(normalized);
        while (matcher.find()) {
            String token = matcher.group();
            if (token.codePoints().allMatch(LocalHashEmbeddingModel::isHan)) addHanFeatures(features, token);
            else addWordFeatures(features, token);
        }
        if (features.isEmpty()) features.put("empty", 1F);

        float[] vector = new float[dimension];
        features.forEach((feature, weight) -> project(vector, feature, weight));
        double norm = 0D;
        for (float component : vector) norm += component * component;
        if (norm > 0D) {
            float divisor = (float) Math.sqrt(norm);
            for (int index = 0; index < vector.length; index++) vector[index] /= divisor;
        }
        return Embedding.from(vector);
    }

    private void addHanFeatures(Map<String, Float> features, String token) {
        int[] points = token.codePoints().toArray();
        for (int index = 0; index < points.length; index++) {
            add(features, "h1:" + codePoints(points, index, 1), 0.35F);
            if (index + 2 <= points.length) add(features, "h2:" + codePoints(points, index, 2), 1.25F);
            if (index + 3 <= points.length) add(features, "h3:" + codePoints(points, index, 3), 0.8F);
        }
        if (points.length <= 8) add(features, "hw:" + token, 0.6F);
    }

    private void addWordFeatures(Map<String, Float> features, String token) {
        add(features, "w:" + token, 1.5F);
        String padded = "^" + token + "$";
        for (int index = 0; index + 3 <= padded.length(); index++) {
            add(features, "w3:" + padded.substring(index, index + 3), 0.45F);
        }
    }

    private void add(Map<String, Float> features, String feature, float weight) {
        features.merge(feature, weight, Math::max);
    }

    private void project(float[] vector, String feature, float weight) {
        long hash = 0xcbf29ce484222325L;
        for (int index = 0; index < feature.length(); index++) {
            hash ^= feature.charAt(index);
            hash *= 0x100000001b3L;
        }
        hash ^= hash >>> 33; hash *= 0xff51afd7ed558ccdL;
        hash ^= hash >>> 33; hash *= 0xc4ceb9fe1a85ec53L;
        hash ^= hash >>> 33;
        int slot = (int) Math.floorMod(hash, dimension);
        vector[slot] += (hash & 1L) == 0L ? weight : -weight;
    }

    private String codePoints(int[] points, int offset, int length) {
        return new String(points, offset, length);
    }

    private static boolean isHan(int codePoint) {
        return Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN;
    }
}
