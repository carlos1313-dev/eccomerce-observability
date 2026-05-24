/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ecommerce.controller;

import com.ecommerce.dto.Dtos;
import com.ecommerce.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author sangr
 */
@RestController @RequestMapping("/api/orders") @RequiredArgsConstructor
@Tag(name = "Órdenes") @SecurityRequirement(name = "bearerAuth")
class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }
    
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
