package com.gym24h.common.exception;

import com.gym24h.common.constant.ErrorCodes;
import com.gym24h.common.logging.RequestIdFilter;
import com.gym24h.presentation.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException exception, HttpServletRequest request) {
        return ResponseEntity.status(exception.getStatus())
                .body(buildError(exception.getCode(), exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getField)
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(buildError(ErrorCodes.VALIDATION_ERROR, message));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException exception) {
        return ResponseEntity.badRequest().body(buildError(ErrorCodes.BUSINESS_RULE_VIOLATION, exception.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildError(ErrorCodes.INTERNAL_ERROR, "Unexpected error"));
    }

    private ErrorResponse buildError(String code, String message) {
        return new ErrorResponse(code, message, Instant.now(), MDC.get(RequestIdFilter.REQUEST_ID));
    }
}
