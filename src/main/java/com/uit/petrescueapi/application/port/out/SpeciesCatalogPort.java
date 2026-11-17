package com.uit.petrescueapi.application.port.out;

import java.util.List;

/** The species catalog and the extra spellings that point at each species. */
public interface SpeciesCatalogPort {

    /** Every active species: canonical English name and Vietnamese label. */
    List<SpeciesNames> species();

    /** Extra spellings ("cun", "puppy", "con cho") and the canonical English name each one means. */
    List<SpeciesAlias> aliases();

    record SpeciesNames(String name, String nameVi) {
    }

    record SpeciesAlias(String aliasKey, String speciesName) {
    }
}
