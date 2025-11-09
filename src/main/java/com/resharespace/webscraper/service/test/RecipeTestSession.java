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

    public RecipeTestSession(UUID id, String url) {
        this.id = Objects.requireNonNull(id, "id");
        this.url = Objects.requireNonNull(url, "url");
        this.createdAt = Instant.now();
        this.steps = new ArrayList<>(List.of(
                new TestStep("Starting scrape for " + url),
                new TestStep("Fetched raw HTML"),
                new TestStep("Processed DOM and collected hrefs"),
                new TestStep("Extracted structured data"),
                new TestStep("Converted processed HTML to Markdown")));
    }

    public UUID getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getUrl() {
        return url;
    }

    public Status getStatus() {
        return status;
    }

    public List<TestStep> getSteps() {
        return Collections.unmodifiableList(steps);
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    @JsonIgnore
    public ScraperEngine.ScrapeExecutionResult getResult() {
        return result;
    }

    @JsonIgnore
    public MultiPageScrapeResult getMultiPageResult() {
        return multiPageResult;
    }

    public boolean isMultiPage() {
        return multiPageResult != null;
    }

    public boolean isCancelRequested() {
        return cancelRequested.get();
    }

    public boolean isCancellable() {
        return status == Status.RUNNING && !cancelRequested.get();
    }

    public void requestCancel() {
        cancelRequested.set(true);
    }

    public synchronized void markRunning() {
        this.status = Status.RUNNING;
    }

    public synchronized void markCompleted(ScraperEngine.ScrapeExecutionResult result) {
        this.result = result;
        this.status = Status.COMPLETED;
    }

    public synchronized void markCompletedMultiPage(MultiPageScrapeResult result) {
        this.multiPageResult = result;
        this.status = Status.COMPLETED;
    }

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

    public synchronized void markCancelled() {
        this.status = Status.CANCELLED;
        for (TestStep step : steps) {
            if (step.state == StepState.RUNNING || step.state == StepState.PENDING) {
                step.state = StepState.CANCELLED;
            }
        }
    }

    public synchronized void markStepRunning(String label) {
        updateStep(label, StepState.RUNNING, null);
    }

    public synchronized void markStepCompleted(String label) {
        updateStep(label, StepState.COMPLETED, null);
    }

    public synchronized void markStepFailed(String label, String message) {
        updateStep(label, StepState.FAILED, message);
    }

    public synchronized void markStepCancelled(String label) {
        updateStep(label, StepState.CANCELLED, null);
    }

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

    private void updateStep(String label, StepState state, String detail) {
        for (TestStep step : steps) {
            if (step.label.equals(label)) {
                step.state = state;
                step.detail = detail;
                break;
            }
        }
    }

    public enum Status {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED,
        CANCELLED
    }

    public enum StepState {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED,
        CANCELLED
    }

    public static class TestStep {
        private final String label;
        private StepState state;
        private String detail;

        private TestStep(String label) {
            this.label = label;
            this.state = StepState.PENDING;
        }

        public String getLabel() {
            return label;
        }

        public StepState getState() {
            return state;
        }

        public String getDetail() {
            return detail;
        }
    }
}

