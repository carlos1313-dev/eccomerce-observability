package com.ecommerce.dto;

import com.ecommerce.entity.User;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class Dtos {

    public record RegisterRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8) String password,
        @NotNull User.Role role) {}

    public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password) {}

    public record AuthResponse(String token, String email, String name, String role) {}

    public record UserResponse(Long id, String name, String email, String role,
                               boolean active, LocalDateTime createdAt) {}

    public record ProductRequest(
        @NotBlank String name,
        String description,
        @NotNull BigDecimal price,
        @NotNull Integer stock) {}

    public record ProductResponse(Long id, String name, String description, BigDecimal price,
                                  Integer stock, boolean active,
                                  LocalDateTime createdAt, LocalDateTime updatedAt) {}

    public record OrderItemRequest(
        @NotNull Long productId,
        @NotNull @Min(1) Integer quantity) {}

    public record CreateOrderRequest(
        @NotNull @Size(min = 1) List<OrderItemRequest> items) {}

    public record OrderItemResponse(Long productId, String productName, Integer quantity,
                                    BigDecimal unitPrice, BigDecimal subtotal) {}

    public record OrderResponse(Long id, Long userId, String userName,
                                List<OrderItemResponse> items, BigDecimal total,
                                String status, LocalDateTime createdAt) {}

    public record ErrorResponse(int status, String error, String message, LocalDateTime timestamp) {}

    public record ValidationErrorResponse(int status, String error,
                                          List<FieldError> fieldErrors, LocalDateTime timestamp) {
        public record FieldError(String field, String message) {}
    }
}
