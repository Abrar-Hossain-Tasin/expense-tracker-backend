package com.poshhouse.backend.controller;

import com.poshhouse.backend.dto.meal.BulkMealUpdateRequest;
import com.poshhouse.backend.dto.meal.MealEntryRequest;
import com.poshhouse.backend.dto.meal.MealEntryResponse;
import com.poshhouse.backend.dto.meal.MealMatrixResponse;
import com.poshhouse.backend.service.MealService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
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

    @GetMapping
    public MealMatrixResponse getMatrix(@RequestParam(required = false) String month) {
        return mealService.getMatrix(month);
    }

    @PutMapping
    public MealEntryResponse upsertEntry(@Valid @RequestBody MealEntryRequest request) {
        return mealService.upsertEntry(request);
    }

    @PostMapping("/bulk")
    public List<MealEntryResponse> bulkSetForDate(@Valid @RequestBody BulkMealUpdateRequest request) {
        return mealService.bulkSetForDate(request);
    }
}
