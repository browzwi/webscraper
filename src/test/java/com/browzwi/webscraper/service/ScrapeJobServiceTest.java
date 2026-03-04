package com.browzwi.webscraper.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.browzwi.webscraper.domain.ScrapeJob;
import com.browzwi.webscraper.domain.ScrapeTarget;
import com.browzwi.webscraper.domain.ScraperRecipe;
import com.browzwi.webscraper.repository.ScrapeJobRepository;
import com.browzwi.webscraper.repository.ScrapeResultDataRepository;
import com.browzwi.webscraper.repository.ScrapeTargetRepository;
import com.browzwi.webscraper.repository.ScraperRecipeRepository;
import com.browzwi.webscraper.scraper.model.OptionsConfig;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScrapeJobServiceTest {

    @Mock
    private ScrapeJobRepository jobRepository;
    @Mock
    private ScrapeTargetRepository targetRepository;
    @Mock
    private ScrapeResultDataRepository resultDataRepository;
    @Mock
    private ScraperRecipeRepository recipeRepository;
    @Mock
    private ScrapeJobSchedulerService schedulerService;

    private ScrapeJobService jobService;

    @BeforeEach
    void setup() {
        jobService = new ScrapeJobService(jobRepository, targetRepository, resultDataRepository,
                recipeRepository, schedulerService, new ObjectMapper());
    }

    @Test
    void createJobPersistsTargetsAndSchedules() {
        UUID recipeId = UUID.randomUUID();
        ScraperRecipe recipe = new ScraperRecipe();
        recipe.setYamlContent("name: Sample\npage:{fields:[{name: title, selectors:[h1]}]}");
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe));

        jobService.createJob("Job A", recipeId, List.of("https://example.com"), null,
                new OptionsConfig(), "tester");

        verify(jobRepository).save(any(ScrapeJob.class));
        ArgumentCaptor<List<ScrapeTarget>> captor = ArgumentCaptor.forClass(List.class);
        verify(targetRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        verify(schedulerService).scheduleJob(any(ScrapeJob.class));
    }
}
