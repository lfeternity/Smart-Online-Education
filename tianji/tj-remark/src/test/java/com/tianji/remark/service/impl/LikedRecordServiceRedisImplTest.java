package com.tianji.remark.service.impl;

import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.utils.UserContext;
import com.tianji.remark.config.LikedProperties;
import com.tianji.remark.domain.dto.LikeRecordFormDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LikedRecordServiceRedisImplTest {

    private SetOperations<String, String> setOperations;
    private ZSetOperations<String, String> zSetOperations;
    private LikedRecordServiceRedisImpl service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        setOperations = mock(SetOperations.class);
        zSetOperations = mock(ZSetOperations.class);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

        LikedProperties properties = new LikedProperties();
        service = new LikedRecordServiceRedisImpl(
                mock(RabbitMqHelper.class), redisTemplate, properties);
        UserContext.setUser(7L);
    }

    @AfterEach
    void tearDown() {
        UserContext.removeUser();
    }

    @Test
    void shouldMaintainUserBusinessIndexWhenCourseIsCollected() {
        LikeRecordFormDTO form = new LikeRecordFormDTO();
        form.setBizId(101L);
        form.setBizType("COURSE");
        form.setLiked(true);
        when(setOperations.add("likes:set:biz:101", "7")).thenReturn(1L);
        when(setOperations.size("likes:set:biz:101")).thenReturn(1L);

        service.addLikeRecord(form);

        verify(setOperations).add("likes:set:user:COURSE:7", "101");
        verify(zSetOperations).add("likes:times:type:COURSE", "101", 1D);
    }

    @Test
    void shouldReturnAllCollectedIdsForCurrentUserAndBusinessType() {
        when(setOperations.members("likes:set:user:COURSE:7"))
                .thenReturn(Set.of("101", "102"));

        Set<Long> result = service.queryLikedBizIds("COURSE");

        assertEquals(Set.of(101L, 102L), result);
    }

    @Test
    void shouldRemoveCourseFromUserBusinessIndexWhenCollectionIsCancelled() {
        LikeRecordFormDTO form = new LikeRecordFormDTO();
        form.setBizId(101L);
        form.setBizType("COURSE");
        form.setLiked(false);
        when(setOperations.remove("likes:set:biz:101", "7")).thenReturn(1L);
        when(setOperations.size("likes:set:biz:101")).thenReturn(0L);

        service.addLikeRecord(form);

        verify(setOperations).remove("likes:set:user:COURSE:7", "101");
        verify(zSetOperations).add("likes:times:type:COURSE", "101", 0D);
    }
}
