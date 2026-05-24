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
    @Column(nullable = false) @Builder.Default private boolean active = true;

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
}
