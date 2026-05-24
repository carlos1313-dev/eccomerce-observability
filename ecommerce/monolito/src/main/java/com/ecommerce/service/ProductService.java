/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ecommerce.service;

import com.ecommerce.audit.AuditService;
import com.ecommerce.dto.Dtos;
import com.ecommerce.entity.Product;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.repository.ProductRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author sangr
 */
@Service @RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final AuditService auditService;

    public ProductService(ProductRepository productRepository, AuditService auditService) {
        this.productRepository = productRepository;
        this.auditService = auditService;
    }

    public List<Dtos.ProductResponse> findAll() {
        return productRepository.findAllByActiveTrue().stream().map(this::toResponse).toList();
    }

    public Dtos.ProductResponse findById(Long id) {
        return productRepository.findByIdAndActiveTrue(id).map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + id));
    }

    @Transactional
    public Dtos.ProductResponse create(Dtos.ProductRequest req, String adminEmail) {
        Product p = productRepository.save(Product.builder().name(req.name())
                .description(req.description()).price(req.price()).stock(req.stock()).build());
        auditService.logSuccess("CREATE_PRODUCT", "PRODUCT", p.getId(), adminEmail, "Stock inicial: " + req.stock());
        return toResponse(p);
    }

    @Transactional
    public Dtos.ProductResponse update(Long id, Dtos.ProductRequest req, String adminEmail) {
        Product p = productRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + id));
        p.setName(req.name()); p.setDescription(req.description());
        p.setPrice(req.price()); p.setStock(req.stock());
        auditService.logSuccess("UPDATE_PRODUCT", "PRODUCT", id, adminEmail, "Actualizado");
        return toResponse(productRepository.save(p));
    }

    @Transactional
    public void softDelete(Long id, String adminEmail) {
        Product p = productRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + id));
        p.setActive(false);
        productRepository.save(p);
        auditService.logSuccess("DELETE_PRODUCT", "PRODUCT", id, adminEmail, "Soft delete: " + p.getName());
    }

    public Dtos.ProductResponse toResponse(Product p) {
        return new Dtos.ProductResponse(p.getId(), p.getName(), p.getDescription(), p.getPrice(),
                p.getStock(), p.isActive(), p.getCreatedAt(), p.getUpdatedAt());
    }
}