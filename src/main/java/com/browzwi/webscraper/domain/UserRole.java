package com.browzwi.webscraper.domain;

/**
 * Represents the available user roles in the web scraper application.
 *
 * <p>Architectural rationale: Enum-based roles provide type-safe role checking
 * and prevent invalid role assignments throughout the application.
 *
 * <p>Key constraints: Roles are fixed and correspond to security permissions
 * defined in the security configuration.
 *
 * @since 1.0
 */
public enum UserRole {
    ROLE_ADMIN,
    ROLE_USER
}
