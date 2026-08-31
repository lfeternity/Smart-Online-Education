package com.tianji.remark.config;

import com.tianji.remark.constants.RedisConstants;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Runtime settings for the like counter. The bean is rebound when Nacos
 * publishes a refreshed configuration, so scheduled jobs and Redis key
 * generation immediately use the new values.
 */
@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "tj.remark.likes")
public class LikedProperties {

    private List<String> bizTypes = new ArrayList<>(Arrays.asList("QA", "NOTE", "COURSE"));
    private int maxBizSize = 30;
    private String bizKeyPrefix = RedisConstants.DEFAULT_LIKES_BIZ_KEY_PREFIX;
    private String timesKeyPrefix = RedisConstants.DEFAULT_LIKES_TIMES_KEY_PREFIX;
    private String userBizKeyPrefix = RedisConstants.DEFAULT_LIKES_USER_BIZ_KEY_PREFIX;
}
