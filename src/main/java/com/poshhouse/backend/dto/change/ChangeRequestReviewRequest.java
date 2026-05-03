package com.poshhouse.backend.dto.change;

import jakarta.validation.constraints.NotNull;

public record ChangeRequestReviewRequest(
    @NotNull(message = "Decision is required.")
    Boolean approved,
    String reviewNote
) {
}
