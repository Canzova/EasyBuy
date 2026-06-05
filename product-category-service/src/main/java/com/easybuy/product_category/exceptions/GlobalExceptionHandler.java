package com.easybuy.product_category.exceptions;

import com.easybuy.product_category.exceptions.customException.ImageUploadFailedException;
import com.easybuy.product_category.exceptions.customException.ResourceNotFoundException;
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

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ExceptionResponse>handleResourceNotFoundException(ResourceNotFoundException e,  HttpServletRequest request){
        return buildResponse(e.getMessage(), 404, HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(ImageUploadFailedException.class)
    public ResponseEntity<ExceptionResponse>handleImageUploadFailedException(ImageUploadFailedException e,  HttpServletRequest request){
       return buildResponse(e.getMessage(), 400, HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    private ResponseEntity<ExceptionResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e, HttpServletRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return buildResponse(message, 400, HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    private ResponseEntity<ExceptionResponse> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        String message = "Invalid value '" + e.getValue() + "' for parameter '" + e.getName() + "'";
        return buildResponse(message, 400, HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    private ResponseEntity<ExceptionResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException e, HttpServletRequest request) {
        return buildResponse("Malformed or missing request body", 400, HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    private ResponseEntity<ExceptionResponse> handleNoResourceFoundException(NoResourceFoundException e, HttpServletRequest request) {
        return buildResponse("Endpoint not found: " + request.getRequestURI(), 404, HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(Exception.class)
    private ResponseEntity<ExceptionResponse> handleGenericException(Exception e, HttpServletRequest request) {
        return buildResponse("An unexpected error occurred", 500, HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    private ResponseEntity<ExceptionResponse> buildResponse(String message, int errorCode, HttpStatus status, HttpServletRequest request) {
        ExceptionResponse exceptionResponse = ExceptionResponse.builder()
                .message(message)
                .errorCode(errorCode)
                .error(status)
                .path(request.getRequestURI())
                .build();
        return new ResponseEntity<>(exceptionResponse, status);
    }
}
