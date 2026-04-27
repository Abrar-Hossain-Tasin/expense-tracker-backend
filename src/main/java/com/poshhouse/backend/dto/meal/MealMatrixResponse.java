package com.poshhouse.backend.dto.meal;

import com.poshhouse.backend.dto.user.UserResponse;
import java.util.List;

public record MealMatrixResponse(
    String month,
    Integer daysInMonth,
    List<UserResponse> users,
    List<MealEntryResponse> entries
) {
}
