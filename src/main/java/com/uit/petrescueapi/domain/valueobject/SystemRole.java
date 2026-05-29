package com.uit.petrescueapi.domain.valueobject;

/**
 * Application-level roles assigned to users.
 * <ul>
 *   <li>{@code USER} – default registered user</li>
 *   <li>{@code ADMIN} – system administrator</li>
 *   <li>{@code MEMBERSHIP} – organization member (shelter / vet center)</li>
 * </ul>
 */
public enum SystemRole {
    USER,
    ADMIN,
    MEMBERSHIP
}
