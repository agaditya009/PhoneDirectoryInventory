package com.assignment.phoneinventory.exception;

import com.assignment.phoneinventory.dto.ErrorResponse;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ErrorResponse handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return new ErrorResponse(HttpStatus.NOT_FOUND.value(),
                "Resource Not Found", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(InvalidCsvFormatException.class)
    public ErrorResponse handleInvalidCsv(InvalidCsvFormatException ex, HttpServletRequest request) {
        return new ErrorResponse(HttpStatus.BAD_REQUEST.value(),
                "Invalid CSV Format", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    public ErrorResponse handleBusinessRule(BusinessRuleViolationException ex, HttpServletRequest request) {
        return new ErrorResponse(HttpStatus.CONFLICT.value(),
                "Business Rule Violation", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ErrorResponse handleOptimisticLock(OptimisticLockingFailureException ex, HttpServletRequest request) {
        return new ErrorResponse(HttpStatus.CONFLICT.value(),
                "Concurrent Modification", "The record was updated by another transaction", request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ErrorResponse handleGeneral(Exception ex, HttpServletRequest request) {
        return new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error", ex.getMessage(), request.getRequestURI());
    }
}
