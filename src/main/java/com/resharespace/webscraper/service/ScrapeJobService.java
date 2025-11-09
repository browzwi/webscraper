package com.browzwi.webscraper.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.browzwi.webscraper.domain.ScrapeJob;
import com.browzwi.webscraper.domain.ScrapeJobStatus;
import com.browzwi.webscraper.domain.ScrapeResultData;
import com.browzwi.webscraper.domain.ScrapeTarget;
import com.browzwi.webscraper.domain.ScrapeTargetStatus;
import com.browzwi.webscraper.domain.ScraperRecipe;
import com.browzwi.webscraper.repository.ScrapeJobRepository;
import com.browzwi.webscraper.repository.ScrapeResultDataRepository;
import com.browzwi.webscraper.repository.ScrapeTargetRepository;
import com.browzwi.webscraper.repository.ScraperRecipeRepository;
import com.browzwi.webscraper.scraper.model.OptionsConfig;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Service for managing scraping jobs, including creation, scheduling, and result retrieval.
 *
 * <p>Architectural rationale: This service manages the complete lifecycle of scraping jobs,
 * coordinating with the scheduler, repositories, and other services to create, execute,
 * and track scraping operations. It handles job persistence and ensures targets are
 * properly associated with jobs.
 *
 * <p>Key constraints: Jobs must be associated with valid recipes and contain at least one URL.
 * The service handles transaction management for job creation operations.
 *
 * @since 1.0
 */
@Service
public class ScrapeJobService {

    private final ScrapeJobRepository jobRepository;
    private final ScrapeTargetRepository targetRepository;
    private final ScrapeResultDataRepository resultDataRepository;
    private final ScraperRecipeRepository recipeRepository;
    private final ScrapeJobSchedulerService schedulerService;
    private final ObjectMapper objectMapper;

    /**
     * Constructor for ScrapeJobService with required dependencies.
     *
     * @param jobRepository repository for managing scraping jobs
     * @param targetRepository repository for managing scraping targets
     * @param resultDataRepository repository for managing scraping results
     * @param recipeRepository repository for managing scraper recipes
     * @param schedulerService service for job scheduling
     * @param objectMapper object mapper for JSON serialization
     */
    public ScrapeJobService(ScrapeJobRepository jobRepository,
                            ScrapeTargetRepository targetRepository,
                            ScrapeResultDataRepository resultDataRepository,
                            ScraperRecipeRepository recipeRepository,
                            ScrapeJobSchedulerService schedulerService,
                            ObjectMapper objectMapper) {
        this.jobRepository = jobRepository;
        this.targetRepository = targetRepository;
        this.resultDataRepository = resultDataRepository;
        this.recipeRepository = recipeRepository;
        this.schedulerService = schedulerService;
        this.objectMapper = objectMapper;
    }

    /**
     * Creates a new scraping job with the specified parameters.
     *
     * <p>Implementation rationale: This method ensures transactional consistency
     * by saving the job and its targets together. It validates that required
     * data is present before creating the job and schedules it for execution.
     *
     * @param name the display name for the job
     * @param recipeId the ID of the scraper recipe to use
     * @param urls the list of URLs to scrape
     * @param cronExpression the cron expression for scheduling (optional)
     * @param overrides optional configuration overrides for the scraping operation
     * @param requestedBy the user who initiated the job
     * @return the created scraping job
     * @throws IllegalArgumentException if the recipe doesn't exist or no URLs are provided
     */
    @Transactional
    public ScrapeJob createJob(String name,
                               UUID recipeId,
                               List<String> urls,
                               String cronExpression,
                               OptionsConfig overrides,
                               String requestedBy) {
        ScraperRecipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new IllegalArgumentException("Recipe not found"));
        ScrapeJob job = new ScrapeJob();
        job.setName(name);
        job.setRecipe(recipe);
        job.setStatus(ScrapeJobStatus.PENDING);
        job.setScheduleCron(StringUtils.hasText(cronExpression) ? cronExpression : null);
        job.setRequestedBy(requestedBy);
        job.setOptionsJson(writeOptions(overrides));
        jobRepository.save(job);

        List<ScrapeTarget> targets = new ArrayList<>();
        urls.stream()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .forEach(url -> {
                    ScrapeTarget target = new ScrapeTarget();
                    target.setJob(job);
                    target.setUrl(url);
                    target.setStatus(ScrapeTargetStatus.PENDING);
                    targets.add(target);
                });
        if (targets.isEmpty()) {
            throw new IllegalArgumentException("At least one URL is required");
        }
        targetRepository.saveAll(targets);
        schedulerService.scheduleJob(job);
        return job;
    }

    /**
     * Retrieves all scraping jobs ordered by creation date (newest first).
     *
     * @return a list of all scraping jobs
     */
    public List<ScrapeJob> listJobs() {
        return jobRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * Retrieves a specific scraping job by its ID, including its recipe details.
     *
     * @param id the UUID of the job to retrieve
     * @return the scraping job with the specified ID
     * @throws IllegalArgumentException if the job doesn't exist
     */
    public ScrapeJob getJob(UUID id) {
        return jobRepository.findWithRecipeById(id)
                .orElseThrow(() -> new IllegalArgumentException("Job not found"));
    }

    /**
     * Retrieves all scraping targets associated with a specific job.
     *
     * @param jobId the UUID of the job whose targets to retrieve
     * @return a list of scraping targets for the specified job
     */
    public List<ScrapeTarget> listTargets(UUID jobId) {
        return targetRepository.findAllByJobId(jobId);
    }

    /**
     * Retrieves a specific scraping target by its ID.
     *
     * @param targetId the UUID of the target to retrieve
     * @return the scraping target with the specified ID
     * @throws IllegalArgumentException if the target doesn't exist
     */
    public ScrapeTarget getTarget(UUID targetId) {
        return targetRepository.findById(targetId)
                .orElseThrow(() -> new IllegalArgumentException("Target not found"));
    }

    /**
     * Finds the scraping result data for a specific target.
     *
     * @param targetId the UUID of the target to find results for
     * @return an optional containing the result data if found, or empty if not
     */
    public Optional<ScrapeResultData> findResult(UUID targetId) {
        return resultDataRepository.findByTargetId(targetId);
    }
    
    /**
     * Finds the latest scraping result data for a specific target.
     *
     * @param targetId the UUID of the target to find results for
     * @return an optional containing the latest result data if found, or empty if not
     */
    public Optional<ScrapeResultData> findLatestResultByTargetId(UUID targetId) {
        return resultDataRepository.findByTargetId(targetId);
    }

    /**
     * Finds the next scheduled execution time for a specific job.
     *
     * @param jobId the UUID of the job to check
     * @return an optional containing the next execution time if scheduled, or empty if not
     */
    public Optional<Instant> findNextRun(UUID jobId) {
        return schedulerService.findNextFireTime(jobId);
    }

    /**
     * Triggers a job to run immediately, bypassing its scheduled time.
     *
     * @param jobId the UUID of the job to run now
     * @throws IllegalArgumentException if the job doesn't exist
     */
    public void runJobNow(UUID jobId) {
        ScrapeJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found"));
        schedulerService.triggerJob(job);
    }

    /**
     * Converts an options configuration to JSON string for storage.
     *
     * <p>Implementation rationale: This private method handles the serialization
     * of options configuration to JSON, providing consistent error handling
     * if serialization fails.
     *
     * @param overrides the options configuration to serialize (may be null)
     * @return the JSON string representation of the options, or null if the input was null
     * @throws IllegalArgumentException if serialization fails
     */
    private String writeOptions(OptionsConfig overrides) {
        if (overrides == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(overrides);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Unable to persist options", e);
        }
    }
}
