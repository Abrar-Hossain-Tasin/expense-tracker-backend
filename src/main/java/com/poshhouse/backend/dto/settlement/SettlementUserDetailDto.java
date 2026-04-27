package com.poshhouse.backend.dto.settlement;

import java.math.BigDecimal;

public record SettlementUserDetailDto(
    Long userId,
    String username,
    boolean active,
    BigDecimal rentShare,
    Integer mealsCount,
    BigDecimal groceryPaid,
    BigDecimal foodBalance,
    BigDecimal houseShare,
    BigDecimal totalOwed
) {
}
