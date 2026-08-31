package com.tianji.agent.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.tianji.agent.api.AgentException;
import com.tianji.agent.config.AgentProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

@Component
public class BusinessClients {

    private final ServiceClient course;
    private final ServiceClient learning;
    private final ServiceClient exam;
    private final ServiceClient search;

    public BusinessClients(WebClient.Builder builder, AgentProperties properties) {
        Duration timeout = properties.getClients().getTimeout();
        this.course = new ServiceClient(builder.clone().baseUrl(properties.getClients().getCourseBaseUrl()).build(), timeout);
        this.learning = new ServiceClient(builder.clone().baseUrl(properties.getClients().getLearningBaseUrl()).build(), timeout);
        this.exam = new ServiceClient(builder.clone().baseUrl(properties.getClients().getExamBaseUrl()).build(), timeout);
        this.search = new ServiceClient(builder.clone().baseUrl(properties.getClients().getSearchBaseUrl()).build(), timeout);
    }

    public JsonNode currentLesson(Long userId, String requestId) {
        return learning.get("/lessons/now", userId, requestId);
    }

    public JsonNode myLessons(Long userId, String requestId, int pageNo, int pageSize) {
        return learning.get("/lessons/page?pageNo=" + pageNo + "&pageSize=" + pageSize, userId, requestId);
    }

    public JsonNode lesson(Long userId, String requestId, Long courseId) {
        return learning.get("/lessons/" + courseId, userId, requestId);
    }

    public boolean hasCourseAccess(Long userId, String requestId, Long courseId) {
        JsonNode result = learning.get("/lessons/" + courseId + "/valid", userId, requestId);
        return !result.isNull() && result.asLong(0) > 0;
    }

    public JsonNode learningProgress(Long userId, String requestId, Long courseId) {
        return learning.get("/learning-records/course/" + courseId, userId, requestId);
    }

    public JsonNode learningPlans(Long userId, String requestId) {
        return learning.get("/lessons/plans?pageNo=1&pageSize=50", userId, requestId);
    }

    public JsonNode courseOutline(Long userId, String requestId, Long courseId) {
        return course.get("/courses/" + courseId + "/catalogs", userId, requestId);
    }

    public JsonNode courseInfo(Long userId, String requestId, Long courseId) {
        return course.get("/course/" + courseId + "?withCatalogue=true&withTeachers=false", userId, requestId);
    }

    public JsonNode searchCourses(Long userId, String requestId, String keyword, int pageSize) {
        return search.get("/courses/portal?keyword={keyword}&pageNo=1&pageSize={pageSize}", userId, requestId,
                Map.of("keyword", keyword, "pageSize", pageSize));
    }

    public JsonNode sectionPractice(Long userId, String requestId, Long bizId) {
        return exam.get("/questions/listOfBiz?bizId=" + bizId, userId, requestId);
    }

    public void createLearningPlan(Long userId, String requestId, JsonNode payload) {
        learning.post("/lessons/plans", userId, requestId, payload);
    }

    public void createNote(Long userId, String requestId, JsonNode payload) {
        learning.post("/notes", userId, requestId, payload);
    }

    public void createQuestion(Long userId, String requestId, JsonNode payload) {
        learning.post("/questions", userId, requestId, payload);
    }

    static final class ServiceClient {
        private final WebClient client;
        private final Duration timeout;

        ServiceClient(WebClient client, Duration timeout) {
            this.client = client;
            this.timeout = timeout;
        }

        JsonNode get(String uri, Long userId, String requestId) {
            return get(uri, userId, requestId, Map.of());
        }

        JsonNode get(String uri, Long userId, String requestId, Map<String, ?> variables) {
            try {
                return client.get().uri(uri, variables)
                        .headers(headers -> identity(headers, userId, requestId))
                        .retrieve().bodyToMono(JsonNode.class).timeout(timeout).block();
            } catch (RuntimeException exception) {
                throw AgentException.unavailable("业务服务暂时不可用，请稍后重试");
            }
        }

        void post(String uri, Long userId, String requestId, JsonNode body) {
            try {
                client.post().uri(uri)
                        .headers(headers -> identity(headers, userId, requestId))
                        .contentType(MediaType.APPLICATION_JSON).bodyValue(body)
                        .retrieve().toBodilessEntity().timeout(timeout).block();
            } catch (RuntimeException exception) {
                throw AgentException.unavailable("业务操作执行失败，请稍后重试");
            }
        }

        private void identity(HttpHeaders headers, Long userId, String requestId) {
            headers.set("user-info", String.valueOf(userId));
            headers.set("requestId", requestId);
        }
    }
}
