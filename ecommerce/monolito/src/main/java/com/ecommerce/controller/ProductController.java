/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ecommerce.controller;

import com.ecommerce.dto.Dtos;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author sangr
 */
@RestController @RequestMapping("/api/products") @RequiredArgsConstructor
@Tag(name = "Productos")
class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

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