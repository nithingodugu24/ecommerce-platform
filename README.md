
# E-Commerce Platform (Microservices)

A robust, scalable E-commerce Backend Based System built with **Spring Boot** and designed using modern architecture.

## Architecture Overview

This project follows **Microservices Architecture** with the following core components:

- **api-gateway**: Single entry for external clients (port `8080`).
- **discovery-service**: Eureka service registry (port `8761`).
- **auth-service**: Authentication and user management (internal port `8000`).
- **product-service**: Product catalog operations with redis cached (internal port `8001`).
- **order-service**: Order placement and order workflows (internal port `8002`).
- **inventory-service**: Inventory stock and updates (internal port `8003`).
- **payment-service**: Payments, Webhook processing and refunds (internal port `8004`).
- **Centralized request logging** Fluent Bit + Elasticsearch + Kibana.
- **Distributed Tracing** with Opentelemetry and Jaeger 
- **common-lib**: Shared Java library module.
---

## Architecture Overview
```
                          Client
                            ↓
                        API Gateway
                            ↓
-------------------------------------------------------
| Auth | User | Product | Order | Payment | Inventory |
-------------------------------------------------------
                ↓               ↑        
                ------Kafka------
```

Each service has:

* Independent database
* Independent business logic
* REST APIs
* Role-based access control
* Logging & Tracing
---

## Project Structure

```text
.
├── api-gateway/
├── auth-service/
├── common-lib/
├── discovery-service/
├── inventory-service/
├── order-service/
├── product-service/
├── payment-service/
└── docker-compose.yml
```

## Tech Stack

| Layer                       | Technology            |
| --------------------------- | --------------------- |
| Backend                     | Spring Boot           |
| Security                    | Spring Security + JWT |
| Service Discovery           | Eureka                |
| API Gateway                 | Spring Cloud Gateway  |
| Inter-Service Communication | OpenFeign             |
| Async Communication         | Apache Kafka          |
| Database                    | PostgreSQL            |
| Caching                     | Redis                 |
| Containerization            | Docker                |
| Logging                     | FluentBit, ELS, Kibana|
| Tracing.                    | Otel, Jaeger          |
| Build Tool                  | Maven                 |

## Key Architectural Patterns

### 1. API Gateway

The API Gateway is the single public entry point. It:

- Validates JWT access tokens.
- Allows public access only to configured public endpoints.
- Blocks `/admin/**` routes unless the token carries the `ADMIN` role.
- Adds `X-USER-ID`, `X-USER-ROLE` headers before forwarding requests.

### 2. Service Discovery

All routed services register with Eureka and are resolved by logical names such as `lb://PRODUCT-SERVICE`.

### 3. Synchronous Internal Calls

The platform uses direct service-to-service HTTP calls for request/response workflows that need immediate results:

- `order-service -> product-service` for product pricing quotes.
- `order-service -> inventory-service` for inventory reservation.
- `payment-service -> order-service` for internal order validation/details.

### 4. Event-Driven Workflows via Kafka

Kafka is used for domain events and eventual consistency between services.

### 5. Transactional Outbox

Used Outbox pattern for outgoing events ,this persist outgoing events into an outbox table and publish them on a schedule. This reduces the chance of losing events between local database writes and Kafka publication.

### 6. Observability

- Structured JSON-style logs are shipped with Fluent Bit into Elasticsearch.
- Traces are exported with OpenTelemetry and visualized in Jaeger.
- The gateway generates/propagates a request ID for cross-service correlation.

---

## Microservices Breakdown

### `auth-service`

Handles identity and user management.

**Public endpoints**
- `POST /auth/register`
- `POST /auth/login`
- `POST /auth/refresh`
- `POST /auth/logout`

**Authenticated endpoints**
- `GET /users/me`
- `PUT /users/me/password`

**Admin endpoints**
- `GET /users`

   `ex: http://localhost:8080/users/me`

**Notes**
- Issues JWT access tokens.
- Stores refresh tokens in an HTTP-only cookie named `refresh_token`.
- Uses method-level authorization for authenticated/admin-only operations.

### `product-service`

Owns the catalog and pricing logic.

**Public endpoints**
- `GET /products`
- `GET /products/{productId}`
- `GET /products/search/{name}`

**Admin endpoints**
- `POST /admin/products`
- `PUT /admin/products/{productId}`
- `DELETE /admin/products/{productId}`

**Internal endpoints**
- `POST /internal/products/quote`

**Notes**
- Publishes `product.created` and `product.deleted` events.
- Uses Redis caching for product-by-id, paginated product listing, and search results.
- Supports internal pricing quotes so orders can be created from authoritative catalog pricing.

### `order-service`

Coordinates order placement and state transitions.

**Authenticated endpoints**
- `POST /orders`
- `GET /orders`
- `GET /orders/{orderId}`
- `POST /orders/cancel/{orderId}`

### 5. Inventory Service

* Listens to `order.created`, `order.cacelled`
* Validates stock, Releases reserved stock
* Emits `inventory.reserved` or `inventory.failed`

### 6. Payment Service

- `POST /payments/pay/{orderId}`
- `POST /payments/webhook`
- Emits Payment status to Order-Servia through events.
- Processing refunds.

## Event Flow (Kafka)

1. Order Created
2. Inventory Check
3. Order Confirmation / Cancellation


## Key Design Concepts Used

* Event-driven architecture
* Loose coupling
* Fault tolerance
* Centralized authentication
* Distributed system communication

## Running with Docker Compose

1. Create a `.env` file in the repository root:

   ```env
   POSTGRES_USER=postgres
   POSTGRES_PASSWORD=admin
   JWT_SECRET=replace-with-a-secure-base64-secret
   ```
2. Build and start all services:

   ```bash
   docker compose up --build
   ```

3. Access:
   - Gateway: `http://localhost:8080`
   - Eureka dashboard: `http://localhost:8761`
   - Kibana dashboard: `http://localhost:5601`
   - Jaeger dashboard: `http://localhost:16686`
