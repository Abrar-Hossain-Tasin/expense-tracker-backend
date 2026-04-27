package com.poshhouse.backend.service;

import com.poshhouse.backend.dto.user.UserResponse;
import com.poshhouse.backend.dto.user.UserUpdateRequest;
import com.poshhouse.backend.entity.User;
import com.poshhouse.backend.exception.ResourceNotFoundException;
import com.poshhouse.backend.repository.UserRepository;
import com.poshhouse.backend.util.MoneyUtils;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers(Boolean activeOnly) {
        List<User> users = Boolean.TRUE.equals(activeOnly)
            ? userRepository.findAllByActiveOrderByUsernameAsc(true)
            : userRepository.findAllByOrderByUsernameAsc();
        return users.stream().map(UserService::toResponse).toList();
    }

    @Transactional
    public UserResponse updateUser(Long userId, UserUpdateRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        if (request.rentShare() != null) {
            user.setRentShare(MoneyUtils.scale(request.rentShare()));
        }
        if (request.active() != null) {
            user.setActive(request.active());
        }

        return toResponse(userRepository.save(user));
    }

    public static UserResponse toResponse(User user) {
        return new UserResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            MoneyUtils.scale(user.getRentShare()),
            Boolean.TRUE.equals(user.getActive()),
            user.getRole().getName(),
            user.getCreatedAt()
        );
    }
}
