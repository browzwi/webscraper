package com.browzwi.webscraper.service.test;

import com.browzwi.webscraper.scraper.model.OptionsConfig;
import com.browzwi.webscraper.scraper.model.RecipeConfig;
import com.browzwi.webscraper.scraper.model.SubPageConfig;
import com.browzwi.webscraper.scraper.service.MultiPageScrapeResult;
import com.browzwi.webscraper.scraper.service.ScraperEngine;
import com.browzwi.webscraper.service.ScraperRecipeService;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

/**
 * Service for managing recipe test sessions that allow users to test scraper
 * recipes against specific URLs. This service creates and tracks test sessions
 * running in background threads with progress reporting.
 *
 * @since 1.0
 */
@Service
public class RecipeTestSessionService {

    private static final Logger log = LoggerFactory.getLogger(RecipeTestSessionService.class);

    private final ScraperRecipeService recipeService;
    private final ScraperEngine scraperEngine;
    private final TaskExecutor taskExecutor;
    private final Map<UUID, RecipeTestSession> sessions = new ConcurrentHashMap<>();

    /**
     * Constructor for RecipeTestSessionService with required dependencies.
     *
     * @param recipeService service for parsing and validating scraper recipes
     * @param scraperEngine engine for executing scraping operations
     * @param taskExecutor executor for running tests in background threads
     */
    public RecipeTestSessionService(ScraperRecipeService recipeService,
                                    ScraperEngine scraperEngine,
                                    @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor) {
        this.recipeService = recipeService;
        this.scraperEngine = scraperEngine;
        this.taskExecutor = taskExecutor;
    }

    /**
     * Starts a new test session for the specified recipe and URL.
     * The test runs in a background thread and the session object is returned
     * immediately for tracking progress.
     *
     * @param yaml the YAML content of the scraper recipe to test
     * @param url the URL to test the recipe against
     * @return the test session object for tracking progress
     */
    public RecipeTestSession startSession(String yaml, String url) {
        UUID sessionId = UUID.randomUUID();
        RecipeTestSession session = new RecipeTestSession(sessionId, url);
        sessions.put(sessionId, session);
        taskExecutor.execute(() -> executeTest(session, yaml, url));
        return session;
    }

    /**
     * Gets an existing test session by its ID.
     *
     * @param sessionId the ID of the session to retrieve
     * @return an optional containing the session if found, or empty if not found
     */
    public Optional<RecipeTestSession> getSession(UUID sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    /**
     * Cancels a running test session by its ID.
     *
     * @param sessionId the ID of the session to cancel
     */
    public void cancel(UUID sessionId) {
        getSession(sessionId).ifPresent(RecipeTestSession::requestCancel);
    }

    /**
     * Executes the actual test for a session in a background thread.
     *
     * @param session the test session to execute
     * @param yaml the YAML content of the recipe to test
     * @param url the URL to test against
     */
    private void executeTest(RecipeTestSession session, String yaml, String url) {
        session.markRunning();
        try {
            RecipeConfig config = recipeService.parse(yaml);
            OptionsConfig overrides = new OptionsConfig();
            ScraperEngine.ProgressListener listener = new SessionProgressListener(session);
            
            // Check if recipe has sub-pages configured for multi-page scraping
            boolean hasSubPages = config.getPage().getSubPages() != null && !config.getPage().getSubPages().isEmpty();
            log.debug("[RecipeTestSession] Session {}: Recipe has sub-pages: {} (count: {})", 
                     session.getId(), hasSubPages, 
                     config.getPage().getSubPages() != null ? config.getPage().getSubPages().size() : 0);
            
            if (hasSubPages) {
                // Add sub-page steps dynamically
                for (SubPageConfig subPage : config.getPage().getSubPages()) {
                    String subPageUrl = url + (subPage.getPath().startsWith("/") ? subPage.getPath() : "/" + subPage.getPath());
                    session.addStep("Starting scrape for " + subPageUrl);
                }
                
                MultiPageScrapeResult result = scraperEngine.executeMultiPage(config, url, overrides, listener);
                session.markCompletedMultiPage(result);
                log.debug("[RecipeTestSession] Session {}: Marked as multi-page completed", session.getId());
            } else {
                var result = scraperEngine.execute(config, url, overrides, listener);
                session.markCompleted(result);
                log.debug("[RecipeTestSession] Session {}: Marked as single-page completed", session.getId());
            }
            
            log.info("[RecipeTestSession] Session {} completed", session.getId());
        } catch (ScraperRecipeService.InvalidRecipeException ex) {
            log.warn("[RecipeTestSession] Session {} failed to parse recipe", session.getId(), ex);
            session.markFailed(ex.getMessage());
        } catch (ScraperEngine.ScrapeCancelledException ex) {
            log.info("[RecipeTestSession] Session {} cancelled", session.getId());
            session.markCancelled();
        } catch (ScraperEngine.ScrapeException ex) {
            String message = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
            log.error("[RecipeTestSession] Session {} failed", session.getId(), ex);
            session.markFailed(message);
        } catch (RuntimeException ex) {
            log.error("[RecipeTestSession] Session {} encountered unexpected failure", session.getId(), ex);
            session.markFailed(ex.getMessage());
        }
    }

    /**
     * Progress listener implementation that updates the test session state.
     *
     * @since 1.0
     */
    private static class SessionProgressListener implements ScraperEngine.ProgressListener {

        private final RecipeTestSession session;

        /**
         * Creates a new progress listener for the specified session.
         *
         * @param session the session to update with progress
         */
        private SessionProgressListener(RecipeTestSession session) {
            this.session = session;
        }

        @Override
        public void onStepStarted(String step) {
            // Add new step if it doesn't exist
            if (session.getSteps().stream().noneMatch(s -> s.getLabel().equals(step))) {
                session.addStep(step);
            }
            session.markStepRunning(step);
        }

        @Override
        public void onStepCompleted(String step) {
            session.markStepCompleted(step);
        }

        @Override
        public void onStepFailed(String step, String message) {
            session.markStepFailed(step, message);
        }

        @Override
        public boolean isCancelled() {
            return session.isCancelRequested();
        }

        @Override
        public void onCancelled(String currentStep) {
            if (currentStep != null) {
                session.markStepCancelled(currentStep);
            }
        }
    }
}

