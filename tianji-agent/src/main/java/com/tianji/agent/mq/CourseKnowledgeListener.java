package com.tianji.agent.mq;

import com.fasterxml.jackson.databind.JsonNode;
import com.tianji.agent.api.RequestIds;
import com.tianji.agent.application.KnowledgeService;
import com.tianji.agent.application.KnowledgeService.KnowledgeInput;
import com.tianji.agent.application.IngestionJobService;
import com.tianji.agent.client.BusinessClients;
import com.tianji.agent.config.AgentProperties;
import com.tianji.agent.domain.KnowledgeSourceType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CourseKnowledgeListener {
    private final KnowledgeService knowledge;
    private final BusinessClients clients;
    private final AgentProperties properties;
    private final IngestionJobService jobs;

    public CourseKnowledgeListener(KnowledgeService knowledge, BusinessClients clients, AgentProperties properties,
                                   IngestionJobService jobs) {
        this.knowledge = knowledge; this.clients = clients; this.properties = properties; this.jobs = jobs;
    }

    @RabbitListener(autoStartup = "${agent.knowledge.mq-enabled:false}", bindings = @QueueBinding(
            value = @Queue(name = "agent.course.up.queue", durable = "true"),
            exchange = @Exchange(name = "course.topic", type = ExchangeTypes.TOPIC), key = "course.up"))
    public void up(Long courseId) {
        String requestId = RequestIds.create();
        JsonNode course = clients.courseInfo(properties.getKnowledge().getIngestionUserId(), requestId, courseId);
        String title = course.path("name").asText("课程 " + courseId);
        var document = knowledge.create(new KnowledgeInput(courseId, null, null, KnowledgeSourceType.COURSE,
                String.valueOf(courseId), title, course.toPrettyString(), "ENROLLED", null));
        var job = jobs.enqueue(document.getId(), properties.getKnowledge().getIngestionUserId());
        log.info("Course knowledge queued, courseId={}, documentId={}, jobId={}", courseId, document.getId(), job.getId());
    }

    @RabbitListener(autoStartup = "${agent.knowledge.mq-enabled:false}", bindings = @QueueBinding(
            value = @Queue(name = "agent.course.inactive.queue", durable = "true"),
            exchange = @Exchange(name = "course.topic", type = ExchangeTypes.TOPIC),
            key = {"course.down", "course.expire", "course.delete"}))
    public void inactive(Long courseId) { knowledge.archiveCourse(courseId); }
}
