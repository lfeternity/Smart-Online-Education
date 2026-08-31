package com.tianji.remark.constants;

/**
 * Default values kept for backwards compatibility. Runtime code reads these
 * values from {@code tj.remark.likes} so Nacos can refresh them dynamically.
 */
public interface RedisConstants {
    String DEFAULT_LIKES_BIZ_KEY_PREFIX = "likes:set:biz:";
    String DEFAULT_LIKES_TIMES_KEY_PREFIX = "likes:times:type:";
    String DEFAULT_LIKES_USER_BIZ_KEY_PREFIX = "likes:set:user:";
}
