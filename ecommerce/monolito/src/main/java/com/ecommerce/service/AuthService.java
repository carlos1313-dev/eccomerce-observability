/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ecommerce.service;

import com.ecommerce.audit.AuditService;
import com.ecommerce.dto.Dtos;
import com.ecommerce.entity.User;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author sangr
 */
@Service @RequiredArgsConstructor @Slf4j
public class AuthService {
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