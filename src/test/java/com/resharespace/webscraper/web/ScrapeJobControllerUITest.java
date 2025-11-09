package com.browzwi.webscraper.web;

import com.browzwi.webscraper.domain.ScrapeJob;
import com.browzwi.webscraper.domain.ScrapeTarget;
import com.browzwi.webscraper.domain.ScraperRecipe;
import com.browzwi.webscraper.domain.ScrapeTargetStatus;
import com.browzwi.webscraper.repository.ScraperRecipeRepository;
import com.browzwi.webscraper.scraper.service.MarkdownConversionService;
import com.browzwi.webscraper.service.ScrapeJobService;
import com.browzwi.webscraper.service.ScraperRecipeService;
import com.browzwi.webscraper.storage.FileStorageService;
import org.springframework.test.util.ReflectionTestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ScrapeJobController.class)
@DisplayName("Scrape Job Controller UI Tests")
class ScrapeJobControllerUITest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ScrapeJobService jobService;

    @MockBean
    private ScraperRecipeRepository recipeRepository;

    @MockBean
    private FileStorageService storageService;

    @MockBean
    private MarkdownConversionService markdownConversionService;

    @MockBean
    private ScraperRecipeService recipeService;

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should render jobs list page")
    void shouldRenderJobsListPage() throws Exception {
        // Given
        ScrapeJob job = createMockJob();
        when(jobService.listJobs()).thenReturn(List.of(job));

        // When & Then
        mockMvc.perform(get("/jobs"))
                .andExpect(status().isOk())
                .andExpect(view().name("jobs/list"))
                .andExpect(model().attributeExists("jobs"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Scrape Jobs")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should display status badges in jobs list")
    void shouldDisplayStatusBadgesInJobsList() throws Exception {
        // Given
        ScrapeJob job = createMockJob();
        when(jobService.listJobs()).thenReturn(List.of(job));

        // When & Then
        mockMvc.perform(get("/jobs"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("badge")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("status-dot")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should render job form with recipe selection")
    void shouldRenderJobFormWithRecipeSelection() throws Exception {
        // Given
        ScraperRecipe recipe = createMockRecipe();
        when(recipeRepository.findAll()).thenReturn(List.of(recipe));

        // When & Then
        mockMvc.perform(get("/jobs/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("jobs/form"))
                .andExpect(model().attributeExists("job"))
                .andExpect(model().attributeExists("recipes"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Create Job")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should display processing overrides checkboxes")
    void shouldDisplayProcessingOverridesCheckboxes() throws Exception {
        // Given
        when(recipeRepository.findAll()).thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/jobs/new"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Strip CSS")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Strip JavaScript")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Processing Overrides")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should render job detail page with stats")
    void shouldRenderJobDetailPageWithStats() throws Exception {
        // Given
        UUID jobId = UUID.randomUUID();
        ScrapeJob job = createMockJob();
        ScrapeTarget target = createMockTarget(job);

        when(jobService.getJob(jobId)).thenReturn(job);
        when(jobService.listTargets(jobId)).thenReturn(List.of(target));

        // When & Then
        mockMvc.perform(get("/jobs/" + jobId))
                .andExpect(status().isOk())
                .andExpect(view().name("jobs/detail"))
                .andExpect(model().attributeExists("job"))
                .andExpect(model().attributeExists("targets"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Scrape Targets")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should display targets table with status indicators")
    void shouldDisplayTargetsTableWithStatusIndicators() throws Exception {
        // Given
        UUID jobId = UUID.randomUUID();
        ScrapeJob job = createMockJob();
        ScrapeTarget target = createMockTarget(job);
        target.setStatus(ScrapeTargetStatus.COMPLETED);

        when(jobService.getJob(jobId)).thenReturn(job);
        when(jobService.listTargets(jobId)).thenReturn(List.of(target));

        // When & Then
        mockMvc.perform(get("/jobs/" + jobId))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("badge-success")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should have HTMX attributes for target details")
    void shouldHaveHtmxAttributesForTargetDetails() throws Exception {
        // Given
        UUID jobId = UUID.randomUUID();
        ScrapeJob job = createMockJob();
        ScrapeTarget target = createMockTarget(job);

        when(jobService.getJob(jobId)).thenReturn(job);
        when(jobService.listTargets(jobId)).thenReturn(List.of(target));

        // When & Then
        mockMvc.perform(get("/jobs/" + jobId))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("hx-get")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("targetDetails")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should display cron expression examples")
    void shouldDisplayCronExpressionExamples() throws Exception {
        // Given
        when(recipeRepository.findAll()).thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/jobs/new"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Common Examples")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("0 0 * * * ?")));
    }

    private ScrapeJob createMockJob() {
        ScrapeJob job = new ScrapeJob();
        job.setName("Test Job");
        job.setRecipe(createMockRecipe());
        job.setScheduleCron("0 0 * * * ?");
        job.setRequestedBy("admin");
        return job;
    }

    private ScraperRecipe createMockRecipe() {
        ScraperRecipe recipe = new ScraperRecipe();
        ReflectionTestUtils.setField(recipe, "id", UUID.randomUUID());
        recipe.setName("Test Recipe");
        recipe.setKey("test-recipe");
        return recipe;
    }

    private ScrapeTarget createMockTarget(ScrapeJob job) {
        ScrapeTarget target = new ScrapeTarget();
        target.setJob(job);
        target.setUrl("https://example.com");
        target.setStatus(ScrapeTargetStatus.PENDING);
        return target;
    }
}
