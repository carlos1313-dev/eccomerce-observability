/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ecommerce.exception;

import com.ecommerce.audit.AuditService;
import com.ecommerce.dto.Dtos;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 *
 * @author sangr
 */
@RestControllerAdvice @Slf4j
public class GlobalExceptionHandler {
    
        private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Dtos.ErrorResponse> notFound(ResourceNotFoundException e) {
        return err(HttpStatus.NOT_FOUND, e.getMessage()); }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<Dtos.ErrorResponse> stock(InsufficientStockException e) {
        return err(HttpStatus.CONFLICT, e.getMessage()); }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<Dtos.ErrorResponse> optimistic(ObjectOptimisticLockingFailureException e) {
        log.warn("Conflicto optimista: {}", e.getMessage());
        return err(HttpStatus.CONFLICT, "Recurso modificado concurrentemente. Reintente."); }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Dtos.ErrorResponse> badCreds(BadCredentialsException e) {
        return err(HttpStatus.UNAUTHORIZED, "Credenciales inválidas"); }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Dtos.ErrorResponse> denied(AccessDeniedException e) {
        return err(HttpStatus.FORBIDDEN, "No tiene permiso para esta operación"); }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Dtos.ErrorResponse> illegal(IllegalArgumentException e) {
        return err(HttpStatus.BAD_REQUEST, e.getMessage()); }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Dtos.ValidationErrorResponse> validation(MethodArgumentNotValidException e) {
        List<Dtos.ValidationErrorResponse.FieldError> fieldErrors = e.getBindingResult().getFieldErrors()
                .stream().map(f -> new Dtos.ValidationErrorResponse.FieldError(f.getField(), f.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest().body(new Dtos.ValidationErrorResponse(
                400, "Error de validación", fieldErrors, LocalDateTime.now())); }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Dtos.ErrorResponse> general(Exception e) {
        log.error("Error inesperado: {}", e.getMessage(), e);
        return err(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor"); }

    private ResponseEntity<Dtos.ErrorResponse> err(HttpStatus s, String msg) {
        return ResponseEntity.status(s).body(
                new Dtos.ErrorResponse(s.value(), s.getReasonPhrase(), msg, LocalDateTime.now())); }
}
