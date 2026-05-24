package com.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity @Table(name = "products")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) private String name;
    @Column(length = 1000)    private String description;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal price;

    /** Control optimista — salvaguarda secundaria al bloqueo pesimista */
    @Version private Long version;

    @Column(nullable = false) private Integer stock;
    @Column(nullable = false) private boolean active = true;

    @CreatedDate  @Column(updatable = false) private LocalDateTime createdAt;
    @LastModifiedDate private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderItem> orderItems;

    public void decreaseStock(int quantity) {
        if (this.stock < quantity) {
            throw new IllegalStateException(
                "Stock insuficiente para '%s'. Disponible: %d, solicitado: %d"
                    .formatted(this.name, this.stock, quantity));
        }
        this.stock -= quantity;
    }

    public void increaseStock(int quantity) { this.stock += quantity; }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<OrderItem> getOrderItems() {
        return orderItems;
    }

    public void setOrderItems(List<OrderItem> orderItems) {
        this.orderItems = orderItems;
    }
    
    // Constructor privado para el builder
private Product(Builder builder) {
    this.id = builder.id;
    this.name = builder.name;
    this.description = builder.description;
    this.price = builder.price;
    this.version = builder.version;
    this.stock = builder.stock;
    this.active = builder.active;
    this.createdAt = builder.createdAt;
    this.updatedAt = builder.updatedAt;
    this.orderItems = builder.orderItems;
}

// Método estático para obtener el builder
public static Builder builder() {
    return new Builder();
}

// Clase Builder interna
public static class Builder {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Long version;
    private Integer stock;
    private boolean active = true; // Valor por defecto como @Builder.Default
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<OrderItem> orderItems;
    
    public Builder id(Long id) {
        this.id = id;
        return this;
    }
    
    public Builder name(String name) {
        this.name = name;
        return this;
    }
    
    public Builder description(String description) {
        this.description = description;
        return this;
    }
    
    public Builder price(BigDecimal price) {
        this.price = price;
        return this;
    }
    
    public Builder version(Long version) {
        this.version = version;
        return this;
    }
    
    public Builder stock(Integer stock) {
        this.stock = stock;
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
    
    public Builder updatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }
    
    public Builder orderItems(List<OrderItem> orderItems) {
        this.orderItems = orderItems;
        return this;
    }
    
    public Product build() {
        return new Product(this);
    }
}
}
