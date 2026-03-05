package com.browzwi.webscraper.service;

import com.browzwi.webscraper.domain.ScrapeJob;
import com.browzwi.webscraper.domain.ScrapeResultData;
import com.browzwi.webscraper.domain.ScrapeTarget;
import com.browzwi.webscraper.scraper.service.MarkdownConversionService;
import com.browzwi.webscraper.storage.FileStorageService;
import com.browzwi.webscraper.storage.FileStorageService.StoredPageResult;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.stereotype.Service;

/**
 * Builds downloadable web archives encapsulating the artefacts for a scrape target execution.
 * Centralises archive generation so controllers remain focused on orchestrating view rendering
 * while this service composes filesystem artefacts and metadata into a portable representation.
 * Archives are emitted as ZIP streams with a .zip extension, include a metadata XML descriptor,
 * and embed artefacts under a randomly generated directory to avoid collisions when jobs run repeatedly.
 *
 * @since 1.0
 */
@Service
public class ScrapeTargetArchiveService {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    private final FileStorageService storageService;
    private final MarkdownConversionService markdownConversionService;

    public ScrapeTargetArchiveService(FileStorageService storageService,
                                      MarkdownConversionService markdownConversionService) {
        this.storageService = storageService;
        this.markdownConversionService = markdownConversionService;
    }

    /**
     * Determines whether an archive can be produced for the given target by checking that at least
     * one artefact (raw HTML, processed HTML, processed Markdown, per-page capture, or structured
     * data) exists. This avoids surprising 404s by exposing the availability check to the
     * controller before attempting archive generation.
     *
     * @param jobId identifier of the parent job whose filesystem directory is inspected
     * @param targetId target identifier used to locate stored artefacts
     * @param result scrape result persistence entity, if captured
     * @return {@code true} when an archive can be created, {@code false} otherwise
     */
    public boolean canBuildArchive(UUID jobId, UUID targetId, Optional<ScrapeResultData> result) {
        return storageService.existsRawHtml(jobId, targetId)
                || storageService.existsProcessedHtml(jobId, targetId)
                || storageService.existsProcessedMarkdown(jobId, targetId)
                || storageService.hasPageArtifacts(jobId, targetId)
                || result.isPresent();
    }

    /**
     * Produces a zipped web archive containing scrape artefacts and an XML metadata descriptor.
     * Uses UTF-8 encoded ZIP entries to ensure cross-platform compatibility and wraps IO failures
     * in a dedicated runtime exception for the controller to translate into HTTP responses.
     *
     * @param job job that triggered the scrape
     * @param target scrape target whose artefacts are requested
     * @param result optional structured result persistence entity
     * @return binary representation of the archive ready for HTTP streaming
     */
    public byte[] buildArchive(ScrapeJob job,
                               ScrapeTarget target,
                               Optional<ScrapeResultData> result) {
        if (!canBuildArchive(job.getId(), target.getId(), result)) {
            throw new ArchiveCreationException("No artefacts available for target " + target.getId());
        }

        String rawHtml = storageService.loadRawHtml(job.getId(), target.getId());
        String processedHtml = storageService.loadProcessedHtml(job.getId(), target.getId());
        String processedMarkdown = storageService.loadProcessedMarkdown(job.getId(), target.getId());
        if ((processedMarkdown == null || processedMarkdown.isBlank())
                && processedHtml != null && !processedHtml.isBlank()) {
            processedMarkdown = markdownConversionService.toMarkdown(processedHtml);
        }
        String structuredJson = result.map(ScrapeResultData::getDataJson).orElse(null);
        List<StoredPageResult> pageResults = storageService.loadPageResults(job.getId(), target.getId());

        List<ArchiveFile> artefacts = new ArrayList<>();
        if (rawHtml != null && !rawHtml.isBlank()) {
            artefacts.add(new ArchiveFile("raw.html", rawHtml));
        }
        if (processedHtml != null && !processedHtml.isBlank()) {
            artefacts.add(new ArchiveFile("processed.html", processedHtml));
        }
        if (processedMarkdown != null && !processedMarkdown.isBlank()) {
            artefacts.add(new ArchiveFile("processed.md", processedMarkdown));
        }
        if (structuredJson != null && !structuredJson.isBlank()) {
            artefacts.add(new ArchiveFile("structured.json", structuredJson));
        }

        List<PageArchive> pageArchives = pageResults.stream()
                .map(page -> toPageArchive(page, artefacts))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        UUID artefactDirId = UUID.randomUUID();
        String metadata = buildMetadata(job, target, result, artefactDirId, artefacts, pageArchives);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos, StandardCharsets.UTF_8)) {
            writeEntry(zos, "metadata.xml", metadata);
            for (ArchiveFile file : artefacts) {
                String entryName = "artifacts/" + artefactDirId + "/" + file.relativePath();
                writeEntry(zos, entryName, file.content());
            }
            zos.finish();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new ArchiveCreationException("Failed to assemble archive for target " + target.getId(), e);
        }
    }

    /**
     * Produces a zipped archive containing all targets' structured data and processed pages for a job.
     * Uses UTF-8 encoded ZIP entries to ensure cross-platform compatibility and wraps IO failures
     * in a dedicated runtime exception for the controller to translate into HTTP responses.
     *
     * @param job the job whose targets are to be archived
     * @return binary representation of the archive ready for HTTP streaming
     */

    private String buildMetadata(ScrapeJob job,
                                 ScrapeTarget target,
                                 Optional<ScrapeResultData> result,
                                 UUID artefactDirId,
                                 List<ArchiveFile> artefacts,
                                 List<PageArchive> pageArchives) {
        String started = formatInstant(target.getStartedAt());
        String finished = formatInstant(target.getFinishedAt());
        String durationSeconds = formatDurationSeconds(target.getStartedAt(), target.getFinishedAt());
        String resultId = result.map(r -> r.getId().toString()).orElse("");
        String resultCreatedAt = result.map(ScrapeResultData::getCreatedAt)
                .map(this::formatInstant)
                .orElse("");

        String filesXml = artefacts.stream()
                .map(file -> "    <file name=\"" + escapeXml(file.relativePath()) + "\">artifacts/"
                        + artefactDirId + "/" + escapeXml(file.relativePath()) + "</file>")
                .collect(Collectors.joining("\n"));

        String pagesXml = pageArchives.isEmpty()
                ? ""
                : pageArchives.stream()
                        .map(page -> buildPageMetadata(artefactDirId, page))
                        .collect(Collectors.joining("\n"));

        return Stream.of(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<webArchive>",
                "  <jobId>" + escapeXml(job.getId().toString()) + "</jobId>",
                "  <targetId>" + escapeXml(target.getId().toString()) + "</targetId>",
                "  <targetUrl>" + escapeXml(target.getUrl()) + "</targetUrl>",
                "  <status>" + escapeXml(target.getStatus().name()) + "</status>",
                "  <startedAt>" + escapeXml(started) + "</startedAt>",
                "  <finishedAt>" + escapeXml(finished) + "</finishedAt>",
                "  <durationSeconds>" + escapeXml(durationSeconds) + "</durationSeconds>",
                "  <resultId>" + escapeXml(resultId) + "</resultId>",
                "  <resultCreatedAt>" + escapeXml(resultCreatedAt) + "</resultCreatedAt>",
                "  <artifactDirectoryId>" + escapeXml(artefactDirId.toString()) + "</artifactDirectoryId>",
                "  <generatedAt>" + escapeXml(formatInstant(Instant.now())) + "</generatedAt>",
                "  <files>",
                filesXml,
                "  </files>",
                "  <pages>",
                pagesXml,
                "  </pages>",
                "</webArchive>")
                .filter(line -> line != null && !line.isBlank())
                .collect(Collectors.joining("\n"));
    }

    private void writeEntry(ZipOutputStream zos, String name, String contents) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        zos.putNextEntry(entry);
        byte[] bytes = contents.getBytes(StandardCharsets.UTF_8);
        zos.write(bytes);
        zos.closeEntry();
    }

    private String formatInstant(Instant instant) {
        return instant == null ? "" : ISO_FORMATTER.format(instant);
    }

    private String formatDurationSeconds(Instant started, Instant finished) {
        if (started == null || finished == null) {
            return "";
        }
        return String.valueOf(Duration.between(started, finished).toSeconds());
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

    private String buildPageMetadata(UUID artefactDirId, PageArchive page) {
        List<String> fileLines = new ArrayList<>();
        page.files().forEach((fileName, relativePath) -> fileLines.add("      <file name=\""
                + escapeXml(fileName) + "\">artifacts/" + artefactDirId + "/" + escapeXml(relativePath) + "</file>"));
        return Stream.of(
                "    <page>",
                "      <key>" + escapeXml(page.pageKey()) + "</key>",
                "      <url>" + escapeXml(page.pageUrl()) + "</url>",
                "      <directory>artifacts/" + artefactDirId + "/" + escapeXml(page.directoryPath()) + "</directory>",
                "      <files>",
                String.join("\n", fileLines),
                "      </files>",
                "    </page>")
                .filter(line -> line != null && !line.isBlank())
                .collect(Collectors.joining("\n"));
    }

    private PageArchive toPageArchive(StoredPageResult page, List<ArchiveFile> artefacts) {
        String directoryPath = "pages/" + page.directoryName();
        LinkedHashMap<String, String> files = new LinkedHashMap<>();
        if (page.rawHtml() != null && !page.rawHtml().isBlank()) {
            String relativePath = directoryPath + "/raw.html";
            files.put("raw.html", relativePath);
            artefacts.add(new ArchiveFile(relativePath, page.rawHtml()));
        }
        if (page.processedHtml() != null && !page.processedHtml().isBlank()) {
            String relativePath = directoryPath + "/processed.html";
            files.put("processed.html", relativePath);
            artefacts.add(new ArchiveFile(relativePath, page.processedHtml()));
        }
        String markdown = page.processedMarkdown();
        if ((markdown == null || markdown.isBlank())
                && page.processedHtml() != null && !page.processedHtml().isBlank()) {
            markdown = markdownConversionService.toMarkdown(page.processedHtml());
        }
        if (markdown != null && !markdown.isBlank()) {
            String relativePath = directoryPath + "/processed.md";
            files.put("processed.md", relativePath);
            artefacts.add(new ArchiveFile(relativePath, markdown));
        }
        if (files.isEmpty()) {
            return null;
        }
        return new PageArchive(page.directoryName(), page.pageKey(), page.pageUrl(), files);
    }

    private record ArchiveFile(String relativePath, String content) {
    }

    private record PageArchive(String directoryName,
                               String pageKey,
                               String pageUrl,
                               LinkedHashMap<String, String> files) {

        String directoryPath() {
            return "pages/" + directoryName;
        }
    }

    public static class ArchiveCreationException extends RuntimeException {

        public ArchiveCreationException(String message) {
            super(message);
        }

        public ArchiveCreationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
