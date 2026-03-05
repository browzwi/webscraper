package com.browzwi.webscraper.web;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller for handling dashboard-related requests.
 * Provides endpoints for the main dashboard view and server time fragments.
 *
 * @since 1.0
 */
@Controller
public class DashboardController {

    /**
     * Handles requests to the dashboard page.
     * Sets up page title and job statistics for the dashboard view.
     *
     * @param model the model to populate with dashboard data
     * @return the dashboard view name
     */
    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model) {
        model.addAttribute("pageTitle", "Dashboard");
        model.addAttribute("jobStats", List.of(
                Map.of("label", "Active Jobs", "value", 3, "icon", "work"),
                Map.of("label", "Pending Targets", "value", 42, "icon", "pending"),
                Map.of("label", "Recipes", "value", 7, "icon", "menu_book")
        ));
        return "dashboard";
    }

    /**
     * Handles requests for the server time fragment.
     * Returns the current server time as a fragment for display.
     *
     * @param model the model to populate with server time
     * @return the server time fragment view
     */
    @GetMapping("/fragments/server-time")
    public String serverTime(Model model) {
        model.addAttribute("now", Instant.now());
        return "fragments :: serverTime";
    }
}
