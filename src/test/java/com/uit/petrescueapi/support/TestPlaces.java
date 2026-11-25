package com.uit.petrescueapi.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uit.petrescueapi.application.service.PostAnalysisValidator;
import com.uit.petrescueapi.application.service.VietnamPlaceResolver;
import com.uit.petrescueapi.infrastructure.persistence.adapter.ProvinceQueryAdapter;

/** The real address catalogue that ships with the app, for tests that resolve places. */
public final class TestPlaces {

    private static final VietnamPlaceResolver RESOLVER =
            new VietnamPlaceResolver(new ProvinceQueryAdapter(new ObjectMapper()));

    private TestPlaces() {
    }

    public static VietnamPlaceResolver resolver() {
        return RESOLVER;
    }

    public static PostAnalysisValidator validator() {
        return new PostAnalysisValidator(RESOLVER);
    }
}
