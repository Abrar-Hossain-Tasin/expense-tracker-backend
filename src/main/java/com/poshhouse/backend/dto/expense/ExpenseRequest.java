package com.poshhouse.backend.dto.expense;

import com.poshhouse.backend.entity.SplitType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ExpenseRequest(
    @NotBlank(message = "Title is required.")
    String title,
    @NotBlank(message = "Category is required.")
    String category,
    @NotNull(message = "Total amount is required.")
    @DecimalMin(value = "0.01", message = "Total amount must be greater than zero.")
    BigDecimal totalAmount,
    @NotNull(message = "Expense date is required.")
    LocalDate expenseDate,
    @NotNull(message = "Split type is required.")
    SplitType splitType,
    Boolean recurring,
    @Valid
    List<ExpenseSplitRequest> splits
) {
}
