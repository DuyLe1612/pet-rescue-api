package com.uit.petrescueapi.domain.service;

import com.uit.petrescueapi.domain.valueobject.FollowupAnswers;
import com.uit.petrescueapi.domain.valueobject.FollowupAnswers.Fear;
import com.uit.petrescueapi.domain.valueobject.FollowupAnswers.Incident;
import com.uit.petrescueapi.domain.valueobject.FollowupAnswers.Support;
import com.uit.petrescueapi.domain.valueobject.FollowupAnswers.Wellbeing;
import com.uit.petrescueapi.domain.valueobject.FollowupSeverity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FollowupRulesTest {

    private static FollowupAnswers answers(Boolean still, Wellbeing w, Fear f, Incident i, Support s) {
        return new FollowupAnswers(still, w, f, i, s, null);
    }

    private static final FollowupAnswers FINE = answers(true, Wellbeing.NORMAL, Fear.NONE, Incident.NONE, Support.NONE);

    @Test
    void everythingFineIsGreen() {
        var result = FollowupRules.assess(FINE);

        assertThat(result.severity()).isEqualTo(FollowupSeverity.GREEN);
        assertThat(result.reasons()).isEmpty();
    }

    @Test
    void consideringReturnIsRed() {
        var result = FollowupRules.assess(answers(true, Wellbeing.NORMAL, Fear.NONE, Incident.NONE, Support.CONSIDERING_RETURN));

        assertThat(result.severity()).isEqualTo(FollowupSeverity.RED);
        assertThat(result.reasons()).containsExactly("CONSIDERING_RETURN");
    }

    @Test
    void aBiteThatInjuredSomeoneIsRedButOneThatDidNotIsYellow() {
        assertThat(FollowupRules.assess(answers(true, Wellbeing.NORMAL, Fear.NONE, Incident.INJURY, Support.NONE)).severity())
                .isEqualTo(FollowupSeverity.RED);
        var minor = FollowupRules.assess(answers(true, Wellbeing.NORMAL, Fear.NONE, Incident.NO_INJURY, Support.NONE));
        assertThat(minor.severity()).isEqualTo(FollowupSeverity.YELLOW);
        assertThat(minor.reasons()).containsExactly("BITE_NO_INJURY");
    }

    @Test
    void aPetThatIsGoneIsRedWhateverElseWasLeftOut() {
        var result = FollowupRules.assess(answers(false, null, null, null, null));

        assertThat(result.severity()).isEqualTo(FollowupSeverity.RED);
        assertThat(result.reasons()).containsExactly("PET_GONE");
    }

    @Test
    void worryingEatingAndFearAreYellowAndAlarmingEatingIsRed() {
        var worried = FollowupRules.assess(answers(true, Wellbeing.WORRIED, Fear.SOME, Incident.NONE, Support.ADVICE));
        assertThat(worried.severity()).isEqualTo(FollowupSeverity.YELLOW);
        assertThat(worried.reasons()).containsExactly("PET_HEALTH_WORRIED", "BEHAVIOR", "SUPPORT_REQUESTED");

        assertThat(FollowupRules.assess(answers(true, Wellbeing.ALARMING, Fear.NONE, Incident.NONE, Support.NONE)).severity())
                .isEqualTo(FollowupSeverity.RED);
    }

    @Test
    void redWinsAndKeepsEveryReason() {
        var result = FollowupRules.assess(answers(true, Wellbeing.WORRIED, Fear.MUCH, Incident.INJURY, Support.CONSIDERING_RETURN));

        assertThat(result.severity()).isEqualTo(FollowupSeverity.RED);
        assertThat(result.reasons()).containsExactly("CONSIDERING_RETURN", "BITE_INJURY", "PET_HEALTH_WORRIED", "BEHAVIOR");
    }
}
