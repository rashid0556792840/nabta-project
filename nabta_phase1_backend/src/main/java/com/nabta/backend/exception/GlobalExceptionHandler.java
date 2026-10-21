package com.nabta.backend.exception;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
@RestControllerAdvice
public class GlobalExceptionHandler {
    public record ApiError(Instant timestamp, int status, String error, String message, String path, Map<String, String> fieldErrors) {}
    @ExceptionHandler(ApiException.class) public ResponseEntity<ApiError> api(ApiException ex, HttpServletRequest req) { return response(ex.getStatus(), ex.getMessage(), req, Map.of()); }
    @ExceptionHandler(MethodArgumentNotValidException.class) public ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String,String> fields = new LinkedHashMap<>(); for (FieldError e : ex.getBindingResult().getFieldErrors()) fields.putIfAbsent(e.getField(), e.getDefaultMessage());
        return response(HttpStatus.BAD_REQUEST, "Please check the submitted fields.", req, fields);
    }
    @ExceptionHandler(BadCredentialsException.class) public ResponseEntity<ApiError> credentials(BadCredentialsException ex, HttpServletRequest req) { return response(HttpStatus.UNAUTHORIZED, "Email or password is incorrect.", req, Map.of()); }
    @ExceptionHandler(HttpMessageNotReadableException.class) public ResponseEntity<ApiError> unreadable(HttpMessageNotReadableException ex, HttpServletRequest req) { return response(HttpStatus.BAD_REQUEST, "Request body is invalid or contains an unsupported value.", req, Map.of()); }
    @ExceptionHandler(DataIntegrityViolationException.class) public ResponseEntity<ApiError> integrity(DataIntegrityViolationException ex, HttpServletRequest req) { return response(HttpStatus.CONFLICT, "The request conflicts with existing data. Refresh and try again.", req, Map.of()); }
    @ExceptionHandler(Exception.class) public ResponseEntity<ApiError> other(Exception ex, HttpServletRequest req) { return response(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected server error occurred.", req, Map.of()); }
    private ResponseEntity<ApiError> response(HttpStatus status, String message, HttpServletRequest req, Map<String,String> fields) {
        ApiError body = new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, req.getRequestURI(), fields);
        return ResponseEntity.status(status).body(body);
    }
}
