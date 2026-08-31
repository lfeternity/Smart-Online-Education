package com.tianji.agent.domain;

import jakarta.persistence.Column;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LongTextColumnMappingTest {

    @Test
    void lobStringsMatchFlywayLongTextColumns() throws Exception {
        Map<Class<?>, String> fields = Map.of(
                ConversationEntity.class, "summary",
                MessageEntity.class, "content",
                PendingActionEntity.class, "payload",
                KnowledgeDocumentEntity.class, "content",
                KnowledgeChunkEntity.class, "content",
                PromptVersionEntity.class, "content"
        );

        for (Map.Entry<Class<?>, String> entry : fields.entrySet()) {
            Field field = entry.getKey().getDeclaredField(entry.getValue());
            assertEquals("LONGTEXT", field.getAnnotation(Column.class).columnDefinition(),
                    entry.getKey().getSimpleName() + "." + entry.getValue());
        }
    }
}
