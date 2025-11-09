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

@Service
public class ScrapeJobSchedulerService {

    private final Scheduler scheduler;

    public ScrapeJobSchedulerService(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

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

    private String jobKey(UUID jobId) {
        return "scrape-job-" + jobId;
    }

    public static class SchedulingException extends RuntimeException {
        public SchedulingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
