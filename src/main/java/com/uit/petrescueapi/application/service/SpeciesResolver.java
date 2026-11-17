package com.uit.petrescueapi.application.service;

import com.uit.petrescueapi.application.port.out.SpeciesCatalogPort;
import com.uit.petrescueapi.application.support.CanonicalText;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Maps what people type for a species ("dog", "DOG", "Chó", "cho", "cún") to the one canonical
 * English name in {@code pet_species}, which is what the database stores and filters on.
 *
 * <p>Matches the English name, the Vietnamese label and {@code pet_species_aliases}, compared as
 * {@link CanonicalText#key} keys. The catalog is read at most once a minute.
 */
@Service
@RequiredArgsConstructor
public class SpeciesResolver {

    /** Stored when a reporter types something the catalog does not know. */
    public static final String OTHER = "Other";

    private static final Duration TTL = Duration.ofMinutes(1);

    private final SpeciesCatalogPort catalog;

    private volatile Map<String, String> byKey;
    private volatile Instant loadedAt = Instant.EPOCH;

    /** The canonical name, or empty when {@code raw} is blank or not in the catalog. */
    public Optional<String> resolve(String raw) {
        String key = CanonicalText.key(raw);
        return key == null ? Optional.empty() : Optional.ofNullable(index().get(key));
    }

    /**
     * For values that must be stored: blank stays {@code null}, an unknown species becomes
     * {@link #OTHER} so the record is still saved and still filterable.
     */
    public String canonicalOrOther(String raw) {
        if (CanonicalText.key(raw) == null) {
            return null;
        }
        return resolve(raw).orElse(OTHER);
    }

    /** For filters: the canonical name when known, otherwise the trimmed text (matches nothing). */
    public String canonicalForFilter(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return resolve(raw).orElse(raw.trim());
    }

    private Map<String, String> index() {
        Map<String, String> current = byKey;
        Instant now = Instant.now();
        if (current == null || now.isAfter(loadedAt.plus(TTL))) {
            current = load();
            byKey = current;
            loadedAt = now;
        }
        return current;
    }

    private Map<String, String> load() {
        Map<String, String> index = new HashMap<>();
        for (SpeciesCatalogPort.SpeciesAlias alias : catalog.aliases()) {
            String key = CanonicalText.key(alias.aliasKey());
            if (key != null) index.put(key, alias.speciesName());
        }
        // Names win over aliases when both spell the same key.
        for (SpeciesCatalogPort.SpeciesNames species : catalog.species()) {
            String vi = CanonicalText.key(species.nameVi());
            if (vi != null) index.put(vi, species.name());
            index.put(CanonicalText.key(species.name()), species.name());
        }
        return Map.copyOf(index);
    }
}
