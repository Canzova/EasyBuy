package com.easybuy.common.exceptions;

import com.easybuy.common.exceptions.customException.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 404 - Resource not found in DB or external service
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ExceptionResponse> handleResourceNotFoundException(ResourceNotFoundException e, HttpServletRequest request) {
        return buildResponse(e.getMessage(), 404, HttpStatus.NOT_FOUND, request);
    }

    // 409 - Resource already exists (e.g. duplicate entry)
    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity<ExceptionResponse> handleResourceAlreadyExistsException(ResourceAlreadyExistsException e, HttpServletRequest request) {
        return buildResponse(e.getMessage(), 409, HttpStatus.CONFLICT, request);
    }

    // 409 - Email already registered
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ExceptionResponse> handleEmailAlreadyExistsException(EmailAlreadyExistsException e, HttpServletRequest request) {
        return buildResponse(e.getMessage(), 409, HttpStatus.CONFLICT, request);
    }

    // 400 - Resource is empty when it should not be (e.g. empty cart on checkout)
    @ExceptionHandler(ResourceEmptyException.class)
    public ResponseEntity<ExceptionResponse> handleResourceEmptyException(ResourceEmptyException e, HttpServletRequest request) {
        return buildResponse(e.getMessage(), 400, HttpStatus.BAD_REQUEST, request);
    }

    // 400 - General business rule violation (e.g. insufficient stock, invalid state transition)
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ExceptionResponse> handleBusinessException(BusinessException e, HttpServletRequest request) {
        return buildResponse(e.getMessage(), 400, HttpStatus.BAD_REQUEST, request);
    }

    // 400 - Image upload to external storage failed
    @ExceptionHandler(ImageUploadFailedException.class)
    public ResponseEntity<ExceptionResponse> handleImageUploadFailedException(ImageUploadFailedException e, HttpServletRequest request) {
        return buildResponse(e.getMessage(), 400, HttpStatus.BAD_REQUEST, request);
    }

    // 400 - Validation failed on @Valid annotated request body fields
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ExceptionResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e, HttpServletRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return buildResponse(message, 400, HttpStatus.BAD_REQUEST, request);
    }

    // 400 - Path variable or request param has wrong type (e.g. string passed where UUID expected)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ExceptionResponse> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        String message = "Invalid value '" + e.getValue() + "' for parameter '" + e.getName() + "'. Expected type: " + e.getRequiredType().getSimpleName();
        return buildResponse(message, 400, HttpStatus.BAD_REQUEST, request);
    }

    // 400 - Request body is missing or JSON is malformed
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ExceptionResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException e, HttpServletRequest request) {
        return buildResponse("Malformed or missing request body", 400, HttpStatus.BAD_REQUEST, request);
    }

    // 404 - No handler found for the requested URL
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ExceptionResponse> handleNoResourceFoundException(NoResourceFoundException e, HttpServletRequest request) {
        return buildResponse("Endpoint not found: " + request.getRequestURI(), 404, HttpStatus.NOT_FOUND, request);
    }

    // 500 - Catch-all for any unhandled exception
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionResponse> handleGenericException(Exception e, HttpServletRequest request) {
        return buildResponse("An unexpected error occurred: " + e.getMessage(), 500, HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    private ResponseEntity<ExceptionResponse> buildResponse(String message, int errorCode, HttpStatus status, HttpServletRequest request) {
        return new ResponseEntity<>(
                ExceptionResponse.builder()
                        .message(message)
                        .errorCode(errorCode)
                        .error(status)
                        .path(request.getRequestURI())
                        .build(),
                status
        );
    }
}
