package com.poshhouse.backend.service;

import com.poshhouse.backend.dto.expense.BulkDeleteExpenseRequest;
import com.poshhouse.backend.dto.expense.ExpenseRequest;
import com.poshhouse.backend.dto.expense.ExpenseResponse;
import com.poshhouse.backend.dto.expense.ExpenseSplitRequest;
import com.poshhouse.backend.dto.expense.ExpenseSplitResponse;
import com.poshhouse.backend.entity.ExpenseSplit;
import com.poshhouse.backend.entity.HouseExpense;
import com.poshhouse.backend.entity.SplitType;
import com.poshhouse.backend.entity.User;
import com.poshhouse.backend.exception.BadRequestException;
import com.poshhouse.backend.exception.ResourceNotFoundException;
import com.poshhouse.backend.repository.HouseExpenseRepository;
import com.poshhouse.backend.repository.UserRepository;
import com.poshhouse.backend.security.UserPrincipal;
import com.poshhouse.backend.util.MonthWindow;
import com.poshhouse.backend.util.MoneyUtils;
import com.poshhouse.backend.util.SplitCalculator;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final HouseExpenseRepository houseExpenseRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<ExpenseResponse> listExpenses(String month) {
        MonthWindow monthWindow = MonthWindow.from(month);
        return houseExpenseRepository.findAllByExpenseDateBetweenOrderByExpenseDateDesc(
                monthWindow.startDate(),
                monthWindow.endDate()
            )
            .stream()
            .map(ExpenseService::toResponse)
            .toList();
    }

    @Transactional
    public ExpenseResponse createExpense(ExpenseRequest request, UserPrincipal currentUser) {
        User creator = userRepository.findById(currentUser.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Creator not found."));

        List<User> splitUsers = resolveSplitUsers(request);
        HouseExpense expense = HouseExpense.builder()
            .title(request.title().trim())
            .category(request.category().trim())
            .totalAmount(MoneyUtils.scale(request.totalAmount()))
            .expenseDate(request.expenseDate())
            .splitType(request.splitType())
            .recurring(Boolean.TRUE.equals(request.recurring()))
            .createdBy(creator)
            .build();

        buildSplits(expense, request, splitUsers);
        return toResponse(houseExpenseRepository.save(expense));
    }

    @Transactional
    public void bulkDelete(BulkDeleteExpenseRequest request) {
        houseExpenseRepository.deleteAllById(request.expenseIds());
    }

    public static ExpenseResponse toResponse(HouseExpense expense) {
        return new ExpenseResponse(
            expense.getId(),
            expense.getTitle(),
            expense.getCategory(),
            MoneyUtils.scale(expense.getTotalAmount()),
            expense.getExpenseDate(),
            expense.getSplitType(),
            Boolean.TRUE.equals(expense.getRecurring()),
            expense.getCreatedBy().getId(),
            expense.getCreatedBy().getUsername(),
            expense.getSplits().stream()
                .map(split -> new ExpenseSplitResponse(
                    split.getId(),
                    split.getUser().getId(),
                    split.getUser().getUsername(),
                    MoneyUtils.scale(split.getShareValue()),
                    SplitCalculator.calculateComputedShare(expense, split)
                ))
                .toList()
        );
    }

    private List<User> resolveSplitUsers(ExpenseRequest request) {
        if (request.splitType() == SplitType.EQUAL && (request.splits() == null || request.splits().isEmpty())) {
            List<User> activeUsers = userRepository.findAllByActiveOrderByUsernameAsc(true);
            if (activeUsers.isEmpty()) {
                throw new BadRequestException("No active users are available for an equal split.");
            }
            return activeUsers;
        }

        if (request.splits() == null || request.splits().isEmpty()) {
            throw new BadRequestException("Split values are required for this split type.");
        }

        LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>();
        List<Long> orderedIds = new ArrayList<>();
        for (ExpenseSplitRequest split : request.splits()) {
            if (!uniqueIds.add(split.userId())) {
                throw new BadRequestException("Each member can only appear once in a split.");
            }
            orderedIds.add(split.userId());
        }

        Map<Long, User> usersById = userRepository.findAllById(uniqueIds).stream()
            .collect(java.util.stream.Collectors.toMap(User::getId, user -> user));

        if (usersById.size() != uniqueIds.size()) {
            throw new ResourceNotFoundException("One or more split members could not be found.");
        }

        return orderedIds.stream().map(usersById::get).toList();
    }

    private void buildSplits(HouseExpense expense, ExpenseRequest request, List<User> splitUsers) {
        BigDecimal totalAmount = MoneyUtils.scale(request.totalAmount());

        switch (request.splitType()) {
            case EQUAL -> {
                List<BigDecimal> shares = SplitCalculator.calculateEqualShares(totalAmount, splitUsers.size());
                for (int index = 0; index < splitUsers.size(); index++) {
                    expense.addSplit(ExpenseSplit.builder().user(splitUsers.get(index)).shareValue(shares.get(index)).build());
                }
            }
            case PERCENT -> {
                Map<Long, BigDecimal> shareValues = requestShareValues(request.splits());
                BigDecimal totalPercent = shareValues.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
                if (!MoneyUtils.sameAmount(totalPercent, BigDecimal.valueOf(100))) {
                    throw new BadRequestException("Percent split must total 100%.");
                }
                splitUsers.forEach(user -> {
                    BigDecimal share = shareValues.get(user.getId());
                    if (share.compareTo(BigDecimal.ZERO) < 0) {
                        throw new BadRequestException("Percent split values cannot be negative.");
                    }
                    expense.addSplit(ExpenseSplit.builder().user(user).shareValue(MoneyUtils.scale(share)).build());
                });
            }
            case FIXED -> {
                Map<Long, BigDecimal> shareValues = requestShareValues(request.splits());
                BigDecimal fixedTotal = shareValues.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
                if (!MoneyUtils.sameAmount(fixedTotal, totalAmount)) {
                    throw new BadRequestException("Fixed split must match the total expense amount.");
                }
                splitUsers.forEach(user -> {
                    BigDecimal share = MoneyUtils.scale(shareValues.get(user.getId()));
                    if (share.compareTo(BigDecimal.ZERO) < 0) {
                        throw new BadRequestException("Fixed split values cannot be negative.");
                    }
                    expense.addSplit(ExpenseSplit.builder().user(user).shareValue(share).build());
                });
            }
            case RATIO -> {
                Map<Long, BigDecimal> shareValues = requestShareValues(request.splits());
                BigDecimal ratioTotal = BigDecimal.ZERO;
                for (User user : splitUsers) {
                    BigDecimal ratio = shareValues.get(user.getId());
                    if (ratio.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new BadRequestException("Ratio split values must be positive.");
                    }
                    if (ratio.stripTrailingZeros().scale() > 0) {
                        throw new BadRequestException("Ratio split values must be whole numbers.");
                    }
                    ratioTotal = ratioTotal.add(ratio);
                    expense.addSplit(ExpenseSplit.builder().user(user).shareValue(MoneyUtils.scale(ratio)).build());
                }
                if (ratioTotal.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BadRequestException("Ratio split must include at least one positive value.");
                }
            }
        }
    }

    private Map<Long, BigDecimal> requestShareValues(List<ExpenseSplitRequest> splits) {
        Map<Long, BigDecimal> values = new LinkedHashMap<>();
        for (ExpenseSplitRequest split : splits) {
            values.put(split.userId(), split.shareValue());
        }
        return values;
    }
}
