package com.tianji.agent.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KnowledgeMessagingConfiguration {
    @Bean
    DirectExchange knowledgeDeadLetterExchange() {
        return new DirectExchange("agent.knowledge.dlx", true, false);
    }

    @Bean
    Queue knowledgeDeadLetterQueue() {
        return new Queue("agent.knowledge.dead.queue", true);
    }

    @Bean
    Binding knowledgeDeadLetterBinding(DirectExchange knowledgeDeadLetterExchange,
                                       Queue knowledgeDeadLetterQueue) {
        return BindingBuilder.bind(knowledgeDeadLetterQueue).to(knowledgeDeadLetterExchange)
                .with("knowledge.failed");
    }
}
