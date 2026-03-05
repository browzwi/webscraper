package com.browzwi.webscraper.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.browzwi.webscraper.domain.ScraperRecipe;
import com.browzwi.webscraper.repository.ScraperRecipeRepository;
import com.browzwi.webscraper.scraper.service.MultiPageScrapeResult;
import com.browzwi.webscraper.service.ScraperRecipeService;
import com.browzwi.webscraper.service.test.RecipeTestSession;
import com.browzwi.webscraper.service.test.RecipeTestSessionService;
import com.browzwi.webscraper.web.dto.RecipeExample;
import com.browzwi.webscraper.web.dto.RecipeForm;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for managing scraper recipes through CRUD operations and testing.
 * Provides endpoints for listing, creating, updating, and testing scraper recipes.
 *
 * @since 1.0
 */
@Controller
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/recipes")
public class RecipeController {

    private static final Logger log = LoggerFactory.getLogger(RecipeController.class);

    private final ScraperRecipeService recipeService;
    private final ScraperRecipeRepository recipeRepository;
    private final RecipeTestSessionService testSessionService;
    private final ObjectMapper objectMapper;

    /**
     * Constructor for RecipeController with required dependencies.
     *
     * @param recipeService service for managing scraper recipes
     * @param recipeRepository repository for managing recipe entities
     * @param testSessionService service for managing recipe test sessions
     * @param objectMapper object mapper for JSON processing
     */
    public RecipeController(ScraperRecipeService recipeService,
                            ScraperRecipeRepository recipeRepository,
                            RecipeTestSessionService testSessionService,
                            ObjectMapper objectMapper) {
        this.recipeService = recipeService;
        this.recipeRepository = recipeRepository;
        this.testSessionService = testSessionService;
        this.objectMapper = objectMapper;
    }

    /**
     * Handles requests to list all scraper recipes.
     * Sets up the model with the list of recipes for the list view.
     *
     * @param model the model to populate with recipe data
     * @return the recipes list view name
     */
    @GetMapping
    public String list(Model model) {
        List<ScraperRecipe> recipes = recipeRepository.findAll();
        model.addAttribute("pageTitle", "Recipes");
        model.addAttribute("recipes", recipes);
        return "recipes/list";
    }

    /**
     * Provides a list of recipe examples that users can reference when creating recipes.
     *
     * @return a list of RecipeExample objects
     */
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

    /**
     * Handles requests to view the form for creating a new recipe.
     * Sets up the model with a new recipe form for the form view.
     *
     * @param model the model to populate with form data
     * @return the recipes form view name
     */
    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("pageTitle", "New Recipe");
        model.addAttribute("recipe", new RecipeForm());
        model.addAttribute("formAction", "/recipes");
        model.addAttribute("testStatus", null);
        return "recipes/form";
    }

    /**
     * Handles requests to view the form for editing an existing recipe.
     * Sets up the model with the recipe data for the form view.
     *
     * @param id the ID of the recipe to edit
     * @param model the model to populate with recipe data
     * @param redirectAttributes attributes for redirect in case of error
     * @return the recipes form view name or redirect to recipes list if not found
     */
    @GetMapping("/{id}")
    public String editForm(@PathVariable UUID id, Model model, RedirectAttributes redirectAttributes) {
        return recipeRepository.findById(id)
                .map(recipe -> {
                    model.addAttribute("pageTitle", "Edit Recipe");
                    model.addAttribute("recipe", RecipeForm.fromEntity(recipe));
                    model.addAttribute("formAction", "/recipes/" + id);
                    model.addAttribute("testStatus", null);
                    return "recipes/form";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Recipe not found");
                    return "redirect:/recipes";
                });
    }

    /**
     * Handles form submissions for creating a new recipe.
     * Validates the form and creates the recipe if valid, or returns the form with errors.
     *
     * @param form the recipe form data
     * @param bindingResult the validation result
     * @param redirectAttributes attributes for redirect after successful creation
     * @param model the model to populate if validation fails
     * @return redirect to recipes list if successful, or return to form if validation fails
     */
    @PostMapping
    public String saveRecipe(@Valid @ModelAttribute("recipe") RecipeForm form,
                             BindingResult bindingResult,
                             RedirectAttributes redirectAttributes,
                             Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("formAction", "/recipes");
            model.addAttribute("testStatus", null);
            return "recipes/form";
        }
        recipeService.create(form.toEntity());
        redirectAttributes.addFlashAttribute("message", "Recipe created successfully");
        return "redirect:/recipes";
    }

    /**
     * Handles form submissions for updating an existing recipe.
     * Validates the form and updates the recipe if valid, or returns the form with errors.
     *
     * @param id the ID of the recipe to update
     * @param form the recipe form data
     * @param result the validation result
     * @param redirectAttributes attributes for redirect after successful update
     * @param model the model to populate if validation fails
     * @return redirect to recipes list if successful, or return to form if validation fails
     */
    @PostMapping("/{id}")
    public String updateRecipe(@PathVariable UUID id,
                               @Valid @ModelAttribute("recipe") RecipeForm form,
                               BindingResult result,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        if (result.hasErrors()) {
            model.addAttribute("formAction", "/recipes/" + id);
            model.addAttribute("testStatus", null);
            return "recipes/form";
        }
        recipeService.update(id, form.toEntity());
        redirectAttributes.addFlashAttribute("message", "Recipe updated successfully");
        return "redirect:/recipes";
    }

    @PostMapping("/test")
    @ResponseBody
    public ResponseEntity<TestSessionResponse> startTest(@RequestParam("yamlContent") String yaml,
                                                         @RequestParam("url") String url) {
        if (url == null || url.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "URL is required for testing");
        }
        RecipeTestSession session = testSessionService.startSession(yaml, url.trim());
        return ResponseEntity.ok(new TestSessionResponse(session.getId()));
    }

    @GetMapping("/test/{sessionId}/status")
    @ResponseBody
    public ResponseEntity<TestStatusResponse> getStatus(@PathVariable UUID sessionId) {
        RecipeTestSession session = testSessionService.getSession(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
        var steps = session.getSteps().stream()
                .map(step -> new TestStatusResponse.TestStepView(step.getLabel(), step.getState(), step.getDetail()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(new TestStatusResponse(session.getId(), session.getStatus(), steps,
                session.getErrorMessage(), session.isCancellable(), session.isCancelRequested()));
    }

    @PostMapping("/test/{sessionId}/cancel")
    @ResponseBody
    public ResponseEntity<Void> cancel(@PathVariable UUID sessionId) {
        testSessionService.cancel(sessionId);
        return ResponseEntity.accepted().build();
    }

    /**
     * Gets the results of a completed recipe test session.
     * This method returns the test results as a template fragment.
     *
     * @param sessionId the ID of the test session to get results for
     * @param pageKey the page key for multi-page scrapes (defaults to "main")
     * @param model the model to populate with test results
     * @return the test result template fragment
     * @throws JsonProcessingException if there's an error processing JSON
     * @throws ResponseStatusException if the session is not found or not completed
     */
    @GetMapping("/test/{sessionId}/result")
    public String getResult(@PathVariable UUID sessionId, 
                           @RequestParam(value = "page", defaultValue = "main") String pageKey,
                           Model model) throws JsonProcessingException {
        RecipeTestSession session = testSessionService.getSession(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
        
        model.addAttribute("sessionId", sessionId);
        model.addAttribute("testStatus", session.getStatus().name());
        
        switch (session.getStatus()) {
            case COMPLETED -> {
                log.info("Session completed - isMultiPage: {}", session.isMultiPage());
                log.debug("Session completed - multiPageResult: {}, result: {}", 
                         session.getMultiPageResult() != null, session.getResult() != null);
                if (session.isMultiPage()) {
                    var multiResult = session.getMultiPageResult();
                    log.info("Multi-page result: {}, pageResults count: {}", 
                        multiResult != null, 
                        multiResult != null ? multiResult.pageResults().size() : 0);
                    
                    model.addAttribute("structured", objectMapper.writerWithDefaultPrettyPrinter()
                            .writeValueAsString(multiResult.combinedStructuredData()));
                    model.addAttribute("progressSteps", multiResult.progressSteps());
                    model.addAttribute("isMultiPage", true);
                    
                    // Preprocess page results with sanitized keys and computed values to avoid complex operations in template
                    Map<String, Object> processedPageResults = new LinkedHashMap<>();
                    for (Map.Entry<String, MultiPageScrapeResult.PageResult> entry : multiResult.pageResults().entrySet()) {
                        String currentPageKey = entry.getKey();
                        MultiPageScrapeResult.PageResult pageResult = entry.getValue();
                        
                        // Create sanitized key for use in HTML ids and attributes
                        String sanitizedKey = sanitizeForHtmlId(currentPageKey);
                        
                        // Create computed IDs for HTML elements
                        String processedHtmlId = "testProcessedHtml__" + sanitizedKey;
                        String markdownId = "testProcessedMarkdown__" + sanitizedKey;
                        String rawId = "testRawHtml__" + sanitizedKey;
                        
                        // Create page label
                        String pageLabel = "main".equals(currentPageKey) ? "Main Page" : currentPageKey;
                        
                        // Store the processed information
                        Map<String, Object> processedPage = new HashMap<>();
                        processedPage.put("result", pageResult);
                        processedPage.put("key", currentPageKey);
                        processedPage.put("label", pageLabel);
                        processedPage.put("sanitizedKey", sanitizedKey);
                        processedPage.put("processedHtmlId", processedHtmlId);
                        processedPage.put("markdownId", markdownId);
                        processedPage.put("rawId", rawId);
                        // Precompute onclick attributes to avoid complex SpEL in template
                        processedPage.put("processedHtmlOnclick", "copyFromElement(this, '#" + processedHtmlId + "')");
                        processedPage.put("markdownOnclick", "copyFromElement(this, '#" + markdownId + "')");
                        processedPage.put("rawOnclick", "copyFromElement(this, '#" + rawId + "')");
                        
                        processedPageResults.put(currentPageKey, processedPage);
                    }
                    
                    model.addAttribute("pageResults", processedPageResults);

                    boolean hasMarkdown = multiResult.pageResults().values().stream()
                        .anyMatch(pageResult -> pageResult.processedMarkdown() != null
                            && !pageResult.processedMarkdown().isBlank());
                    model.addAttribute("hasMarkdown", hasMarkdown);

                    // Get specific page result for display (for legacy consumers)
                    var pageResult = multiResult.pageResults().get(pageKey);
                    if (pageResult != null) {
                        model.addAttribute("processedHtml", pageResult.processedHtml());
                        model.addAttribute("processedMarkdown", pageResult.processedMarkdown());
                        model.addAttribute("rawHtml", pageResult.rawHtml());
                    } else {
                        model.addAttribute("processedHtml", "");
                        model.addAttribute("processedMarkdown", "");
                        model.addAttribute("rawHtml", "");
                    }
                } else {
                    var result = session.getResult();
                    log.info("Single-page result: {}", result != null);
                    model.addAttribute("structured", objectMapper.writerWithDefaultPrettyPrinter()
                            .writeValueAsString(result.structuredData()));
                    model.addAttribute("processedHtml", result.processedHtml());
                    model.addAttribute("processedMarkdown", result.processedMarkdown());
                    model.addAttribute("rawHtml", result.rawHtml());
                    model.addAttribute("progressSteps", result.progressSteps());
                    model.addAttribute("isMultiPage", false);
                    boolean hasMarkdown = result != null
                        && result.processedMarkdown() != null
                        && !result.processedMarkdown().isBlank();
                    model.addAttribute("hasMarkdown", hasMarkdown);
                }
            }
            case FAILED -> {
                String message = session.getErrorMessage() != null ? session.getErrorMessage() : "Unknown failure";
                model.addAttribute("structured", "Test failed: " + message);
                model.addAttribute("processedHtml", "");
                model.addAttribute("processedMarkdown", "");
                model.addAttribute("rawHtml", "");
                model.addAttribute("progressSteps", session.progressMessages());
                model.addAttribute("isMultiPage", false);
                model.addAttribute("hasMarkdown", false);
            }
            case CANCELLED -> {
                model.addAttribute("structured", "Test cancelled by user");
                model.addAttribute("processedHtml", "");
                model.addAttribute("processedMarkdown", "");
                model.addAttribute("rawHtml", "");
                model.addAttribute("progressSteps", session.progressMessages());
                model.addAttribute("isMultiPage", false);
                model.addAttribute("hasMarkdown", false);
            }
            default -> throw new ResponseStatusException(HttpStatus.ACCEPTED, "Session still running");
        }
        return "recipes/test-result :: result";
    }

    /**
     * Sanitizes a string for use as an HTML ID attribute.
     * Replaces problematic characters with hyphens or removes them.
     *
     * @param input the input string to sanitize
     * @return the sanitized string safe for HTML IDs
     */
    private String sanitizeForHtmlId(String input) {
        if (input == null) {
            return "";
        }
        // Replace problematic characters with hyphens or remove them
        return input.replaceAll("[^a-zA-Z0-9_\\-]", "-");
    }

    /**
     * Record representing the response for starting a test session.
     *
     * @param sessionId the ID of the created test session
     * @since 1.0
     */
    public record TestSessionResponse(UUID sessionId) {
    }

    /**
     * Record representing the response for getting test session status.
     *
     * @param sessionId the ID of the test session
     * @param status the current status of the test session
     * @param steps the list of steps in the test session
     * @param errorMessage the error message if the test failed
     * @param cancellable whether the session can be cancelled
     * @param cancelRequested whether cancellation has been requested
     * @since 1.0
     */
    public record TestStatusResponse(UUID sessionId,
                                     RecipeTestSession.Status status,
                                     List<TestStepView> steps,
                                     String errorMessage,
                                     boolean cancellable,
                                     boolean cancelRequested) {

        /**
         * Record representing the view of a test step.
         *
         * @param label the label of the test step
         * @param state the state of the test step
         * @param detail additional details about the test step
         * @since 1.0
         */
        public record TestStepView(String label, RecipeTestSession.StepState state, String detail) {
        }
    }
}
