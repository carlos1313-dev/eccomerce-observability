package com.ecommerce.service;

import com.ecommerce.audit.AuditService;
import com.ecommerce.dto.Dtos;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.User;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// ── AuthService ──────────────────────────────────────────────
@Service @RequiredArgsConstructor @Slf4j
class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtUtils jwtUtils;
    private final AuditService auditService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, UserDetailsService userDetailsService, JwtUtils jwtUtils, AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtUtils = jwtUtils;
        this.auditService = auditService;
    }
    
    

    @Transactional
    public Dtos.UserResponse register(Dtos.RegisterRequest req) {
        if (userRepository.existsByEmail(req.email()))
            throw new IllegalArgumentException("Ya existe un usuario con email: " + req.email());
        User user = userRepository.save(User.builder().name(req.name()).email(req.email())
                .password(passwordEncoder.encode(req.password())).role(req.role()).build());
        auditService.logSuccess("REGISTER", "USER", user.getId(), req.email(), "Rol: " + req.role());
        return toResponse(user);
    }

    public Dtos.AuthResponse login(Dtos.LoginRequest req) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.email(), req.password()));
        } catch (BadCredentialsException e) {
            auditService.logFailure("LOGIN", "USER", null, req.email(), "Credenciales inválidas");
            throw new BadCredentialsException("Credenciales inválidas");
        }
        UserDetails ud = userDetailsService.loadUserByUsername(req.email());
        User user = userRepository.findByEmail(req.email()).orElseThrow();
        auditService.logSuccess("LOGIN", "USER", user.getId(), req.email(), "Login exitoso");
        return new Dtos.AuthResponse(jwtUtils.generateToken(ud), user.getEmail(),
                user.getName(), user.getRole().name());
    }

    private Dtos.UserResponse toResponse(User u) {
        return new Dtos.UserResponse(u.getId(), u.getName(), u.getEmail(),
                u.getRole().name(), u.isActive(), u.getCreatedAt());
    }
}

// ── ProductService ───────────────────────────────────────────
@Service @RequiredArgsConstructor
class ProductService {
    private final ProductRepository productRepository;
    private final AuditService auditService;

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
