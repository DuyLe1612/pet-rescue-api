package com.uit.petrescueapi.application.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uit.petrescueapi.infrastructure.config.JacksonConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppTimeTest {

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            // with an offset: the same instant, as UTC
            "2026-11-12T10:00:00Z,          2026-11-12T10:00",
            "2026-11-12T10:00:00.000Z,      2026-11-12T10:00",
            "2026-11-12T17:00:00+07:00,     2026-11-12T10:00",
            "2026-11-12T03:30:00-05:00,     2026-11-12T08:30",
            // the browser's datetime-local value, converted by the web before sending
            "2026-11-12T00:30:00+07:00,     2026-11-11T17:30",
            // no offset: taken as UTC, as the API did before
            "2026-11-12T10:00:00,           2026-11-12T10:00",
            "2026-11-12T10:00,              2026-11-12T10:00",
            // a date alone: the start of that day in Vietnam
            "2026-11-12,                    2026-11-11T17:00",
    })
    void readsWhatClientsSend(String sent, LocalDateTime utc) {
        assertThat(AppTime.fromWire(sent)).isEqualTo(utc);
    }

    @Test
    void keepsFractionsSoCursorsStayExact() {
        LocalDateTime value = LocalDateTime.of(2026, 11, 12, 10, 0, 1, 123_456_000);

        assertThat(AppTime.toWire(value)).isEqualTo("2026-11-12T10:00:01.123456Z");
        assertThat(AppTime.fromWire(AppTime.toWire(value))).isEqualTo(value);
        assertThat(AppTime.toWire(LocalDateTime.of(2026, 11, 12, 10, 0))).isEqualTo("2026-11-12T10:00:00Z");
    }

    @Test
    void refusesWhatIsNotADateTime() {
        assertThatThrownBy(() -> AppTime.fromWire("12/11/2026")).isInstanceOf(DateTimeParseException.class);
        assertThatThrownBy(() -> AppTime.fromWire("tomorrow")).isInstanceOf(DateTimeParseException.class);
    }

    @Test
    void aMonthStartsAtMidnightInVietnam() {
        LocalDateTime[] november = AppTime.monthUtc(YearMonth.of(2026, 11));
        LocalDateTime[] year = AppTime.yearUtc(2026);

        assertThat(november[0]).isEqualTo(LocalDateTime.of(2026, 10, 31, 17, 0));
        assertThat(november[1]).isEqualTo(LocalDateTime.of(2026, 11, 30, 17, 0));
        assertThat(year[0]).isEqualTo(LocalDateTime.of(2025, 12, 31, 17, 0));
        assertThat(year[1]).isEqualTo(LocalDateTime.of(2026, 12, 31, 17, 0));
    }

    @Test
    void pinningMakesTheHostZoneIrrelevant() {
        TimeZone before = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
            AppTime.pinJvmToUtc();
            assertThat(TimeZone.getDefault().getID()).isIn("UTC", "Z");
        } finally {
            TimeZone.setDefault(before);
        }
    }

    // ── the API's ObjectMapper ──────────────────────

    record Body(LocalDateTime at) {
    }

    private final ObjectMapper mapper = new JacksonConfig().objectMapper();

    @Test
    void theApiWritesEveryDateTimeWithZ() throws Exception {
        assertThat(mapper.writeValueAsString(new Body(LocalDateTime.of(2026, 11, 12, 10, 0))))
                .isEqualTo("{\"at\":\"2026-11-12T10:00:00Z\"}");
    }

    @Test
    void theApiReadsOffsetsInBodies() throws Exception {
        assertThat(mapper.readValue("{\"at\":\"2026-11-12T17:00:00+07:00\"}", Body.class).at())
                .isEqualTo(LocalDateTime.of(2026, 11, 12, 10, 0));
        assertThat(mapper.readValue("{\"at\":\"2026-11-12T10:00:00.000Z\"}", Body.class).at())
                .isEqualTo(LocalDateTime.of(2026, 11, 12, 10, 0));
        assertThat(mapper.readValue("{\"at\":null}", Body.class).at()).isNull();
    }
}
