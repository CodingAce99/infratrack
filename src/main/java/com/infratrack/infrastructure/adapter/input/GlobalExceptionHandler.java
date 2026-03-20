package com.infratrack.infrastructure.adapter.input;

import com.infratrack.domain.model.DuplicateIpAddressException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;

public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateIpAddressException.class)
    public ResponseEntity<Map<String, String>> handleDuplicateIpAddressException(
            DuplicateIpAddressException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("Error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(
            IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }
}
