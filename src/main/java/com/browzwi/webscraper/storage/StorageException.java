package com.browzwi.webscraper.storage;

/**
 * Exception thrown when there are issues with file storage operations.
 * This exception is used internally by the storage service to wrap
 * underlying IO or filesystem errors.
 *
 * @since 1.0
 */
class StorageException extends RuntimeException {

    /**
     * Constructs a storage exception with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause of the exception
     */
    StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
