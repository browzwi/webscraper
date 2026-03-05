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
 * Represents a specific URL target for a scraping job, tracking the execution state
 * and results of scraping a single web page.
 *
 * <p>Architectural rationale: This entity allows a single job to scrape multiple
 * URLs and track each target's status individually, enabling fine-grained monitoring
 * and error handling of individual targets within a job.
 *
 * <p>Key constraints: Each target is associated with exactly one scraping job
 * and maintains its own execution timeline and result storage path.
 *
 * @since 1.0
 */
@Entity
@Table(name = "scrape_targets")
public class ScrapeTarget {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "char(36)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private ScrapeJob job;

    @Column(nullable = false, length = 2048)
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ScrapeTargetStatus status = ScrapeTargetStatus.PENDING;

    @Column(name = "result_path", length = 512)
    private String resultPath;

    @Column(name = "raw_file_name", length = 255)
    private String rawFileName;

    @Column(name = "processed_file_name", length = 255)
    private String processedFileName;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    /**
     * Gets the unique identifier for this scrape target.
     *
     * @return the target's UUID identifier
     */
    public UUID getId() {
        return id;
    }

    /**
     * Gets the scraping job that this target belongs to.
     *
     * @return the parent scraping job
     */
    public ScrapeJob getJob() {
        return job;
    }

    /**
     * Sets the scraping job that this target belongs to.
     *
     * @param job the parent scraping job to associate with this target
     */
    public void setJob(ScrapeJob job) {
        this.job = job;
    }

    /**
     * Gets the URL to be scraped for this target.
     *
     * @return the target URL string
     */
    public String getUrl() {
        return url;
    }

    /**
     * Sets the URL to be scraped for this target.
     *
     * @param url the target URL string to set
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Gets the current status of this scrape target.
     *
     * @return the current target status
     */
    public ScrapeTargetStatus getStatus() {
        return status;
    }

    /**
     * Sets the status of this scrape target.
     *
     * @param status the target status to set
     */
    public void setStatus(ScrapeTargetStatus status) {
        this.status = status;
    }

    /**
     * Gets the file system path where results for this target are stored.
     *
     * @return the result storage path
     */
    public String getResultPath() {
        return resultPath;
    }

    /**
     * Sets the file system path where results for this target are stored.
     *
     * @param resultPath the result storage path to set
     */
    public void setResultPath(String resultPath) {
        this.resultPath = resultPath;
    }

    /**
     * Gets the filename of the raw HTML content for this target.
     *
     * @return the raw HTML filename
     */
    public String getRawFileName() {
        return rawFileName;
    }

    /**
     * Sets the filename of the raw HTML content for this target.
     *
     * @param rawFileName the raw HTML filename to set
     */
    public void setRawFileName(String rawFileName) {
        this.rawFileName = rawFileName;
    }

    /**
     * Gets the filename of the processed HTML content for this target.
     *
     * @return the processed HTML filename
     */
    public String getProcessedFileName() {
        return processedFileName;
    }

    /**
     * Sets the filename of the processed HTML content for this target.
     *
     * @param processedFileName the processed HTML filename to set
     */
    public void setProcessedFileName(String processedFileName) {
        this.processedFileName = processedFileName;
    }

    /**
     * Gets the timestamp when scraping for this target started.
     *
     * @return the start timestamp, or null if not started
     */
    public Instant getStartedAt() {
        return startedAt;
    }

    /**
     * Sets the timestamp when scraping for this target started.
     *
     * @param startedAt the start timestamp to set
     */
    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    /**
     * Gets the timestamp when scraping for this target finished.
     *
     * @return the finish timestamp, or null if not finished
     */
    public Instant getFinishedAt() {
        return finishedAt;
    }

    /**
     * Sets the timestamp when scraping for this target finished.
     *
     * @param finishedAt the finish timestamp to set
     */
    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }

    /**
     * Gets the error message if scraping this target failed.
     *
     * @return the error message, or null if no error occurred
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Sets the error message if scraping this target failed.
     *
     * @param errorMessage the error message to set
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
