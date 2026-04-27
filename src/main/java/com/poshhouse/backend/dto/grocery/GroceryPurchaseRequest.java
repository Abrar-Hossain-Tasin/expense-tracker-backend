package com.poshhouse.backend.dto.grocery;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record GroceryPurchaseRequest(
    Long payerId,
    @NotNull(message = "Amount is required.")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero.")
    BigDecimal amount,
    @NotNull(message = "Purchase date is required.")
    LocalDate purchaseDate,
    String description
) {
}
