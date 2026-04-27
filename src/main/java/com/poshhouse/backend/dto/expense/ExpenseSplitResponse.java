package com.poshhouse.backend.dto.expense;

import java.math.BigDecimal;

public record ExpenseSplitResponse(
    Long id,
    Long userId,
    String username,
    BigDecimal shareValue,
    BigDecimal computedAmount
) {
}
