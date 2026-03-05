package com.browzwi.webscraper.web;

import com.browzwi.webscraper.service.settings.ScrapeFetcherType;
import com.browzwi.webscraper.service.settings.DiscoverySourceType;
import com.browzwi.webscraper.service.settings.SettingsService;
import com.browzwi.webscraper.web.dto.SettingsForm;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for managing application settings through the web interface.
 *
 * @since 1.0
 */
@Controller
@RequestMapping("/settings")
@PreAuthorize("hasRole('ADMIN')")
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    public String view(Model model) {
        SettingsForm form = new SettingsForm();
        form.setFetcherType(settingsService.getFetcherType());
        form.setDiscoverySource(DiscoverySourceType.valueOf(settingsService.getDiscoverySourceType()));
        form.setGoogleSearchSiteFilter(settingsService.getGoogleSearchSiteFilter());
        form.setClaudeApiKey(settingsService.getClaudeApiKey());
        
        model.addAttribute("pageTitle", "Settings");
        model.addAttribute("settings", form);
        model.addAttribute("fetcherOptions", ScrapeFetcherType.values());
        model.addAttribute("discoverySources", DiscoverySourceType.values());
        return "settings/index";
    }

    @PostMapping
    public String update(@ModelAttribute("settings") SettingsForm form,
                         RedirectAttributes redirectAttributes,
                         @RequestParam(value = "saveScraper", required = false) String saveScraper,
                         @RequestParam(value = "saveDiscovery", required = false) String saveDiscovery) {
        
        if (saveScraper != null) {
            settingsService.updateFetcherType(form.getFetcherType());
            redirectAttributes.addFlashAttribute("message", "Scraper engine settings saved successfully");
        } else if (saveDiscovery != null) {
            settingsService.updateDiscoverySourceType(form.getDiscoverySource());
            settingsService.updateGoogleSearchSiteFilter(form.getGoogleSearchSiteFilter());
            settingsService.updateClaudeApiKey(form.getClaudeApiKey());
            redirectAttributes.addFlashAttribute("message", "URL discovery settings saved successfully");
        }
        
        return "redirect:/settings";
    }
}
