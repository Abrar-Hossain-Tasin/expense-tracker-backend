package com.poshhouse.backend.dto.settlement;

import java.math.BigDecimal;

public record SettlementSummaryDto(
    BigDecimal totalGrocery,
    Integer totalMeals,
    BigDecimal costPerMeal,
    BigDecimal totalHouseExpenses,
    Integer activeUsers
) {
}
