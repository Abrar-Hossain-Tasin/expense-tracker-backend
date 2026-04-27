package com.poshhouse.backend.dto.meal;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record BulkMealUpdateRequest(
    @NotNull(message = "Date is required.")
    LocalDate date,
    @NotNull(message = "Meals count is required.")
    @Min(value = 0, message = "Meals count cannot be negative.")
    Integer mealsCount
) {
}
