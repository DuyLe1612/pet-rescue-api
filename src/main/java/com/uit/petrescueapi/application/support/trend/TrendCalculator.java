package com.uit.petrescueapi.application.support.trend;

public class TrendCalculator {

    public static double calculatePercentageChange(long current, long previous) {
        if (previous == 0) {
            return current > 0 ? 100.0 : 0.0;
        }
        double change = ((double) (current - previous) / previous) * 100.0;
        return Math.round(change * 10.0) / 10.0; // Round to 1 decimal place
    }

    public static String calculateTrend(long current, long previous) {
        if (current > previous) {
            return "up";
        } else if (current < previous) {
            return "down";
        }
        return "neutral";
    }
}
