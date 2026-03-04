package com.browzwi.webscraper.storage;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for file storage settings in the web scraper application.
 * This class defines the root directory where scraping artifacts are stored.
 *
 * @since 1.0
 */
@ConfigurationProperties(prefix = "webscraper.storage")
public class FileStorageProperties {

    private Path root = Paths.get("./data");

    /**
     * Gets the root directory path for file storage.
     *
     * @return the root directory path
     */
    public Path getRoot() {
        return root;
    }

    /**
     * Sets the root directory path for file storage.
     *
     * @param root the root directory path to set
     */
    public void setRoot(Path root) {
        this.root = root;
    }
}
