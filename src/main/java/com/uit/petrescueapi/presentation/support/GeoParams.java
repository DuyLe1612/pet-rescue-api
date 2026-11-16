package com.uit.petrescueapi.presentation.support;

import com.uit.petrescueapi.domain.exception.BusinessException;

/** Checks the optional area parameters of list and map endpoints, so a half-given area is refused rather than ignored. */
public final class GeoParams {

    private static final String CODE = "INVALID_GEO_PARAMS";

    private GeoParams() {
    }

    /** True for a complete box, false when no corner was given. */
    public static boolean hasBoundingBox(Double minLat, Double minLng, Double maxLat, Double maxLng) {
        if (minLat == null && minLng == null && maxLat == null && maxLng == null) {
            return false;
        }
        if (minLat == null || minLng == null || maxLat == null || maxLng == null) {
            throw new BusinessException("A bounding box needs minLat, minLng, maxLat and maxLng together", CODE);
        }
        requireLatitude(minLat, "minLat");
        requireLatitude(maxLat, "maxLat");
        requireLongitude(minLng, "minLng");
        requireLongitude(maxLng, "maxLng");
        if (minLat > maxLat || minLng > maxLng) {
            throw new BusinessException("minLat and minLng must not be greater than maxLat and maxLng", CODE);
        }
        return true;
    }

    /** True for a complete point, false when neither coordinate was given. {@code distance} may be null. */
    public static boolean hasPoint(Double lat, Double lng, Double distance) {
        if (lat == null && lng == null) {
            if (distance != null) {
                throw new BusinessException("distance needs lat and lng", CODE);
            }
            return false;
        }
        if (lat == null || lng == null) {
            throw new BusinessException("lat and lng must be given together", CODE);
        }
        requireLatitude(lat, "lat");
        requireLongitude(lng, "lng");
        if (distance != null && !(distance > 0)) {
            throw new BusinessException("distance must be greater than 0 (meters)", CODE);
        }
        return true;
    }

    private static void requireLatitude(double value, String name) {
        if (!(value >= -90 && value <= 90)) {
            throw new BusinessException(name + " must be between -90 and 90", CODE);
        }
    }

    private static void requireLongitude(double value, String name) {
        if (!(value >= -180 && value <= 180)) {
            throw new BusinessException(name + " must be between -180 and 180", CODE);
        }
    }
}
