package com.poshhouse.backend.service;

import com.poshhouse.backend.dto.settlement.SettlementResponse;
import com.poshhouse.backend.dto.settlement.SettlementSummaryDto;
import com.poshhouse.backend.dto.settlement.SettlementUserDetailDto;
import com.poshhouse.backend.entity.ExpenseSplit;
import com.poshhouse.backend.entity.GroceryPurchase;
import com.poshhouse.backend.entity.HouseExpense;
import com.poshhouse.backend.entity.MealEntry;
import com.poshhouse.backend.entity.User;
import com.poshhouse.backend.repository.GroceryPurchaseRepository;
import com.poshhouse.backend.repository.HouseExpenseRepository;
import com.poshhouse.backend.repository.MealEntryRepository;
import com.poshhouse.backend.repository.UserRepository;
import com.poshhouse.backend.util.MonthWindow;
import com.poshhouse.backend.util.MoneyUtils;
import com.poshhouse.backend.util.SplitCalculator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SettlementService {

    private final UserRepository userRepository;
    private final GroceryPurchaseRepository groceryPurchaseRepository;
    private final MealEntryRepository mealEntryRepository;
    private final HouseExpenseRepository houseExpenseRepository;

    @Transactional(readOnly = true)
    public SettlementResponse getSettlement(String month) {
        MonthWindow monthWindow = MonthWindow.from(month);
        List<User> users = userRepository.findAllByOrderByUsernameAsc();
        List<GroceryPurchase> groceries = groceryPurchaseRepository.findAllByMonthKeyOrderByPurchaseDateDesc(monthWindow.monthKey());
        List<MealEntry> meals = mealEntryRepository.findAllWithUsersByDateBetween(monthWindow.startDate(), monthWindow.endDate());
        List<HouseExpense> expenses = houseExpenseRepository.findAllByExpenseDateBetweenOrderByExpenseDateDesc(
            monthWindow.startDate(),
            monthWindow.endDate()
        );

        BigDecimal totalGrocery = groceries.stream()
            .map(GroceryPurchase::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        int totalMeals = meals.stream().mapToInt(MealEntry::getMealsCount).sum();
        BigDecimal totalHouseExpenses = expenses.stream()
            .map(HouseExpense::getTotalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal costPerMeal = totalMeals == 0
            ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
            : totalGrocery.divide(BigDecimal.valueOf(totalMeals), 2, RoundingMode.HALF_UP);

        Map<Long, BigDecimal> groceriesByUser = new HashMap<>();
        for (GroceryPurchase grocery : groceries) {
            groceriesByUser.merge(grocery.getPayer().getId(), MoneyUtils.scale(grocery.getAmount()), BigDecimal::add);
        }

        Map<Long, Integer> mealsByUser = new HashMap<>();
        for (MealEntry mealEntry : meals) {
            mealsByUser.merge(mealEntry.getUser().getId(), mealEntry.getMealsCount(), Integer::sum);
        }

        Map<Long, BigDecimal> houseShareByUser = new HashMap<>();
        for (HouseExpense expense : expenses) {
            for (ExpenseSplit split : expense.getSplits()) {
                houseShareByUser.merge(
                    split.getUser().getId(),
                    SplitCalculator.calculateComputedShare(expense, split),
                    BigDecimal::add
                );
            }
        }

        List<SettlementUserDetailDto> userDetails = users.stream().map(user -> {
            int userMeals = mealsByUser.getOrDefault(user.getId(), 0);
            BigDecimal groceryPaid = MoneyUtils.scale(groceriesByUser.getOrDefault(user.getId(), BigDecimal.ZERO));
            BigDecimal foodBalance = MoneyUtils.scale(costPerMeal.multiply(BigDecimal.valueOf(userMeals)).subtract(groceryPaid));
            BigDecimal houseShare = MoneyUtils.scale(houseShareByUser.getOrDefault(user.getId(), BigDecimal.ZERO));
            BigDecimal totalOwed = MoneyUtils.scale(foodBalance.add(houseShare));

            return new SettlementUserDetailDto(
                user.getId(),
                user.getUsername(),
                Boolean.TRUE.equals(user.getActive()),
                MoneyUtils.scale(user.getRentShare()),
                userMeals,
                groceryPaid,
                foodBalance,
                houseShare,
                totalOwed
            );
        }).toList();

        SettlementSummaryDto summary = new SettlementSummaryDto(
            MoneyUtils.scale(totalGrocery),
            totalMeals,
            costPerMeal,
            MoneyUtils.scale(totalHouseExpenses),
            (int) users.stream().filter(user -> Boolean.TRUE.equals(user.getActive())).count()
        );

        return new SettlementResponse(monthWindow.monthKey(), summary, userDetails);
    }
}
