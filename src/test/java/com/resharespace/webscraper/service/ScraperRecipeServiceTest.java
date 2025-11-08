package com.browzwi.webscraper.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.browzwi.webscraper.domain.ScraperRecipe;
import com.browzwi.webscraper.repository.ScraperRecipeRepository;
import com.browzwi.webscraper.scraper.model.RecipeConfig;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScraperRecipeServiceTest {

    @Mock
    private ScraperRecipeRepository repository;

    @InjectMocks
    private ScraperRecipeService service;

    private static final String VALID_YAML = """
            name: Test Recipe
            page:
              fields:
                - name: title
                  selectors:
                    - h1
                  source: TEXT
            """;

    @Test
    void parseReturnsConfig() {
        RecipeConfig config = service.parse(VALID_YAML);
        assertThat(config.getName()).isEqualTo("Test Recipe");
        assertThat(config.getPage().getFields()).hasSize(1);
    }

    @Test
    void validateThrowsForMissingFields() {
        String invalidYaml = """
                name: Bad Recipe
                page: {}
                """;
        assertThrows(ScraperRecipeService.InvalidRecipeException.class,
                () -> service.validateYaml(invalidYaml));
    }

    @Test
    void updateCopiesValues() {
        UUID id = UUID.randomUUID();
        var recipe = new ScraperRecipe();
        recipe.setName("Existing");
        recipe.setKey("existing");
        recipe.setYamlContent(VALID_YAML);
        recipe.setEnabled(true);
        when(repository.findById(id)).thenReturn(Optional.of(recipe));

        var update = new ScraperRecipe();
        update.setName("Updated");
        update.setKey("updated");
        update.setYamlContent(VALID_YAML);
        update.setEnabled(false);

        when(repository.save(recipe)).thenReturn(recipe);

        ScraperRecipe saved = service.update(id, update);
        assertThat(saved.getName()).isEqualTo("Updated");
        assertThat(saved.getKey()).isEqualTo("updated");
    }
}
