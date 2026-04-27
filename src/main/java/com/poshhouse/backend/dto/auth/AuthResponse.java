package com.poshhouse.backend.dto.auth;

import com.poshhouse.backend.dto.user.UserResponse;

public record AuthResponse(String accessToken, String refreshToken, UserResponse user) {
}
