package com.browzwi.webscraper.web;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model) {
        model.addAttribute("pageTitle", "Dashboard");
        model.addAttribute("jobStats", List.of(
                Map.of("label", "Active Jobs", "value", 3),
                Map.of("label", "Pending Targets", "value", 42),
                Map.of("label", "Recipes", "value", 7)
        ));
        return "dashboard";
    }

    @GetMapping("/fragments/server-time")
    public String serverTime(Model model) {
        model.addAttribute("now", Instant.now());
        return "fragments :: serverTime";
    }
}
