package com.app.exceptions;

import com.app.kafka.KafkaProducerService;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {


    private final KafkaProducerService kafkaProducerService;

    @Autowired
    public GlobalExceptionHandler(KafkaProducerService kafkaProducerService) {
        this.kafkaProducerService = kafkaProducerService;
    }

    //409
    public static class ConflictException extends RuntimeException {
        public ConflictException(String message) {
            super(message);
        }
    }

    // 404
    public static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String message) {
            super(message);
        }
    }

    // 400
    public static class BadRequestException extends RuntimeException {
        public BadRequestException(String message) {
            super(message);
        }
    }

    // 401
    public static class UnauthorizedException extends RuntimeException {
        public UnauthorizedException(String message) {
            super(message);
        }
    }

    // 403
    public static class ForbiddenException extends RuntimeException {
        public ForbiddenException(String message) {
            super(message);
        }
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationException(MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> errorDetails = new HashMap<>();
        errorDetails.put("errorCode", "CMS-0001");
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            errorDetails.put(error.getField(), error.getDefaultMessage());
        });
        return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, String>> handleConflictException(ConflictException ex, WebRequest request) {
        Map<String, String> errorDetails = new HashMap<>();
        errorDetails.put("errorCode", "CMS-0002");
        errorDetails.put("message", ex.getMessage());

        sendErrorMessageToKafka("CMS-0002", ex.getMessage());

        return new ResponseEntity<>(errorDetails, HttpStatus.CONFLICT);
    }

    // 404
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        Map<String, String> errorDetails = new HashMap<>();
        errorDetails.put("errorCode", "CMS-0003");
        errorDetails.put("message", "Failed to find item");
        return new ResponseEntity<>(errorDetails, HttpStatus.NOT_FOUND);
    }

    // 400: Bad Request
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Map<String, String>> handleBadRequestException(BadRequestException ex, WebRequest request) {
        Map<String, String> errorDetails = new HashMap<>();
        errorDetails.put("errorCode", "CMS-0004");
        errorDetails.put("message", ex.getMessage());
        return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
    }

    // 401: Unauthorized
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, String>> handleUnauthorizedException(UnauthorizedException ex, WebRequest request) {
        Map<String, String> errorDetails = new HashMap<>();
        errorDetails.put("errorCode", "CMS-0005");
        errorDetails.put("message", ex.getMessage());
        return new ResponseEntity<>(errorDetails, HttpStatus.UNAUTHORIZED);
    }

    // 403: Forbidden
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Map<String, String>> handleForbiddenException(ForbiddenException ex, WebRequest request) {
        Map<String, String> errorDetails = new HashMap<>();
        errorDetails.put("errorCode", "CMS-0006");
        errorDetails.put("message", ex.getMessage());

        sendErrorMessageToKafka("CMS-0006", ex.getMessage());

        return new ResponseEntity<>(errorDetails, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleJsonParseException(HttpMessageNotReadableException ex, WebRequest request) {
        Throwable cause = ex.getCause();
        String errorMessage = "Invalid JSON format";

        if (cause instanceof JsonParseException) {
            errorMessage = "Malformed JSON request";
        } else if (cause instanceof JsonMappingException) {
            errorMessage = "JSON field mismatch or invalid data structure";
        }

        Map<String, String> errorDetails = new HashMap<>();
        errorDetails.put("errorCode", "CMS-0007");
        errorDetails.put("message", errorMessage);

        return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
    }

    // 5XX
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGlobalException(Exception ex, WebRequest request) {
        Map<String, String> errorDetails = new HashMap<>();
        errorDetails.put("errorCode", "CMS-0008");
        errorDetails.put("message", "An unexpected error occurred: " + ex.getMessage());

        sendErrorMessageToKafka("CMS-0008", ex.getMessage());

        return new ResponseEntity<>(errorDetails, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private void sendErrorMessageToKafka(String errorCode, String message) {
        try {
            LocalDateTime now = LocalDateTime.now();
            String timestamp = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String kafkaMessage = String.format("Timestamp: %s, Error occurred. Code: %s, Message: %s", timestamp, errorCode, message);
            kafkaProducerService.sendMessage("retry-db-write-from-cache", "error", kafkaMessage);
            log.info("Successfully sent error message to Kafka: {}", kafkaMessage);
        } catch (Exception e) {
            log.error("Failed to send error message to Kafka. ErrorCode: {}, Message: {}, Exception: {}", errorCode, message, e.getMessage(), e);
        }
    }
}
