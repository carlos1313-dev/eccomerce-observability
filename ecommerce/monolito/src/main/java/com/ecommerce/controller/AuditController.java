/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ecommerce.controller;

import com.ecommerce.entity.AuditLog;
import com.ecommerce.repository.AuditLogRepository;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author sangr
 */
@RestController @RequestMapping("/api/audit") @RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')") @Tag(name = "Auditoría") @SecurityRequirement(name = "bearerAuth")
class AuditController {
    private final AuditLogRepository repo;

    public AuditController(AuditLogRepository repo) {
        this.repo = repo;
    }
    
    @GetMapping public ResponseEntity<List<AuditLog>> all() { return ResponseEntity.ok(repo.findAll()); }
    @GetMapping("/user/{email}") public ResponseEntity<List<AuditLog>> byUser(@PathVariable String email) {
        return ResponseEntity.ok(repo.findByUserEmailOrderByTimestampDesc(email)); }
    @GetMapping("/entity/{type}") public ResponseEntity<List<AuditLog>> byEntity(@PathVariable String type) {
        return ResponseEntity.ok(repo.findByEntityTypeOrderByTimestampDesc(type)); }
}