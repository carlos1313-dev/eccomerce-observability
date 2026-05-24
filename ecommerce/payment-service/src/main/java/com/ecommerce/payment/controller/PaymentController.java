package com.ecommerce.payment.controller;

import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * El payment-service expone SOLO endpoints de consulta por HTTP.
 * Toda la escritura ocurre vía eventos RabbitMQ, nunca por HTTP directo.
 * Esto cumple la restricción: "No se permite comunicación HTTP directa
 * entre microservicios en los flujos cubiertos por eventos."
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Pagos", description = "Consulta de pagos — escritura solo por eventos RabbitMQ")
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Consultar pago por ID de orden")
    public ResponseEntity<Payment> getByOrderId(@PathVariable Long orderId) {
        return paymentService.findByOrderId(orderId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @Operation(summary = "Listar todos los pagos")
    public ResponseEntity<List<Payment>> getAll() {
        return ResponseEntity.ok(paymentService.findAll());
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Filtrar pagos por estado (PENDING, SUCCESS, FAILED)")
    public ResponseEntity<List<Payment>> getByStatus(@PathVariable Payment.Status status) {
        return ResponseEntity.ok(paymentService.findByStatus(status));
    }
}
