package com.browzwi.webscraper.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Service for managing file storage of scraping artifacts including raw HTML,
 * processed HTML, and Markdown conversions for both single and multi-page scrapes.
 * This service provides methods for storing and retrieving scraping results
 * organized by job and target IDs in a hierarchical directory structure.
 *
 * @since 1.0
 */
@Service
public class FileStorageService {

    private static final String RAW_HTML_FILE = "raw.html";
    private static final String PROCESSED_HTML_FILE = "processed.html";
    private static final String PROCESSED_MARKDOWN_FILE = "processed.md";
    private static final String PAGES_DIRECTORY = "pages";
    private static final String PAGE_META_FILE = "meta.properties";
    private static final String JOB_ARCHIVES_DIRECTORY = "archives";

    private final Path root;

    public FileStorageService(FileStorageProperties properties) {
        this.root = properties.getRoot().toAbsolutePath().normalize();
    }

    public Path resolveJobDir(UUID jobId) {
        return ensureDirectory(jobDirPath(jobId));
    }

    public Path resolveTargetDir(UUID jobId, UUID targetId) {
        return ensureDirectory(targetDirPath(jobId, targetId));
    }

    public Path storeRawHtml(UUID jobId, UUID targetId, String html) {
        return writeFile(resolveTargetDir(jobId, targetId).resolve(RAW_HTML_FILE), html);
    }

    public Path storeProcessedHtml(UUID jobId, UUID targetId, String html) {
        return writeFile(resolveTargetDir(jobId, targetId).resolve(PROCESSED_HTML_FILE), html);
    }

    /**
     * Persists the Markdown conversion for a scrape target to make archive generation deterministic.
     * Storing Markdown avoids repeated conversions during archive assembly while providing 
     * parity between single and multi-page scrapes.
     *
     * @param jobId identifier of the owning job directory
     * @param targetId identifier of the scrape target directory
     * @param markdown rendered Markdown content, may be {@code null}
     * @return path of the stored Markdown file for potential troubleshooting
     */
    public Path storeProcessedMarkdown(UUID jobId, UUID targetId, String markdown) {
        return writeFile(resolveTargetDir(jobId, targetId).resolve(PROCESSED_MARKDOWN_FILE), markdown);
    }

    public String loadRawHtml(UUID jobId, UUID targetId) {
        return readFile(resolveTargetDir(jobId, targetId).resolve(RAW_HTML_FILE));
    }

    public String loadProcessedHtml(UUID jobId, UUID targetId) {
        return readFile(resolveTargetDir(jobId, targetId).resolve(PROCESSED_HTML_FILE));
    }

    /**
     * Loads previously converted Markdown for the given target.
     *
     * @param jobId job identifier locating the parent directory
     * @param targetId target identifier used to resolve the directory
     * @return Markdown contents or {@code null} when the file has not been produced
     */
    public String loadProcessedMarkdown(UUID jobId, UUID targetId) {
        return readFile(resolveTargetDir(jobId, targetId).resolve(PROCESSED_MARKDOWN_FILE));
    }

    public boolean existsRawHtml(UUID jobId, UUID targetId) {
        return Files.exists(targetDirPath(jobId, targetId).resolve(RAW_HTML_FILE));
    }

    public boolean existsProcessedHtml(UUID jobId, UUID targetId) {
        return Files.exists(targetDirPath(jobId, targetId).resolve(PROCESSED_HTML_FILE));
    }

    /**
     * Determines whether Markdown output exists for a given target.
     *
     * @param jobId job identifier locating the parent directory
     * @param targetId target identifier used to resolve the directory
     * @return {@code true} when Markdown has been stored, {@code false} otherwise
     */
    public boolean existsProcessedMarkdown(UUID jobId, UUID targetId) {
        return Files.exists(targetDirPath(jobId, targetId).resolve(PROCESSED_MARKDOWN_FILE));
    }

    /**
     * Removes all previously stored per-page artefacts so the next run starts from a clean slate.
     *
     * @param jobId job identifier locating the job directory
     * @param targetId target identifier locating the target directory
     */
    public void clearPageArtifacts(UUID jobId, UUID targetId) {
        Path pagesDir = targetDirPath(jobId, targetId).resolve(PAGES_DIRECTORY);
        if (!Files.exists(pagesDir)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(pagesDir)) {
            stream.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            throw new StorageException("Failed to delete " + path, e);
                        }
                    });
        } catch (IOException e) {
            throw new StorageException("Failed to clear page artefacts for target " + targetId, e);
        }
    }

    /**
     * Stores the artefacts of a single page involved in a multi-page scrape execution.
     *
     * <p>Directories are prefixed with the execution order to keep archival output predictable.</p>
     *
     * @param jobId job identifier locating the parent directory
     * @param targetId target identifier locating the target directory
     * @param order 1-based index indicating scraping order
     * @param pageKey logical key such as {@code main} or {@code about}
     * @param pageUrl resolved URL of the scraped page
     * @param rawHtml raw HTML contents, may be {@code null}
     * @param processedHtml processed HTML contents, may be {@code null}
     * @param markdown Markdown contents, may be {@code null}
     */
    public void storePageResult(UUID jobId,
                                UUID targetId,
                                int order,
                                String pageKey,
                                String pageUrl,
                                String rawHtml,
                                String processedHtml,
                                String markdown) {
        Path pageDir = ensureDirectory(pageDirPath(jobId, targetId, order, pageKey));
        if (rawHtml != null) {
            writeFile(pageDir.resolve(RAW_HTML_FILE), rawHtml);
        }
        if (processedHtml != null) {
            writeFile(pageDir.resolve(PROCESSED_HTML_FILE), processedHtml);
        }
        if (markdown != null) {
            writeFile(pageDir.resolve(PROCESSED_MARKDOWN_FILE), markdown);
        }
        writeFile(pageDir.resolve(PAGE_META_FILE), "key=" + safeMetaValue(pageKey) + "\nurl=" + safeMetaValue(pageUrl));
    }

    /**
     * Reads back all stored page-level artefacts for archival packaging.
     *
     * @param jobId job identifier locating the parent directory
     * @param targetId target identifier locating the target directory
     * @return ordered list of stored page artefacts
     */
    public List<StoredPageResult> loadPageResults(UUID jobId, UUID targetId) {
        Path pagesDir = targetDirPath(jobId, targetId).resolve(PAGES_DIRECTORY);
        if (!Files.exists(pagesDir)) {
            return List.of();
        }
        try (Stream<Path> directories = Files.list(pagesDir)) {
            return directories
                    .filter(Files::isDirectory)
                    .sorted()
                    .map(this::readPageResult)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new StorageException("Failed to read page artefacts for target " + targetId, e);
        }
    }

    /**
     * Indicates whether the filesystem contains per-page artefacts for the given target.
     *
     * @param jobId job identifier locating the parent directory
     * @param targetId target identifier locating the target directory
     * @return {@code true} when at least one page directory is present
     */
    public boolean hasPageArtifacts(UUID jobId, UUID targetId) {
        Path pagesDir = targetDirPath(jobId, targetId).resolve(PAGES_DIRECTORY);
        if (!Files.exists(pagesDir)) {
            return false;
        }
        try (Stream<Path> entries = Files.list(pagesDir)) {
            return entries.anyMatch(Files::isDirectory);
        } catch (IOException e) {
            throw new StorageException("Failed to inspect page artefacts for target " + targetId, e);
        }
    }

    /**
     * Ensures the archive directory for the given job exists and returns its path.
     *
     * @param jobId identifier of the job whose archive directory is requested
     * @return absolute path of the archive directory
     */
    public Path resolveJobArchiveDir(UUID jobId) {
        return ensureDirectory(jobArchiveDirPath(jobId));
    }

    /**
     * Resolves an archive file path under the job archive directory, creating the directory if needed.
     *
     * @param jobId identifier of the job
     * @param filename desired archive filename
     * @return absolute path to the archive file location
     */
    public Path resolveJobArchivePath(UUID jobId, String filename) {
        return resolveJobArchiveDir(jobId).resolve(filename);
    }

    /**
     * Locates the newest archive for the given job based on last modified time.
     *
     * @param jobId identifier of the job whose archives are inspected
     * @return optional containing the most recent archive path when it exists
     */
    public Optional<Path> findLatestJobArchive(UUID jobId) {
        Path archiveDir = jobArchiveDirPath(jobId);
        if (!Files.exists(archiveDir)) {
            return Optional.empty();
        }
        try (Stream<Path> files = Files.list(archiveDir)) {
            return files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".zip"))
                    .max(Comparator.comparingLong(this::lastModifiedSafe));
        } catch (IOException e) {
            throw new StorageException("Failed to inspect archives for job " + jobId, e);
        }
    }

    private Path jobDirPath(UUID jobId) {
        return root.resolve(jobId.toString());
    }

    private Path targetDirPath(UUID jobId, UUID targetId) {
        return jobDirPath(jobId).resolve(targetId.toString());
    }

    private Path jobArchiveDirPath(UUID jobId) {
        return jobDirPath(jobId).resolve(JOB_ARCHIVES_DIRECTORY);
    }

    private Path pageDirPath(UUID jobId, UUID targetId, int order, String pageKey) {
        String prefix = order < 10 ? "0" + order : String.valueOf(order);
        String directoryName = prefix + "-" + sanitizePageKey(pageKey);
        return targetDirPath(jobId, targetId).resolve(PAGES_DIRECTORY).resolve(directoryName);
    }

    private Path ensureDirectory(Path directory) {
        try {
            Files.createDirectories(directory);
            return directory;
        } catch (IOException e) {
            throw new StorageException("Could not create directory %s".formatted(directory), e);
        }
    }

    private Path writeFile(Path path, String contents) {
        try {
            Files.writeString(path, contents == null ? "" : contents, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return path;
        } catch (IOException e) {
            throw new StorageException("Failed to write file %s".formatted(path), e);
        }
    }

    private String readFile(Path path) {
        try {
            return Files.exists(path) ? Files.readString(path, StandardCharsets.UTF_8) : null;
        } catch (IOException e) {
            throw new StorageException("Failed to read file %s".formatted(path), e);
        }
    }

    private StoredPageResult readPageResult(Path directory) {
        String directoryName = directory.getFileName().toString();
        Map<String, String> meta = readMeta(directory.resolve(PAGE_META_FILE));
        String key = meta.getOrDefault("key", directoryName);
        String url = meta.getOrDefault("url", "");
        String rawHtml = readFile(directory.resolve(RAW_HTML_FILE));
        String processedHtml = readFile(directory.resolve(PROCESSED_HTML_FILE));
        String markdown = readFile(directory.resolve(PROCESSED_MARKDOWN_FILE));
        return new StoredPageResult(directoryName, key, url, rawHtml, processedHtml, markdown);
    }

    private Map<String, String> readMeta(Path metaPath) {
        if (!Files.exists(metaPath)) {
            return Map.of();
        }
        try {
            List<String> lines = Files.readAllLines(metaPath, StandardCharsets.UTF_8);
            Map<String, String> meta = new HashMap<>();
            for (String line : lines) {
                if (!StringUtils.hasText(line) || !line.contains("=")) {
                    continue;
                }
                String[] pair = line.split("=", 2);
                if (pair.length == 2) {
                    meta.put(pair[0].trim(), pair[1].trim());
                }
            }
            return meta;
        } catch (IOException e) {
            throw new StorageException("Failed to read page metadata " + metaPath, e);
        }
    }

    private String sanitizePageKey(String pageKey) {
        if (!StringUtils.hasText(pageKey)) {
            return "page";
        }
        String normalized = pageKey.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
        normalized = normalized.replaceAll("^-+", "").replaceAll("-+$", "");
        return normalized.isBlank() ? "page" : normalized;
    }

    private String safeMetaValue(String value) {
        return value == null ? "" : value.replaceAll("\n", " ").trim();
    }

    private long lastModifiedSafe(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            throw new StorageException("Failed to read last modified time for " + path, e);
        }
    }

    public record StoredPageResult(String directoryName,
                                   String pageKey,
                                   String pageUrl,
                                   String rawHtml,
                                   String processedHtml,
                                   String processedMarkdown) {
    }
}
