package com.poshhouse.backend.util;

import com.poshhouse.backend.exception.BadRequestException;
import java.time.YearMonth;

public record MonthWindow(String monthKey, java.time.LocalDate startDate, java.time.LocalDate endDate) {

    public static MonthWindow from(String month) {
        try {
            YearMonth yearMonth = month == null || month.isBlank() ? YearMonth.now() : YearMonth.parse(month);
            return new MonthWindow(yearMonth.toString(), yearMonth.atDay(1), yearMonth.atEndOfMonth());
        } catch (RuntimeException exception) {
            throw new BadRequestException("Month must use the YYYY-MM format.");
        }
    }
}
