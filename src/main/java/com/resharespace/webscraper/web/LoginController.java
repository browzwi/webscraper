package com.browzwi.webscraper.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller for handling login-related requests.
 * Provides the endpoint for the login page.
 *
 * @since 1.0
 */
@Controller
public class LoginController {

    /**
     * Handles requests to the login page.
     * Returns the login view template.
     *
     * @return the login view name
     */
    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
