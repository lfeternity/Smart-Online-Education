package com.tianji.agent.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tianji.agent.api.AgentException;
import com.tianji.agent.application.KnowledgeService;
import com.tianji.agent.application.PendingActionService;
import com.tianji.agent.client.BusinessClients;
import com.tianji.agent.api.ConversationDtos.ChatContext;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

class AgentToolsTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void sectionPracticeNeverExposesAnswers() {
        BusinessClients clients = Mockito.mock(BusinessClients.class);
        ObjectNode question = mapper.createObjectNode().put("id", 1).put("name", "题目")
                .put("answer", "A").put("analysis", "解析");
        Mockito.when(clients.hasCourseAccess(7L, "req", 9L)).thenReturn(true);
        Mockito.when(clients.sectionPractice(7L, "req", 11L)).thenReturn(mapper.createArrayNode().add(question));
        AgentTools tools = new AgentTools(new AgentRequestContext(7L, "req", "conv", "msg",
                new ChatContext(9L, 1L, 2L, 0, "practice")), clients,
                Mockito.mock(PendingActionService.class), Mockito.mock(KnowledgeService.class), mapper);

        var data = tools.getSectionPractice(11L, 9L).data();
        assertFalse(data.get(0).has("answer"));
        assertFalse(data.get(0).has("analysis"));
        Mockito.verify(clients).hasCourseAccess(7L, "req", 9L);
    }

    @Test
    void progressRejectsCourseWithoutAccess() {
        BusinessClients clients = Mockito.mock(BusinessClients.class);
        Mockito.when(clients.hasCourseAccess(7L, "req", 9L)).thenReturn(false);
        AgentTools tools = new AgentTools(new AgentRequestContext(7L, "req", "conv", "msg",
                new ChatContext(9L, 1L, 2L, 0, "video")), clients,
                Mockito.mock(PendingActionService.class), Mockito.mock(KnowledgeService.class), mapper);
        assertThrows(AgentException.class, () -> tools.getLearningProgress(9L));
        Mockito.verify(clients, Mockito.never()).learningProgress(anyLong(), anyString(), anyLong());
    }
}
