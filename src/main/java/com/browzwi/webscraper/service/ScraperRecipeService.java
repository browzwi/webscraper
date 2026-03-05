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

/**
 * Service for managing scraper recipes, including creation, validation, and persistence.
 *
 * <p>Architectural rationale: This service handles the validation and management of
 * YAML-based scraping recipes, ensuring they conform to the expected format and
 * providing CRUD operations for recipe management in the application.
 *
 * <p>Key constraints: Each recipe must have valid YAML content that conforms to
 * the RecipeConfig schema and includes required fields for proper scraping operation.
 *
 * @since 1.0
 */
@Service
public class ScraperRecipeService {

    private final ScraperRecipeRepository repository;
    private final Yaml yaml;

    /**
     * Constructor for ScraperRecipeService with required dependencies.
     *
     * @param repository repository for managing scraper recipes
     */
    public ScraperRecipeService(ScraperRecipeRepository repository) {
        this.repository = repository;
        var loaderOptions = new LoaderOptions();
        loaderOptions.setAllowDuplicateKeys(false);
        this.yaml = new Yaml(new Constructor(RecipeConfig.class, loaderOptions));
    }

    /**
     * Parses YAML content into a RecipeConfig object.
     *
     * <p>Implementation rationale: This method provides safe YAML parsing using
     * a configured SnakeYAML instance that prevents duplicate keys and ensures
     * type safety by using the RecipeConfig constructor.
     *
     * @param yamlContent the YAML string to parse
     * @return the parsed RecipeConfig object
     * @throws InvalidRecipeException if the YAML is invalid, empty, or doesn't represent a recipe
     */
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

    /**
     * Validates that the provided YAML content represents a valid recipe.
     *
     * @param yamlContent the YAML string to validate
     * @throws InvalidRecipeException if the YAML is invalid or doesn't represent a valid recipe
     */
    public void validateYaml(String yamlContent) {
        var config = parse(yamlContent);
        validate(config);
    }

    /**
     * Creates a new scraper recipe after validating its YAML content.
     *
     * @param recipe the recipe to create
     * @return the created recipe with assigned ID
     * @throws InvalidRecipeException if the recipe's YAML content is invalid
     */
    public ScraperRecipe create(ScraperRecipe recipe) {
        validateYaml(recipe.getYamlContent());
        return repository.save(recipe);
    }

    /**
     * Updates an existing scraper recipe with the provided values.
     *
     * @param id the ID of the recipe to update
     * @param updated the updated recipe values
     * @return the updated recipe
     * @throws RecipeNotFoundException if no recipe exists with the given ID
     * @throws InvalidRecipeException if the updated recipe's YAML content is invalid
     */
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

    /**
     * Retrieves all scraper recipes.
     *
     * @return a list of all available recipes
     */
    public List<ScraperRecipe> list() {
        return repository.findAll();
    }

    /**
     * Retrieves a specific scraper recipe by its ID.
     *
     * @param id the ID of the recipe to retrieve
     * @return the recipe with the specified ID
     * @throws RecipeNotFoundException if no recipe exists with the given ID
     */
    public ScraperRecipe get(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecipeNotFoundException(id));
    }

    /**
     * Deletes a scraper recipe by its ID.
     *
     * @param id the ID of the recipe to delete
     */
    public void delete(UUID id) {
        repository.deleteById(id);
    }

    /**
     * Validates a parsed recipe configuration for required fields and values.
     *
     * <p>Implementation rationale: This private method implements comprehensive
     * validation of recipe configuration to ensure it has the minimum required
     * elements for successful scraping.
     *
     * @param config the recipe configuration to validate
     * @throws InvalidRecipeException if the configuration is invalid
     */
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

    /**
     * Validates a single field configuration for required attributes.
     *
     * <p>Implementation rationale: This private method ensures each field in
     * the recipe has the required configuration values based on its source type.
     *
     * @param field the field configuration to validate
     * @throws InvalidRecipeException if the field configuration is invalid
     */
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

    /**
     * Exception thrown when a scraper recipe is invalid or malformed.
     *
     * <p>Business scenario: This exception occurs during recipe validation or parsing
     * when the YAML content doesn't conform to the expected schema or contains
     * invalid configuration values.
     *
     * @since 1.0
     */
    public static class InvalidRecipeException extends RuntimeException {
        /**
         * Constructs an exception with the specified detail message.
         *
         * @param message the detail message
         */
        public InvalidRecipeException(String message) {
            super(message);
        }

        /**
         * Constructs an exception with the specified detail message and cause.
         *
         * @param message the detail message
         * @param cause the cause of the exception
         */
        public InvalidRecipeException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Exception thrown when a scraper recipe with the specified ID is not found.
     *
     * <p>Business scenario: This exception occurs when trying to access a recipe
     * that doesn't exist in the repository, typically during update or retrieval operations.
     *
     * @since 1.0
     */
    public static class RecipeNotFoundException extends RuntimeException {
        /**
         * Constructs an exception for the specified recipe ID.
         *
         * @param id the ID of the recipe that was not found
         */
        public RecipeNotFoundException(UUID id) {
            super("Recipe %s not found".formatted(id));
        }
    }
}
