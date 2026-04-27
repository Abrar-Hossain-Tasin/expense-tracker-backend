package com.poshhouse.backend.dto.meal;

import java.time.LocalDate;

public record MealEntryResponse(Long id, Long userId, LocalDate date, Integer mealsCount) {
}
