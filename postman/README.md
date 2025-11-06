# Postman Collection for Sales Order API

## Import Instructions

1. Open Postman
2. Click **Import** button
3. Select `Sales_Order_API.postman_collection.json`
4. The collection will be imported with all requests

## Environment Setup

### Create Environment Variables

1. Click on **Environments** in Postman
2. Create a new environment (e.g., "Local Development")
3. Add the following variables:

| Variable | Initial Value | Current Value |
|----------|---------------|---------------|
| `base_url` | `http://localhost:8080` | `http://localhost:8080` |
| `jwt_token` | (empty) | (auto-populated after login) |
| `user_role` | (empty) | (auto-populated after login) |

### For Different Environments

- **Local:** `base_url = http://localhost:8080`
- **Docker:** `base_url = http://localhost:8080`
- **Production:** `base_url = https://your-production-url.com`

## Usage Flow

### 1. Authenticate First
1. Run **"Login - Get JWT Token"** request
2. The JWT token will be automatically saved to `jwt_token` variable
3. All subsequent requests will use this token

### 2. Create an Order
1. Run **"Create Order"** request
2. Note the `id` from the response
3. Use this ID for other operations

### 3. Query Orders
- **"List All Orders"** - Get all orders with default pagination
- **"List Orders with Filters"** - Filter by dates, pagination, sorting
- **"Get Order by ID"** - Get specific order details

### 4. Cancel Order
- Run **"Cancel Order"** with the order ID

## Request Examples

### Create Order Request Body
```json
{
    "customerId": 1,
    "items": [
        {
            "catalogItemId": 1,
            "quantity": 2
        },
        {
            "catalogItemId": 2,
            "quantity": 1
        }
    ]
}
```

### Query Parameters for List Orders
- `creationDateFrom`: Start date (YYYY-MM-DD)
- `creationDateTo`: End date (YYYY-MM-DD)
- `cancellationDateFrom`: Cancellation start date
- `cancellationDateTo`: Cancellation end date
- `page`: Page number (default: 0)
- `size`: Page size (default: 20)
- `sortBy`: Field to sort by (default: createdAt)
- `sortDirection`: asc or desc (default: desc)

## Testing Tips

1. **Always authenticate first** - The login request automatically saves the token
2. **Use variables** - The collection uses `{{jwt_token}}` for authorization
3. **Check responses** - All responses include detailed order information
4. **Test error cases** - Try invalid IDs, missing fields, etc.

## Troubleshooting

### 401 Unauthorized
- Make sure you've run the login request first
- Check that `jwt_token` variable is set
- Token might have expired (default: 24 hours)

### 404 Not Found
- Verify the order ID exists
- Check that `base_url` is correct

### 400 Bad Request
- Validate request body format
- Check required fields are present
- Ensure customerId and catalogItemId exist

