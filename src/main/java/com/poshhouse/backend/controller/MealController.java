package com.poshhouse.backend.controller;

import com.poshhouse.backend.dto.change.ChangeRequestResponse;
import com.poshhouse.backend.dto.meal.BulkMealUpdateRequest;
import com.poshhouse.backend.dto.meal.MealEntryRequest;
import com.poshhouse.backend.dto.meal.MealEntryResponse;
import com.poshhouse.backend.dto.meal.MealMatrixResponse;
import com.poshhouse.backend.security.UserPrincipal;
import com.poshhouse.backend.service.ChangeRequestService;
import com.poshhouse.backend.service.MealService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/meals")
@RequiredArgsConstructor
public class MealController {

    private final MealService mealService;
    private final ChangeRequestService changeRequestService;

    @GetMapping
    public MealMatrixResponse getMatrix(@RequestParam(required = false) String month) {
        return mealService.getMatrix(month);
    }

    @PutMapping
    public ChangeRequestResponse upsertEntry(
        @Valid @RequestBody MealEntryRequest request,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return changeRequestService.submitMealChangeRequest(request, principal);
    }

    @PostMapping("/bulk")
    public ChangeRequestResponse bulkSetForDate(
        @Valid @RequestBody BulkMealUpdateRequest request,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return changeRequestService.submitBulkMealChangeRequest(request, principal);
    }
}
