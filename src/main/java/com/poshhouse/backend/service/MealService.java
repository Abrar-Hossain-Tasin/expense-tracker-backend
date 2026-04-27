package com.poshhouse.backend.service;

import com.poshhouse.backend.dto.meal.BulkMealUpdateRequest;
import com.poshhouse.backend.dto.meal.MealEntryRequest;
import com.poshhouse.backend.dto.meal.MealEntryResponse;
import com.poshhouse.backend.dto.meal.MealMatrixResponse;
import com.poshhouse.backend.entity.MealEntry;
import com.poshhouse.backend.entity.User;
import com.poshhouse.backend.exception.ResourceNotFoundException;
import com.poshhouse.backend.repository.MealEntryRepository;
import com.poshhouse.backend.repository.UserRepository;
import com.poshhouse.backend.util.MonthWindow;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MealService {

    private final MealEntryRepository mealEntryRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public MealMatrixResponse getMatrix(String month) {
        MonthWindow monthWindow = MonthWindow.from(month);
        List<User> users = userRepository.findAllByOrderByUsernameAsc();
        List<MealEntryResponse> entries = mealEntryRepository.findAllWithUsersByDateBetween(
                monthWindow.startDate(),
                monthWindow.endDate()
            )
            .stream()
            .map(MealService::toResponse)
            .toList();

        return new MealMatrixResponse(
            monthWindow.monthKey(),
            monthWindow.endDate().getDayOfMonth(),
            users.stream().map(UserService::toResponse).toList(),
            entries
        );
    }

    @Transactional
    public MealEntryResponse upsertEntry(MealEntryRequest request) {
        User user = userRepository.findById(request.userId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        MealEntry entry = mealEntryRepository.findByUserIdAndDate(request.userId(), request.date())
            .orElseGet(() -> MealEntry.builder().user(user).date(request.date()).build());

        entry.setMealsCount(request.mealsCount());
        return toResponse(mealEntryRepository.save(entry));
    }

    @Transactional
    public List<MealEntryResponse> bulkSetForDate(BulkMealUpdateRequest request) {
        List<User> activeUsers = userRepository.findAllByActiveOrderByUsernameAsc(true);
        List<MealEntry> updatedEntries = new ArrayList<>(activeUsers.size());

        for (User user : activeUsers) {
            MealEntry entry = mealEntryRepository.findByUserIdAndDate(user.getId(), request.date())
                .orElseGet(() -> MealEntry.builder().user(user).date(request.date()).build());
            entry.setMealsCount(request.mealsCount());
            updatedEntries.add(entry);
        }

        return mealEntryRepository.saveAll(updatedEntries).stream().map(MealService::toResponse).toList();
    }

    public static MealEntryResponse toResponse(MealEntry entry) {
        return new MealEntryResponse(entry.getId(), entry.getUser().getId(), entry.getDate(), entry.getMealsCount());
    }
}
