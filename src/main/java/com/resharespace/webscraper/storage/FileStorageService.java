package com.browzwi.webscraper.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class FileStorageService {

    private final Path root;

    public FileStorageService(FileStorageProperties properties) {
        this.root = properties.getRoot().toAbsolutePath().normalize();
    }

    public Path resolveJobDir(UUID jobId) {
        return ensureDirectory(root.resolve(jobId.toString()));
    }

    public Path resolveTargetDir(UUID jobId, UUID targetId) {
        return ensureDirectory(resolveJobDir(jobId).resolve(targetId.toString()));
    }

    public Path storeRawHtml(UUID jobId, UUID targetId, String html) {
        return writeFile(resolveTargetDir(jobId, targetId).resolve("raw.html"), html);
    }

    public Path storeProcessedHtml(UUID jobId, UUID targetId, String html) {
        return writeFile(resolveTargetDir(jobId, targetId).resolve("processed.html"), html);
    }

    public String loadRawHtml(UUID jobId, UUID targetId) {
        return readFile(resolveTargetDir(jobId, targetId).resolve("raw.html"));
    }

    public String loadProcessedHtml(UUID jobId, UUID targetId) {
        return readFile(resolveTargetDir(jobId, targetId).resolve("processed.html"));
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
}
