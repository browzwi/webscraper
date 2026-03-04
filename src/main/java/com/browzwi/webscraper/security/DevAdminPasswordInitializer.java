package com.browzwi.webscraper.security;

import com.browzwi.webscraper.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Initializes or resets the development/test administrator password on application startup.
 * This component runs only in dev and test profiles and allows setting a known
 * admin password for development and testing purposes.
 *
 * @since 1.0
 */
@Component
@Profile({"dev", "test"})
public class DevAdminPasswordInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevAdminPasswordInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminPlainPassword;

    /**
     * Constructor for DevAdminPasswordInitializer with required dependencies.
     *
     * @param userRepository repository for managing users
     * @param passwordEncoder encoder for hashing passwords
     * @param adminUsername the username for the admin account (configured via property)
     * @param adminPlainPassword the plain text password for the admin account (configured via property)
     */
    public DevAdminPasswordInitializer(UserRepository userRepository,
                                       PasswordEncoder passwordEncoder,
                                       @Value("${webscraper.security.admin.username:admin}") String adminUsername,
                                       @Value("${webscraper.security.admin.plain-password:}") String adminPlainPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminPlainPassword = adminPlainPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (adminPlainPassword == null || adminPlainPassword.isBlank()) {
            return;
        }
        userRepository.findByUsername(adminUsername).ifPresent(user -> {
            String encoded = passwordEncoder.encode(adminPlainPassword);
            if (!passwordEncoder.matches(adminPlainPassword, user.getPasswordHash())) {
                userRepository.updatePassword(adminUsername, encoded, adminPlainPassword);
                log.info("Dev/test admin password reset for user '{}'", adminUsername);
            }
        });
    }
}
