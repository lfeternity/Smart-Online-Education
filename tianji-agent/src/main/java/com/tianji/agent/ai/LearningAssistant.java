package com.tianji.agent.ai;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

public interface LearningAssistant {
    TokenStream chat(@MemoryId String conversationId, @UserMessage String message);
}
