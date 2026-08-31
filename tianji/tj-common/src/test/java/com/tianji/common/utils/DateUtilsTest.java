package com.tianji.common.utils;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DateUtilsTest {

    @Test
    void shouldReturnMondayToSundayForWeekRange() {
        LocalDate wednesday = LocalDate.of(2026, 8, 26);

        assertEquals(LocalDateTime.of(2026, 8, 24, 0, 0),
                DateUtils.getWeekBeginTime(wednesday));
        assertEquals(LocalDateTime.of(LocalDate.of(2026, 8, 30), LocalTime.MAX),
                DateUtils.getWeekEndTime(wednesday));
    }
}
