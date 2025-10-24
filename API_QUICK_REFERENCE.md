# West Bethel Motel Booking System - API Quick Reference

**Base URL:** `http://localhost:8080/api/v1`

---

## Endpoints Summary

### Availability
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/availability` | Search available rooms with pricing |

### Reservations
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/reservations` | Create a new reservation |
| POST | `/reservations/{confirmationNumber}/cancel` | Cancel a reservation |

### Payments
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/invoices/{invoiceId}/payments/authorize` | Authorize payment (hold funds) |
| POST | `/payments/{paymentId}/capture` | Capture authorized payment |
| POST | `/payments/{paymentId}/refund` | Refund a payment |
| POST | `/payments/{paymentId}/void` | Void payment authorization |

### Loyalty
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/loyalty/{guestId}` | Get loyalty account summary |
| POST | `/loyalty/{guestId}/accrue` | Add loyalty points |
| POST | `/loyalty/{guestId}/redeem` | Redeem loyalty points |

### Reports
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/reports` | Generate operational report (CSV) |

---

## Test Data (from DataSeeder)

### Property
- **ID:** `00000000-0000-0000-0000-000000000001`
- **Code:** WBM
- **Name:** West Bethel Motel

### Room Types
| Code | ID | Price | Capacity |
|------|-----|-------|----------|
| STANDARD | `00000000-0000-0000-0000-000000000010` | $89 | 2 guests |
| DELUXE | `00000000-0000-0000-0000-000000000011` | $129 | 3 guests |
| SUITE | `00000000-0000-0000-0000-000000000012` | $199 | 4 guests |

### Test Guests
| Name | ID | Email | Loyalty |
|------|-----|-------|---------|
| John Doe | `00000000-0000-0000-0000-000000000020` | john.doe@example.com | No |
| Jane Smith | `00000000-0000-0000-0000-000000000021` | jane.smith@example.com | GOLD (2500 pts) |
| Bob Jones | `00000000-0000-0000-0000-000000000022` | bob.jones@example.com | No |

### Rate Plans
| Name | ID | Channel |
|------|-----|---------|
| Standard Rate | `00000000-0000-0000-0000-000000000030` | DIRECT |
| Weekend Special | `00000000-0000-0000-0000-000000000031` | DIRECT |

---

## Quick Examples

### 1. Search Availability
```bash
curl "http://localhost:8080/api/v1/availability?propertyId=00000000-0000-0000-0000-000000000001&startDate=2025-11-15&endDate=2025-11-17&adults=2"
```

### 2. Create Booking
```bash
curl -X POST http://localhost:8080/api/v1/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "propertyId": "00000000-0000-0000-0000-000000000001",
    "guestId": "00000000-0000-0000-0000-000000000020",
    "checkIn": "2025-11-15",
    "checkOut": "2025-11-17",
    "adults": 2,
    "children": 0,
    "ratePlanId": "00000000-0000-0000-0000-000000000030",
    "roomTypeIds": ["00000000-0000-0000-0000-000000000010"]
  }'
```

### 3. Check Loyalty Points
```bash
curl "http://localhost:8080/api/v1/loyalty/00000000-0000-0000-0000-000000000021"
```

### 4. Authorize Payment
```bash
curl -X POST http://localhost:8080/api/v1/invoices/{invoiceId}/payments/authorize \
  -H "Content-Type: application/json" \
  -d '{
    "method": "CARD_NOT_PRESENT",
    "paymentToken": "tok_visa_4242",
    "amount": {"amount": 278.00, "currency": "USD"},
    "initiatedBy": "john.doe@example.com"
  }'
```

---

## Enumerations Quick Reference

### BookingStatus
`HOLD` | `CONFIRMED` | `CHECKED_IN` | `CHECKED_OUT` | `CANCELLED` | `NO_SHOW`

### PaymentMethod
`CARD_PRESENT` | `CARD_NOT_PRESENT` | `CASH` | `BANK_TRANSFER` | `MOBILE_WALLET`

### PaymentStatus
`INITIATED` | `AUTHORIZED` | `CAPTURED` | `REFUNDED` | `VOIDED` | `FAILED`

### LoyaltyTier
`STANDARD` | `SILVER` | `GOLD` | `PLATINUM`

### ReportType
`DAILY_OCCUPANCY` | `REVENUE_SUMMARY` | `ADR_TREND` | `HOUSEKEEPING_ROSTER` | `LOYALTY_ACTIVITY`

---

## Common HTTP Status Codes

| Code | Meaning | When Used |
|------|---------|-----------|
| 200 | OK | Successful operation |
| 400 | Bad Request | Validation error, invalid input |
| 404 | Not Found | Resource not found |
| 500 | Internal Server Error | Unexpected server error |

---

For complete documentation, see [API_DOCUMENTATION.md](./API_DOCUMENTATION.md)
