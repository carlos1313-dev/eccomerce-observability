package com.ecommerce.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad de pago — persiste en payments_db, base de datos exclusiva
 * del payment-service. Nunca comparte tablas con el monolito.
 *
 * La referencia al monolito es solo por ID (orderId, userId),
 * nunca por FK cruzada entre bases de datos.
 */
@Entity
@Table(name = "payments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ID de la orden en el monolito — referencia lógica, no FK */
    @Column(nullable = false, unique = true)
    private Long orderId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Status status = Status.PENDING;

    /** ID de transacción simulado — en producción vendría de la pasarela */
    private String transactionId;

    /** Razón del rechazo si status = FAILED */
    private String failureReason;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime processedAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
    }

    public enum Status { PENDING, SUCCESS, FAILED }
}
