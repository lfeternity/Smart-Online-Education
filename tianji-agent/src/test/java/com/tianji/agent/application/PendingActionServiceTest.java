package com.tianji.agent.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tianji.agent.api.AgentException;
import com.tianji.agent.client.BusinessClients;
import com.tianji.agent.domain.PendingActionEntity;
import com.tianji.agent.domain.PendingActionStatus;
import com.tianji.agent.domain.PendingActionType;
import com.tianji.agent.persistence.PendingActionRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

class PendingActionServiceTest {
    @Test
    void expiredActionCannotExecute() throws Exception {
        PendingActionRepository repository = Mockito.mock(PendingActionRepository.class);
        BusinessClients clients = Mockito.mock(BusinessClients.class);
        PendingActionEntity entity = new PendingActionEntity(); entity.setId("a"); entity.setUserId(7L);
        entity.setStatus(PendingActionStatus.PENDING); entity.setExpireTime(LocalDateTime.now().minusMinutes(1));
        entity.setActionType(PendingActionType.CREATE_NOTE); entity.setPayload("{\"courseId\":1,\"chapterId\":2,\"sectionId\":3,\"content\":\"x\",\"noteMoment\":0}");
        Mockito.when(repository.findByIdAndUserId("a", 7L)).thenReturn(Optional.of(entity));
        PendingActionService service = new PendingActionService(repository, clients, new ObjectMapper());
        assertThrows(AgentException.class, () -> service.confirm(7L, "req", "a"));
        Mockito.verify(clients, Mockito.never()).createNote(any(), any(), any());
        Mockito.verify(repository).save(entity);
    }

    @Test
    void confirmationExecutesExactlyOnce() throws Exception {
        PendingActionRepository repository = Mockito.mock(PendingActionRepository.class);
        BusinessClients clients = Mockito.mock(BusinessClients.class);
        PendingActionEntity entity = new PendingActionEntity(); entity.setId("a"); entity.setUserId(7L);
        entity.setStatus(PendingActionStatus.PENDING); entity.setExpireTime(LocalDateTime.now().plusMinutes(5));
        entity.setActionType(PendingActionType.CREATE_NOTE);
        entity.setPayload("{\"courseId\":1,\"chapterId\":2,\"sectionId\":3,\"content\":\"x\",\"noteMoment\":0}");
        Mockito.when(repository.findByIdAndUserId("a", 7L)).thenReturn(Optional.of(entity));
        Mockito.when(repository.save(entity)).thenReturn(entity);
        PendingActionService service = new PendingActionService(repository, clients, new ObjectMapper());

        assertEquals("CONFIRMED", service.confirm(7L, "req", "a").status());
        assertEquals("CONFIRMED", service.confirm(7L, "req", "a").status());

        Mockito.verify(clients, Mockito.times(1)).createNote(Mockito.eq(7L), Mockito.eq("req"), any());
    }
}
