package com.uit.petrescueapi.application.port.out;

import java.util.List;

/** Reference values the AI analysis must stay within (PostgreSQL taxonomy + tags). */
public interface AnalysisCatalogPort {

    /** Canonical English species names, e.g. "Dog", "Cat". */
    List<String> speciesNames();

    /** Codes of non-deleted tags. */
    List<String> tagCodes();
}
