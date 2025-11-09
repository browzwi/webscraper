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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
    private final ObjectMapper objectMapper;

    public ScrapeJobController(ScrapeJobService jobService,
                               ScraperRecipeRepository recipeRepository,
                               ScrapeTargetArchiveService archiveService,
                               ObjectMapper objectMapper) {
        this.jobService = jobService;
        this.recipeRepository = recipeRepository;
        this.archiveService = archiveService;
        this.objectMapper = objectMapper.copy().enable(SerializationFeature.INDENT_OUTPUT);
    }

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

    @PostMapping("/{id}/run")
    public ResponseEntity<Void> runJob(@PathVariable UUID id) {
        jobService.runJobNow(id);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/new")
    public String newJob(Model model) {
        model.addAttribute("pageTitle", "New Job");
        model.addAttribute("job", new JobForm());
        model.addAttribute("recipes", recipeRepository.findAll());
        return "jobs/form";
    }

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

    @GetMapping("/{id}")
    public String jobDetail(@PathVariable UUID id, Model model) {
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

    @GetMapping("/{jobId}/targets/{targetId}")
    public String targetDetails(@PathVariable UUID jobId,
                                @PathVariable UUID targetId,
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

    @GetMapping("/{jobId}/targets/{targetId}/archive")
    public ResponseEntity<ByteArrayResource> downloadArchive(@PathVariable UUID jobId,
                                                              @PathVariable UUID targetId) {
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

    @GetMapping("/{jobId}/targets/{targetId}/status")
    public String targetProgress(@PathVariable UUID jobId,
                                 @PathVariable UUID targetId,
                                 Model model) {
        jobService.getJob(jobId);
        ScrapeTarget target = jobService.getTarget(targetId);
        Optional<ScrapeResultData> resultOpt = jobService.findResult(targetId);
        ScrapeTargetProgressView progressView = buildProgressView(target, resultOpt);
        model.addAttribute("progress", progressView);
        return "jobs/target-progress :: content";
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
        return "target-" + target.getId() + ".webarchive";
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
}
