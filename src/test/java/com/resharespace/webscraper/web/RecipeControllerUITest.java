package com.browzwi.webscraper.web;

import com.browzwi.webscraper.domain.ScraperRecipe;
import com.browzwi.webscraper.repository.ScraperRecipeRepository;
import com.browzwi.webscraper.service.ScraperRecipeService;
import com.browzwi.webscraper.service.test.RecipeTestSessionService;
import com.browzwi.webscraper.web.dto.RecipeForm;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RecipeController.class)
@DisplayName("Recipe Controller UI Tests")
class RecipeControllerUITest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ScraperRecipeService recipeService;

    @MockBean
    private ScraperRecipeRepository recipeRepository;

    @MockBean
    private RecipeTestSessionService recipeTestSessionService;

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should render recipes list page")
    void shouldRenderRecipesListPage() throws Exception {
        // Given
        ScraperRecipe recipe = createMockRecipe();
        when(recipeRepository.findAll()).thenReturn(List.of(recipe));

        // When & Then
        mockMvc.perform(get("/recipes"))
                .andExpect(status().isOk())
                .andExpect(view().name("recipes/list"))
                .andExpect(model().attributeExists("recipes"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Recipes")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Recipe Library")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should render recipe form with dark theme")
    void shouldRenderRecipeFormWithDarkTheme() throws Exception {
        // When & Then
        mockMvc.perform(get("/recipes/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("recipes/form"))
                .andExpect(model().attributeExists("recipe"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("card")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("form-input-dark")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should display recipe table with status badges")
    void shouldDisplayRecipeTableWithStatusBadges() throws Exception {
        // Given
        ScraperRecipe recipe = createMockRecipe();
        recipe.setEnabled(true);
        when(recipeRepository.findAll()).thenReturn(List.of(recipe));

        // When & Then
        mockMvc.perform(get("/recipes"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("badge")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("badge-success")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should have HTMX search filter")
    void shouldHaveHtmxSearchFilter() throws Exception {
        // Given
        when(recipeRepository.findAll()).thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/recipes"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("hx-get")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("recipes/filter")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should render edit form with existing recipe data")
    void shouldRenderEditFormWithExistingRecipeData() throws Exception {
        // Given
        UUID recipeId = UUID.randomUUID();
        ScraperRecipe recipe = createMockRecipe();
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe));

        // When & Then
        mockMvc.perform(get("/recipes/" + recipeId))
                .andExpect(status().isOk())
                .andExpect(view().name("recipes/form"))
                .andExpect(model().attributeExists("recipe"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should display example recipes section")
    void shouldDisplayExampleRecipesSection() throws Exception {
        // When & Then
        mockMvc.perform(get("/recipes/new"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Example Recipes")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("lightbulb")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should have action buttons with Material icons")
    void shouldHaveActionButtonsWithMaterialIcons() throws Exception {
        // Given
        ScraperRecipe recipe = createMockRecipe();
        when(recipeRepository.findAll()).thenReturn(List.of(recipe));

        // When & Then
        mockMvc.perform(get("/recipes"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("edit")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("play_arrow")));
    }

    private ScraperRecipe createMockRecipe() {
        ScraperRecipe recipe = new ScraperRecipe();
        ReflectionTestUtils.setField(recipe, "id", UUID.randomUUID());
        recipe.setName("Test Recipe");
        recipe.setKey("test-recipe");
        recipe.setDescription("Test Description");
        recipe.setYamlContent("name: Test Recipe");
        recipe.setEnabled(true);
        return recipe;
    }
}
