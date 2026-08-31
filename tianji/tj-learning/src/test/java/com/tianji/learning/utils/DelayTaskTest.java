package com.tianji.learning.utils;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.DelayQueue;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
class DelayTaskTest {
    @Test
    void testDelayQueue() throws InterruptedException {
        // 1.初始化延迟队列
        DelayQueue<DelayTask<String>> queue = new DelayQueue<>();
        // 2.向队列中添加延迟执行的任务
        log.info("开始初始化延迟任务。。。。");
        queue.add(new DelayTask<>("延迟任务3", Duration.ofMillis(300)));
        queue.add(new DelayTask<>("延迟任务1", Duration.ofMillis(100)));
        queue.add(new DelayTask<>("延迟任务2", Duration.ofMillis(200)));
        // 3.尝试执行任务
        List<String> executionOrder = new ArrayList<>(queue.size());
        while (!queue.isEmpty()) {
            DelayTask<String> task = queue.take();
            executionOrder.add(task.getData());
            log.info("开始执行延迟任务：{}", task.getData());
        }
        assertEquals(List.of("延迟任务1", "延迟任务2", "延迟任务3"), executionOrder);
    }
}
