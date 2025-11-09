package com.browzwi.webscraper.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a user in the web scraper application with authentication and authorization.
 *
 * <p>Architectural rationale: This entity manages user credentials and roles,
 * providing the foundation for access control and authorization in the application.
 * The design supports different user roles with varying levels of access.
 *
 * <p>Key constraints: Usernames must be unique. Passwords are stored as secure
 * hashes, with plain text passwords being temporary values used during updates.
 *
 * @since 1.0
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "char(36)")
    private UUID id;

    @Column(nullable = false, unique = true, length = 64)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private UserRole role;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    @Column(name = "plain_password", length = 64)
    private String plainPassword;

    /**
     * Gets the unique identifier for this user.
     *
     * @return the user's UUID identifier
     */
    public UUID getId() {
        return id;
    }

    /**
     * Gets the username for this user.
     *
     * @return the username string
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the username for this user.
     *
     * @param username the username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Gets the hashed password for this user.
     *
     * @return the password hash string
     */
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * Sets the hashed password for this user.
     *
     * @param passwordHash the password hash to set
     */
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /**
     * Gets the role assigned to this user.
     *
     * @return the user's role
     */
    public UserRole getRole() {
        return role;
    }

    /**
     * Sets the role assigned to this user.
     *
     * @param role the role to assign to the user
     */
    public void setRole(UserRole role) {
        this.role = role;
    }

    /**
     * Checks if this user account is enabled.
     *
     * @return true if the user can authenticate, false otherwise
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets whether this user account is enabled.
     *
     * @param enabled true to enable the account, false to disable it
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Gets the timestamp when this user account was created.
     *
     * @return the creation timestamp
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Gets the timestamp when this user account was last updated.
     *
     * @return the last update timestamp
     */
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Gets the plain text password (used temporarily during updates).
     *
     * @return the plain text password, or null if not set
     */
    public String getPlainPassword() {
        return plainPassword;
    }

    /**
     * Sets the plain text password (used temporarily during updates).
     *
     * @param plainPassword the plain text password to set
     */
    public void setPlainPassword(String plainPassword) {
        this.plainPassword = plainPassword;
    }
}
