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
        model.addAttribute("pageTitle", "Settings");
        model.addAttribute("settings", form);
        model.addAttribute("options", ScrapeFetcherType.values());
        return "settings/index";
    }

    @PostMapping
    public String update(@ModelAttribute("settings") SettingsForm form,
                         RedirectAttributes redirectAttributes) {
        settingsService.updateFetcherType(form.getFetcherType());
        redirectAttributes.addFlashAttribute("message", "Settings saved");
        return "redirect:/settings";
    }
}
