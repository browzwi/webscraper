package com.browzwi.webscraper.repository;

import com.browzwi.webscraper.domain.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository for managing user entities in the database.
 * Provides CRUD operations for User entities and methods
 * for user authentication and password management.
 *
 * @since 1.0
 */
public interface UserRepository extends JpaRepository<User, UUID> {
    /**
     * Finds a user by their username.
     *
     * @param username the username to search for
     * @return an optional containing the user if found, or empty if not found
     */
    Optional<User> findByUsername(String username);

    /**
     * Updates the password for a user with the specified username.
     * This method updates both the password hash and the plain password field.
     *
     * @param username the username of the user whose password to update
     * @param hash the new password hash to set
     * @param plain the new plain password to set
     * @return the number of rows affected (should be 1 if successful, 0 if user not found)
     */
    @Modifying
    @Query("update User u set u.passwordHash = :hash, u.plainPassword = :plain where u.username = :username")
    int updatePassword(@Param("username") String username,
                       @Param("hash") String hash,
                       @Param("plain") String plain);
}
