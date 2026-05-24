package com.ecommerce.controller;

import com.ecommerce.audit.AuditService;
import com.ecommerce.dto.Dtos;
import com.ecommerce.entity.AuditLog;
import com.ecommerce.repository.AuditLogRepository;
import com.ecommerce.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// ── AuthController ───────────────────────────────────────────
@RestController @RequestMapping("/api/auth") @RequiredArgsConstructor
@Tag(name = "Autenticación")
class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<Dtos.UserResponse> register(@Valid @RequestBody Dtos.RegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(req)); }

    @PostMapping("/login")
    public ResponseEntity<Dtos.AuthResponse> login(@Valid @RequestBody Dtos.LoginRequest req) {
        return ResponseEntity.ok(authService.login(req)); }
}

// ── ProductController ────────────────────────────────────────
@RestController @RequestMapping("/api/products") @RequiredArgsConstructor
@Tag(name = "Productos")
class ProductController {
    private final ProductService productService;

    @GetMapping public ResponseEntity<List<Dtos.ProductResponse>> findAll() {
        return ResponseEntity.ok(productService.findAll()); }

    @GetMapping("/{id}") public ResponseEntity<Dtos.ProductResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.findById(id)); }

    @PostMapping @PreAuthorize("hasRole('ADMIN')") @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Crear producto (ADMIN)")
    public ResponseEntity<Dtos.ProductResponse> create(
            @Valid @RequestBody Dtos.ProductRequest req,
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(req, ud.getUsername())); }

    @PutMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Dtos.ProductResponse> update(@PathVariable Long id,
            @Valid @RequestBody Dtos.ProductRequest req, @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(productService.update(id, req, ud.getUsername())); }

    @DeleteMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal UserDetails ud) {
        productService.softDelete(id, ud.getUsername()); return ResponseEntity.noContent().build(); }
}

// ── OrderController ──────────────────────────────────────────
@RestController @RequestMapping("/api/orders") @RequiredArgsConstructor
@Tag(name = "Órdenes") @SecurityRequirement(name = "bearerAuth")
class OrderController {
    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Crear orden — inicia flujo Saga, responde 202 PENDING")
    public ResponseEntity<Dtos.OrderResponse> create(
            @Valid @RequestBody Dtos.CreateOrderRequest req,
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(orderService.createOrder(req, ud.getUsername())); }

    @GetMapping("/my") public ResponseEntity<List<Dtos.OrderResponse>> myOrders(
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(orderService.getMyOrders(ud.getUsername())); }

    @GetMapping("/{id}") public ResponseEntity<Dtos.OrderResponse> getById(
            @PathVariable Long id, @AuthenticationPrincipal UserDetails ud) {
        boolean isAdmin = ud.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return ResponseEntity.ok(orderService.getOrderById(id, ud.getUsername(), isAdmin)); }

    @GetMapping @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Todas las órdenes (ADMIN)")
    public ResponseEntity<List<Dtos.OrderResponse>> all() {
        return ResponseEntity.ok(orderService.getAllOrders()); }
}

// ── AuditController ──────────────────────────────────────────
@RestController @RequestMapping("/api/audit") @RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')") @Tag(name = "Auditoría") @SecurityRequirement(name = "bearerAuth")
class AuditController {
    private final AuditLogRepository repo;

    @GetMapping public ResponseEntity<List<AuditLog>> all() { return ResponseEntity.ok(repo.findAll()); }
    @GetMapping("/user/{email}") public ResponseEntity<List<AuditLog>> byUser(@PathVariable String email) {
        return ResponseEntity.ok(repo.findByUserEmailOrderByTimestampDesc(email)); }
    @GetMapping("/entity/{type}") public ResponseEntity<List<AuditLog>> byEntity(@PathVariable String type) {
        return ResponseEntity.ok(repo.findByEntityTypeOrderByTimestampDesc(type)); }
}
