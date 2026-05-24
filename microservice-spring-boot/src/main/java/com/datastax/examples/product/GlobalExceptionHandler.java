package com.datastax.examples.product;

import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.UUID;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemResponse> handleResourceNotFoundException(ResourceNotFoundException ex) {
        return problemResponse(HttpStatus.NOT_FOUND, "Resource Not Found", ex.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ProblemResponse> handleRuntimeException(RuntimeException ex) {
        String detail = ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred";
        return problemResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", detail);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        String fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + " - " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        ProblemResponse problem = newProblem(HttpStatus.BAD_REQUEST, "Bad Request",
                "Validation failed: " + fieldErrors);
        return new ResponseEntity<>(problem, headers, HttpStatus.BAD_REQUEST);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatus status, WebRequest request) {
        if (!(body instanceof ProblemResponse)) {
            String detail = ex.getMessage() != null ? ex.getMessage() : status.getReasonPhrase();
            body = newProblem(status, status.getReasonPhrase(), detail);
        }
        return new ResponseEntity<>(body, headers, status);
    }

    private ResponseEntity<ProblemResponse> problemResponse(HttpStatus status, String title, String detail) {
        return ResponseEntity.status(status).body(newProblem(status, title, detail));
    }

    private ProblemResponse newProblem(HttpStatus status, String title, String detail) {
        return new ProblemResponse("about:blank", title, status.value(), detail, currentTraceId());
    }

    private String currentTraceId() {
        String traceId = MDC.get("traceId");
        return traceId != null ? traceId : UUID.randomUUID().toString();
    }
}
