package com.uit.petrescueapi.domain.service;

import com.uit.petrescueapi.domain.valueobject.FollowupAnswers;
import com.uit.petrescueapi.domain.valueobject.FollowupSeverity;

import java.util.ArrayList;
import java.util.List;

/**
 * Fixed rules, not a model: easy to explain to an organization and never missing a case because a
 * model was wrong. Answers only ever raise work for the organization; they never change reputation.
 */
public final class FollowupRules {

    private FollowupRules() {
    }

    public record Assessment(FollowupSeverity severity, List<String> reasons) {
    }

    public static Assessment assess(FollowupAnswers a) {
        List<String> red = new ArrayList<>();
        List<String> yellow = new ArrayList<>();

        if (Boolean.FALSE.equals(a.stillHasPet())) {
            red.add("PET_GONE");
        }
        if (a.support() == FollowupAnswers.Support.CONSIDERING_RETURN) {
            red.add("CONSIDERING_RETURN");
        }
        if (a.incident() == FollowupAnswers.Incident.INJURY) {
            red.add("BITE_INJURY");
        }
        if (a.wellbeing() == FollowupAnswers.Wellbeing.ALARMING) {
            red.add("PET_HEALTH");
        }

        if (a.wellbeing() == FollowupAnswers.Wellbeing.WORRIED) {
            yellow.add("PET_HEALTH_WORRIED");
        }
        if (a.fear() == FollowupAnswers.Fear.SOME || a.fear() == FollowupAnswers.Fear.MUCH) {
            yellow.add("BEHAVIOR");
        }
        if (a.incident() == FollowupAnswers.Incident.NO_INJURY) {
            yellow.add("BITE_NO_INJURY");
        }
        if (a.support() == FollowupAnswers.Support.ADVICE) {
            yellow.add("SUPPORT_REQUESTED");
        }

        List<String> reasons = new ArrayList<>(red);
        reasons.addAll(yellow);
        if (!red.isEmpty()) {
            return new Assessment(FollowupSeverity.RED, reasons);
        }
        return new Assessment(yellow.isEmpty() ? FollowupSeverity.GREEN : FollowupSeverity.YELLOW, reasons);
    }
}
