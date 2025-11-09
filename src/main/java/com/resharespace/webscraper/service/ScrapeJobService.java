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

@Service
public class ScrapeJobService {

    private final ScrapeJobRepository jobRepository;
    private final ScrapeTargetRepository targetRepository;
    private final ScrapeResultDataRepository resultDataRepository;
    private final ScraperRecipeRepository recipeRepository;
    private final ScrapeJobSchedulerService schedulerService;
    private final ObjectMapper objectMapper;

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

    public List<ScrapeJob> listJobs() {
        return jobRepository.findAllByOrderByCreatedAtDesc();
    }

    public ScrapeJob getJob(UUID id) {
        return jobRepository.findWithRecipeById(id)
                .orElseThrow(() -> new IllegalArgumentException("Job not found"));
    }

    public List<ScrapeTarget> listTargets(UUID jobId) {
        return targetRepository.findAllByJobId(jobId);
    }

    public ScrapeTarget getTarget(UUID targetId) {
        return targetRepository.findById(targetId)
                .orElseThrow(() -> new IllegalArgumentException("Target not found"));
    }

    public Optional<ScrapeResultData> findResult(UUID targetId) {
        return resultDataRepository.findByTargetId(targetId);
    }

    public Optional<Instant> findNextRun(UUID jobId) {
        return schedulerService.findNextFireTime(jobId);
    }

    public void runJobNow(UUID jobId) {
        ScrapeJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found"));
        schedulerService.triggerJob(job);
    }

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
