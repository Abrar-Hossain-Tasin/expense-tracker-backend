package com.poshhouse.backend.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MoneyUtils {

    private MoneyUtils() {
    }

    public static BigDecimal scale(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    public static boolean sameAmount(BigDecimal left, BigDecimal right) {
        return scale(left).compareTo(scale(right)) == 0;
    }
}
