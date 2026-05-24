package com.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;
import java.util.List;

@Entity @Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false) private String email;
    @Column(nullable = false) private String password;
    @Column(nullable = false) private String name;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Role role;

    @Column(nullable = false) 
    private boolean active = true;

    @CreatedDate @Column(updatable = false) private LocalDateTime createdAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Order> orders;

    public enum Role { ADMIN, CLIENTE }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<Order> getOrders() {
        return orders;
    }

    public void setOrders(List<Order> orders) {
        this.orders = orders;
    }
    
    // Constructor privado para el builder
private User(Builder builder) {
    this.id = builder.id;
    this.email = builder.email;
    this.password = builder.password;
    this.name = builder.name;
    this.role = builder.role;
    this.active = builder.active;
    this.createdAt = builder.createdAt;
    this.orders = builder.orders;
}

// Método estático para obtener el builder
public static Builder builder() {
    return new Builder();
}

// Clase Builder interna
public static class Builder {
    private Long id;
    private String email;
    private String password;
    private String name;
    private Role role;
    private boolean active = true; // Valor por defecto como @Builder.Default
    private LocalDateTime createdAt;
    private List<Order> orders;
    
    public Builder id(Long id) {
        this.id = id;
        return this;
    }
    
    public Builder email(String email) {
        this.email = email;
        return this;
    }
    
    public Builder password(String password) {
        this.password = password;
        return this;
    }
    
    public Builder name(String name) {
        this.name = name;
        return this;
    }
    
    public Builder role(Role role) {
        this.role = role;
        return this;
    }
    
    public Builder active(boolean active) {
        this.active = active;
        return this;
    }
    
    public Builder createdAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }
    
    public Builder orders(List<Order> orders) {
        this.orders = orders;
        return this;
    }
    
    public User build() {
        return new User(this);
    }
}
}
