package com.poshhouse.backend.dto.expense;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record BulkDeleteExpenseRequest(
    @NotEmpty(message = "Select at least one expense to delete.")
    List<Long> expenseIds
) {
}
