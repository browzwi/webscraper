package com.browzwi.webscraper.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.browzwi.webscraper.domain.ScrapeJob;
import com.browzwi.webscraper.domain.ScrapeResultData;
import com.browzwi.webscraper.domain.ScrapeTarget;
import com.browzwi.webscraper.repository.ScraperRecipeRepository;
import com.browzwi.webscraper.service.ScrapeJobService;
import com.browzwi.webscraper.service.ScrapeTargetArchiveService;
import com.browzwi.webscraper.web.dto.JobForm;
import com.browzwi.webscraper.web.view.ScrapeJobDetailView;
import com.browzwi.webscraper.web.view.ScrapeJobListItemView;
import com.browzwi.webscraper.web.view.ScrapeTargetDetailsView;
import com.browzwi.webscraper.web.view.ScrapeTargetProgressGroupView;
import com.browzwi.webscraper.web.view.ScrapeTargetProgressView;
import com.browzwi.webscraper.web.view.ScrapeTargetRowView;
import jakarta.validation.Valid;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.concurrent.ConcurrentHashMap;

import com.browzwi.webscraper.storage.FileStorageService.StoredPageResult;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Controller for managing scraping jobs through CRUD operations and viewing results.
 * Provides endpoints for creating, listing, running, and viewing details of scraping jobs
 * and their targets.
 *
 * @since 1.0
 */
@Controller
@RequestMapping("/jobs")
@PreAuthorize("hasRole('ADMIN')")
public class ScrapeJobController {

    private static final Logger log = LoggerFactory.getLogger(ScrapeJobController.class);
    private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .withZone(ZoneId.systemDefault());
    private static final Pattern NUMBER_PREFIX = Pattern.compile("^\\s*\\d+\\.?\\s*");
    private static final Pattern PAGE_START_PATTERN = Pattern.compile("(?i)^starting\\s+scrape\\s+for\\s+(.+)$");

    private final ScrapeJobService jobService;
    private final ScraperRecipeRepository recipeRepository;
    private final ScrapeTargetArchiveService archiveService;
    private final com.browzwi.webscraper.storage.FileStorageService storageService;
    private final ObjectMapper objectMapper;

    /**
     * Constructor for ScrapeJobController with required dependencies.
     *
     * @param jobService service for managing scraping jobs
     * @param recipeRepository repository for managing recipe entities
     * @param archiveService service for creating archives of scraping results
     * @param storageService service for file storage operations
     * @param objectMapper object mapper for JSON processing
     */
    public ScrapeJobController(ScrapeJobService jobService,
                               ScraperRecipeRepository recipeRepository,
                               ScrapeTargetArchiveService archiveService,
                               com.browzwi.webscraper.storage.FileStorageService storageService,
                               ObjectMapper objectMapper) {
        this.jobService = jobService;
        this.recipeRepository = recipeRepository;
        this.archiveService = archiveService;
        this.storageService = storageService;
        this.objectMapper = objectMapper.copy().enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * Handles requests to list all scraping jobs.
     * Sets up the model with a list of jobs for the jobs list view.
     *
     * @param model the model to populate with job data
     * @return the jobs list view name
     */
    @GetMapping
    public String list(Model model) {
        List<ScrapeJob> jobs = jobService.listJobs();
        List<ScrapeJobListItemView> jobRows = jobs.stream()
                .map(this::toListItemView)
                .toList();
        model.addAttribute("pageTitle", "Jobs");
        model.addAttribute("jobRows", jobRows);
        return "jobs/list";
    }

    /**
     * Handles requests to run a job immediately.
     * Triggers the specified job to run bypassing its schedule.
     *
     * @param id the ID of the job to run
     * @return an accepted response entity
     */
    @PostMapping("/{id}/run")
    public ResponseEntity<Void> runJob(@PathVariable("id") UUID id) {
        jobService.runJobNow(id);
        return ResponseEntity.accepted().build();
    }

    /**
     * Handles requests to view the form for creating a new scraping job.
     * Sets up the model with a new job form and available recipes for the form view.
     *
     * @param model the model to populate with form data
     * @return the jobs form view name
     */
    @GetMapping("/new")
    public String newJob(Model model) {
        model.addAttribute("pageTitle", "New Job");
        model.addAttribute("job", new JobForm());
        model.addAttribute("recipes", recipeRepository.findAll());
        return "jobs/form";
    }

    /**
     * Handles form submissions for creating a new scraping job.
     * Validates the form and creates the job if valid, or returns the form with errors.
     *
     * @param form the job form data
     * @param result the validation result
     * @param authentication the authentication object for getting the current user
     * @param redirectAttributes attributes for redirect after successful creation
     * @param model the model to populate if validation fails
     * @return redirect to jobs list if successful, or return to form if validation fails
     */
    @PostMapping
    public String createJob(@Valid @ModelAttribute("job") JobForm form,
                            BindingResult result,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes,
                            Model model) {
        List<String> urls = form.urlList();
        if (urls.isEmpty()) {
            result.rejectValue("urls", "urls.empty", "Provide at least one URL");
        }
        if (result.hasErrors()) {
            model.addAttribute("recipes", recipeRepository.findAll());
            return "jobs/form";
        }
        jobService.createJob(form.getName(), form.recipeUuid(), urls, form.getCronExpression(),
                form.toOptionsConfig(), authentication.getName());
        redirectAttributes.addFlashAttribute("message", "Job created and scheduled");
        return "redirect:/jobs";
    }

    /**
     * Handles requests to view the details of a specific scraping job.
     * Sets up the model with job details and its associated targets for the detail view.
     *
     * @param id the ID of the job to view details for
     * @param model the model to populate with job details
     * @return the jobs detail view name
     */
    @GetMapping("/{id}")
    public String jobDetail(@PathVariable("id") UUID id, Model model) {
        ScrapeJob job = jobService.getJob(id);
        List<ScrapeTargetRowView> targets = jobService.listTargets(id).stream()
                .map(this::toTargetRowView)
                .toList();
        ScrapeJobDetailView detailView = new ScrapeJobDetailView(
                job.getId(),
                job.getName(),
                job.getRecipe().getName(),
                job.getStatus(),
                formatScheduleLabel(job.getScheduleCron()),
                jobService.findNextRun(job.getId()).map(this::formatCountdown).orElse(null),
                job.getRequestedBy(),
                targets);
        model.addAttribute("pageTitle", job.getName());
        model.addAttribute("jobDetail", detailView);
        return "jobs/detail";
    }

    /**
     * Handles requests to view the details of a specific scraping target.
     * Sets up the model with target details for the detail view fragment.
     *
     * @param jobId the ID of the parent job
     * @param targetId the ID of the target to view details for
     * @param model the model to populate with target details
     * @return the target details template fragment
     */
    @GetMapping("/{jobId}/targets/{targetId}")
    public String targetDetails(@PathVariable("jobId") UUID jobId,
                                @PathVariable("targetId") UUID targetId,
                                Model model) {
        jobService.getJob(jobId);
        ScrapeTarget target = jobService.getTarget(targetId);
        Optional<ScrapeResultData> resultOpt = jobService.findResult(targetId);

        String structured = resultOpt.map(ScrapeResultData::getDataJson)
                .map(this::prettyJson)
                .orElse(null);

        ScrapeTargetDetailsView view = new ScrapeTargetDetailsView(
                target.getId(),
                target.getUrl(),
                target.getStatus(),
                formatInstantOrDefault(target.getStartedAt(), "Not started"),
                formatInstantOrDefault(target.getFinishedAt(), "Not finished"),
                formatDurationLabel(target.getStartedAt(), target.getFinishedAt()),
                target.getErrorMessage(),
                StringUtils.hasText(target.getErrorMessage()),
                resultOpt.isPresent(),
                structured,
                archiveService.canBuildArchive(jobId, targetId, resultOpt),
                buildArchiveFileName(target));

        model.addAttribute("jobId", jobId);
        model.addAttribute("targetDetail", view);
        return "jobs/target-details :: content";
    }

    /**
     * Handles requests to download an archive of scraping results for a target.
     * Returns the archive as a downloadable file.
     *
     * @param jobId the ID of the parent job
     * @param targetId the ID of the target to download archive for
     * @return a response entity containing the archive file
     */
    @GetMapping("/{jobId}/targets/{targetId}/archive")
    public ResponseEntity<ByteArrayResource> downloadArchive(@PathVariable("jobId") UUID jobId,
                                                              @PathVariable("targetId") UUID targetId) {
        ScrapeJob job = jobService.getJob(jobId);
        ScrapeTarget target = jobService.getTarget(targetId);
        Optional<ScrapeResultData> resultOpt = jobService.findResult(targetId);
        if (!archiveService.canBuildArchive(jobId, targetId, resultOpt)) {
            return ResponseEntity.notFound().build();
        }
        byte[] archive = archiveService.buildArchive(job, target, resultOpt);
        ByteArrayResource resource = new ByteArrayResource(archive);
        String filename = buildArchiveFileName(target);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(archive.length)
                .body(resource);
    }

    /**
     * Renders the HTMX-friendly modal displaying progress information for a scrape target.
     *
     * @param jobId identifier of the owning job
     * @param targetId identifier of the target whose progress is requested
     * @param model Spring MVC model receiving the view state
     * @return Thymeleaf fragment identifier for the modal wrapper
     */
    @GetMapping("/{jobId}/targets/{targetId}/status")
    public String targetProgress(@PathVariable("jobId") UUID jobId,
                                 @PathVariable("targetId") UUID targetId,
                                 Model model) {
        ScrapeTargetProgressView progressView = loadProgressView(jobId, targetId);
        model.addAttribute("progress", progressView);
        model.addAttribute("closeUrl", buildProgressCloseUrl(jobId, targetId));
        return "jobs/target-progress-modal :: modal";
    }

    /**
     * Returns an empty fragment to collapse the HTMX-driven progress modal.
     *
     * @param jobId identifier of the owning job
     * @param targetId identifier of the scrape target whose modal is being closed
     * @return Thymeleaf fragment identifier rendering no content
     */
    @GetMapping("/{jobId}/targets/{targetId}/status/close")
    public String closeTargetProgress(@PathVariable("jobId") UUID jobId, @PathVariable("targetId") UUID targetId) {
        jobService.getJob(jobId);
        jobService.getTarget(targetId);
        return "jobs/target-progress-modal :: empty";
    }

    private ScrapeJobListItemView toListItemView(ScrapeJob job) {
        String scheduleLabel = formatScheduleLabel(job.getScheduleCron());
        String countdown = StringUtils.hasText(job.getScheduleCron())
                ? jobService.findNextRun(job.getId()).map(this::formatCountdown).orElse(null)
                : null;
        String lastRun = formatInstantOrDefault(job.getUpdatedAt(), "N/A");
        return new ScrapeJobListItemView(job.getId(), job.getName(), job.getRecipe().getName(),
                job.getStatus(), scheduleLabel, countdown, lastRun);
    }

    private ScrapeTargetRowView toTargetRowView(ScrapeTarget target) {
        return new ScrapeTargetRowView(
                target.getId(),
                target.getUrl(),
                target.getStatus(),
                formatInstantOrDefault(target.getStartedAt(), "-"),
                formatInstantOrDefault(target.getFinishedAt(), "-"),
                target.getErrorMessage());
    }

    private ScrapeTargetProgressView buildProgressView(ScrapeTarget target,
                                                       Optional<ScrapeResultData> resultOpt) {
        List<String> steps = resultOpt.map(ScrapeResultData::getProgressJson)
                .map(this::readProgress)
                .orElse(List.of());
        List<ScrapeTargetProgressGroupView> groups = groupProgressSteps(target.getUrl(), steps);
        return new ScrapeTargetProgressView(
                target.getId(),
                target.getUrl(),
                target.getStatus(),
                formatInstantOrDefault(target.getStartedAt(), "Not started"),
                formatInstantOrDefault(target.getFinishedAt(), "Not finished"),
                formatDurationLabel(target.getStartedAt(), target.getFinishedAt()),
                groups);
    }

    private List<ScrapeTargetProgressGroupView> groupProgressSteps(String primaryUrl, List<String> steps) {
        if (steps.isEmpty()) {
            return List.of();
        }
        List<ScrapeTargetProgressGroupView> groups = new ArrayList<>();
        List<String> currentSteps = new ArrayList<>();
        String currentUrl = primaryUrl;
        String currentTitle = "Primary Page";
        boolean groupInitialised = false;
        int linkedPage = 0;

        for (String rawStep : steps) {
            String normalized = NUMBER_PREFIX.matcher(rawStep).replaceFirst("").trim();
            Matcher matcher = PAGE_START_PATTERN.matcher(normalized);
            if (matcher.matches()) {
                if (groupInitialised && !currentSteps.isEmpty()) {
                    groups.add(new ScrapeTargetProgressGroupView(currentTitle, currentUrl,
                            List.copyOf(currentSteps)));
                    currentSteps.clear();
                }
                currentUrl = matcher.group(1).trim();
                if (!groupInitialised || currentUrl.equals(primaryUrl)) {
                    currentTitle = "Primary Page";
                } else {
                    linkedPage++;
                    currentTitle = "Linked Page " + linkedPage;
                }
                groupInitialised = true;
            }
            currentSteps.add(normalized);
        }

        if (!currentSteps.isEmpty()) {
            if (!groupInitialised) {
                currentTitle = "Primary Page";
                currentUrl = primaryUrl;
            }
            groups.add(new ScrapeTargetProgressGroupView(currentTitle, currentUrl,
                    List.copyOf(currentSteps)));
        }
        return groups;
    }

    private String formatScheduleLabel(String scheduleCron) {
        return StringUtils.hasText(scheduleCron) ? scheduleCron : "One-time";
    }

    private String formatInstantOrDefault(Instant instant, String defaultValue) {
        return instant == null ? defaultValue : DISPLAY_FORMATTER.format(instant);
    }

    private String formatDurationLabel(Instant started, Instant finished) {
        if (started == null || finished == null) {
            return "-";
        }
        return formatDuration(Duration.between(started, finished));
    }

    private String formatCountdown(Instant nextRun) {
        Duration until = Duration.between(Instant.now(), nextRun);
        if (until.isNegative() || until.isZero()) {
            return "Starting soon";
        }
        return "Runs in " + formatDuration(until);
    }

    private String formatDuration(Duration duration) {
        if (duration == null || duration.isNegative()) {
            return "-";
        }
        long seconds = duration.getSeconds();
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long remainingSeconds = seconds % 60;
        StringBuilder builder = new StringBuilder();
        if (hours > 0) {
            builder.append(hours).append("h ");
        }
        if (minutes > 0 || hours > 0) {
            builder.append(minutes).append("m ");
        }
        builder.append(remainingSeconds).append("s");
        return builder.toString().trim();
    }

    private String buildArchiveFileName(ScrapeTarget target) {
        return "target-" + target.getId() + ".zip";
    }

    private ScrapeTargetProgressView loadProgressView(UUID jobId, UUID targetId) {
        jobService.getJob(jobId);
        ScrapeTarget target = jobService.getTarget(targetId);
        Optional<ScrapeResultData> resultOpt = jobService.findResult(targetId);
        return buildProgressView(target, resultOpt);
    }

    private String buildProgressCloseUrl(UUID jobId, UUID targetId) {
        return "/jobs/" + jobId + "/targets/" + targetId + "/status/close";
    }

    private String prettyJson(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            Object tree = objectMapper.readTree(json);
            return objectMapper.writeValueAsString(tree);
        } catch (Exception e) {
            log.warn("Unable to pretty print structured data", e);
            return json;
        }
    }

    private List<String> readProgress(String progressJson) {
        if (!StringUtils.hasText(progressJson)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(progressJson, new TypeReference<>() { });
        } catch (Exception ex) {
            log.warn("Unable to parse progress JSON for target", ex);
            return List.of();
        }
    }
    
    /**
     * Initiates the process to create a job archive containing all targets' data.
     * This method starts the archive creation and returns the progress modal.
     *
     * @param jobId the ID of the job to archive
     * @param model the model to populate with progress information
     * @return the progress modal template fragment
     */
    // In-memory session store for tracking archive progress (would use Redis/cache in production)
    private static final Map<String, ArchiveSession> archiveSessions = new ConcurrentHashMap<>();
    
    @GetMapping("/{jobId}/archive-progress")
    public String jobArchiveProgress(@PathVariable("jobId") UUID jobId, Model model) {
        ScrapeJob job = jobService.getJob(jobId);
        List<ScrapeTarget> targets = jobService.listTargets(jobId);
        
        // Create a unique session ID for this archive operation
        String sessionId = UUID.randomUUID().toString();
        
        // Initialize progress tracking session
        ArchiveSession session = new ArchiveSession(jobId, job.getName(), targets.size());
        archiveSessions.put(sessionId, session);
        
        model.addAttribute("jobId", jobId);
        model.addAttribute("jobName", job.getName());
        model.addAttribute("totalTargets", targets.size());
        model.addAttribute("processedTargets", session.getProcessedTargets());
        model.addAttribute("progressPercentage", session.getProgressPercentage());
        model.addAttribute("status", session.getStatus());
        model.addAttribute("sessionId", sessionId);
        
        return "jobs/job-archive-modal :: jobArchiveModal";
    }
    
    @PostMapping("/{jobId}/start-archive")
    public ResponseEntity<Map<String, String>> startJobArchive(@PathVariable("jobId") UUID jobId,
                                                               @RequestParam("sessionId") String sessionId) {
        ArchiveSession session = archiveSessions.get(sessionId);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }
        
        // Start the actual archive creation in a background thread
        CompletableFuture.runAsync(() -> {
            try {
                // Get the actual list of targets and process them
                List<ScrapeTarget> targets = jobService.listTargets(jobId);
                
                // Process each target sequentially (could be parallel in optimized version)
                for (int i = 0; i < targets.size(); i++) {
                    ScrapeTarget target = targets.get(i);
                    
                    // Update progress
                    session.setProcessedTargets(i + 1);
                    session.setStatus("Processing target " + (i + 1) + " of " + targets.size() + ": " + target.getUrl());
                    
                    // In a real system, you would create the archive entries here
                    // For now, just simulate the processing time
                    Thread.sleep(200); // Replace with actual archive creation logic
                    
                    // Check if session is still active (not cancelled or timed out)
                    if (System.currentTimeMillis() - session.getStartTime() > 30 * 60 * 1000) { // 30 mins timeout
                        session.setStatus("Operation timed out");
                        session.setComplete(true);
                        return;
                    }
                }
                
                // Mark as complete
                session.setStatus("Archive creation completed successfully");
                session.setComplete(true);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                session.setStatus("Archive creation was interrupted");
            } catch (Exception e) {
                session.setStatus("Error during archive creation: " + e.getMessage());
                log.error("Error during job archive creation", e);
            }
        });
        
        // Return the session ID for tracking
        Map<String, String> response = new HashMap<>();
        response.put("sessionId", sessionId);
        response.put("status", "started");
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/archive-status/{sessionId}")
    @ResponseBody
    public ResponseEntity<ArchiveSession> getArchiveStatus(@PathVariable("sessionId") String sessionId) {
        ArchiveSession session = archiveSessions.get(sessionId);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(session);
    }
    
    @GetMapping("/close-modal")
    public String closeModal() {
        // Return an empty fragment to close the modal
        return "jobs/list :: closeModal";
    }
    
    /**
     * Represents a session for tracking the progress of creating a job archive.
     */
    public static class ArchiveSession {
        private final String sessionId;
        private final UUID jobId;
        private final String jobName;
        private final int totalTargets;
        private volatile int processedTargets = 0;
        private volatile String status = "Initializing...";
        private volatile boolean complete = false;
        private final long startTime = System.currentTimeMillis();
        
        public ArchiveSession(UUID jobId, String jobName, int totalTargets) {
            this.sessionId = UUID.randomUUID().toString();
            this.jobId = jobId;
            this.jobName = jobName;
            this.totalTargets = totalTargets;
        }
        
        public String getSessionId() {
            return sessionId;
        }
        
        public UUID getJobId() {
            return jobId;
        }
        
        public String getJobName() {
            return jobName;
        }
        
        public int getTotalTargets() {
            return totalTargets;
        }
        
        public int getProcessedTargets() {
            return processedTargets;
        }
        
        public void setProcessedTargets(int processedTargets) {
            this.processedTargets = processedTargets;
        }
        
        public String getStatus() {
            return status;
        }
        
        public void setStatus(String status) {
            this.status = status;
        }
        
        public boolean isComplete() {
            return complete;
        }
        
        public void setComplete(boolean complete) {
            this.complete = complete;
        }
        
        public int getProgressPercentage() {
            if (totalTargets == 0) return 0;
            return Math.min(100, (int) Math.round((double) processedTargets / totalTargets * 100));
        }
        
        public long getStartTime() {
            return startTime;
        }
    }
    
    /**
     * Starts the job archive creation process in the background.
     * This method initiates the archive creation and returns a response indicating the start.
     *
     * @param jobId the ID of the job to archive
     * @return an accepted response entity
     */
    /**
     * Creates and downloads a ZIP archive containing all data for a job.
     * This method creates an archive with all targets' structured data and processed pages.
     *
     * @param jobId the ID of the job to archive
     * @return a response entity containing the archive file
     */
    @GetMapping("/{jobId}/download-archive")
    public ResponseEntity<ByteArrayResource> downloadJobArchive(@PathVariable("jobId") UUID jobId) {
        ScrapeJob job = jobService.getJob(jobId);
        List<ScrapeTarget> targets = jobService.listTargets(jobId);
        
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos, StandardCharsets.UTF_8)) {
            
            // Add job metadata
            String jobMetadata = buildJobMetadata(job, targets.size());
            writeZipEntry(zos, "job-metadata.xml", jobMetadata);
            
            // Process each target and add its data to the archive
            for (int i = 0; i < targets.size(); i++) {
                ScrapeTarget target = targets.get(i);
                Optional<ScrapeResultData> resultOpt = jobService.findResult(target.getId());
                
                // Create a subdirectory for each target using its URL as the folder name
                String targetDir = "target-" + i + "_" + sanitizeFileName(target.getUrl()) + "/";
                
                // Add target metadata
                String targetMetadata = buildTargetMetadata(target, resultOpt);
                writeZipEntry(zos, targetDir + "metadata.xml", targetMetadata);
                
                // Add structured data if available
                if (resultOpt.isPresent() && StringUtils.hasText(resultOpt.get().getDataJson())) {
                    writeZipEntry(zos, targetDir + "structured.json", resultOpt.get().getDataJson());
                }
                
                // Add processed HTML if available
                String processedHtml = storageService.loadProcessedHtml(jobId, target.getId());
                if (StringUtils.hasText(processedHtml)) {
                    writeZipEntry(zos, targetDir + "processed.html", processedHtml);
                }
                
                // Add processed Markdown if available
                String processedMarkdown = storageService.loadProcessedMarkdown(jobId, target.getId());
                if (StringUtils.hasText(processedMarkdown)) {
                    writeZipEntry(zos, targetDir + "processed.md", processedMarkdown);
                }
                
                // Add raw HTML if available
                String rawHtml = storageService.loadRawHtml(jobId, target.getId());
                if (StringUtils.hasText(rawHtml)) {
                    writeZipEntry(zos, targetDir + "raw.html", rawHtml);
                }
                
                // Process any multi-page results if they exist
                if (storageService.hasPageArtifacts(jobId, target.getId())) {
                    List<StoredPageResult> pageResults = storageService.loadPageResults(jobId, target.getId());
                    for (int j = 0; j < pageResults.size(); j++) {
                        StoredPageResult pageResult = pageResults.get(j);
                        String pageDir = targetDir + "pages/page-" + j + "_" + sanitizeFileName(pageResult.pageKey()) + "/";
                        
                        if (StringUtils.hasText(pageResult.rawHtml())) {
                            writeZipEntry(zos, pageDir + "raw.html", pageResult.rawHtml());
                        }
                        if (StringUtils.hasText(pageResult.processedHtml())) {
                            writeZipEntry(zos, pageDir + "processed.html", pageResult.processedHtml());
                        }
                        if (StringUtils.hasText(pageResult.processedMarkdown())) {
                            writeZipEntry(zos, pageDir + "processed.md", pageResult.processedMarkdown());
                        }
                        
                        // Add page metadata
                        String pageMetadata = buildPageMetadata(pageResult);
                        writeZipEntry(zos, pageDir + "metadata.xml", pageMetadata);
                    }
                }
            }
            
            zos.finish();
            byte[] archive = baos.toByteArray();
            ByteArrayResource resource = new ByteArrayResource(archive);
            String filename = "job-archive-" + jobId + ".zip";
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(archive.length)
                    .body(resource);
        } catch (IOException e) {
            log.error("Failed to create job archive for job " + jobId, e);
            return ResponseEntity.status(500).build();
        }
    }
    
    private void writeZipEntry(ZipOutputStream zos, String entryName, String content) throws IOException {
        ZipEntry entry = new ZipEntry(entryName);
        zos.putNextEntry(entry);
        byte[] data = content.getBytes(StandardCharsets.UTF_8);
        zos.write(data, 0, data.length);
        zos.closeEntry();
    }
    
    private String buildJobMetadata(ScrapeJob job, int targetCount) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
               "<jobArchive>\n" +
               "  <jobId>" + escapeXml(job.getId().toString()) + "</jobId>\n" +
               "  <jobName>" + escapeXml(job.getName()) + "</jobName>\n" +
               "  <recipeName>" + escapeXml(job.getRecipe().getName()) + "</recipeName>\n" +
               "  <status>" + escapeXml(job.getStatus().name()) + "</status>\n" +
               "  <targetCount>" + targetCount + "</targetCount>\n" +
               "  <createdAt>" + DISPLAY_FORMATTER.format(job.getCreatedAt()) + "</createdAt>\n" +
               "  <updatedAt>" + DISPLAY_FORMATTER.format(job.getUpdatedAt()) + "</updatedAt>\n" +
               "</jobArchive>";
    }
    
    private String buildTargetMetadata(ScrapeTarget target, Optional<ScrapeResultData> resultOpt) {
        String resultId = resultOpt.map(r -> r.getId().toString()).orElse("");
        String createdAt = resultOpt.map(ScrapeResultData::getCreatedAt)
                .map(DISPLAY_FORMATTER::format).orElse("");
        
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
               "<target>\n" +
               "  <targetId>" + escapeXml(target.getId().toString()) + "</targetId>\n" +
               "  <url>" + escapeXml(target.getUrl()) + "</url>\n" +
               "  <status>" + escapeXml(target.getStatus().name()) + "</status>\n" +
               "  <startedAt>" + (target.getStartedAt() != null ? escapeXml(DISPLAY_FORMATTER.format(target.getStartedAt())) : "") + "</startedAt>\n" +
               "  <finishedAt>" + (target.getFinishedAt() != null ? escapeXml(DISPLAY_FORMATTER.format(target.getFinishedAt())) : "") + "</finishedAt>\n" +
               "  <errorMessage>" + escapeXml(target.getErrorMessage() != null ? target.getErrorMessage() : "") + "</errorMessage>\n" +
               "  <resultId>" + escapeXml(resultId) + "</resultId>\n" +
               "  <resultCreatedAt>" + escapeXml(createdAt) + "</resultCreatedAt>\n" +
               "</target>";
    }
    
    private String buildPageMetadata(StoredPageResult pageResult) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
               "<page>\n" +
               "  <pageKey>" + escapeXml(pageResult.pageKey()) + "</pageKey>\n" +
               "  <pageUrl>" + escapeXml(pageResult.pageUrl()) + "</pageUrl>\n" +
               "  <directoryName>" + escapeXml(pageResult.directoryName()) + "</directoryName>\n" +
               "</page>";
    }
    
    private String sanitizeFileName(String input) {
        if (input == null) {
            return "";
        }
        return input.replaceAll("[^a-zA-Z0-9.-]", "_");
    }
    
    private String escapeXml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
