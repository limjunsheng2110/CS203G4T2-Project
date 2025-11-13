package com.cs203.tariffg4t2.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        
        Map<String, Object> errors = new HashMap<>();
        
        // Check if it's from HsResolverController (chatbot)
        if (ex.getBindingResult().getObjectName().equals("hsResolveRequestDTO")) {
            errors.put("error", true);
            errors.put("message", "Please provide a valid product description between 10 and 2000 characters to help identify the HS code.");
            errors.put("timestamp", LocalDateTime.now().toString());
            
            logger.warn("Invalid HS resolve request - description validation failed");
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
        }
        
        // Generic validation error for other endpoints
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            String fieldName = error.getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        errors.put("error", true);
        errors.put("timestamp", LocalDateTime.now().toString());
        
        logger.warn("Validation error: {}", errors);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }
}
