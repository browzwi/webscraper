package com.browzwi.webscraper.web;

import com.browzwi.webscraper.domain.DiscoveredBusiness;
import com.browzwi.webscraper.domain.DiscoveryJob;
import com.browzwi.webscraper.service.BusinessWebsiteScrapeService;
import com.browzwi.webscraper.service.UrlDiscoveryService;
import com.browzwi.webscraper.service.settings.SettingsService;
import com.browzwi.webscraper.service.settings.DiscoverySourceType;
import com.browzwi.webscraper.repository.DiscoveryJobRepository;
import com.browzwi.webscraper.repository.DiscoveredBusinessRepository;
import jakarta.validation.Valid;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for managing URL discovery operations.
 *
 * @since 1.0
 */
@Controller
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/discovery")
public class DiscoveryController {

    private final UrlDiscoveryService urlDiscoveryService;
    private final DiscoveryJobRepository discoveryJobRepository;
    private final DiscoveredBusinessRepository businessRepository;

    public DiscoveryController(UrlDiscoveryService urlDiscoveryService,
                               DiscoveryJobRepository discoveryJobRepository,
                               DiscoveredBusinessRepository businessRepository) {
        this.urlDiscoveryService = urlDiscoveryService;
        this.discoveryJobRepository = discoveryJobRepository;
        this.businessRepository = businessRepository;
    }

    @GetMapping
    public String showDiscoveryPage(Model model) {
        List<DiscoveryJob> jobs = urlDiscoveryService.listJobs();
        model.addAttribute("pageTitle", "URL Discovery");
        model.addAttribute("jobs", jobs);
        model.addAttribute("discoveryForm", new DiscoveryForm());
        return "discovery/index";
    }

    @PostMapping("/start")
    public String startDiscovery(@Valid @ModelAttribute("discoveryForm") DiscoveryForm form,
                                  Model model) {
        if (form.getKeyword() == null || form.getKeyword().isBlank() ||
            form.getLocation() == null || form.getLocation().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Keyword and location are required");
        }

        // Start discovery in background thread
        DiscoveryJob job = new DiscoveryJob();
        job.setKeyword(form.getKeyword());
        job.setLocation(form.getLocation());
        job.setTargetPageType(form.getTargetPageType());
        job.setCustomSearchPattern(form.getCustomPattern());
        job.setStatus("RUNNING");
        job.setCreatedAt(LocalDateTime.now());
        job.setDiscoverySource("GMAPS_GOOGLE_SEARCH");
        discoveryJobRepository.save(job);

        // Run discovery asynchronously
        CompletableFuture.runAsync(() -> {
            try {
                urlDiscoveryService.discoverAsync(
                    job.getId(), 
                    form.getKeyword(), 
                    form.getLocation(), 
                    form.getTargetPageType(),
                    form.getCustomPattern(),
                    null
                );
            } catch (Exception e) {
                job.setStatus("FAILED");
                job.setCompletedAt(LocalDateTime.now());
                discoveryJobRepository.save(job);
            }
        });

        model.addAttribute("jobId", job.getId());
        model.addAttribute("keyword", form.getKeyword());
        model.addAttribute("location", form.getLocation());
        return "discovery/progress :: progressFragment";
    }

    @GetMapping("/{jobId}/progress")
    public String getProgress(@PathVariable Long jobId, Model model) {
        DiscoveryJob job = discoveryJobRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        
        model.addAttribute("job", job);
        model.addAttribute("businesses", businessRepository.findByDiscoveryJobIdOrderByBusinessNameAsc(jobId));
        
        if ("COMPLETED".equals(job.getStatus()) || "FAILED".equals(job.getStatus())) {
            return "discovery/progress :: completedFragment";
        }
        return "discovery/progress :: progressFragment";
    }

    @GetMapping("/job/{jobId}/status")
    public String getJobStatus(@PathVariable Long jobId, Model model) {
        DiscoveryJob job = discoveryJobRepository.findById(jobId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));
        
        List<DiscoveredBusiness> businesses = businessRepository.findByDiscoveryJobIdOrderByBusinessNameAsc(jobId);
        
        model.addAttribute("job", job);
        model.addAttribute("businesses", businesses);
        return "discovery/results :: businessList";
    }

    @GetMapping("/{jobId}/results")
    public String viewResults(@PathVariable Long jobId, Model model) {
        DiscoveryJob job = urlDiscoveryService.getJob(jobId);
        List<DiscoveredBusiness> businesses = businessRepository.findByDiscoveryJobIdOrderByBusinessNameAsc(jobId);

        model.addAttribute("pageTitle", "Discovery Results");
        model.addAttribute("job", job);
        model.addAttribute("businesses", businesses);
        return "discovery/results";
    }

    @GetMapping("/{jobId}/export")
    @ResponseBody
    public ResponseEntity<byte[]> exportResults(@PathVariable Long jobId) throws IOException {
        List<DiscoveredBusiness> businesses = businessRepository.findByDiscoveryJobIdOrderByBusinessNameAsc(jobId);

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Discovered Businesses");

            Row headerRow = sheet.createRow(0);
            String[] headers = {"Business Name", "Address", "Email", "Social Media Links", "Contact Number", "Website"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            int rowNum = 1;
            for (DiscoveredBusiness business : businesses) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(business.getBusinessName() != null ? business.getBusinessName() : "");
                row.createCell(1).setCellValue(business.getAddress() != null ? business.getAddress() : "");
                row.createCell(2).setCellValue(business.getEmailAddress() != null ? business.getEmailAddress() : "");
                row.createCell(3).setCellValue(business.getSocialMediaLinks() != null ? business.getSocialMediaLinks() : "");
                row.createCell(4).setCellValue(business.getPhoneNumber() != null ? business.getPhoneNumber() : "");
                row.createCell(5).setCellValue(business.getWebsiteUrl() != null ? business.getWebsiteUrl() : "");
            }

            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);
            sheet.autoSizeColumn(2);
            sheet.autoSizeColumn(3);
            sheet.autoSizeColumn(4);
            sheet.autoSizeColumn(5);

            workbook.write(outputStream);

            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            httpHeaders.setContentDispositionFormData("attachment", "discovery-results-" + jobId + ".xlsx");

            return new ResponseEntity<>(outputStream.toByteArray(), httpHeaders, HttpStatus.OK);
        }
    }

    @PostMapping("/business/{businessId}/scrape")
    public String scrapeBusinessWebsite(@PathVariable Long businessId,
                                        RedirectAttributes redirectAttributes) {
        DiscoveredBusiness business = businessRepository.findById(businessId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Business not found"));

        redirectAttributes.addFlashAttribute("message", "Use Jobs to scrape discovered URLs");
        return "redirect:/discovery/" + business.getDiscoveryJob().getId() + "/results";
    }

    public static class DiscoveryForm {
        private String keyword = "";
        private String location = "";
        private String targetPageType = "FACEBOOK";
        private String customPattern = "";

        public DiscoveryForm() {
        }

        public String getKeyword() {
            return keyword;
        }

        public void setKeyword(String keyword) {
            this.keyword = keyword;
        }

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }

        public String getTargetPageType() {
            return targetPageType;
        }

        public void setTargetPageType(String targetPageType) {
            this.targetPageType = targetPageType;
        }

        public String getCustomPattern() {
            return customPattern;
        }

        public void setCustomPattern(String customPattern) {
            this.customPattern = customPattern;
        }
    }
}
