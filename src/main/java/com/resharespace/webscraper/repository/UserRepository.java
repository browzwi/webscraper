package com.browzwi.webscraper.repository;

import com.browzwi.webscraper.domain.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);

    @Modifying
    @Query("update User u set u.passwordHash = :hash, u.plainPassword = :plain where u.username = :username")
    int updatePassword(@Param("username") String username,
                       @Param("hash") String hash,
                       @Param("plain") String plain);
}
