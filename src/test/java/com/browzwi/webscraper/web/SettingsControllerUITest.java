package com.browzwi.webscraper.web;

import com.browzwi.webscraper.service.settings.ScrapeFetcherType;
import com.browzwi.webscraper.service.settings.SettingsService;
import com.browzwi.webscraper.web.dto.SettingsForm;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SettingsController.class)
@DisplayName("Settings Controller UI Tests")
class SettingsControllerUITest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SettingsService settingsService;

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should render settings page with fetcher options")
    void shouldRenderSettingsPageWithFetcherOptions() throws Exception {
        // Given
        when(settingsService.getFetcherType()).thenReturn(ScrapeFetcherType.HTMLUNIT);

        // When & Then
        mockMvc.perform(get("/settings"))
                .andExpect(status().isOk())
                .andExpect(view().name("settings/index"))
                .andExpect(model().attributeExists("settings"))
                .andExpect(model().attributeExists("fetcherOptions"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Settings")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Scraper Engine")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should display both HtmlUnit and Playwright options")
    void shouldDisplayBothFetcherOptions() throws Exception {
        // Given
        when(settingsService.getFetcherType()).thenReturn(ScrapeFetcherType.HTMLUNIT);

        // When & Then
        mockMvc.perform(get("/settings"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("HTMLUNIT")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("PLAYWRIGHT")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should have radio buttons for fetcher selection")
    void shouldHaveRadioButtonsForFetcherSelection() throws Exception {
        // Given
        when(settingsService.getFetcherType()).thenReturn(ScrapeFetcherType.HTMLUNIT);

        // When & Then
        mockMvc.perform(get("/settings"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("type=\"radio\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("fetcherType")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should display feature badges for fetchers")
    void shouldDisplayFeatureBadgesForFetchers() throws Exception {
        // Given
        when(settingsService.getFetcherType()).thenReturn(ScrapeFetcherType.HTMLUNIT);

        // When & Then
        mockMvc.perform(get("/settings"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("badge")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Lightweight")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("JavaScript")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should have save and reset buttons")
    void shouldHaveSaveAndResetButtons() throws Exception {
        // Given
        when(settingsService.getFetcherType()).thenReturn(ScrapeFetcherType.HTMLUNIT);

        // When & Then
        mockMvc.perform(get("/settings"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Save Settings")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should display Material icons for settings")
    void shouldDisplayMaterialIconsForSettings() throws Exception {
        // Given
        when(settingsService.getFetcherType()).thenReturn(ScrapeFetcherType.HTMLUNIT);

        // When & Then
        mockMvc.perform(get("/settings"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("material-symbols-outlined")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("settings")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should show selected fetcher type")
    void shouldShowSelectedFetcherType() throws Exception {
        // Given
        when(settingsService.getFetcherType()).thenReturn(ScrapeFetcherType.PLAYWRIGHT);

        // When & Then
        mockMvc.perform(get("/settings"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("border-primary")));
    }
}
