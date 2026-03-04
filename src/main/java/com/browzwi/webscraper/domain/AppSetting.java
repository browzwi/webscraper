package com.browzwi.webscraper.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Represents an application setting that can be configured at runtime.
 *
 * <p>Architectural rationale: This entity allows the application to store
 * and retrieve configuration values that can be changed without redeployment,
 * supporting features like scraper engine selection and other operational parameters.
 *
 * <p>Key constraints: Each setting has a unique key and a string value,
 * with settings being identified by their key rather than an auto-generated ID.
 *
 * @since 1.0
 */
@Entity
@Table(name = "app_settings")
public class AppSetting {

    @Id
    @Column(name = "setting_key", length = 64)
    private String key;

    @Column(name = "setting_value", length = 255)
    private String value;

    /**
     * Default constructor for JPA.
     */
    public AppSetting() {
    }

    /**
     * Constructs an application setting with the specified key and value.
     *
     * @param key the unique key identifying the setting
     * @param value the value of the setting
     */
    public AppSetting(String key, String value) {
        this.key = key;
        this.value = value;
    }

    /**
     * Gets the unique key identifying this application setting.
     *
     * @return the setting key
     */
    public String getKey() {
        return key;
    }

    /**
     * Sets the unique key identifying this application setting.
     *
     * @param key the setting key to set
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * Gets the value of this application setting.
     *
     * @return the setting value
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the value of this application setting.
     *
     * @param value the setting value to set
     */
    public void setValue(String value) {
        this.value = value;
    }
}
