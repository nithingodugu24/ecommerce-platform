
# E-Commerce Platform (Microservices)

A robust, scalable E-commerce Backend Based System built with **Spring Boot** and designed using modern architecture.

## Architecture Overview

This project follows **Microservices Architecture** with the following core components:

- **api-gateway**: Single entry for external clients (port `8080`).
- **discovery-service**: Eureka service registry (port `8761`).
- **auth-service**: Authentication and user management (internal port `8000`).
- **product-service**: Product catalog operations (internal port `8001`).
- **order-service**: Order placement and order workflows (internal port `8002`).
- **inventory-service**: Inventory stock and updates (internal port `8003`).
- **common-lib**: Shared Java library module.
---

## System Architecture

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

---

## Project Structure

```text
.
├── api-gateway/
├── auth-service/
├── common-lib/
├── db-init/
├── discovery-service/
├── inventory-service/
├── order-service/
├── product-service/
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
| Build Tool                  | Maven                 |


## Microservices Breakdown

### 1. Auth Service

* User login
* JWT token generation
* User profile management
* Admin: view all users

### 2. Product Service

* Add / Update products (Admin)
* View products (Public)
* Pricing logic

### 3. Order Service

* Create order
* Emits `order.created` event
* Handles order success / failure

### 4. Inventory Service

* Listens to `order.created`
* Validates stock
* Emits `inventory.reserved` or `inventory.failed`

## Event Flow (Kafka)

1. Order Created
2. Inventory Check
3. Order Confirmation / Cancellation


## Key Design Concepts Used

* Database per service
* Event-driven architecture
* Loose coupling
* Fault tolerance
* Centralized authentication
* Distributed system communication

## Running with Docker Compose (Recommended)

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

## Local Development (Without Docker)

If running services manually:

- Start PostgreSQL and create required databases (`auth_db`, `product_db`, `order_db`, `inventory_db`).
- Start Kafka.
- Start `discovery-service` first.
- Start each business service with the `dev` profile (for local DB/Kafka defaults).
- Start `api-gateway` last.

Example:

```bash
cd discovery-service && ./mvnw spring-boot:run
cd ../auth-service && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

## Gateway Routes

Configured routes:

- `/auth/**`, `/users/**` → `auth-service`
- `/products/**` → `product-service`
- `/orders/**` → `order-service`
- `/inventory/**` → `inventory-service`

