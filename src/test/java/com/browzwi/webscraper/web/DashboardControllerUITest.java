package com.browzwi.webscraper.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DashboardController.class)
@Import(com.browzwi.webscraper.security.SecurityConfig.class)
@DisplayName("Dashboard UI Tests")
class DashboardControllerUITest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should render dashboard page with stats")
    void shouldRenderDashboardPageWithStats() throws Exception {
        // When & Then
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("jobStats"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Welcome back")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Active Jobs")));
    }

    @Test
    @DisplayName("Should redirect to login when not authenticated")
    void shouldRedirectToLoginWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should contain dark theme classes")
    void shouldContainDarkThemeClasses() throws Exception {
        // When & Then
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("dark:text-white")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("bg-zinc-900/50")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should contain Material Symbols icons")
    void shouldContainMaterialSymbolsIcons() throws Exception {
        // When & Then
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("material-symbols-outlined")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should have HTMX attributes for server time")
    void shouldHaveHtmxAttributesForServerTime() throws Exception {
        // When & Then
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("hx-get")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/fragments/server-time")));
    }
}
