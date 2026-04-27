package com.poshhouse.backend.dto.user;

import java.math.BigDecimal;

public record UserUpdateRequest(BigDecimal rentShare, Boolean active) {
}
