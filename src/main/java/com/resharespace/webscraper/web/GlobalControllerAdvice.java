package com.browzwi.webscraper.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Global controller advice that provides common functionality across all controllers.
 * Makes the request URI available to all views for consistent UI rendering.
 *
 * @since 1.0
 */
@ControllerAdvice
public class GlobalControllerAdvice {

    /**
     * Adds the current request URI to the model for all views.
     * This allows templates to have access to the current URI for navigation highlighting.
     *
     * @param request the HTTP servlet request
     * @return the request URI
     */
    @ModelAttribute("requestURI")
    public String getRequestURI(HttpServletRequest request) {
        return request.getRequestURI();
    }
}
