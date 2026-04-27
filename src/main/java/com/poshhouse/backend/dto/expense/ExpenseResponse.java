package com.poshhouse.backend.dto.expense;

import com.poshhouse.backend.entity.SplitType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ExpenseResponse(
    Long id,
    String title,
    String category,
    BigDecimal totalAmount,
    LocalDate expenseDate,
    SplitType splitType,
    boolean recurring,
    Long createdById,
    String createdByName,
    List<ExpenseSplitResponse> splits
) {
}
