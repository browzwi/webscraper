package com.browzwi.webscraper.service.job;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.browzwi.webscraper.domain.ScrapeJob;
import com.browzwi.webscraper.domain.ScrapeJobStatus;
import com.browzwi.webscraper.domain.ScrapeResultData;
import com.browzwi.webscraper.domain.ScrapeTarget;
import com.browzwi.webscraper.domain.ScrapeTargetStatus;
import com.browzwi.webscraper.repository.ScrapeJobRepository;
import com.browzwi.webscraper.repository.ScrapeResultDataRepository;
import com.browzwi.webscraper.repository.ScrapeTargetRepository;
import com.browzwi.webscraper.scraper.model.OptionsConfig;
import com.browzwi.webscraper.scraper.model.RecipeConfig;
import com.browzwi.webscraper.scraper.service.ScraperEngine;
import com.browzwi.webscraper.scraper.service.ScraperEngine.ProgressListener;
import com.browzwi.webscraper.storage.FileStorageService;
import com.browzwi.webscraper.service.ScraperRecipeService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ScrapeQuartzJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(ScrapeQuartzJob.class);

    private final ScrapeJobRepository jobRepository;
    private final ScrapeTargetRepository targetRepository;
    private final ScrapeResultDataRepository resultRepository;
    private final ScraperRecipeService recipeService;
    private final ScraperEngine scraperEngine;
    private final FileStorageService storageService;
    private final ObjectMapper objectMapper;

    public ScrapeQuartzJob(ScrapeJobRepository jobRepository,
                           ScrapeTargetRepository targetRepository,
                           ScrapeResultDataRepository resultRepository,
                           ScraperRecipeService recipeService,
                           ScraperEngine scraperEngine,
                           FileStorageService storageService,
                           ObjectMapper objectMapper) {
        this.jobRepository = jobRepository;
        this.targetRepository = targetRepository;
        this.resultRepository = resultRepository;
        this.recipeService = recipeService;
        this.scraperEngine = scraperEngine;
        this.storageService = storageService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void execute(JobExecutionContext context) throws JobExecutionException {
        UUID jobId = UUID.fromString(context.getMergedJobDataMap().getString("jobId"));
        ScrapeJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
        job.setStatus(ScrapeJobStatus.RUNNING);
        jobRepository.save(job);

        try {
            RecipeConfig recipeConfig = recipeService.parse(job.getRecipe().getYamlContent());
            OptionsConfig overrides = readOptions(job.getOptionsJson());
            boolean jobFailed = false;

            List<ScrapeTarget> targets = targetRepository.findAllByJobId(jobId);
            for (ScrapeTarget target : targets) {
                target.setStatus(ScrapeTargetStatus.RUNNING);
                target.setStartedAt(Instant.now());
                targetRepository.save(target);
                try {
                    // Check if recipe has sub-pages for multi-page scraping
                    if (recipeConfig.getPage().getSubPages() != null && !recipeConfig.getPage().getSubPages().isEmpty()) {
                        // Handle multi-page scraping
                        var multiResult = scraperEngine.executeMultiPage(recipeConfig, target.getUrl(), overrides, ProgressListener.noop());
                        // For multi-page results, we'll store the combined structured data for now
                        // Detailed per-page storage would require additional schema changes
                        
                        storageService.storeRawHtml(job.getId(), target.getId(), ""); // Multi-page doesn't have a single raw HTML
                        storageService.storeProcessedHtml(job.getId(), target.getId(), ""); // Multi-page doesn't have a single processed HTML

                        ScrapeResultData data = resultRepository.findByTargetId(target.getId())
                                .orElseGet(() -> {
                                    ScrapeResultData d = new ScrapeResultData();
                                    d.setTarget(target);
                                    return d;
                                });
                        data.setDataJson(objectMapper.writeValueAsString(multiResult.combinedStructuredData()));
                        data.setProgressJson(writeProgress(multiResult.progressSteps()));
                        resultRepository.save(data);
                    } else {
                        // Handle single-page scraping
                        var result = scraperEngine.execute(recipeConfig, target.getUrl(), overrides, ProgressListener.noop());
                        storageService.storeRawHtml(job.getId(), target.getId(), result.rawHtml());
                        storageService.storeProcessedHtml(job.getId(), target.getId(), result.processedHtml());

                        ScrapeResultData data = resultRepository.findByTargetId(target.getId())
                                .orElseGet(() -> {
                                    ScrapeResultData d = new ScrapeResultData();
                                    d.setTarget(target);
                                    return d;
                                });
                        data.setDataJson(objectMapper.writeValueAsString(result.structuredData()));
                        data.setProgressJson(writeProgress(result.progressSteps()));
                        resultRepository.save(data);
                    }

                    target.setStatus(ScrapeTargetStatus.COMPLETED);
                    target.setFinishedAt(Instant.now());
                    targetRepository.save(target);
                } catch (Exception ex) {
                    log.error("Target {} failed", target.getId(), ex);
                    jobFailed = true;
                    target.setStatus(ScrapeTargetStatus.FAILED);
                    target.setFinishedAt(Instant.now());
                    target.setErrorMessage(ex.getMessage());
                    targetRepository.save(target);
                }
            }

            job.setStatus(jobFailed ? ScrapeJobStatus.FAILED : ScrapeJobStatus.COMPLETED);
            jobRepository.save(job);
        } catch (Exception e) {
            job.setStatus(ScrapeJobStatus.FAILED);
            jobRepository.save(job);
            throw new JobExecutionException("Scrape job failed", e);
        }
    }

    private OptionsConfig readOptions(String optionsJson) {
        if (optionsJson == null || optionsJson.isBlank()) {
            return new OptionsConfig();
        }
        try {
            return objectMapper.readValue(optionsJson, OptionsConfig.class);
        } catch (Exception ex) {
            log.warn("Unable to parse options JSON, proceeding with defaults", ex);
            return new OptionsConfig();
        }
    }

    private String writeProgress(List<String> steps) {
        if (steps == null || steps.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(steps);
        } catch (JsonProcessingException ex) {
            log.warn("Unable to serialize progress steps", ex);
            return null;
        }
    }
}
