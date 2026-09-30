package com.example.blog.config;

import static org.springframework.http.HttpStatus.*;

import com.example.blog.shared.exception.BadRequestException;
import com.example.blog.shared.exception.ResourceNotFoundException;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final MessageSource messageSource;

    GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handle(Exception e) {
        logger.error("Unhandled exception", e);
        ProblemDetail problemDetail =
                ProblemDetail.forStatusAndDetail(INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        problemDetail.setTitle(message("error.internal.title"));
        problemDetail.setProperty("errors", List.of(message("error.internal.detail")));
        return problemDetail;
    }

    @Override
    public ResponseEntity<Object> handleMethodArgumentNotValid(
            @NonNull MethodArgumentNotValidException ex,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request) {
        LOG.error("Validation error", ex);
        var errors = ex.getAllErrors().stream()
                .map(error -> messageSource.getMessage(error, LocaleContextHolder.getLocale()))
                .toList();

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(BAD_REQUEST, message("error.validation.detail"));
        problemDetail.setTitle(message("error.validation.title"));
        problemDetail.setProperty("errors", errors);
        return ResponseEntity.status(BAD_REQUEST).body(problemDetail);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handle(BadCredentialsException e) {
        LOG.error("Bad credentials", e);
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(UNAUTHORIZED, e.getMessage());
        problemDetail.setTitle(message("error.unauthorized.title"));
        problemDetail.setProperty("errors", List.of(e.getMessage()));
        return problemDetail;
    }

    @ExceptionHandler(BadRequestException.class)
    public ProblemDetail handle(BadRequestException e) {
        LOG.error("Bad request", e);
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(BAD_REQUEST, e.getMessage());
        problemDetail.setTitle(message("error.bad-request.title"));
        problemDetail.setProperty("errors", List.of(e.getMessage()));
        return problemDetail;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handle(ResourceNotFoundException e) {
        LOG.error("Resource not found", e);
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(NOT_FOUND, e.getMessage());
        problemDetail.setTitle(message("error.not-found.title"));
        problemDetail.setProperty("errors", List.of(e.getMessage()));
        return problemDetail;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handle(AccessDeniedException e) {
        LOG.error("Access denied", e);
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(FORBIDDEN, e.getMessage());
        problemDetail.setTitle(message("error.forbidden.title"));
        problemDetail.setProperty("errors", List.of(e.getMessage()));
        return problemDetail;
    }

    private String message(String code) {
        return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
    }
}
