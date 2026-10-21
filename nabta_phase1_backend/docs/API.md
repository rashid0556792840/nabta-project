# Nabta Backend API
Base URL (development): `http://localhost:8080`

JSON is used for request and response bodies. Protected endpoints require `Authorization: Bearer <token>`. List endpoints return Spring's paginated JSON shape (`content`, `totalElements`, `totalPages`, etc.).

## Public endpoints
| Method | Path | Purpose |
|---|---|---|
| GET | `/api/health` | Simple health check |
| POST | `/api/auth/register` | Register a customer |
| POST | `/api/auth/login` | Login; returns token and user |
| GET | `/api/products?q=&categoryId=&page=0&size=20` | Browse/search active products |
| GET | `/api/products/{id}` | Product details |
| GET | `/api/categories` | Active categories |
| GET | `/api/categories/{id}/products` | Products in category |
| GET | `/api/company` | Public company contact settings |

## Customer endpoints (Bearer token)
| Method | Path | Purpose |
|---|---|---|
| GET | `/api/auth/me` or `/api/users/me` | Current account |
| PUT | `/api/users/me` | Update display name |
| POST | `/api/orders` | Place order; send unique `Idempotency-Key` header (UUID) for safe retry |
| GET | `/api/orders/my?page=0&size=20` | Own order history |
| GET | `/api/orders/{id}` | Own order detail; other customers' orders return 404 |

Example order JSON:
```json
{
  "items": [{"productId": 1, "quantity": 2}],
  "deliveryAddress": "Building 5, Street 10, City",
  "contactPhone": "+911234567890",
  "notes": "Call on arrival",
  "paymentMethod": "CASH_ON_DELIVERY"
}
```
The backend recalculates totals using database prices and locks products while checking/updating stock. Delivery charge is currently zero because no delivery-fee rule was supplied. Cash on delivery is the only configured payment method; no online payment is simulated.

## Administrator endpoints (`ROLE_ADMIN`)
| Method | Path | Purpose |
|---|---|---|
| GET | `/api/admin/dashboard` | Database-derived statistics |
| GET/POST | `/api/admin/products` | List/create products |
| PUT/DELETE | `/api/admin/products/{id}` | Update/deactivate product |
| GET/POST | `/api/admin/categories` | List/create categories |
| PUT/DELETE | `/api/admin/categories/{id}` | Update/deactivate category |
| GET | `/api/admin/inventory` | Stock list and low-stock indicator (threshold 5) |
| PUT | `/api/admin/inventory/{productId}?quantity=12` | Set a nonnegative stock quantity |
| GET | `/api/admin/orders?status=PENDING` | List/filter orders |
| PUT | `/api/admin/orders/{id}/status` | Update valid order status |
| GET | `/api/admin/users` | List registered accounts |
| PUT | `/api/admin/users/{id}/status` | Activate/disable account |
| PUT | `/api/admin/company` | Update company contact details |

Product JSON:
```json
{"name":"Snake Plant","description":"Indoor plant","price":19.99,"imageUrl":"https://example.com/plant.jpg","categoryId":1,"stockQuantity":20,"active":true}
```
Category JSON:
```json
{"name":"Plants","description":"Indoor and outdoor plants","imageUrl":null,"active":true}
```
Status JSON: `{"status":"CONFIRMED"}`. Allowed flow is `PENDING -> CONFIRMED -> PROCESSING -> COMPLETED`; cancellation is allowed from `PENDING` or `CONFIRMED` and restores stock.

## Error format
Errors generally return `{ "timestamp": "...", "status": 400, "error": "Bad Request", "message": "...", "path": "/api/...", "fieldErrors": {} }`.
