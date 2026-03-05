package com.browzwi.webscraper.web.dto;

/**
 * Data transfer object representing an example scraper recipe for user reference.
 * Contains example content that users can reference when creating their own recipes.
 *
 * @param id the unique identifier for this example
 * @param title the display title for this example
 * @param description a brief description of the example
 * @param yaml the YAML content of the example recipe
 * @since 1.0
 */
public record RecipeExample(String id, String title, String description, String yaml) {
}
