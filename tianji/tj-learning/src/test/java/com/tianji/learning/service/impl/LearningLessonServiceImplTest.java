package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.client.course.CatalogueClient;
import com.tianji.api.client.course.CourseClient;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.vo.LearningPlanPageVO;
import com.tianji.learning.mapper.LearningLessonMapper;
import com.tianji.learning.mapper.LearningRecordMapper;
import com.tianji.learning.mapper.PointsRecordMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LearningLessonServiceImplTest {

    @AfterEach
    void tearDown() {
        UserContext.removeUser();
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void shouldIncludeCurrentWeekPointsWhenPlanPageIsEmpty() {
        CourseClient courseClient = mock(CourseClient.class);
        CatalogueClient catalogueClient = mock(CatalogueClient.class);
        LearningRecordMapper recordMapper = mock(LearningRecordMapper.class);
        PointsRecordMapper pointsRecordMapper = mock(PointsRecordMapper.class);
        LearningLessonMapper lessonMapper = mock(LearningLessonMapper.class);
        LearningLessonServiceImpl service = new LearningLessonServiceImpl(
                courseClient, catalogueClient, recordMapper, pointsRecordMapper);
        ReflectionTestUtils.setField(service, "baseMapper", lessonMapper);

        UserContext.setUser(101L);
        when(recordMapper.selectCount(any())).thenReturn(3);
        when(lessonMapper.queryTotalPlan(101L)).thenReturn(8);
        when(pointsRecordMapper.queryUserPointsByDateRange(eq(101L), any(), any())).thenReturn(42);
        when(lessonMapper.selectPage(any(Page.class), any(Wrapper.class))).thenAnswer(invocation -> {
            Page<LearningLesson> page = invocation.getArgument(0);
            page.setRecords(Collections.emptyList());
            page.setTotal(0);
            return page;
        });

        LearningPlanPageVO result = service.queryMyPlans(new PageQuery());

        assertEquals(3, result.getWeekFinished());
        assertEquals(8, result.getWeekTotalPlan());
        assertEquals(42, result.getWeekPoints());
        assertEquals(0L, result.getTotal());
    }
}
