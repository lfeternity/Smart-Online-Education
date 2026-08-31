package com.tianji.promotion.handler;

import com.tianji.promotion.service.ICouponService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CouponIssueTaskHandler {

    private final ICouponService couponService;

    @XxlJob("couponIssueJobHandler")
    public void handleCouponIssueJob(){
        // 1.获取分片信息，作为页码，每页最多查询 20条
        int index = XxlJobHelper.getShardIndex();
        int size = Integer.parseInt(XxlJobHelper.getJobParam());
        // 2.处理到期开始和到期结束的优惠券
        couponService.issueCouponByPage(index, size);
        couponService.finishCouponByPage(index, size);
    }
}
