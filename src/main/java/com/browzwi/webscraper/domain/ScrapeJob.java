package com.browzwi.webscraper.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a scheduled scraping job that defines what content to scrape
 * and when to execute the scraping operation.
 *
 * <p>Architectural rationale: This entity serves as the central point for
 * job scheduling and tracking, linking to a specific scraper recipe and
 * maintaining job execution state. It supports both one-time and recurring
 * scraping operations through its scheduleCron field.
 *
 * <p>Key constraints: Each job must be associated with a valid scraper recipe
 * and maintain its execution status for monitoring and logging purposes.
 *
 * @since 1.0
 */
@Entity
@Table(name = "scrape_jobs")
public class ScrapeJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "char(36)")
    private UUID id;

    @Column(nullable = false, length = 128)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", nullable = false)
    private ScraperRecipe recipe;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ScrapeJobStatus status = ScrapeJobStatus.PENDING;

    @Column(name = "schedule_cron", length = 64)
    private String scheduleCron;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    @Column(name = "requested_by", length = 64)
    private String requestedBy;

    @Column(name = "options_json", columnDefinition = "longtext")
    private String optionsJson;

    /**
     * Gets the unique identifier for this scraping job.
     *
     * @return the job's UUID identifier
     */
    public UUID getId() {
        return id;
    }

    /**
     * Gets the name of this scraping job.
     *
     * @return the job name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of this scraping job.
     *
     * @param name the job name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the scraper recipe associated with this job.
     *
     * @return the scraper recipe
     */
    public ScraperRecipe getRecipe() {
        return recipe;
    }

    /**
     * Sets the scraper recipe for this job.
     *
     * @param recipe the scraper recipe to associate with this job
     */
    public void setRecipe(ScraperRecipe recipe) {
        this.recipe = recipe;
    }

    /**
     * Gets the current status of this scraping job.
     *
     * @return the current job status
     */
    public ScrapeJobStatus getStatus() {
        return status;
    }

    /**
     * Sets the status of this scraping job.
     *
     * @param status the job status to set
     */
    public void setStatus(ScrapeJobStatus status) {
        this.status = status;
    }

    /**
     * Gets the cron expression for scheduling this job.
     *
     * @return the cron expression for recurring execution, or null for one-time jobs
     */
    public String getScheduleCron() {
        return scheduleCron;
    }

    /**
     * Sets the cron expression for scheduling this job.
     *
     * @param scheduleCron the cron expression for recurring execution
     */
    public void setScheduleCron(String scheduleCron) {
        this.scheduleCron = scheduleCron;
    }

    /**
     * Gets the timestamp when this job was created.
     *
     * @return the creation timestamp
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Gets the timestamp when this job was last updated.
     *
     * @return the last update timestamp
     */
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Gets the user who requested this scraping job.
     *
     * @return the requesting user's identifier
     */
    public String getRequestedBy() {
        return requestedBy;
    }

    /**
     * Sets the user who requested this scraping job.
     *
     * @param requestedBy the requesting user's identifier
     */
    public void setRequestedBy(String requestedBy) {
        this.requestedBy = requestedBy;
    }

    /**
     * Gets the JSON string containing job-specific options.
     *
     * @return the job options in JSON format
     */
    public String getOptionsJson() {
        return optionsJson;
    }

    /**
     * Sets the JSON string containing job-specific options.
     *
     * @param optionsJson the job options in JSON format
     */
    public void setOptionsJson(String optionsJson) {
        this.optionsJson = optionsJson;
    }
}
