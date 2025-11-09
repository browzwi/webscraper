package com.browzwi.webscraper.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a scraper recipe that defines how to extract data from web pages.
 *
 * <p>Architectural rationale: This entity stores YAML-based configurations that
 * specify scraping rules, selectors, and data extraction patterns. It allows
 * users to define reusable scraping templates without writing code.
 *
 * <p>Key constraints: Each recipe must have a unique key for identification
 * and reference from jobs. The YAML content must conform to the expected
 * scraper configuration format.
 *
 * @since 1.0
 */
@Entity
@Table(name = "scraper_recipes")
public class ScraperRecipe {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "char(36)")
    private UUID id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(name = "recipe_key", nullable = false, unique = true, length = 128)
    private String key;

    @Column(length = 512)
    private String description;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "yaml_content", nullable = false, columnDefinition = "longtext")
    private String yamlContent;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    @Column(name = "created_by", length = 64)
    private String createdBy;

    /**
     * Gets the unique identifier for this scraper recipe.
     *
     * @return the recipe's UUID identifier
     */
    public UUID getId() {
        return id;
    }

    /**
     * Gets the display name of this scraper recipe.
     *
     * @return the recipe name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the display name of this scraper recipe.
     *
     * @param name the recipe name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the unique key for this scraper recipe.
     *
     * @return the recipe key used for identification and reference
     */
    public String getKey() {
        return key;
    }

    /**
     * Sets the unique key for this scraper recipe.
     *
     * @param key the unique key to identify this recipe
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * Gets the description of this scraper recipe.
     *
     * @return the recipe description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of this scraper recipe.
     *
     * @param description the recipe description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Checks if this scraper recipe is enabled for use.
     *
     * @return true if the recipe can be used in scraping jobs, false otherwise
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets whether this scraper recipe is enabled for use.
     *
     * @param enabled true to enable the recipe, false to disable it
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Gets the YAML content that defines the scraping configuration.
     *
     * @return the YAML string containing scraping rules and selectors
     */
    public String getYamlContent() {
        return yamlContent;
    }

    /**
     * Sets the YAML content that defines the scraping configuration.
     *
     * @param yamlContent the YAML string containing scraping rules and selectors
     */
    public void setYamlContent(String yamlContent) {
        this.yamlContent = yamlContent;
    }

    /**
     * Gets the timestamp when this recipe was created.
     *
     * @return the creation timestamp
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Gets the timestamp when this recipe was last updated.
     *
     * @return the last update timestamp
     */
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Gets the user who created this scraper recipe.
     *
     * @return the creator's identifier
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * Sets the user who created this scraper recipe.
     *
     * @param createdBy the creator's identifier
     */
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
