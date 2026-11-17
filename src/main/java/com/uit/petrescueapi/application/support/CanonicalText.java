package com.uit.petrescueapi.application.support;

import java.text.Normalizer;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Turns the many ways people write the same word into one comparable form, so "Dog", "DOG",
 * " dog ", "Chó" and "cho" can be matched against the same catalog entry.
 *
 * <p>Only for matching and for building codes. What gets stored is always the catalog's own
 * canonical value, never the key.
 */
public final class CanonicalText {

    private static final Pattern MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]+");

    private CanonicalText() {
    }

    /**
     * Lower case, no diacritics ("đ" becomes "d"), every run of other characters becomes one
     * underscore, no underscore at either end. {@code null} or blank gives {@code null}.
     */
    public static String key(String value) {
        return fold(value, "_");
    }

    /** Same as {@link #key} with hyphens, the shape used for tag codes: "Tìm chủ" becomes "tim-chu". */
    public static String slug(String value) {
        return fold(value, "-");
    }

    /**
     * Tag codes as stored: each one slugged, blanks dropped, duplicates removed in order.
     * {@code null} stays {@code null} so a patch can still mean "leave the tags alone".
     */
    public static List<String> slugs(List<String> values) {
        if (values == null) {
            return null;
        }
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String value : values) {
            String slug = slug(value);
            if (slug != null) result.add(slug);
        }
        return List.copyOf(result);
    }

    private static String fold(String value, String separator) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String noMarks = MARKS.matcher(Normalizer.normalize(value.trim(), Normalizer.Form.NFD)).replaceAll("");
        String ascii = noMarks.replace('đ', 'd').replace('Đ', 'D').toLowerCase(Locale.ROOT);
        String folded = NON_ALNUM.matcher(ascii).replaceAll(separator);
        int start = 0;
        int end = folded.length();
        while (start < end && folded.startsWith(separator, start)) start += separator.length();
        while (end > start && folded.startsWith(separator, end - separator.length())) end -= separator.length();
        return start < end ? folded.substring(start, end) : null;
    }
}
