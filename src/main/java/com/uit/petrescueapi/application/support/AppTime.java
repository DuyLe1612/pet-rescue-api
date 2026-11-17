package com.uit.petrescueapi.application.support;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.TimeZone;

/**
 * The one time convention of the API.
 *
 * <ul>
 *   <li>Inside the application every {@link LocalDateTime} is a UTC wall-clock time. The JVM is
 *       pinned to UTC at start-up ({@link #pinJvmToUtc()}), so the database session, Hibernate,
 *       MongoDB and {@code LocalDateTime.now()} all agree no matter where the server runs.</li>
 *   <li>On the wire every date-time carries its offset: the API writes {@code ...Z} and accepts
 *       {@code Z} or any offset such as {@code +07:00}.</li>
 *   <li>Calendar questions ("this month", "this day") are asked in {@link #BUSINESS_ZONE}, the zone
 *       the users live in, then turned back into UTC.</li>
 * </ul>
 */
public final class AppTime {

    public static final ZoneId UTC = ZoneOffset.UTC;

    /** Vietnam has no daylight saving time, so this is always +07:00. */
    public static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    /** {@code 2026-11-12T10:00:00Z}; fractions are kept, so cursors built on them stay exact. */
    private static final DateTimeFormatter WIRE = new DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            .appendLiteral('Z')
            .toFormatter();

    private static final DateTimeFormatter LOCAL_WITH_OPTIONAL_SECONDS = new DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_LOCAL_DATE)
            .appendLiteral('T')
            .appendPattern("HH:mm")
            .optionalStart().appendLiteral(':').appendValue(ChronoField.SECOND_OF_MINUTE, 2)
            .optionalStart().appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true).optionalEnd()
            .optionalEnd()
            .toFormatter();

    private AppTime() {
    }

    /** Called first thing in {@code main}, before any connection or clock is read. */
    public static void pinJvmToUtc() {
        TimeZone.setDefault(TimeZone.getTimeZone(UTC));
    }

    /** A UTC wall-clock time as the API writes it. */
    public static String toWire(LocalDateTime utc) {
        return utc.format(WIRE);
    }

    /**
     * Reads a date-time sent by a client into a UTC wall-clock time.
     *
     * <ul>
     *   <li>With {@code Z} or an offset: converted to UTC.</li>
     *   <li>Without an offset: taken as UTC, which is how the API behaved before offsets were
     *       required. New clients should always send one.</li>
     *   <li>A date alone ({@code 2026-11-12}): the start of that day in Vietnam.</li>
     * </ul>
     *
     * @throws DateTimeParseException when the text is none of these
     */
    public static LocalDateTime fromWire(String text) {
        String value = text.trim();
        if (value.length() == 10) {
            return startOfDayUtc(LocalDate.parse(value));
        }
        if (hasOffset(value)) {
            return OffsetDateTime.parse(value).withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
        }
        return LocalDateTime.parse(value, LOCAL_WITH_OPTIONAL_SECONDS);
    }

    /** 00:00 of that day in Vietnam, as a UTC wall-clock time. */
    public static LocalDateTime startOfDayUtc(LocalDate day) {
        return day.atStartOfDay(BUSINESS_ZONE).withZoneSameInstant(UTC).toLocalDateTime();
    }

    /** The month in Vietnam, as UTC wall-clock bounds: start inclusive, end exclusive. */
    public static LocalDateTime[] monthUtc(YearMonth month) {
        return new LocalDateTime[]{startOfDayUtc(month.atDay(1)), startOfDayUtc(month.plusMonths(1).atDay(1))};
    }

    /** The calendar year in Vietnam, as UTC wall-clock bounds: start inclusive, end exclusive. */
    public static LocalDateTime[] yearUtc(int year) {
        return new LocalDateTime[]{startOfDayUtc(LocalDate.of(year, 1, 1)), startOfDayUtc(LocalDate.of(year + 1, 1, 1))};
    }

    private static boolean hasOffset(String value) {
        if (value.endsWith("Z") || value.endsWith("z")) {
            return true;
        }
        int timeStart = value.indexOf('T');
        if (timeStart < 0) {
            return false;
        }
        String time = value.substring(timeStart);
        return time.indexOf('+') > 0 || time.indexOf('-') > 0;
    }
}
