package com.poshhouse.backend.service;

import com.poshhouse.backend.dto.auth.AuthResponse;
import com.poshhouse.backend.dto.auth.LoginRequest;
import com.poshhouse.backend.dto.auth.RefreshTokenRequest;
import com.poshhouse.backend.dto.auth.SignupRequest;
import com.poshhouse.backend.dto.user.UserResponse;
import com.poshhouse.backend.entity.RefreshToken;
import com.poshhouse.backend.entity.Role;
import com.poshhouse.backend.entity.User;
import com.poshhouse.backend.exception.BadRequestException;
import com.poshhouse.backend.exception.ResourceNotFoundException;
import com.poshhouse.backend.repository.RefreshTokenRepository;
import com.poshhouse.backend.repository.RoleRepository;
import com.poshhouse.backend.repository.UserRepository;
import com.poshhouse.backend.security.JwtUtils;
import com.poshhouse.backend.security.UserPrincipal;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtils jwtUtils;

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        String username = request.username().trim();
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByUsername(username)) {
            throw new BadRequestException("Username is already taken.");
        }
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email is already in use.");
        }

        Role userRole = roleRepository.findByName("ROLE_USER")
            .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_USER").build()));

        User user = userRepository.save(User.builder()
            .username(username)
            .email(email)
            .passwordHash(passwordEncoder.encode(request.password()))
            .role(userRole)
            .build());

        return issueTokens(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String identifier = request.usernameOrEmail().trim();
        User user = userRepository.findByUsername(identifier)
            .or(() -> userRepository.findByEmail(identifier.toLowerCase(Locale.ROOT)))
            .orElseThrow(() -> new BadRequestException("Invalid credentials."));

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new BadRequestException("This member is inactive.");
        }

        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(user.getUsername(), request.password())
        );

        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken storedToken = refreshTokenRepository.findByToken(request.refreshToken())
            .orElseThrow(() -> new BadRequestException("Refresh token is invalid."));

        User user = storedToken.getUser();
        UserPrincipal principal = UserPrincipal.create(user);

        if (Boolean.TRUE.equals(storedToken.getRevoked())
            || !Boolean.TRUE.equals(user.getActive())
            || !jwtUtils.isRefreshTokenValid(storedToken.getToken(), principal)
            || storedToken.getExpiresAt().isBefore(java.time.LocalDateTime.now(java.time.ZoneOffset.UTC))) {
            refreshTokenRepository.deleteByUserId(user.getId());
            throw new BadRequestException("Refresh token has expired.");
        }

        return issueTokens(user);
    }

    @Transactional(readOnly = true)
    public UserResponse me(UserPrincipal principal) {
        User user = userRepository.findById(principal.getId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        return UserService.toResponse(user);
    }

    private AuthResponse issueTokens(User user) {
        UserPrincipal principal = UserPrincipal.create(user);
        String accessToken = jwtUtils.generateAccessToken(principal);
        String refreshToken = jwtUtils.generateRefreshToken(principal);

        refreshTokenRepository.deleteByUserId(user.getId());
        refreshTokenRepository.save(RefreshToken.builder()
            .token(refreshToken)
            .user(user)
            .expiresAt(jwtUtils.getExpirationDateTime(refreshToken))
            .build());

        return new AuthResponse(accessToken, refreshToken, UserService.toResponse(user));
    }
}
