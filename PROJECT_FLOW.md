# Sales Order API - Project Flow Explanation

## 📋 Overview

This is a **Sales Order Management REST API** built with Spring Boot. It allows you to create, retrieve, filter, and cancel sales orders with proper authentication and data integrity.

## 🏗️ Architecture Flow

### 1. **Request Flow (How a Request Travels Through the System)**

```
Client Request
    ↓
[JWT Authentication Filter] → Validates JWT token
    ↓
[Security Config] → Checks if user has required role
    ↓
[Controller Layer] → Receives HTTP request, validates input
    ↓
[Service Layer] → Business logic, calculations, validations
    ↓
[Repository Layer] → Database queries (JPA/Hibernate)
    ↓
[H2 Database] → Data storage
    ↓
[Response] → Returns JSON response to client
```

### 2. **Detailed Component Breakdown**

#### **A. Authentication Flow**

```
1. User sends login request → POST /api/v1/auth/login
   {
     "username": "admin",
     "password": "password"
   }

2. AuthController receives request
   - Validates credentials (simplified for demo)
   - Assigns role (USER or ADMIN based on username)

3. JwtTokenProvider generates JWT token
   - Contains: username, role, expiration time
   - Signed with secret key

4. Client receives token
   {
     "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
     "username": "admin",
     "role": "ADMIN"
   }

5. Client uses token in subsequent requests
   Authorization: Bearer <token>
```

#### **B. Order Creation Flow**

```
1. Client sends: POST /api/v1/orders
   {
     "customerId": 1,
     "items": [
       {
         "catalogItemId": 1,
         "quantity": 2
       }
     ]
   }

2. OrderController receives request
   - Validates request body using Bean Validation
   - Calls OrderService.createOrder()

3. OrderService processes:
   a. Validates customer exists
   b. Loads catalog items
   c. Creates SalesOrder entity
   d. For each item:
      - Creates OrderItem with SNAPSHOT of price
      - Calculates item total (price × quantity)
   e. Calculates:
      - Subtotal = sum of all item totals
      - VAT = subtotal × VAT rate (15%)
      - Total = subtotal + VAT
   f. Saves order to database
   g. Maps to OrderResponse DTO

4. Response sent back with:
   - Order ID
   - Customer name (fetched from database)
   - Items with prices
   - Dates formatted as dd/MM/yyyy
   - Subtotal, VAT, Total
```

#### **C. Order Retrieval Flow**

```
1. Client sends: GET /api/v1/orders/{id}

2. OrderController → OrderService.getOrderById()

3. OrderService:
   - Fetches order from database
   - Fetches customer to get name
   - Maps to OrderResponse with formatted dates

4. Returns complete order details
```

#### **D. Order Listing with Filters Flow**

```
1. Client sends: GET /api/v1/orders?creationDateFrom=2024-01-01&page=0&size=20

2. OrderController extracts query parameters
   - creationDateFrom, creationDateTo
   - cancellationDateFrom, cancellationDateTo
   - page, size, sortBy, sortDirection

3. OrderService.listOrders():
   - Converts LocalDate filters to Instant
   - Creates Pageable with sorting
   - Calls repository.findByFilters()

4. SalesOrderRepository:
   - Executes JPQL query with filters
   - Returns paginated results

5. Response includes:
   - content: List of orders
   - page, size, totalElements, totalPages
   - first, last flags
```

#### **E. Order Cancellation Flow**

```
1. Client sends: POST /api/v1/orders/{id}/cancel

2. OrderService.cancelOrder():
   - Fetches order
   - Checks if already cancelled
   - Sets status = "CANCELLED"
   - Sets cancelledAt = current timestamp
   - Saves to database (with optimistic locking)

3. Returns updated order
```

## 🔐 Security Flow

### JWT Authentication Filter

Every request (except login) goes through `JwtAuthenticationFilter`:

```java
1. Extracts token from "Authorization: Bearer <token>" header
2. Validates token signature and expiration
3. Extracts username and role from token
4. Creates Authentication object
5. Sets it in SecurityContext
6. Request continues to controller
```

### Role-Based Access Control

- `/api/v1/auth/**` → Public (no authentication)
- `/api/v1/orders/**` → Requires USER or ADMIN role
- `/h2-console/**` → Public (for database access)

## 💾 Database Flow

### Entity Relationships

```
Customer (1) ──→ (N) SalesOrder
                      │
                      └──→ (N) OrderItem
                              │
                              └──→ (1) CatalogItem (reference only)
```

### Price Immutability

**Key Feature**: Order prices are FIXED at creation time!

```
1. When order is created:
   - CatalogItem.price = $19.99
   - OrderItem.itemPrice = $19.99 (snapshot stored)

2. Later, catalog price changes:
   - CatalogItem.price = $25.99
   - OrderItem.itemPrice = $19.99 (UNCHANGED!)

3. This ensures historical orders maintain original prices
```

### Optimistic Locking

```java
@Version
private Long version;
```

- Prevents concurrent modification conflicts
- Each update increments version
- If versions don't match → OptimisticLockException

## 📊 Data Flow Example

### Complete Order Creation Example

**Step 1: Initial Data (Seeded on startup)**
```
Customer: {id: 1, name: "Alice"}
CatalogItem: {id: 1, name: "Blue Widget", price: 19.99}
```

**Step 2: Create Order Request**
```json
POST /api/v1/orders
{
  "customerId": 1,
  "items": [
    {"catalogItemId": 1, "quantity": 2}
  ]
}
```

**Step 3: Processing**
```
1. Validate customer exists ✓
2. Load catalog item: price = $19.99
3. Create OrderItem:
   - itemName = "Blue Widget"
   - itemPrice = 19.99 (snapshot)
   - quantity = 2
   - totalPrice = 39.98
4. Calculate:
   - subtotal = 39.98
   - vat = 39.98 × 0.15 = 5.997
   - total = 45.977
5. Save to database
```

**Step 4: Response**
```json
{
  "id": 1,
  "customerName": "Alice",
  "items": [
    {
      "itemName": "Blue Widget",
      "itemPrice": 19.99,
      "quantity": 2,
      "totalPrice": 39.98
    }
  ],
  "subtotal": 39.98,
  "vat": 5.997,
  "total": 45.977,
  "creationDate": "15/01/2024",
  "status": "CREATED"
}
```

## 🔄 Transaction Management

All service methods that modify data use `@Transactional`:

```java
@Transactional
public OrderResponse createOrder(...) {
    // All database operations are atomic
    // If any step fails, entire transaction rolls back
}
```

## 📝 Key Design Patterns

1. **DTO Pattern**: Separates API contracts from domain models
2. **Repository Pattern**: Abstracts data access
3. **Service Layer**: Encapsulates business logic
4. **Optimistic Locking**: Prevents concurrent conflicts
5. **JWT Authentication**: Stateless security

## 🎯 Main Features Explained

### 1. **Fixed Pricing**
- Prices stored in OrderItem at creation time
- Catalog price changes don't affect existing orders

### 2. **Date Filtering**
- Filter by creation date range
- Filter by cancellation date range
- Supports null values (orders not cancelled)

### 3. **Pagination**
- Default: 20 items per page
- Configurable page size
- Returns total count and pages

### 4. **Sorting**
- Default: Sort by createdAt descending
- Configurable field and direction

### 5. **Date Formatting**
- Database stores: Instant (timestamp)
- API returns: "dd/MM/yyyy" format
- Example: "15/01/2024"

## 🚀 How to Run Locally

### Prerequisites
- Java 17 or higher
- Maven 3.6+ (or use Maven wrapper)

### Step 1: Navigate to Project
```bash
cd /Users/hsinghkali/Downloads/sales-order-api
```

### Step 2: Build the Project
```bash
# If you have Maven installed:
mvn clean install

# Or if Maven wrapper exists:
./mvnw clean install
```

### Step 3: Run the Application
```bash
# Using Maven:
mvn spring-boot:run

# Or using Maven wrapper:
./mvnw spring-boot:run

# Or run the JAR directly:
java -jar target/sales-order-api-0.0.1-SNAPSHOT.jar
```

### Step 4: Verify It's Running
- Application starts on: **http://localhost:8080**
- H2 Console: **http://localhost:8080/h2-console**
  - JDBC URL: `jdbc:h2:mem:ordersdb`
  - Username: `sa`
  - Password: (leave empty)

### Step 5: Test the API

#### 5.1 Login to Get Token
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "password"
  }'
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "username": "admin",
  "role": "ADMIN"
}
```

#### 5.2 Create an Order
```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -d '{
    "customerId": 1,
    "items": [
      {
        "catalogItemId": 1,
        "quantity": 2
      }
    ]
  }'
```

#### 5.3 Get Order by ID
```bash
curl -X GET http://localhost:8080/api/v1/orders/1 \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

#### 5.4 List Orders with Filters
```bash
curl -X GET "http://localhost:8080/api/v1/orders?page=0&size=10&sortBy=createdAt&sortDirection=desc" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

#### 5.5 Cancel an Order
```bash
curl -X POST http://localhost:8080/api/v1/orders/1/cancel \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

## 🧪 Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=OrderServiceTest

# Run with coverage (if configured)
mvn test jacoco:report
```

## 🐳 Running with Docker

```bash
# Build and run
docker-compose up --build

# Run in background
docker-compose up -d

# Stop
docker-compose down
```

## 📁 Project Structure

```
sales-order-api/
├── src/
│   ├── main/
│   │   ├── java/com/example/salesorder/
│   │   │   ├── config/          # Security, JWT setup
│   │   │   ├── controller/       # REST endpoints
│   │   │   ├── domain/          # Database entities
│   │   │   ├── dto/             # Request/Response objects
│   │   │   ├── exception/       # Error handling
│   │   │   ├── repository/      # Data access
│   │   │   ├── service/         # Business logic
│   │   │   └── util/            # Utilities
│   │   └── resources/
│   │       ├── application.yml  # Configuration
│   │       └── db/changelog/    # Database migrations
│   └── test/                     # Test classes
├── Dockerfile
├── docker-compose.yml
└── pom.xml
```

## 🔍 Troubleshooting

### Issue: Port 8080 already in use
```bash
# Change port in application.yml:
server:
  port: 8081
```

### Issue: Database connection error
- Check H2 console is accessible
- Verify JDBC URL matches application.yml

### Issue: JWT token invalid
- Make sure token is included in Authorization header
- Check token hasn't expired (default: 24 hours)
- Verify JWT_SECRET matches

### Issue: Compilation errors
```bash
# Clean and rebuild
mvn clean install
```

## 📚 Next Steps

1. **Test the API** using Postman or curl
2. **Explore H2 Console** to see database tables
3. **Review the code** to understand implementation
4. **Run tests** to see how components work
5. **Modify and extend** based on your needs

## 💡 Key Takeaways

1. **Layered Architecture**: Clear separation of concerns
2. **Security First**: JWT authentication on all endpoints
3. **Data Integrity**: Optimistic locking prevents conflicts
4. **Price Immutability**: Historical data preservation
5. **Scalable Design**: Pagination, filtering, sorting ready for production

---

**Happy Coding! 🚀**

