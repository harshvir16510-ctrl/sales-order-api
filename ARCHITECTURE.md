# Architecture Documentation

## Overview

The Sales Order Management API follows **Domain-Driven Design (DDD)** principles with a **layered architecture** pattern, ensuring clear separation of concerns, maintainability, and scalability.

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                        Client Layer                              │
│              (REST API Consumers - Postman, Frontend)            │
└────────────────────────────┬────────────────────────────────────┘
                             │ HTTPS
                             ↓
┌─────────────────────────────────────────────────────────────────┐
│                    Security Layer                                │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  JwtAuthenticationFilter                                  │  │
│  │  - Extracts JWT token from Authorization header          │  │
│  │  - Validates token signature and expiration              │  │
│  │  - Sets SecurityContext with user roles                  │  │
│  └──────────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  SecurityConfig                                          │  │
│  │  - Configures endpoint access rules                      │  │
│  │  - Role-based authorization (USER, ADMIN)                │  │
│  └──────────────────────────────────────────────────────────┘  │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ↓
┌─────────────────────────────────────────────────────────────────┐
│                    Controller Layer                              │
│  ┌──────────────────┐  ┌──────────────────┐                  │
│  │ AuthController   │  │ OrderController   │                  │
│  │ - /auth/login    │  │ - /orders         │                  │
│  └──────────────────┘  └──────────────────┘                  │
│         │                      │                               │
│         │                      │                               │
│  ┌──────▼──────────────────────▼──────┐                        │
│  │  Input Validation (Bean Validation)│                        │
│  │  - @Valid annotations              │                        │
│  │  - Request DTOs                    │                        │
│  └────────────────────────────────────┘                        │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ↓
┌─────────────────────────────────────────────────────────────────┐
│                     Service Layer                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  OrderService                                              │  │
│  │  - Business logic orchestration                           │  │
│  │  - Transaction management (@Transactional)                │  │
│  │  - Price calculations (subtotal, VAT, total)              │  │
│  │  - Customer validation                                    │  │
│  │  - Catalog item validation                                │  │
│  └──────────────────────────────────────────────────────────┘  │
│         │                                                       │
│         │ Uses                                                  │
│         ↓                                                       │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Domain Models (Entities)                                  │  │
│  │  - SalesOrder (with @Version for optimistic locking)      │  │
│  │  - OrderItem (price snapshot)                             │  │
│  │  - Customer                                                │  │
│  │  - CatalogItem                                             │  │
│  └──────────────────────────────────────────────────────────┘  │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ↓
┌─────────────────────────────────────────────────────────────────┐
│                   Repository Layer                               │
│  ┌──────────────────┐  ┌──────────────────┐                  │
│  │ SalesOrderRepo   │  │ CustomerRepo     │                  │
│  │ - Custom queries │  │ - Basic CRUD     │                  │
│  │ - Filtering      │  └──────────────────┘                  │
│  │ - Pagination     │  ┌──────────────────┐                  │
│  └──────────────────┘  │ CatalogItemRepo  │                  │
│                        └──────────────────┘                  │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ↓
┌─────────────────────────────────────────────────────────────────┐
│                    Database Layer                                │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  H2 Database (Development) / PostgreSQL (Production)   │  │
│  │  - Liquibase migrations                                   │  │
│  │  - Optimistic locking (version column)                   │  │
│  │  - Indexes on frequently queried fields                  │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

## Design Patterns Used

### 1. Domain-Driven Design (DDD)
- **Bounded Context**: Sales Order domain
- **Entities**: SalesOrder, OrderItem, Customer, CatalogItem
- **Value Objects**: Embedded in entities (prices, dates)
- **Aggregates**: SalesOrder is the aggregate root

### 2. Layered Architecture
- **Presentation Layer**: Controllers handle HTTP
- **Application Layer**: Services contain business logic
- **Domain Layer**: Entities with business rules
- **Infrastructure Layer**: Repositories and database

### 3. Repository Pattern
- Abstracts data access
- Custom queries for complex operations
- Spring Data JPA implementation

### 4. DTO Pattern
- Separates API contracts from domain models
- Request DTOs: `CreateOrderRequest`, `OrderItemRequest`
- Response DTOs: `OrderResponse`, `PageResponse`

### 5. Strategy Pattern
- Date formatting strategy via `DateFormatter`
- Extensible for different date formats

### 6. Filter Pattern
- `JwtAuthenticationFilter` for authentication
- Chain of responsibility for security

## Technical Decisions and Justifications

### 1. Why DDD?
- **Reason**: Complex business domain with rules (price immutability, order lifecycle)
- **Benefit**: Clear domain boundaries, maintainable business logic

### 2. Why Layered Architecture?
- **Reason**: Separation of concerns, testability
- **Benefit**: Easy to test each layer independently

### 3. Why JWT Authentication?
- **Reason**: Stateless, scalable, microservices-ready
- **Benefit**: No session storage needed, works across multiple instances

### 4. Why Optimistic Locking?
- **Reason**: Better performance than pessimistic locking, prevents lost updates
- **Benefit**: Handles concurrent modifications without blocking

### 5. Why Liquibase?
- **Reason**: Version-controlled schema changes, rollback support
- **Benefit**: Reproducible database state across environments

### 6. Why H2 for Development?
- **Reason**: Zero configuration, in-memory option, fast startup
- **Benefit**: Quick development and testing cycles

## Data Flow

### Order Creation Flow

```
1. Client Request
   POST /api/v1/orders
   {
     "customerId": 1,
     "items": [{"catalogItemId": 1, "quantity": 2}]
   }

2. JWT Filter
   - Validates token
   - Sets SecurityContext

3. OrderController
   - Validates request body
   - Calls OrderService.createOrder()

4. OrderService
   - Validates customer exists
   - Loads catalog items
   - Creates OrderItem with price snapshot
   - Calculates totals (subtotal, VAT, total)
   - Saves SalesOrder (transactional)

5. Repository
   - JPA saves to database
   - Version field auto-incremented

6. Response
   - Maps to OrderResponse DTO
   - Returns to client
```

### Concurrency Handling

```
User A reads Order (version=1)
User B reads Order (version=1)
User A updates Order → version becomes 2
User B tries to update → OptimisticLockException
User B must refresh and retry
```

## Scalability Considerations

### Current Design
- Stateless application (JWT tokens)
- Database connection pooling (HikariCP)
- Pagination for large datasets

### Future Enhancements
- **Caching**: Redis for catalog items
- **Read Replicas**: Separate read/write databases
- **Microservices**: Split into order, catalog, customer services
- **Message Queue**: Async order processing

## Security Architecture

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │ 1. Login Request
       ↓
┌─────────────┐
│ AuthController│
└──────┬──────┘
       │ 2. Generate JWT
       ↓
┌─────────────┐
│JwtTokenProvider│
└──────┬──────┘
       │ 3. Return Token
       ↓
┌─────────────┐
│   Client    │
└──────┬──────┘
       │ 4. API Request + Token
       ↓
┌─────────────┐
│JwtAuthFilter│
└──────┬──────┘
       │ 5. Validate Token
       ↓
┌─────────────┐
│SecurityConfig│
└──────┬──────┘
       │ 6. Check Authorization
       ↓
┌─────────────┐
│  Controller │
└─────────────┘
```

## Database Schema

```
┌──────────────┐
│   Customer   │
├──────────────┤
│ id (PK)      │
│ name         │
└──────┬───────┘
       │
       │ 1:N
       ↓
┌──────────────┐
│  SalesOrder  │
├──────────────┤
│ id (PK)      │
│ order_ref    │
│ customer_id  │──┐
│ subtotal     │  │
│ vat          │  │
│ total        │  │
│ created_at   │  │
│ cancelled_at │  │
│ status       │  │
│ version      │  │ (Optimistic Lock)
└──────┬───────┘  │
       │          │
       │ 1:N      │
       ↓          │
┌──────────────┐  │
│  OrderItem   │  │
├──────────────┤  │
│ id (PK)      │  │
│ order_id (FK)│──┘
│ catalog_item_id│
│ item_name    │  (Price Snapshot)
│ item_price   │
│ quantity     │
│ total_price  │
└──────────────┘

┌──────────────┐
│ CatalogItem  │
├──────────────┤
│ id (PK)      │
│ sku          │
│ name         │
│ price        │ (Can change)
│ updated_at   │
└──────────────┘
```

## Component Interactions

### Request Processing
1. **Filter Chain**: Security filters → Controller
2. **Validation**: Bean Validation → Service
3. **Business Logic**: Service → Domain Models
4. **Persistence**: Repository → Database
5. **Response**: Domain → DTO → JSON

### Error Handling
1. **Exception**: Thrown in any layer
2. **Handler**: `ApiExceptionHandler` catches
3. **Response**: Standardized error DTO
4. **Client**: Receives formatted error

## Performance Optimizations

1. **Lazy Loading**: JPA relationships loaded on demand
2. **Connection Pooling**: HikariCP for efficient connections
3. **Pagination**: Limits result sets
4. **Indexes**: On createdAt, cancelledAt, customerId
5. **Batch Operations**: Hibernate batch inserts/updates

## Future Architecture Evolution

### Phase 1: Caching
- Add Redis for catalog items
- Cache frequently accessed orders

### Phase 2: Microservices
- Order Service (current)
- Catalog Service
- Customer Service
- API Gateway

### Phase 3: Event-Driven
- Order events published to message queue
- Async processing for notifications
- Event sourcing for audit trail

