package com.tianji.remark.task;

import com.tianji.remark.config.LikedProperties;
import com.tianji.remark.service.ILikedRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class LikedTimesCheckTask {

    private final ILikedRecordService recordService;
    private final LikedProperties properties;

    @Scheduled(fixedDelay = 20000)
    public void checkLikedTimes(){
        for (String bizType : properties.getBizTypes()) {
            recordService.readLikedTimesAndSendMessage(bizType, properties.getMaxBizSize());
        }
    }
}
