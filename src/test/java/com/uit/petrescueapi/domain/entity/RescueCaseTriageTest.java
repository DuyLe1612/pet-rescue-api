package com.uit.petrescueapi.domain.entity;

import com.uit.petrescueapi.domain.valueobject.RescuePriority;
import com.uit.petrescueapi.domain.valueobject.TriageStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static com.uit.petrescueapi.domain.valueobject.RescuePriority.CRITICAL;
import static com.uit.petrescueapi.domain.valueobject.RescuePriority.HIGH;
import static com.uit.petrescueapi.domain.valueobject.RescuePriority.LOW;
import static com.uit.petrescueapi.domain.valueobject.RescuePriority.NORMAL;
import static org.assertj.core.api.Assertions.assertThat;

/** The model may raise a priority, never lower it. */
class RescueCaseTriageTest {

    private static RescueCaseTriage analysed(RescuePriority urgency) {
        return RescueCaseTriage.builder().status(TriageStatus.ANALYZED).aiUrgency(urgency).build();
    }

    @ParameterizedTest(name = "reporter {0}, AI {1} -> {2}")
    @CsvSource({
            "LOW,      CRITICAL, CRITICAL",
            "NORMAL,   HIGH,     HIGH",
            "HIGH,     CRITICAL, CRITICAL",
            // the model never hides a case the reporter flagged
            "CRITICAL, LOW,      CRITICAL",
            "HIGH,     NORMAL,   HIGH",
            "NORMAL,   LOW,      NORMAL",
            "HIGH,     HIGH,     HIGH",
    })
    void takesTheHigherOfTheTwo(RescuePriority reporter, RescuePriority ai, RescuePriority expected) {
        assertThat(RescueCaseTriage.effectivePriority(reporter, analysed(ai))).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({"PENDING", "FAILED"})
    void aTriageThatIsNotFinishedChangesNothing(TriageStatus status) {
        RescueCaseTriage unfinished = RescueCaseTriage.builder().status(status).aiUrgency(CRITICAL).build();

        assertThat(RescueCaseTriage.effectivePriority(LOW, unfinished)).isEqualTo(LOW);
    }

    @Test
    void withoutATriageOrAnUrgencyTheReportersChoiceStands() {
        assertThat(RescueCaseTriage.effectivePriority(HIGH, null)).isEqualTo(HIGH);
        assertThat(RescueCaseTriage.effectivePriority(NORMAL, analysed(null))).isEqualTo(NORMAL);
        assertThat(RescueCaseTriage.effectivePriority(null, analysed(HIGH))).isEqualTo(HIGH);
        assertThat(RescueCaseTriage.effectivePriority(null, null)).isNull();
    }
}
