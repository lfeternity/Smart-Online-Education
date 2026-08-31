package com.tianji.agent.client;

import com.tianji.agent.config.AgentProperties;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BusinessClientsContractTest {
    @Test
    void sendsTrustedIdentityAndUsesLearningContractPath() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setHeader("Content-Type", "application/json")
                    .setBody("{\"progress\":50}"));
            server.start();
            AgentProperties properties = new AgentProperties();
            String url = server.url("/").toString();
            properties.getClients().setLearningBaseUrl(url);
            properties.getClients().setCourseBaseUrl(url);
            properties.getClients().setExamBaseUrl(url);
            properties.getClients().setSearchBaseUrl(url);
            BusinessClients clients = new BusinessClients(WebClient.builder(), properties);

            assertEquals(50, clients.learningProgress(7L, "request-1", 9L).path("progress").asInt());
            var request = server.takeRequest();
            assertEquals("/learning-records/course/9", request.getPath());
            assertEquals("7", request.getHeader("user-info"));
            assertEquals("request-1", request.getHeader("requestId"));
        }
    }
}
