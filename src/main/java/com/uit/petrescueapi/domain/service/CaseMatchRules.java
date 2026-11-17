package com.uit.petrescueapi.domain.service;

import com.uit.petrescueapi.domain.valueobject.CaseMatchCandidate;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Scores how likely a found or rescued animal is someone's lost pet, from text and attributes only.
 * No photos and no AI: the same pair always gets the same score and the same reasons, and it works
 * with the AI turned off. The radii are first guesses to tune against real data.
 */
public final class CaseMatchRules {

    public static final int DOG_RADIUS_M = 3000;
    public static final int CAT_RADIUS_M = 1000;
    public static final int OTHER_RADIUS_M = 2000;
    public static final int LOST_WINDOW_DAYS = 30;

    private static final Set<String> STOP_WORDS = Set.of(
            "co", "có", "là", "và", "con", "bé", "màu", "một", "rất", "hơi", "bị", "của", "đeo", "nhỏ", "lớn", "to",
            "the", "and", "with", "has", "a", "an", "of");

    private CaseMatchRules() {
    }

    public record Score(int score, List<String> reasons) {
    }

    /** The search radius follows the lost animal: dogs roam further than cats. */
    public static int radiusFor(String lostSpecies) {
        String key = speciesKey(lostSpecies);
        return switch (key) {
            case "dog" -> DOG_RADIUS_M;
            case "cat" -> CAT_RADIUS_M;
            default -> OTHER_RADIUS_M;
        };
    }

    public static Score score(CaseMatchCandidate c) {
        List<String> reasons = new ArrayList<>();
        int score = 0;

        int radius = radiusFor(c.lostSpecies());
        double closeness = Math.max(0, 1 - c.distanceMeters() / radius);
        score += (int) Math.round(40 * closeness);
        reasons.add("Cách khoảng " + roundTo100(c.distanceMeters()) + " m");

        if (c.lostSpecies() != null && c.otherSpecies() != null) {
            score += 20;
            reasons.add("Cùng loài");
        } else {
            score += 10;
            reasons.add("Một bên chưa ghi loài");
        }

        Set<String> colorOverlap = overlap(tokens(c.lostColor()), tokens(c.otherColor(), c.otherDescription()));
        if (!colorOverlap.isEmpty()) {
            score += 20;
            reasons.add("Màu giống: " + String.join(", ", colorOverlap));
        }

        Set<String> markOverlap = overlap(tokens(c.lostMarks(), c.lostDescription()), tokens(c.otherMarks(), c.otherDescription()));
        markOverlap.removeAll(colorOverlap);
        if (markOverlap.size() >= 2) {
            score += 20;
        } else if (markOverlap.size() == 1) {
            score += 10;
        }
        if (!markOverlap.isEmpty()) {
            reasons.add("Đặc điểm trùng: " + String.join(", ", markOverlap));
        }

        if (c.hoursAfterLost() != null) {
            long days = Math.max(0, Math.round(c.hoursAfterLost() / 24));
            reasons.add(days == 0 ? "Báo trong ngày bé bị lạc" : "Báo " + days + " ngày sau khi bé bị lạc");
        }
        return new Score(Math.min(100, score), reasons);
    }

    static Set<String> tokens(String... texts) {
        Set<String> out = new LinkedHashSet<>();
        for (String text : texts) {
            if (text == null) {
                continue;
            }
            String normalized = Normalizer.normalize(text, Normalizer.Form.NFC).toLowerCase(Locale.ROOT);
            for (String word : normalized.split("[^\\p{L}\\p{N}]+")) {
                if (word.length() >= 2 && !STOP_WORDS.contains(word)) {
                    out.add(word);
                }
            }
        }
        return out;
    }

    private static Set<String> overlap(Set<String> a, Set<String> b) {
        Set<String> out = new LinkedHashSet<>(a);
        out.retainAll(b);
        return out;
    }

    private static String speciesKey(String species) {
        if (species == null) {
            return "";
        }
        String s = Normalizer.normalize(species.trim(), Normalizer.Form.NFC).toLowerCase(Locale.ROOT);
        if (s.equals("dog") || s.equals("chó") || s.equals("cho")) {
            return "dog";
        }
        if (s.equals("cat") || s.equals("mèo") || s.equals("meo")) {
            return "cat";
        }
        return s;
    }

    private static long roundTo100(double meters) {
        return Math.max(100, Math.round(meters / 100.0) * 100);
    }
}
