// src/main/java/com/safar_zone_backend/exception/ResourceNotFoundException.java
package com.safar_zone_backend.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * ✅ Thrown when a requested resource (package, vehicle, user) is not found
 * HTTP Status: 404 NOT FOUND
 */
@Getter
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    private final String resourceName;
    private final String fieldName;
    private final Object fieldValue;

    /**
     * Simple constructor - just message
     * Usage: throw new ResourceNotFoundException("Package not found");
     */
    public ResourceNotFoundException(String message) {
        super(message);
        this.resourceName = null;
        this.fieldName = null;
        this.fieldValue = null;
    }

    /**
     * Detailed constructor - with resource & field info
     * Usage: throw new ResourceNotFoundException("Package", "id", packageId);
     */
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue));
        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }
}