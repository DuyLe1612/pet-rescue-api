package com.uit.petrescueapi.domain.valueobject;

/**
 * System roles, one per kind of account. Names are the codes stored in {@code roles.code}.
 * <ul>
 *   <li>{@code ADMIN} – administrator of the whole platform</li>
 *   <li>{@code USER} – an ordinary account</li>
 *   <li>{@code MEMBER} – membership: the account belongs to an organization. What it may do there
 *       (OWNER, STAFF, VET...) is its {@link OrganizationRole}, checked only on organization routes.</li>
 * </ul>
 * A person gets MEMBER when they join an organization and goes back to USER when they leave the last one.
 */
public enum SystemRole {
    USER,
    ADMIN,
    MEMBER
}
