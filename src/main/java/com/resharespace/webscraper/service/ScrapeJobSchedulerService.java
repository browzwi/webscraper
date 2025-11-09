package com.browzwi.webscraper.service;

import com.browzwi.webscraper.domain.ScrapeJob;
import com.browzwi.webscraper.service.job.ScrapeQuartzJob;
import java.time.Instant;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Service for managing the scheduling and execution of scraping jobs using Quartz scheduler.
 * This service handles the creation, scheduling, and triggering of Quartz jobs for scraping operations.
 *
 * @since 1.0
 */
@Service
public class ScrapeJobSchedulerService {

    private final Scheduler scheduler;

    /**
     * Constructor for ScrapeJobSchedulerService with required dependencies.
     *
     * @param scheduler the Quartz scheduler instance
     */
    public ScrapeJobSchedulerService(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    /**
     * Schedules a scraping job in the Quartz scheduler.
     * If a job with the same ID already exists in the scheduler, it will be replaced.
     * For scheduled jobs, uses the cron expression from the job; for one-time jobs, runs immediately.
     *
     * @param job the scraping job to schedule
     * @throws SchedulingException if there's an error scheduling the job
     */
    public void scheduleJob(ScrapeJob job) {
        try {
            JobKey key = JobKey.jobKey(jobKey(job.getId()));
            JobDetail detail = JobBuilder.newJob(ScrapeQuartzJob.class)
                    .withIdentity(key)
                    .usingJobData("jobId", job.getId().toString())
                    .build();

            Trigger trigger = buildTrigger(job, key);
            if (scheduler.checkExists(detail.getKey())) {
                scheduler.deleteJob(detail.getKey());
            }
            scheduler.scheduleJob(detail, trigger);
        } catch (SchedulerException e) {
            throw new SchedulingException("Failed to schedule job " + job.getId(), e);
        }
    }

    /**
     * Triggers a scraping job to run immediately.
     * If the job isn't already scheduled, it will be scheduled first before triggering.
     *
     * @param job the scraping job to trigger
     * @throws SchedulingException if there's an error triggering the job
     */
    public void triggerJob(ScrapeJob job) {
        try {
            JobKey key = JobKey.jobKey(jobKey(job.getId()));
            if (!scheduler.checkExists(key)) {
                scheduleJob(job);
            }
            scheduler.triggerJob(key);
        } catch (SchedulerException e) {
            throw new SchedulingException("Failed to trigger job " + job.getId(), e);
        }
    }

    /**
     * Finds the next scheduled execution time for a specific job.
     *
     * @param jobId the ID of the job to check
     * @return an optional containing the next execution time if scheduled, or empty if not
     * @throws SchedulingException if there's an error determining the next fire time
     */
    public Optional<Instant> findNextFireTime(UUID jobId) {
        JobKey key = JobKey.jobKey(jobKey(jobId));
        try {
            if (!scheduler.checkExists(key)) {
                return Optional.empty();
            }
            List<? extends Trigger> triggers = scheduler.getTriggersOfJob(key);
            return triggers.stream()
                    .map(Trigger::getNextFireTime)
                    .filter(date -> date != null)
                    .map(Date::toInstant)
                    .min(Comparator.naturalOrder());
        } catch (SchedulerException e) {
            throw new SchedulingException("Failed to determine next fire time for job " + jobId, e);
        }
    }

    /**
     * Builds a Quartz trigger for the specified job.
     * Uses cron scheduling if the job has a cron expression, otherwise schedules to start immediately.
     *
     * @param job the scraping job for which to build the trigger
     * @param key the job key to associate with the trigger
     * @return a configured Quartz trigger
     */
    private Trigger buildTrigger(com.browzwi.webscraper.domain.ScrapeJob job, JobKey key) {
        TriggerBuilder<Trigger> builder = TriggerBuilder.newTrigger()
                .withIdentity("trigger-" + job.getId())
                .forJob(key);
        if (StringUtils.hasText(job.getScheduleCron())) {
            builder.withSchedule(CronScheduleBuilder.cronSchedule(job.getScheduleCron()));
        } else {
            builder.startNow();
        }
        return builder.build();
    }

    /**
     * Generates a consistent job key name for the specified job ID.
     *
     * @param jobId the ID of the job
     * @return a string representing the job key
     */
    private String jobKey(UUID jobId) {
        return "scrape-job-" + jobId;
    }

    /**
     * Exception thrown when there are issues with job scheduling operations.
     *
     * @since 1.0
     */
    public static class SchedulingException extends RuntimeException {
        /**
         * Constructs a scheduling exception with the specified message and cause.
         *
         * @param message the detail message
         * @param cause the cause of the exception
         */
        public SchedulingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
