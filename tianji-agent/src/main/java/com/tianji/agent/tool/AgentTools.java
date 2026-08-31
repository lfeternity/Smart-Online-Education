package com.tianji.agent.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tianji.agent.api.AgentException;
import com.tianji.agent.api.ConversationDtos.PendingActionResponse;
import com.tianji.agent.application.KnowledgeService;
import com.tianji.agent.application.PendingActionService;
import com.tianji.agent.client.BusinessClients;
import com.tianji.agent.domain.PendingActionType;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;

public class AgentTools {

    private final AgentRequestContext context;
    private final BusinessClients clients;
    private final PendingActionService actions;
    private final KnowledgeService knowledge;
    private final ObjectMapper objectMapper;
    private final List<KnowledgeService.SearchHit> retrieved = new ArrayList<>();

    public AgentTools(AgentRequestContext context, BusinessClients clients, PendingActionService actions,
                      KnowledgeService knowledge, ObjectMapper objectMapper) {
        this.context = context;
        this.clients = clients;
        this.actions = actions;
        this.knowledge = knowledge;
        this.objectMapper = objectMapper;
    }

    @Tool(name = "get_current_lesson", value = "查询当前登录学员最近正在学习的课程")
    public ToolResult<JsonNode> getCurrentLesson() {
        return ok(clients.currentLesson(context.userId(), context.requestId()));
    }

    @Tool(name = "get_my_lessons", value = "分页查询当前登录学员的课表")
    public ToolResult<JsonNode> getMyLessons(
            @P(value = "页码，从1开始", required = false) Integer pageNo,
            @P(value = "每页条数，最多20", required = false) Integer pageSize) {
        int page = pageNo == null ? 1 : Math.max(1, pageNo);
        int size = pageSize == null ? 10 : Math.max(1, Math.min(20, pageSize));
        return ok(clients.myLessons(context.userId(), context.requestId(), page, size));
    }

    @Tool(name = "get_learning_progress", value = "查询当前学员在指定已报名课程中的学习进度")
    public ToolResult<JsonNode> getLearningProgress(@P("课程ID") Long courseId) {
        Long id = requireCourse(courseId);
        requireAccess(id);
        return ok(clients.learningProgress(context.userId(), context.requestId(), id));
    }

    @Tool(name = "get_course_outline", value = "查询课程的章、节和练习目录")
    public ToolResult<JsonNode> getCourseOutline(@P("课程ID") Long courseId) {
        Long id = requireCourse(courseId);
        requireAccess(id);
        return ok(clients.courseOutline(context.userId(), context.requestId(), id));
    }

    @Tool(name = "search_courses", value = "按关键词搜索公开课程，不用于检索课程讲义内容")
    public ToolResult<JsonNode> searchCourses(@P("课程关键词") String keyword,
                                               @P(value = "返回数量，最多10", required = false) Integer limit) {
        if (keyword == null || keyword.isBlank()) throw AgentException.badRequest("课程关键词不能为空");
        return ok(clients.searchCourses(context.userId(), context.requestId(), keyword.trim(),
                limit == null ? 5 : Math.max(1, Math.min(10, limit))));
    }

    @Tool(name = "get_section_practice", value = "查询章节练习题干和选项；为保护答题过程，永远不返回答案和解析")
    public ToolResult<JsonNode> getSectionPractice(@P("练习或小节业务ID") Long bizId,
                                                    @P(value = "所属课程ID", required = false) Long courseId) {
        if (bizId == null || bizId <= 0) throw AgentException.badRequest("练习ID无效");
        Long id = requireCourse(courseId);
        requireAccess(id);
        JsonNode questions = clients.sectionPractice(context.userId(), context.requestId(), bizId);
        return ok(stripAnswers(questions.deepCopy()));
    }

    @Tool(name = "retrieve_course_knowledge", value = "从当前学员有权访问的课程资料检索答案依据和可引用来源")
    public ToolResult<?> retrieveCourseKnowledge(@P("自然语言检索问题") String query,
                                                  @P(value = "课程ID", required = false) Long courseId) {
        Long id = requireCourse(courseId);
        requireAccess(id);
        List<KnowledgeService.SearchHit> hits = knowledge.search(context.userId(), context.requestId(), id,
                context.page() == null ? null : context.page().sectionId(), query);
        retrieved.addAll(hits);
        return ToolResult.ok(hits, context.requestId());
    }

    @Tool(name = "prepare_learning_plan", value = "准备创建每周学习计划，仅生成待确认操作，不会立即执行")
    public ToolResult<PendingActionResponse> prepareLearningPlan(@P("课程表中的课程ID") Long courseId,
                                                                  @P("每周计划学习章节数，1到50") Integer freq) {
        requireAccess(courseId);
        Map<String, Object> payload = new LinkedHashMap<>(); payload.put("courseId", courseId); payload.put("freq", freq);
        return prepare(PendingActionType.CREATE_LEARNING_PLAN, payload, "为课程创建每周 " + freq + " 节的学习计划");
    }

    @Tool(name = "prepare_note", value = "准备保存当前课程笔记，仅生成待确认操作，不会立即执行")
    public ToolResult<PendingActionResponse> prepareNote(@P("笔记正文") String content,
                                                         @P(value = "是否私密", required = false) Boolean isPrivate) {
        var page = requirePageContext();
        Map<String, Object> payload = new LinkedHashMap<>(); payload.put("content", content);
        payload.put("noteMoment", page.playMoment() == null ? 0 : page.playMoment()); payload.put("isPrivate", isPrivate == null || isPrivate);
        payload.put("courseId", page.courseId()); payload.put("chapterId", page.chapterId()); payload.put("sectionId", page.sectionId());
        return prepare(PendingActionType.CREATE_NOTE, payload, "保存当前小节笔记");
    }

    @Tool(name = "prepare_question", value = "准备发布课程问题，仅生成待确认操作，不会立即执行")
    public ToolResult<PendingActionResponse> prepareQuestion(@P("问题标题") String title,
                                                             @P("问题详细描述") String description,
                                                             @P(value = "是否匿名", required = false) Boolean anonymity) {
        var page = requirePageContext();
        Map<String, Object> payload = new LinkedHashMap<>(); payload.put("title", title); payload.put("description", description);
        payload.put("anonymity", anonymity != null && anonymity); payload.put("courseId", page.courseId());
        payload.put("chapterId", page.chapterId()); payload.put("sectionId", page.sectionId());
        return prepare(PendingActionType.CREATE_QUESTION, payload, "发布问题：" + title);
    }

    private ToolResult<PendingActionResponse> prepare(PendingActionType type, Map<String, ?> payload, String summary) {
        return ok(actions.prepare(context.userId(), context.conversationId(), type,
                objectMapper.valueToTree(payload), summary));
    }

    private Long requireCourse(Long requested) {
        Long id = context.courseIdOr(requested);
        if (id == null || id <= 0) throw AgentException.badRequest("请先选择课程");
        return id;
    }

    private void requireAccess(Long courseId) {
        if (courseId == null || !clients.hasCourseAccess(context.userId(), context.requestId(), courseId)) {
            throw AgentException.forbidden("无权访问该课程");
        }
    }

    private com.tianji.agent.api.ConversationDtos.ChatContext requirePageContext() {
        var page = context.page();
        if (page == null || page.courseId() == null || page.chapterId() == null || page.sectionId() == null) {
            throw AgentException.badRequest("当前页面缺少课程章节上下文");
        }
        requireAccess(page.courseId());
        return page;
    }

    private JsonNode stripAnswers(JsonNode node) {
        if (node instanceof ObjectNode object) {
            object.remove(java.util.List.of("answer", "analysis"));
            object.fields().forEachRemaining(entry -> stripAnswers(entry.getValue()));
        } else if (node instanceof ArrayNode array) {
            array.forEach(this::stripAnswers);
        }
        return node;
    }

    private <T> ToolResult<T> ok(T data) {
        return ToolResult.ok(data, context.requestId());
    }

    public List<KnowledgeService.SearchHit> retrievedCitations() {
        return List.copyOf(retrieved);
    }
}
