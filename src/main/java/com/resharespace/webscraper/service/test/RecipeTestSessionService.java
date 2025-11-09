package com.browzwi.webscraper.service.test;

import com.browzwi.webscraper.scraper.model.OptionsConfig;
import com.browzwi.webscraper.scraper.model.RecipeConfig;
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

@Service
public class RecipeTestSessionService {

    private static final Logger log = LoggerFactory.getLogger(RecipeTestSessionService.class);

    private final ScraperRecipeService recipeService;
    private final ScraperEngine scraperEngine;
    private final TaskExecutor taskExecutor;
    private final Map<UUID, RecipeTestSession> sessions = new ConcurrentHashMap<>();

    public RecipeTestSessionService(ScraperRecipeService recipeService,
                                    ScraperEngine scraperEngine,
                                    @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor) {
        this.recipeService = recipeService;
        this.scraperEngine = scraperEngine;
        this.taskExecutor = taskExecutor;
    }

    public RecipeTestSession startSession(String yaml, String url) {
        UUID sessionId = UUID.randomUUID();
        RecipeTestSession session = new RecipeTestSession(sessionId, url);
        sessions.put(sessionId, session);
        taskExecutor.execute(() -> executeTest(session, yaml, url));
        return session;
    }

    public Optional<RecipeTestSession> getSession(UUID sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    public void cancel(UUID sessionId) {
        getSession(sessionId).ifPresent(RecipeTestSession::requestCancel);
    }

    private void executeTest(RecipeTestSession session, String yaml, String url) {
        session.markRunning();
        try {
            RecipeConfig config = recipeService.parse(yaml);
            OptionsConfig overrides = new OptionsConfig();
            ScraperEngine.ProgressListener listener = new SessionProgressListener(session);
            
            // Check if recipe has sub-pages configured for multi-page scraping
            if (config.getPage().getSubPages() != null && !config.getPage().getSubPages().isEmpty()) {
                MultiPageScrapeResult result = scraperEngine.executeMultiPage(config, url, overrides, listener);
                session.markCompletedMultiPage(result);
            } else {
                var result = scraperEngine.execute(config, url, overrides, listener);
                session.markCompleted(result);
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

    private static class SessionProgressListener implements ScraperEngine.ProgressListener {

        private final RecipeTestSession session;

        private SessionProgressListener(RecipeTestSession session) {
            this.session = session;
        }

        @Override
        public void onStepStarted(String step) {
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

