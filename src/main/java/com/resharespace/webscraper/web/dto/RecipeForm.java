package com.browzwi.webscraper.web.dto;

import com.browzwi.webscraper.domain.ScraperRecipe;
import jakarta.validation.constraints.NotBlank;

/**
 * Data transfer object for scraper recipe form data.
 * Contains the fields for creating or updating a scraper recipe through the web interface.
 *
 * @since 1.0
 */
public class RecipeForm {

    private String id;
    @NotBlank(message = "Name is required")
    private String name;
    @NotBlank(message = "Key is required")
    private String key;
    private String description;
    private boolean enabled = true;
    @NotBlank(message = "YAML content cannot be empty")
    private String yamlContent;

    /**
     * Creates a RecipeForm from a ScraperRecipe entity.
     *
     * @param recipe the ScraperRecipe entity to convert from
     * @return a RecipeForm populated with the entity's data
     */
    public static RecipeForm fromEntity(ScraperRecipe recipe) {
        RecipeForm form = new RecipeForm();
        form.setId(recipe.getId().toString());
        form.setName(recipe.getName());
        form.setKey(recipe.getKey());
        form.setDescription(recipe.getDescription());
        form.setEnabled(recipe.isEnabled());
        form.setYamlContent(recipe.getYamlContent());
        return form;
    }

    /**
     * Converts this form to a ScraperRecipe entity.
     *
     * @return a ScraperRecipe entity populated with the form's data
     */
    public ScraperRecipe toEntity() {
        ScraperRecipe recipe = new ScraperRecipe();
        recipe.setName(name);
        recipe.setKey(key);
        recipe.setDescription(description);
        recipe.setEnabled(enabled);
        recipe.setYamlContent(yamlContent);
        return recipe;
    }

    /**
     * Gets the recipe ID.
     *
     * @return the recipe ID as a string
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the recipe ID.
     *
     * @param id the recipe ID to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets the recipe name.
     *
     * @return the recipe name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the recipe name.
     *
     * @param name the recipe name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the recipe key.
     *
     * @return the recipe key
     */
    public String getKey() {
        return key;
    }

    /**
     * Sets the recipe key.
     *
     * @param key the recipe key to set
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * Gets the recipe description.
     *
     * @return the recipe description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the recipe description.
     *
     * @param description the recipe description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Checks if the recipe is enabled.
     *
     * @return true if the recipe is enabled, false otherwise
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets whether the recipe is enabled.
     *
     * @param enabled true to enable the recipe, false to disable it
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Gets the YAML content of the recipe.
     *
     * @return the YAML content string
     */
    public String getYamlContent() {
        return yamlContent;
    }

    /**
     * Sets the YAML content of the recipe.
     *
     * @param yamlContent the YAML content string to set
     */
    public void setYamlContent(String yamlContent) {
        this.yamlContent = yamlContent;
    }
}
