package com.browzwi.webscraper.web;

import com.browzwi.webscraper.service.settings.ScrapeFetcherType;
import com.browzwi.webscraper.service.settings.SettingsService;
import com.browzwi.webscraper.web.dto.SettingsForm;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for managing application settings through the web interface.
 * Provides endpoints for viewing and updating application settings such as scraper fetcher type.
 *
 * @since 1.0
 */
@Controller
@RequestMapping("/settings")
@PreAuthorize("hasRole('ADMIN')")
public class SettingsController {

    private final SettingsService settingsService;

    /**
     * Constructor for SettingsController with required dependencies.
     *
     * @param settingsService service for managing application settings
     */
    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    /**
     * Handles requests to view the settings page.
     * Sets up the model with current settings and available options for the settings view.
     *
     * @param model the model to populate with settings data
     * @return the settings view name
     */
    @GetMapping
    public String view(Model model) {
        SettingsForm form = new SettingsForm();
        form.setFetcherType(settingsService.getFetcherType());
        model.addAttribute("pageTitle", "Settings");
        model.addAttribute("settings", form);
        model.addAttribute("options", ScrapeFetcherType.values());
        return "settings/index";
    }

    /**
     * Handles form submissions for updating application settings.
     * Updates the settings and redirects back to the settings page.
     *
     * @param form the settings form data
     * @param redirectAttributes attributes for redirect after successful update
     * @return redirect to settings page
     */
    @PostMapping
    public String update(@ModelAttribute("settings") SettingsForm form,
                         RedirectAttributes redirectAttributes) {
        settingsService.updateFetcherType(form.getFetcherType());
        redirectAttributes.addFlashAttribute("message", "Settings saved");
        return "redirect:/settings";
    }
}
