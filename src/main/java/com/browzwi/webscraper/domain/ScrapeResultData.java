package com.browzwi.webscraper.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Represents the extracted data results from a completed scraping operation.
 *
 * <p>Architectural rationale: This entity stores the structured data extracted
 * from web pages according to the scraper recipe configuration, allowing the
 * application to access and export the scraped information.
 *
 * <p>Key constraints: Result data is associated with a specific scrape target
 * and contains the extracted data in JSON format along with optional progress tracking.
 *
 * @since 1.0
 */
@Entity
@Table(name = "scrape_result_data")
public class ScrapeResultData {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "char(36)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_id", nullable = false)
    private ScrapeTarget target;

    @Column(name = "data_json", nullable = false, columnDefinition = "longtext")
    private String dataJson;

    @Column(name = "progress_json", columnDefinition = "longtext")
    private String progressJson;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    /**
     * Gets the unique identifier for this result data record.
     *
     * @return the result data's UUID identifier
     */
    public UUID getId() {
        return id;
    }

    /**
     * Gets the scrape target associated with this result data.
     *
     * @return the scrape target that produced this result
     */
    public ScrapeTarget getTarget() {
        return target;
    }

    /**
     * Sets the scrape target associated with this result data.
     *
     * @param target the scrape target that produced this result
     */
    public void setTarget(ScrapeTarget target) {
        this.target = target;
    }

    /**
     * Gets the JSON string containing the extracted data from scraping.
     *
     * @return the extracted data in JSON format
     */
    public String getDataJson() {
        return dataJson;
    }

    /**
     * Sets the JSON string containing the extracted data from scraping.
     *
     * @param dataJson the extracted data in JSON format
     */
    public void setDataJson(String dataJson) {
        this.dataJson = dataJson;
    }

    /**
     * Gets the JSON string containing progress information from scraping.
     *
     * @return the progress information in JSON format, or null if not available
     */
    public String getProgressJson() {
        return progressJson;
    }

    /**
     * Sets the JSON string containing progress information from scraping.
     *
     * @param progressJson the progress information in JSON format
     */
    public void setProgressJson(String progressJson) {
        this.progressJson = progressJson;
    }

    /**
     * Gets the timestamp when this result data was created.
     *
     * @return the creation timestamp
     */
    public Instant getCreatedAt() {
        return createdAt;
    }
}
