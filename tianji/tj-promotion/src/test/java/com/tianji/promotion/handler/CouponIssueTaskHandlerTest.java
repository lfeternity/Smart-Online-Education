package com.tianji.promotion.handler;

import com.tianji.promotion.service.ICouponService;
import com.xxl.job.core.context.XxlJobContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CouponIssueTaskHandlerTest {

    @AfterEach
    void clearJobContext() {
        XxlJobContext.setXxlJobContext(null);
    }

    @Test
    void shouldPassZeroBasedShardIndexToBothCouponTasks() {
        ICouponService couponService = mock(ICouponService.class);
        CouponIssueTaskHandler handler = new CouponIssueTaskHandler(couponService);
        XxlJobContext.setXxlJobContext(new XxlJobContext(1L, "20", null, 2, 4));

        handler.handleCouponIssueJob();

        verify(couponService).issueCouponByPage(2, 20);
        verify(couponService).finishCouponByPage(2, 20);
    }
}
