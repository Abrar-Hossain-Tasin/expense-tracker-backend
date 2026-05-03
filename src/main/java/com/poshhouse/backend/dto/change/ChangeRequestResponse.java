package com.poshhouse.backend.dto.change;

import com.poshhouse.backend.entity.ChangeRequestAction;
import com.poshhouse.backend.entity.ChangeRequestStatus;
import com.poshhouse.backend.entity.ChangeRequestTarget;
import java.time.LocalDateTime;
import java.util.List;

public record ChangeRequestResponse(
    Long id,
    String title,
    ChangeRequestTarget targetType,
    ChangeRequestAction actionType,
    ChangeRequestStatus status,
    String monthKey,
    Long requestedById,
    String requestedByName,
    Long reviewedById,
    String reviewedByName,
    LocalDateTime requestedAt,
    LocalDateTime reviewedAt,
    String reviewNote,
    List<String> details
) {
}
