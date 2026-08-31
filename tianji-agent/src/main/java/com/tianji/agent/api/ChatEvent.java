package com.tianji.agent.api;

import java.util.Map;

public record ChatEvent(String type, Map<String, Object> data) {

    public static ChatEvent of(String type, Object... keyValues) {
        java.util.LinkedHashMap<String, Object> values = new java.util.LinkedHashMap<>();
        for (int index = 0; index < keyValues.length; index += 2) {
            values.put(String.valueOf(keyValues[index]), keyValues[index + 1]);
        }
        return new ChatEvent(type, values);
    }
}
