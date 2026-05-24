package com.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity @Table(name = "order_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false) private Integer quantity;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal unitPrice;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal subtotal;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }
    
    // Constructor privado para el builder
private OrderItem(Builder builder) {
    this.id = builder.id;
    this.order = builder.order;
    this.product = builder.product;
    this.quantity = builder.quantity;
    this.unitPrice = builder.unitPrice;
    this.subtotal = builder.subtotal;
}

// Método estático para obtener el builder
public static Builder builder() {
    return new Builder();
}

// Clase Builder interna
public static class Builder {
    private Long id;
    private Order order;
    private Product product;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
    
    public Builder id(Long id) {
        this.id = id;
        return this;
    }
    
    public Builder order(Order order) {
        this.order = order;
        return this;
    }
    
    public Builder product(Product product) {
        this.product = product;
        return this;
    }
    
    public Builder quantity(Integer quantity) {
        this.quantity = quantity;
        return this;
    }
    
    public Builder unitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
        return this;
    }
    
    public Builder subtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
        return this;
    }
    
    public OrderItem build() {
        return new OrderItem(this);
    }
}
}
