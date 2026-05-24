# E-Commerce — Taller 2 / Proyecto Final

Evolución del monolito del Taller 1 (repositorio https://github.com/carlos1313-dev/eccomerce-java-avanzado) hacia arquitectura orientada a eventos con observabilidad distribuida.

## Opción elegida: **B — Microservicio de Pagos por eventos**

---

## Stack tecnológico

| Componente | Tecnología |
|---|---|
| Lenguaje | Java 21 |
| Framework | Spring Boot 3.2 |
| Mensajería | RabbitMQ 3.13 |
| Persistencia | PostgreSQL 15 (BD separada por servicio) |
| Seguridad | Spring Security + JWT + BCrypt |
| Observabilidad | OpenTelemetry Agent + Grafana LGTM |
| Contenedores | Docker + Docker Compose |

---

## Levantar todo el sistema

```bash
# Un solo comando levanta los 6 servicios
docker compose up --build
```

Primera ejecución tarda ~3 minutos (descarga imágenes y compila).

### URLs disponibles

| Servicio | URL |
|---|---|
| Monolito API | http://localhost:8080 |
| Monolito Swagger | http://localhost:8080/swagger-ui.html |
| Payment Service API | http://localhost:8082 |
| Payment Service Swagger | http://localhost:8082/swagger-ui.html |
| RabbitMQ Management | http://localhost:15672 (guest/guest) |
| Grafana | http://localhost:3000 (admin/admin) |

---

## Credenciales de prueba

**Registrar Admin** — `POST http://localhost:8080/api/auth/register`
```json
{ "name": "Admin", "email": "admin@ecommerce.com", "password": "Admin1234", "role": "ADMIN" }
```

**Registrar Cliente** — `POST http://localhost:8080/api/auth/register`
```json
{ "name": "Cliente", "email": "cliente@ecommerce.com", "password": "Cliente1234", "role": "CLIENTE" }
```

**Login** — `POST http://localhost:8080/api/auth/login` → copiar el `token` de la respuesta.

---

## Flujo Saga completo — cómo probarlo

### Happy path (pago aprobado)

```bash
# 1. Crear un producto (con token ADMIN)
POST /api/products
{ "name": "Laptop", "price": 1500.00, "stock": 10 }

# 2. Crear una orden (con token CLIENTE)
POST /api/orders
{ "items": [{ "productId": 1, "quantity": 1 }] }
# → Responde 202 ACCEPTED con status: "PENDING"

# 3. Esperar ~1 segundo y consultar el estado
GET /api/orders/1
# → status debería ser "CONFIRMED"

# 4. Verificar el pago
GET http://localhost:8082/api/payments/order/1
# → status: "SUCCESS"
```

### Ruta de compensación (pago rechazado)

Para forzar un pago fallido, cambiar en docker-compose.yml:
```yaml
APP_PAYMENT_FAILURE_RATE: 1.0   # 100% de fallos
```

El resultado será:
- `GET /api/orders/1` → `status: "CANCELLED"`
- `GET http://localhost:8082/api/payments/order/1` → `status: "FAILED"`
- El stock del producto se restaura automáticamente

---

## Observabilidad en Grafana

1. Abrir http://localhost:3000 (admin/admin)
2. **Trazas distribuidas**: Explore → Seleccionar fuente Tempo → buscar por servicio
3. **Métricas**: Dashboards → Import → ID `19004` (Spring Boot Dashboard)
4. **Logs**: Explore → Seleccionar fuente Loki → buscar `{service_name="ecommerce-monolito"}`

---

## Prueba de concurrencia

```bash
# Solo requiere H2 en memoria, no necesita Docker
cd monolito
mvn test -Dtest=ConcurrencyTest
```

Resultado esperado:
```
Stock inicial:      5
Compradores:        20
Compras exitosas:   5
Stock final en BD:  0
BUILD SUCCESS
```

---

## Estructura del proyecto

```
ecommerce/
├── docker-compose.yml          ← Orquesta todo
├── monolito/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
│       └── main/java/com/ecommerce/
│           ├── events/         ← Events.java, EventPublisher, SagaCompensationListener
│           ├── config/         ← RabbitMQConfig, SecurityConfig, AsyncConfig
│           ├── service/        ← OrderService (publica OrderCreated)
│           └── ...             ← Todo el Taller 1
└── payment-service/
    ├── Dockerfile
    ├── pom.xml
    └── src/
        └── main/java/com/ecommerce/payment/
            ├── events/         ← PaymentEvents, OrderCreatedListener
            ├── service/        ← PaymentService (lógica simulada)
            ├── entity/         ← Payment
            └── controller/     ← PaymentController (solo lectura HTTP)
```
