package com.browzwi.webscraper.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.browzwi.webscraper.domain.ScrapeJob;
import com.browzwi.webscraper.domain.ScrapeResultData;
import com.browzwi.webscraper.domain.ScrapeTarget;
import com.browzwi.webscraper.repository.ScraperRecipeRepository;
import com.browzwi.webscraper.service.ScrapeJobService;
import com.browzwi.webscraper.scraper.service.MarkdownConversionService;
import com.browzwi.webscraper.storage.FileStorageService;
import com.browzwi.webscraper.web.dto.JobForm;
import jakarta.validation.Valid;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/jobs")
@PreAuthorize("hasRole('ADMIN')")
public class ScrapeJobController {

    private static final Logger log = LoggerFactory.getLogger(ScrapeJobController.class);

    private final ScrapeJobService jobService;
    private final ScraperRecipeRepository recipeRepository;
    private final FileStorageService storageService;
    private final MarkdownConversionService markdownConversionService;
    private final ObjectMapper objectMapper;

    public ScrapeJobController(ScrapeJobService jobService,
                               ScraperRecipeRepository recipeRepository,
                               FileStorageService storageService,
                               MarkdownConversionService markdownConversionService,
                               ObjectMapper objectMapper) {
        this.jobService = jobService;
        this.recipeRepository = recipeRepository;
        this.storageService = storageService;
        this.markdownConversionService = markdownConversionService;
        this.objectMapper = objectMapper.copy().enable(SerializationFeature.INDENT_OUTPUT);
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("pageTitle", "Jobs");
        model.addAttribute("jobs", jobService.listJobs());
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
        List<ScrapeTarget> targets = jobService.listTargets(id);
        model.addAttribute("pageTitle", job.getName());
        model.addAttribute("job", job);
        model.addAttribute("targets", targets);
        return "jobs/detail";
    }

    @GetMapping("/{jobId}/targets/{targetId}")
    public String targetDetails(@PathVariable UUID jobId,
                                @PathVariable UUID targetId,
                                @RequestParam(name = "view", required = false) String view,
                                Model model) {
        jobService.getJob(jobId);
        ScrapeTarget target = jobService.getTarget(targetId);
        Optional<ScrapeResultData> result = jobService.findResult(targetId);
        String structured = result.map(r -> prettyJson(r.getDataJson())).orElse(null);
        List<String> progressSteps = result.map(ScrapeResultData::getProgressJson)
                .map(this::readProgress)
                .orElseGet(Collections::emptyList);
        String rawHtml = storageService.loadRawHtml(jobId, targetId);
        String processedHtml = storageService.loadProcessedHtml(jobId, targetId);
        String processedMarkdown = processedHtml != null && !processedHtml.isBlank()
                ? markdownConversionService.toMarkdown(processedHtml)
                : null;
        model.addAttribute("target", target);
        model.addAttribute("structured", structured);
        model.addAttribute("rawHtml", rawHtml);
        model.addAttribute("processedHtml", processedHtml);
        model.addAttribute("processedMarkdown", processedMarkdown);
        model.addAttribute("progressSteps", progressSteps);
        model.addAttribute("hasResult", result.isPresent());
        model.addAttribute("hasRawHtml", rawHtml != null && !rawHtml.isBlank());
        model.addAttribute("hasProcessedHtml", processedHtml != null && !processedHtml.isBlank());
        model.addAttribute("hasMarkdown", processedMarkdown != null && !processedMarkdown.isBlank());
        model.addAttribute("hasProgress", !progressSteps.isEmpty());
        model.addAttribute("activeTab", resolveActiveTab(view, result.isPresent(), processedHtml, processedMarkdown,
                !progressSteps.isEmpty(), rawHtml));
        return "jobs/target-details :: content";
    }

    private String prettyJson(String json) {
        try {
            Object tree = objectMapper.readTree(json);
            return objectMapper.writeValueAsString(tree);
        } catch (Exception e) {
            return json;
        }
    }

    private List<String> readProgress(String progressJson) {
        if (progressJson == null || progressJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(progressJson, new TypeReference<>() { });
        } catch (Exception ex) {
            log.warn("Unable to parse progress JSON for target", ex);
            return Collections.emptyList();
        }
    }

    private String resolveActiveTab(String view,
                                    boolean hasResult,
                                    String processedHtml,
                                    String processedMarkdown,
                                    boolean hasProgress,
                                    String rawHtml) {
        if (view != null) {
            String normalized = view.toLowerCase();
            if ("progress".equals(normalized) && hasProgress) {
                return "tab-steps";
            }
            if ("html".equals(normalized) && processedHtml != null && !processedHtml.isBlank()) {
                return "tab-html";
            }
            if ("markdown".equals(normalized) && processedMarkdown != null && !processedMarkdown.isBlank()) {
                return "tab-markdown";
            }
            if ("raw".equals(normalized) && rawHtml != null && !rawHtml.isBlank()) {
                return "tab-raw";
            }
            if ("data".equals(normalized) && hasResult) {
                return "tab-data";
            }
        }
        if (hasResult) {
            return "tab-data";
        }
        return "tab-overview";
    }
}
