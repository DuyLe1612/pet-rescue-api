package com.uit.petrescueapi.domain.valueobject;

/** How an owner came to have a pet. */
public enum AcquisitionType {
    /** A person registered their own pet. */
    SELF_REGISTERED,
    /** Adopted through the platform; the ownership carries the application id. */
    ADOPTED,
    /** Held by an organization: created by it, taken in from a case, or handed back. */
    ORG_INTAKE,
    /** Ownership moved by hand (an admin or an organization owner), not through an adoption. */
    TRANSFERRED
}
