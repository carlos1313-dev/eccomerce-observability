# Justificación Técnica — Taller 2

## 1. Opción elegida: B — Microservicio de Pagos

**Justificación:** El monolito del Taller 1 ya implementa correctamente usuarios, productos, órdenes y auditoría. Romperlo todo en múltiples microservicios introduciría complejidad accidental sin beneficio pedagógico adicional. La Opción B permite demostrar el patrón Saga de forma limpia: un flujo crítico (pago) con eventos normales y de compensación perfectamente delimitados, sin necesidad de reescribir lo que ya funciona.

---

## 2. Límites de dominio y descomposición

**¿Cómo se definieron los límites?**

Se aplicó el principio de *Single Responsibility* a nivel de servicio: cada microservicio es responsable de un dominio de negocio y dueño exclusivo de sus datos.

| Servicio | Dominio | Base de datos | Comunicación |
|---|---|---|---|
| Monolito | Usuarios, Productos, Órdenes, Auditoría | `ecommerce_db` | HTTP (entrada) + RabbitMQ (salida/entrada) |
| Payment Service | Pagos | `payments_db` | RabbitMQ (entrada/salida) + HTTP (solo consultas) |

La separación de bases de datos es intencional: el payment-service nunca lee directamente tablas del monolito. La referencia es solo lógica (por `orderId`), nunca una FK cruzada entre BDs. Esto garantiza autonomía real: el payment-service puede desplegarse, escalarse y fallar de forma independiente.

---

## 3. Flujo Saga — Diseño y justificación

Se implementó el patrón **Saga Coreografía** (no orquestación). En vez de un orquestador central, cada servicio reacciona a eventos y publica los suyos. Esto reduce el acoplamiento: ningún servicio conoce la existencia del otro directamente.

### Happy path
```
Cliente → POST /api/orders
Monolito: reserva stock + guarda orden PENDING + publica OrderCreated
Payment Service: consume OrderCreated → procesa pago → publica PaymentSucceeded
Monolito: consume PaymentSucceeded → orden pasa a CONFIRMED
```

### Ruta de compensación
```
Payment Service: pago falla → publica PaymentFailed
Monolito: consume PaymentFailed →
    1. Libera stock (increaseStock por cada ítem)
    2. Orden pasa a CANCELLED
    (ambos en una sola @Transactional)
```

### ¿Qué pasa si el payment-service está caído?

1. El monolito publica `OrderCreated` en RabbitMQ (`durable=true`).
2. El mensaje queda persistido en `order.created.queue` aunque el servicio esté caído.
3. El cliente recibe `202 ACCEPTED` inmediatamente; la orden queda en `PENDING`.
4. Cuando el payment-service vuelve, RabbitMQ entrega los mensajes pendientes en orden.
5. Si tras 3 reintentos el mensaje sigue fallando, va a la `order.created.dlq` para análisis manual.

El cliente puede hacer polling a `GET /api/orders/{id}` para ver el estado final.

---

## 4. Estrategia de concurrencia (heredada del Taller 1)

Se mantiene el bloqueo pesimista (`SELECT FOR UPDATE`) en la creación de órdenes. Con el flujo Saga, el stock se descuenta al crear la orden (no al confirmar el pago). Si el pago falla, el listener de compensación devuelve el stock dentro de su propia `@Transactional`. Esto garantiza que nunca hay stock negativo y que la liberación es atómica.

---

## 5. Observabilidad — OpenTelemetry + Grafana LGTM

Se instrumenta cada microservicio con el **agente OTEL** (`-javaagent:otel-agent.jar`), que intercepta automáticamente:
- Llamadas HTTP entrantes y salientes
- Operaciones JDBC (queries a PostgreSQL)
- Operaciones AMQP (publicación/consumo en RabbitMQ)

Todos los datos (trazas, métricas, logs) se exportan al stack `grafana/otel-lgtm:latest` via OTLP HTTP (puerto 4318).

**¿Cómo rastrear una solicitud entre microservicios?**

El agente OTEL propaga automáticamente el `trace-id` en los headers AMQP. Una orden creada genera un trace que cruza:
1. `POST /api/orders` en el monolito
2. `publish OrderCreated` a RabbitMQ
3. `consume OrderCreated` en payment-service
4. `publish PaymentSucceeded/Failed` a RabbitMQ
5. `consume PaymentResult` en el monolito

Todo visible en Grafana → Explore → Tempo como un único trace distribuido con el mismo `trace-id`.

---

## 6. Seguridad entre microservicios

- **El payment-service no expone endpoints de escritura por HTTP.** Toda la comunicación de negocio ocurre por RabbitMQ.
- **RabbitMQ** usa credenciales configuradas en variables de entorno, nunca en código.
- **JWT** se mantiene exclusivamente en el monolito. El payment-service no necesita autenticar usuarios porque no recibe requests externos.
- Las credenciales de BD son distintas por servicio y configuradas vía variables de entorno en docker-compose.
