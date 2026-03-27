package com.e_comerce.backend.exception;

import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.e_comerce.backend.payload.response.APIResponse;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;

@RestControllerAdvice
public class MyGlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(MyGlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> notValidException(MethodArgumentNotValidException e) {
        Map<String, String> response = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(err -> {
            String fieldName = ((FieldError)err).getField();
            String message = err.getDefaultMessage();
            response.put(fieldName, message);
        });
        return new ResponseEntity<Map<String, String>>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<APIResponse> resourceNotFoundException(ResourceNotFoundException e) {
        APIResponse apiResponse = new APIResponse(e.getMessage(), false);
        return new ResponseEntity<APIResponse>(apiResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(APIException.class)
    public ResponseEntity<APIResponse> apiException(APIException e) {
        APIResponse apiResponse = new APIResponse(e.getMessage(), false);
        return new ResponseEntity<APIResponse>(apiResponse, HttpStatus.BAD_REQUEST);
    }

    // DEBUG: Catch-all handler to see the actual exception causing 500
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleAllExceptions(Exception e) {
        logger.error("=== UNHANDLED EXCEPTION ===", e);
        
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        String stackTrace = sw.toString();
        // Limit stack trace length for response
        if (stackTrace.length() > 2000) {
            stackTrace = stackTrace.substring(0, 2000) + "\n... (truncated)";
        }

        Map<String, String> response = new HashMap<>();
        response.put("error", e.getClass().getName());
        response.put("message", e.getMessage() != null ? e.getMessage() : "No message");
        response.put("stackTrace", stackTrace);
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
