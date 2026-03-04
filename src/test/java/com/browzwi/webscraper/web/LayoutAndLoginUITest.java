package com.browzwi.webscraper.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Layout and Login UI Tests")
class LayoutAndLoginUITest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Should render login page with dark theme")
    void shouldRenderLoginPageWithDarkTheme() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Sign in to your account")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Web Scraper")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("bg-background-dark")));
    }

    @Test
    @DisplayName("Should have login form with username and password fields")
    void shouldHaveLoginFormWithFields() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"username\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"password\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("type=\"password\"")));
    }

    @Test
    @DisplayName("Should have Material Symbols icon on login page")
    void shouldHaveMaterialSymbolsIconOnLoginPage() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("material-symbols-outlined")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("data_object")));
    }

    @Test
    @DisplayName("Should display dev credentials notice")
    void shouldDisplayDevCredentialsNotice() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Development Mode")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("application-dev.yml")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should have consistent navigation across all pages")
    void shouldHaveConsistentNavigation() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Dashboard")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Recipes")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Jobs")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Settings")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Logout")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should have sticky header with backdrop blur")
    void shouldHaveStickyHeaderWithBackdropBlur() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("sticky top-0")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("backdrop-blur")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should load Tailwind CSS stylesheet")
    void shouldLoadTailwindCssStylesheet() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/styles.css")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should load HTMX library")
    void shouldLoadHtmxLibrary() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("htmx")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should have CSRF token in layout")
    void shouldHaveCsrfTokenInLayout() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("csrfToken")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("X-CSRF-TOKEN")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should have HTMX loading indicator")
    void shouldHaveHtmxLoadingIndicator() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("htmx-indicator")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Working...")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should have utility functions for clipboard operations")
    void shouldHaveUtilityFunctionsForClipboard() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("copyYaml")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("copyFromElement")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("insertYaml")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should have active navigation highlighting")
    void shouldHaveActiveNavigationHighlighting() throws Exception {
        mockMvc.perform(get("/recipes"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("nav-link-active")));
    }

    @Test
    @DisplayName("Should display error message on login failure")
    void shouldDisplayErrorMessageOnLoginFailure() throws Exception {
        mockMvc.perform(get("/login?error"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Invalid username or password")));
    }

    @Test
    @DisplayName("Should display logout success message")
    void shouldDisplayLogoutSuccessMessage() throws Exception {
        mockMvc.perform(get("/login?logout"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("logged out successfully")));
    }
}
