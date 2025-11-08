package com.browzwi.webscraper.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.browzwi.webscraper.domain.ScraperRecipe;
import com.browzwi.webscraper.repository.ScraperRecipeRepository;
import com.browzwi.webscraper.scraper.model.OptionsConfig;
import com.browzwi.webscraper.scraper.model.RecipeConfig;
import com.browzwi.webscraper.scraper.service.ScraperEngine;
import com.browzwi.webscraper.service.ScraperRecipeService;
import com.browzwi.webscraper.web.dto.RecipeExample;
import com.browzwi.webscraper.web.dto.RecipeForm;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/recipes")
public class RecipeController {

    private final ScraperRecipeService recipeService;
    private final ScraperRecipeRepository recipeRepository;
    private final ScraperEngine scraperEngine;
    private final ObjectMapper objectMapper;

    public RecipeController(ScraperRecipeService recipeService,
                            ScraperRecipeRepository recipeRepository,
                            ScraperEngine scraperEngine,
                            ObjectMapper objectMapper) {
        this.recipeService = recipeService;
        this.recipeRepository = recipeRepository;
        this.scraperEngine = scraperEngine;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public String list(Model model) {
        List<ScraperRecipe> recipes = recipeRepository.findAll();
        model.addAttribute("pageTitle", "Recipes");
        model.addAttribute("recipes", recipes);
        return "recipes/list";
    }

    @ModelAttribute("recipeExamples")
    public List<RecipeExample> recipeExamples() {
        return List.of(
                new RecipeExample("site-pages",
                        "Entire site and sub-pages",
                        "Follows internal links while staying under the same domain.",
                        """
                        name: Site and Sub-Pages
                        match:
                          domains:
                            - example.com
                        page:
                          hrefSelector: "a[href^='/']"
                          fields:
                            - name: title
                              selectors: ["title", "h1"]
                              source: TEXT
                            - name: body
                              selectors: ["article", "main"]
                              source: HTML
                        options:
                          stripCss: true
                          stripJs: true
                        """),
                new RecipeExample("sub-domains",
                        "Site and sub-domains",
                        "Targets multiple sub-domains with regex-based URL filters.",
                        """
                        name: Domain + Subdomains
                        match:
                          urlRegexes:
                            - "https?://([a-z0-9-]+\\.)*example.com/.*"
                        page:
                          contentRoot: "body"
                          fields:
                            - name: links
                              selectors: ["a[href]"]
                              source: ATTR
                              attributeName: href
                              multiple: true
                        options:
                          extractHrefsFirst: true
                        """),
                new RecipeExample("single-page",
                        "Single page with curated child pages",
                        "Scrapes a specific page and only the hand-picked sub pages you supply.",
                        """
                        name: Single Page With Children
                        match:
                          domains: [example.com]
                        page:
                          hrefSelector: "section.related a"
                          fields:
                            - name: headline
                              selectors: ["header h1"]
                              source: TEXT
                            - name: heroImage
                              selectors: ["img.hero"]
                              source: ATTR
                              attributeName: src
                        options:
                          removeAttributes: true
                        """
                ),
                new RecipeExample("facebook-page",
                        "Facebook page with about/photos",
                        "Targets any public Facebook page slug plus common child sections like /about or /photos.",
                        """
                        name: Facebook Public Page
                        match:
                          urlRegexes:
                            - 'https://www.facebook.com/[A-Za-z0-9\\.]+($|/.*)'
                        page:
                          hrefSelector: "nav a[href*='facebook.com/']"
                          fields:
                            - name: title
                              selectors: ["h1", "title"]
                              source: TEXT
                            - name: coverPhoto
                              selectors: ["img[alt~='cover']", "image"]
                              source: ATTR
                              attributeName: src
                            - name: sections
                              selectors: ["div[role='main'] section"]
                              source: HTML
                              multiple: true
                        options:
                          stripCss: true
                          stripJs: true
                          removeAttributes: true
                        """
                )
        );
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("pageTitle", "New Recipe");
        model.addAttribute("recipe", new RecipeForm());
        model.addAttribute("formAction", "/recipes");
        return "recipes/form";
    }

    @GetMapping("/{id}")
    public String editForm(@PathVariable UUID id, Model model, RedirectAttributes redirectAttributes) {
        return recipeRepository.findById(id)
                .map(recipe -> {
                    model.addAttribute("pageTitle", "Edit Recipe");
                    model.addAttribute("recipe", RecipeForm.fromEntity(recipe));
                    model.addAttribute("formAction", "/recipes/" + id);
                    return "recipes/form";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Recipe not found");
                    return "redirect:/recipes";
                });
    }

    @PostMapping
    public String saveRecipe(@Valid @ModelAttribute("recipe") RecipeForm form,
                             BindingResult bindingResult,
                             RedirectAttributes redirectAttributes,
                             Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("formAction", "/recipes");
            return "recipes/form";
        }
        recipeService.create(form.toEntity());
        redirectAttributes.addFlashAttribute("message", "Recipe created successfully");
        return "redirect:/recipes";
    }

    @PostMapping("/{id}")
    public String updateRecipe(@PathVariable UUID id,
                               @Valid @ModelAttribute("recipe") RecipeForm form,
                               BindingResult result,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        if (result.hasErrors()) {
            model.addAttribute("formAction", "/recipes/" + id);
            return "recipes/form";
        }
        recipeService.update(id, form.toEntity());
        redirectAttributes.addFlashAttribute("message", "Recipe updated successfully");
        return "redirect:/recipes";
    }

    @PostMapping("/test")
    public String testRecipe(@RequestParam("yamlContent") String yaml,
                             @RequestParam("url") String url,
                             Model model) throws JsonProcessingException {
        try {
            RecipeConfig config = recipeService.parse(yaml);
            var result = scraperEngine.execute(config, url, new OptionsConfig());
            model.addAttribute("structured", objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(result.structuredData()));
            model.addAttribute("processedHtml", result.processedHtml());
            model.addAttribute("processedMarkdown", result.processedMarkdown());
            model.addAttribute("rawHtml", result.rawHtml());
            model.addAttribute("progressSteps", result.progressSteps());
        } catch (Exception e) {
            model.addAttribute("structured", "Test failed: " + e.getMessage());
            model.addAttribute("processedHtml", "");
            model.addAttribute("processedMarkdown", "");
            model.addAttribute("rawHtml", "");
            model.addAttribute("progressSteps", List.of("Failed: " + e.getMessage()));
        }
        return "recipes/test-result :: result";
    }
}
