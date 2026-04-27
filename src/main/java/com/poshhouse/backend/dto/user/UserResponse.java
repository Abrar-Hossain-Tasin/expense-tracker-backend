package com.poshhouse.backend.dto.user;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UserResponse(
    Long id,
    String username,
    String email,
    BigDecimal rentShare,
    boolean active,
    String role,
    LocalDateTime createdAt
) {
}
