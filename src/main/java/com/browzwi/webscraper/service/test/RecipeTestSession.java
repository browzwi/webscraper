package com.browzwi.webscraper.service.test;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.browzwi.webscraper.scraper.service.MultiPageScrapeResult;
import com.browzwi.webscraper.scraper.service.ScraperEngine;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Represents a single recipe test session that tracks the progress and results
 * of testing a scraper recipe against a URL. This class manages the state of
 * the test execution and provides methods to update the test status and steps.
 *
 * @since 1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecipeTestSession {

    private final UUID id;
    private final Instant createdAt;
    private final String url;
    private final List<TestStep> steps;
    private final AtomicBoolean cancelRequested = new AtomicBoolean(false);

    private volatile Status status = Status.PENDING;
    private volatile String errorMessage;
    private volatile ScraperEngine.ScrapeExecutionResult result;
    private volatile MultiPageScrapeResult multiPageResult;

    /**
     * Creates a new test session with the specified ID and URL.
     *
     * @param id the unique identifier for this test session
     * @param url the URL to test against the scraper recipe
     */
    public RecipeTestSession(UUID id, String url) {
        this.id = Objects.requireNonNull(id, "id");
        this.url = Objects.requireNonNull(url, "url");
        this.createdAt = Instant.now();
        this.steps = new ArrayList<>();
        initializeDefaultSteps();
    }

    /**
     * Initializes the default test steps for the session.
     */
    private void initializeDefaultSteps() {
        steps.addAll(List.of(
                new TestStep("Starting scrape for " + url),
                new TestStep("Fetched raw HTML"),
                new TestStep("Processed DOM and collected hrefs"),
                new TestStep("Extracted structured data"),
                new TestStep("Converted processed HTML to Markdown")));
    }

    /**
     * Adds a new step to the test session.
     *
     * @param stepLabel the label for the new step
     */
    public synchronized void addStep(String stepLabel) {
        steps.add(new TestStep(stepLabel));
    }

    /**
     * Gets the unique identifier for this test session.
     *
     * @return the session ID
     */
    public UUID getId() {
        return id;
    }

    /**
     * Gets the timestamp when this test session was created.
     *
     * @return the creation timestamp
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Gets the URL being tested in this session.
     *
     * @return the test URL
     */
    public String getUrl() {
        return url;
    }

    /**
     * Gets the current status of the test session.
     *
     * @return the session status
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Gets the list of test steps in this session.
     *
     * @return an unmodifiable list of test steps
     */
    public List<TestStep> getSteps() {
        return Collections.unmodifiableList(steps);
    }

    /**
     * Gets the error message if the test failed.
     *
     * @return the error message, or null if no error occurred
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Gets the result of the scraping operation.
     *
     * @return the scraping execution result, or null if not completed
     */
    @JsonIgnore
    public ScraperEngine.ScrapeExecutionResult getResult() {
        return result;
    }

    /**
     * Gets the result of the multi-page scraping operation.
     *
     * @return the multi-page scraping result, or null if not completed
     */
    @JsonIgnore
    public MultiPageScrapeResult getMultiPageResult() {
        return multiPageResult;
    }

    /**
     * Checks if this session represents a multi-page scraping operation.
     *
     * @return true if multi-page result exists, false otherwise
     */
    public boolean isMultiPage() {
        return multiPageResult != null;
    }

    /**
     * Checks if cancellation has been requested for this session.
     *
     * @return true if cancellation requested, false otherwise
     */
    public boolean isCancelRequested() {
        return cancelRequested.get();
    }

    /**
     * Checks if the session can be cancelled (is running and not already cancelled).
     *
     * @return true if cancellable, false otherwise
     */
    public boolean isCancellable() {
        return status == Status.RUNNING && !cancelRequested.get();
    }

    /**
     * Requests cancellation of this test session.
     */
    public void requestCancel() {
        cancelRequested.set(true);
    }

    /**
     * Marks the test session as running.
     */
    public synchronized void markRunning() {
        this.status = Status.RUNNING;
    }

    /**
     * Marks the test session as completed with the specified result.
     *
     * @param result the scraping execution result
     */
    public synchronized void markCompleted(ScraperEngine.ScrapeExecutionResult result) {
        this.result = result;
        this.status = Status.COMPLETED;
    }

    /**
     * Marks the test session as completed with the specified multi-page result.
     *
     * @param result the multi-page scraping result
     */
    public synchronized void markCompletedMultiPage(MultiPageScrapeResult result) {
        this.multiPageResult = result;
        this.status = Status.COMPLETED;
    }

    /**
     * Marks the test session as failed with the specified error message.
     *
     * @param message the error message
     */
    public synchronized void markFailed(String message) {
        this.errorMessage = message;
        this.status = Status.FAILED;
        boolean updated = false;
        for (TestStep step : steps) {
            if (step.state == StepState.RUNNING) {
                step.state = StepState.FAILED;
                step.detail = message;
                updated = true;
            } else if (step.state == StepState.PENDING && !updated) {
                step.state = StepState.FAILED;
                step.detail = message;
                updated = true;
            }
        }
    }

    /**
     * Marks the test session as cancelled.
     */
    public synchronized void markCancelled() {
        this.status = Status.CANCELLED;
        for (TestStep step : steps) {
            if (step.state == StepState.RUNNING || step.state == StepState.PENDING) {
                step.state = StepState.CANCELLED;
            }
        }
    }

    /**
     * Marks the specified step as running.
     *
     * @param label the label of the step to update
     */
    public synchronized void markStepRunning(String label) {
        updateStep(label, StepState.RUNNING, null);
    }

    /**
     * Marks the specified step as completed.
     *
     * @param label the label of the step to update
     */
    public synchronized void markStepCompleted(String label) {
        updateStep(label, StepState.COMPLETED, null);
    }

    /**
     * Marks the specified step as failed with the given message.
     *
     * @param label the label of the step to update
     * @param message the failure message
     */
    public synchronized void markStepFailed(String label, String message) {
        updateStep(label, StepState.FAILED, message);
    }

    /**
     * Marks the specified step as cancelled.
     *
     * @param label the label of the step to update
     */
    public synchronized void markStepCancelled(String label) {
        updateStep(label, StepState.CANCELLED, null);
    }

    /**
     * Gets the progress messages for the test session.
     *
     * @return a list of progress messages
     */
    public synchronized List<String> progressMessages() {
        if (status == Status.COMPLETED) {
            if (multiPageResult != null) {
                return new ArrayList<>(multiPageResult.progressSteps());
            } else if (result != null) {
                return new ArrayList<>(result.progressSteps());
            }
        }
        List<String> messages = new ArrayList<>();
        for (TestStep step : steps) {
            if (step.state == StepState.COMPLETED) {
                messages.add(step.label);
            } else if (step.state == StepState.FAILED) {
                messages.add(step.label + (step.detail != null ? " (" + step.detail + ")" : ""));
            } else if (step.state == StepState.CANCELLED) {
                messages.add(step.label + " (cancelled)");
            }
        }
        if (status == Status.FAILED && errorMessage != null) {
            messages.add("Failed: " + errorMessage);
        } else if (status == Status.CANCELLED) {
            messages.add("Cancelled by user");
        }
        return messages;
    }

    /**
     * Updates the state of a step with the specified label.
     *
     * @param label the label of the step to update
     * @param state the new state for the step
     * @param detail optional detail message for the step
     */
    private void updateStep(String label, StepState state, String detail) {
        for (TestStep step : steps) {
            if (step.label.equals(label)) {
                step.state = state;
                step.detail = detail;
                break;
            }
        }
    }

    /**
     * Enum representing the possible statuses of a test session.
     *
     * @since 1.0
     */
    public enum Status {
        /**
         * Test has not yet started.
         */
        PENDING,
        /**
         * Test is currently running.
         */
        RUNNING,
        /**
         * Test completed successfully.
         */
        COMPLETED,
        /**
         * Test failed with an error.
         */
        FAILED,
        /**
         * Test was cancelled by the user.
         */
        CANCELLED
    }

    /**
     * Enum representing the possible states of a test step.
     *
     * @since 1.0
     */
    public enum StepState {
        /**
         * Step has not yet started.
         */
        PENDING,
        /**
         * Step is currently running.
         */
        RUNNING,
        /**
         * Step completed successfully.
         */
        COMPLETED,
        /**
         * Step failed with an error.
         */
        FAILED,
        /**
         * Step was cancelled by the user.
         */
        CANCELLED
    }

    /**
     * Represents a single step in the test process, tracking its state and details.
     *
     * @since 1.0
     */
    public static class TestStep {
        private final String label;
        private StepState state;
        private String detail;

        /**
         * Creates a new test step with the specified label.
         *
         * @param label the label for this test step
         */
        private TestStep(String label) {
            this.label = label;
            this.state = StepState.PENDING;
        }

        /**
         * Gets the label of this test step.
         *
         * @return the step label
         */
        public String getLabel() {
            return label;
        }

        /**
         * Gets the current state of this test step.
         *
         * @return the step state
         */
        public StepState getState() {
            return state;
        }

        /**
         * Gets the detail message for this test step.
         *
         * @return the detail message, or null if not set
         */
        public String getDetail() {
            return detail;
        }
    }
}

