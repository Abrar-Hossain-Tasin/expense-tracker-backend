package com.poshhouse.backend.dto.expense;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ExpenseSplitRequest(
    @NotNull(message = "User is required.")
    Long userId,
    @NotNull(message = "Share value is required.")
    BigDecimal shareValue
) {
}
