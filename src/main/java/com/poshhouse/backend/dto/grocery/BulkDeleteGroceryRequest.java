package com.poshhouse.backend.dto.grocery;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record BulkDeleteGroceryRequest(
    @NotEmpty(message = "Select at least one grocery record to remove.")
    List<Long> purchaseIds
) {
}
