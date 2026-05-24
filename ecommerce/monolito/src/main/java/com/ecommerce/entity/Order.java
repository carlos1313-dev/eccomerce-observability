package com.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity @Table(name = "orders")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Order {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal total;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Status status = Status.PENDING;

    @CreatedDate @Column(updatable = false) private LocalDateTime createdAt;

    /**
     * Estados del ciclo de vida de la orden en la Saga:
     *
     *  PENDING   → recién creada, esperando resultado del pago
     *  CONFIRMED → PaymentSucceeded recibido (happy path)
     *  CANCELLED → PaymentFailed + compensación ejecutada
     */
    public enum Status { PENDING, CONFIRMED, CANCELLED }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    // Constructor privado para el builder
private Order(Builder builder) {
    this.id = builder.id;
    this.user = builder.user;
    this.items = builder.items;
    this.total = builder.total;
    this.status = builder.status;
    this.createdAt = builder.createdAt;
}

// Método estático para obtener el builder
public static Builder builder() {
    return new Builder();
}

// Clase Builder interna
public static class Builder {
    private Long id;
    private User user;
    private List<OrderItem> items = new ArrayList<>(); // Valor por defecto como @Builder.Default
    private BigDecimal total;
    private Status status = Status.PENDING; // Valor por defecto como @Builder.Default
    private LocalDateTime createdAt;
    
    public Builder id(Long id) {
        this.id = id;
        return this;
    }
    
    public Builder user(User user) {
        this.user = user;
        return this;
    }
    
    public Builder items(List<OrderItem> items) {
        this.items = items;
        return this;
    }
    
    public Builder total(BigDecimal total) {
        this.total = total;
        return this;
    }
    
    public Builder status(Status status) {
        this.status = status;
        return this;
    }
    
    public Builder createdAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }
    
    public Order build() {
        return new Order(this);
    }
}
}
