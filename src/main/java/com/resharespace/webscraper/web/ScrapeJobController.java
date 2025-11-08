package com.browzwi.webscraper.web;

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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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
                                Model model) {
        jobService.getJob(jobId);
        ScrapeTarget target = jobService.getTarget(targetId);
        Optional<ScrapeResultData> result = jobService.findResult(targetId);
        String structured = result.map(r -> prettyJson(r.getDataJson())).orElse("No structured data yet");
        String rawHtml = Optional.ofNullable(storageService.loadRawHtml(jobId, targetId))
                .orElse("No raw HTML stored yet");
        String processedHtml = Optional.ofNullable(storageService.loadProcessedHtml(jobId, targetId))
                .orElse("No processed HTML stored yet");
        String processedMarkdown = markdownConversionService.toMarkdown(processedHtml);
        model.addAttribute("target", target);
        model.addAttribute("structured", structured);
        model.addAttribute("rawHtml", rawHtml);
        model.addAttribute("processedHtml", processedHtml);
        model.addAttribute("processedMarkdown", processedMarkdown);
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
}
