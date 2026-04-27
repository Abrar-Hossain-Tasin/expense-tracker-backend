package com.poshhouse.backend.dto.grocery;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GroceryPurchaseResponse(
    Long id,
    Long payerId,
    String payerName,
    BigDecimal amount,
    LocalDate purchaseDate,
    String description
) {
}
