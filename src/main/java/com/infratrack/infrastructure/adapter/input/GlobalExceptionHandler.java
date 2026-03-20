package com.infratrack.infrastructure.adapter.input;

import com.infratrack.domain.model.AssetNotFoundException;
import com.infratrack.domain.model.DuplicateIpAddressException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateIpAddressException.class)
    public ResponseEntity<Map<String, String>> handleDuplicateIpAddressException(
            DuplicateIpAddressException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(AssetNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleAssetNotFound(
            AssetNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }
}
