package com.browzwi.webscraper.service;

import com.browzwi.webscraper.domain.ScraperRecipe;
import com.browzwi.webscraper.repository.ScraperRecipeRepository;
import com.browzwi.webscraper.scraper.model.FieldConfig;
import com.browzwi.webscraper.scraper.model.PageConfig;
import com.browzwi.webscraper.scraper.model.RecipeConfig;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

@Service
public class ScraperRecipeService {

    private final ScraperRecipeRepository repository;
    private final Yaml yaml;

    public ScraperRecipeService(ScraperRecipeRepository repository) {
        this.repository = repository;
        var loaderOptions = new LoaderOptions();
        loaderOptions.setAllowDuplicateKeys(false);
        this.yaml = new Yaml(new Constructor(RecipeConfig.class, loaderOptions));
    }

    public RecipeConfig parse(String yamlContent) {
        try {
            Object raw = yaml.load(yamlContent);
            if (raw == null) {
                throw new InvalidRecipeException("YAML document is empty");
            }
            if (!(raw instanceof RecipeConfig config)) {
                throw new InvalidRecipeException("YAML does not describe a recipe");
            }
            return config;
        } catch (RuntimeException ex) {
            throw new InvalidRecipeException("Failed to parse YAML", ex);
        }
    }

    public void validateYaml(String yamlContent) {
        var config = parse(yamlContent);
        validate(config);
    }

    public ScraperRecipe create(ScraperRecipe recipe) {
        validateYaml(recipe.getYamlContent());
        return repository.save(recipe);
    }

    public ScraperRecipe update(UUID id, ScraperRecipe updated) {
        var existing = repository.findById(id)
                .orElseThrow(() -> new RecipeNotFoundException(id));
        validateYaml(updated.getYamlContent());
        existing.setName(updated.getName());
        existing.setKey(updated.getKey());
        existing.setDescription(updated.getDescription());
        existing.setEnabled(updated.isEnabled());
        existing.setYamlContent(updated.getYamlContent());
        existing.setCreatedBy(updated.getCreatedBy());
        return repository.save(existing);
    }

    public List<ScraperRecipe> list() {
        return repository.findAll();
    }

    public ScraperRecipe get(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecipeNotFoundException(id));
    }

    public void delete(UUID id) {
        repository.deleteById(id);
    }

    private void validate(RecipeConfig config) {
        if (config.getName() == null || config.getName().isBlank()) {
            throw new InvalidRecipeException("Recipe name is required");
        }
        var page = config.getPage();
        if (page == null) {
            throw new InvalidRecipeException("Page section is required");
        }
        var fields = page.getFields();
        if (fields == null || fields.isEmpty()) {
            throw new InvalidRecipeException("At least one field must be defined");
        }
        fields.forEach(this::validateField);
    }

    private void validateField(FieldConfig field) {
        if (field.getName() == null || field.getName().isBlank()) {
            throw new InvalidRecipeException("Field name is required");
        }
        if (field.getSelectors() == null || field.getSelectors().isEmpty()) {
            throw new InvalidRecipeException("Field %s requires selectors".formatted(field.getName()));
        }
        if (field.getSource() == null) {
            throw new InvalidRecipeException("Field %s source is required".formatted(field.getName()));
        }
        if (field.getSource().name().equals("ATTR") && (field.getAttributeName() == null || field.getAttributeName().isBlank())) {
            throw new InvalidRecipeException("Field %s requires attributeName for ATTR source".formatted(field.getName()));
        }
    }

    public static class InvalidRecipeException extends RuntimeException {
        public InvalidRecipeException(String message) {
            super(message);
        }

        public InvalidRecipeException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class RecipeNotFoundException extends RuntimeException {
        public RecipeNotFoundException(UUID id) {
            super("Recipe %s not found".formatted(id));
        }
    }
}
