package com.browzwi.webscraper.web.dto;

import com.browzwi.webscraper.domain.ScraperRecipe;
import jakarta.validation.constraints.NotBlank;

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

    public ScraperRecipe toEntity() {
        ScraperRecipe recipe = new ScraperRecipe();
        recipe.setName(name);
        recipe.setKey(key);
        recipe.setDescription(description);
        recipe.setEnabled(enabled);
        recipe.setYamlContent(yamlContent);
        return recipe;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getYamlContent() {
        return yamlContent;
    }

    public void setYamlContent(String yamlContent) {
        this.yamlContent = yamlContent;
    }
}
