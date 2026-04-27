package com.poshhouse.backend.util;

import com.poshhouse.backend.entity.ExpenseSplit;
import com.poshhouse.backend.entity.HouseExpense;
import com.poshhouse.backend.entity.SplitType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public final class SplitCalculator {

    private SplitCalculator() {
    }

    public static List<BigDecimal> calculateEqualShares(BigDecimal totalAmount, int peopleCount) {
        if (peopleCount <= 0) {
            return List.of();
        }

        long totalCents = MoneyUtils.scale(totalAmount).movePointRight(2).longValueExact();
        long baseShare = totalCents / peopleCount;
        long remainder = totalCents % peopleCount;

        List<BigDecimal> shares = new ArrayList<>(peopleCount);
        for (int index = 0; index < peopleCount; index++) {
            long cents = baseShare + (index < remainder ? 1 : 0);
            shares.add(BigDecimal.valueOf(cents, 2));
        }

        return shares;
    }

    public static BigDecimal calculateComputedShare(HouseExpense expense, ExpenseSplit split) {
        return switch (expense.getSplitType()) {
            case EQUAL, FIXED -> MoneyUtils.scale(split.getShareValue());
            case PERCENT -> MoneyUtils.scale(
                expense.getTotalAmount().multiply(split.getShareValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
            );
            case RATIO -> {
                BigDecimal totalRatio = expense.getSplits().stream()
                    .map(ExpenseSplit::getShareValue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                if (totalRatio.compareTo(BigDecimal.ZERO) == 0) {
                    yield BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                }
                yield MoneyUtils.scale(
                    expense.getTotalAmount().multiply(split.getShareValue()).divide(totalRatio, 2, RoundingMode.HALF_UP)
                );
            }
        };
    }

    public static boolean requiresSplitEntries(SplitType splitType) {
        return splitType != SplitType.EQUAL;
    }
}
