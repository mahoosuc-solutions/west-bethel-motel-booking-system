# West Bethel Motel Booking System - API Documentation

**Version:** 1.0
**Base URL:** `http://localhost:8080/api/v1`
**Date:** 2025-10-23

---

## Table of Contents

1. [Overview](#overview)
2. [API Endpoints](#api-endpoints)
   - [Availability](#availability-api)
   - [Reservations](#reservations-api)
   - [Payments](#payments-api)
   - [Loyalty](#loyalty-api)
   - [Reporting](#reporting-api)
3. [Data Models](#data-models)
4. [Error Handling](#error-handling)
5. [Sample Requests](#sample-requests)
6. [Postman Collection](#postman-collection)
7. [API Design Assessment](#api-design-assessment)

---

## Overview

The West Bethel Motel Booking System provides a comprehensive REST API for managing hotel reservations, payments, guest loyalty programs, and operational reporting. The API follows RESTful principles and uses JSON for request and response payloads.

### Key Features
- Room availability search with pricing
- Reservation creation and cancellation
- Payment authorization, capture, refund, and void operations
- Loyalty points accrual and redemption
- Operational reporting (CSV export)

### Authentication
Currently, the API does not require authentication. **Authentication will be added in a future release.**

### Content Type
All endpoints accept and return `application/json` unless otherwise specified.

---

## API Endpoints

### Availability API

#### Search Room Availability

Search for available rooms based on dates, occupancy, and room type preferences.

**Endpoint:** `GET /api/v1/availability`

**Query Parameters:**

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| propertyId | UUID | Yes | - | Property identifier |
| startDate | Date (ISO-8601) | Yes | - | Check-in date (YYYY-MM-DD) |
| endDate | Date (ISO-8601) | Yes | - | Check-out date (YYYY-MM-DD) |
| adults | Integer | No | 1 | Number of adult guests |
| children | Integer | No | 0 | Number of children |
| roomTypes | Set<String> | No | - | Filter by room type codes (e.g., STANDARD, DELUXE, SUITE) |

**Success Response: 200 OK**

```json
{
  "roomTypes": [
    {
      "roomTypeCode": "STANDARD",
      "availableRooms": 5,
      "nightlyRates": [
        {
          "stayDate": "2025-11-01",
          "currency": "USD",
          "amount": "89.00"
        },
        {
          "stayDate": "2025-11-02",
          "currency": "USD",
          "amount": "89.00"
        }
      ]
    },
    {
      "roomTypeCode": "DELUXE",
      "availableRooms": 3,
      "nightlyRates": [
        {
          "stayDate": "2025-11-01",
          "currency": "USD",
          "amount": "129.00"
        },
        {
          "stayDate": "2025-11-02",
          "currency": "USD",
          "amount": "129.00"
        }
      ]
    }
  ]
}
```

**Error Responses:**

- `400 Bad Request` - Invalid date format or parameters
- `500 Internal Server Error` - Server processing error

**Validation Rules:**
- `startDate` must be before `endDate`
- `startDate` cannot be in the past
- `adults` must be >= 1
- `children` must be >= 0

---

### Reservations API

#### Create Reservation

Create a new hotel reservation.

**Endpoint:** `POST /api/v1/reservations`

**Request Body:**

```json
{
  "propertyId": "00000000-0000-0000-0000-000000000001",
  "guestId": "00000000-0000-0000-0000-000000000020",
  "checkIn": "2025-11-01",
  "checkOut": "2025-11-03",
  "adults": 2,
  "children": 0,
  "ratePlanId": "00000000-0000-0000-0000-000000000030",
  "roomTypeIds": [
    "00000000-0000-0000-0000-000000000010"
  ],
  "addonIds": [],
  "paymentToken": "tok_visa_4242",
  "source": "DIRECT"
}
```

**Field Descriptions:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| propertyId | UUID | Yes | Property identifier |
| guestId | UUID | Yes | Guest/customer identifier |
| checkIn | Date | Yes | Check-in date (must be in future) |
| checkOut | Date | Yes | Check-out date (must be after check-in) |
| adults | Integer | Yes | Number of adults (min: 1) |
| children | Integer | Yes | Number of children (min: 0) |
| ratePlanId | UUID | Yes | Selected rate plan |
| roomTypeIds | Set<UUID> | Yes | Room types to book (at least 1) |
| addonIds | Set<UUID> | No | Optional add-ons/extras |
| paymentToken | String | No | Payment method token |
| source | String | No | Booking source/channel |

**Success Response: 200 OK**

```json
{
  "bookingId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "confirmationNumber": "WBM-20251023-ABC123",
  "status": "CONFIRMED"
}
```

**Booking Status Values:**
- `HOLD` - Temporary hold on inventory
- `CONFIRMED` - Confirmed reservation
- `CHECKED_IN` - Guest has checked in
- `CHECKED_OUT` - Guest has checked out
- `CANCELLED` - Reservation cancelled
- `NO_SHOW` - Guest did not arrive

**Error Responses:**

- `400 Bad Request` - Validation errors or invalid request data
- `404 Not Found` - Guest, property, or rate plan not found
- `500 Internal Server Error` - Server processing error

**Validation Rules:**
- `checkIn` must be a future date
- `checkOut` must be a future date and after `checkIn`
- `adults` must be at least 1
- `children` must be 0 or greater
- `roomTypeIds` cannot be empty

---

#### Cancel Reservation

Cancel an existing reservation.

**Endpoint:** `POST /api/v1/reservations/{confirmationNumber}/cancel`

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| confirmationNumber | String | Reservation confirmation number |

**Request Body:**

```json
{
  "reason": "Customer requested cancellation due to change of plans",
  "requestedBy": "john.doe@example.com"
}
```

**Field Descriptions:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| reason | String | Yes | Reason for cancellation |
| requestedBy | String | Yes | Email or identifier of person requesting cancellation |

**Success Response: 200 OK**

```json
{
  "bookingId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "confirmationNumber": "WBM-20251023-ABC123",
  "status": "CANCELLED"
}
```

**Error Responses:**

- `400 Bad Request` - Validation errors
- `404 Not Found` - Confirmation number not found
- `500 Internal Server Error` - Server processing error

**Validation Rules:**
- `reason` cannot be blank
- `requestedBy` cannot be blank

---

### Payments API

#### Authorize Payment

Authorize a payment for an invoice (holds funds but does not capture).

**Endpoint:** `POST /api/v1/invoices/{invoiceId}/payments/authorize`

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| invoiceId | UUID | Invoice identifier |

**Request Body:**

```json
{
  "method": "CARD_NOT_PRESENT",
  "paymentToken": "tok_visa_4242424242424242",
  "amount": {
    "amount": 278.00,
    "currency": "USD"
  },
  "initiatedBy": "john.doe@example.com"
}
```

**Field Descriptions:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| method | Enum | Yes | Payment method (see Payment Methods below) |
| paymentToken | String | Yes | Payment token from payment processor |
| amount.amount | Decimal | Yes | Payment amount (must be > 0.00) |
| amount.currency | String | Yes | ISO 4217 currency code (e.g., USD) |
| initiatedBy | String | Yes | User/system initiating the payment |

**Payment Methods:**
- `CARD_PRESENT` - Card physically present
- `CARD_NOT_PRESENT` - Online/phone card payment
- `CASH` - Cash payment
- `BANK_TRANSFER` - Bank transfer/ACH
- `MOBILE_WALLET` - Mobile wallet payment

**Success Response: 200 OK**

```json
{
  "paymentId": "f1e2d3c4-b5a6-7890-cdef-123456789abc",
  "status": "AUTHORIZED",
  "processorReference": "auth_1234567890",
  "failureReason": null
}
```

**Payment Status Values:**
- `INITIATED` - Payment initiated
- `AUTHORIZED` - Payment authorized (funds held)
- `CAPTURED` - Payment captured (funds transferred)
- `REFUNDED` - Payment refunded
- `VOIDED` - Authorization voided
- `FAILED` - Payment failed

**Error Responses:**

- `400 Bad Request` - Validation errors or invalid amount
- `404 Not Found` - Invoice not found
- `500 Internal Server Error` - Payment processor error

**Validation Rules:**
- `amount` must be greater than 0.00
- `currency` must be a valid ISO 4217 code
- `paymentToken` cannot be blank
- `initiatedBy` cannot be blank

---

#### Capture Payment

Capture a previously authorized payment (completes the transaction).

**Endpoint:** `POST /api/v1/payments/{paymentId}/capture`

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| paymentId | UUID | Payment identifier |

**Request Body:** None

**Success Response: 200 OK**

```json
{
  "paymentId": "f1e2d3c4-b5a6-7890-cdef-123456789abc",
  "status": "CAPTURED",
  "processorReference": "cap_1234567890",
  "failureReason": null
}
```

**Error Responses:**

- `404 Not Found` - Payment not found
- `400 Bad Request` - Payment not in AUTHORIZED status
- `500 Internal Server Error` - Payment processor error

---

#### Refund Payment

Refund a captured payment (full or partial).

**Endpoint:** `POST /api/v1/payments/{paymentId}/refund`

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| paymentId | UUID | Payment identifier |

**Request Body:**

```json
{
  "method": "CARD_NOT_PRESENT",
  "amount": {
    "amount": 278.00,
    "currency": "USD"
  }
}
```

**Field Descriptions:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| method | Enum | Yes | Payment method (must match original) |
| amount.amount | Decimal | Yes | Refund amount (must be > 0.00) |
| amount.currency | String | Yes | ISO 4217 currency code |

**Success Response: 200 OK**

```json
{
  "paymentId": "f1e2d3c4-b5a6-7890-cdef-123456789abc",
  "status": "REFUNDED",
  "processorReference": "rfnd_1234567890",
  "failureReason": null
}
```

**Error Responses:**

- `404 Not Found` - Payment not found
- `400 Bad Request` - Payment not in CAPTURED status or refund amount exceeds original
- `500 Internal Server Error` - Payment processor error

**Validation Rules:**
- `amount` must be greater than 0.00
- Refund amount cannot exceed captured amount

---

#### Void Payment Authorization

Void a payment authorization (releases held funds).

**Endpoint:** `POST /api/v1/payments/{paymentId}/void`

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| paymentId | UUID | Payment identifier |

**Request Body:** None

**Success Response: 200 OK**

```json
{
  "paymentId": "f1e2d3c4-b5a6-7890-cdef-123456789abc",
  "status": "VOIDED",
  "processorReference": "void_1234567890",
  "failureReason": null
}
```

**Error Responses:**

- `404 Not Found` - Payment not found
- `400 Bad Request` - Payment not in AUTHORIZED status
- `500 Internal Server Error` - Payment processor error

---

### Loyalty API

#### Accrue Loyalty Points

Add loyalty points to a guest's account.

**Endpoint:** `POST /api/v1/loyalty/{guestId}/accrue`

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| guestId | UUID | Guest identifier |

**Request Body:**

```json
{
  "points": 500,
  "description": "Bonus points for direct booking"
}
```

**Field Descriptions:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| points | Long | Yes | Number of points to add (min: 0) |
| description | String | No | Description/reason for accrual |

**Success Response: 200 OK**

```json
{
  "tier": "GOLD",
  "pointsBalance": 3000,
  "pointsExpiringSoon": 0
}
```

**Loyalty Tier Values:**
- `STANDARD` - Standard tier
- `SILVER` - Silver tier
- `GOLD` - Gold tier
- `PLATINUM` - Platinum tier

**Error Responses:**

- `400 Bad Request` - Validation errors (negative points)
- `404 Not Found` - Guest not found or no loyalty profile
- `500 Internal Server Error` - Server processing error

**Validation Rules:**
- `points` must be 0 or greater

---

#### Redeem Loyalty Points

Redeem loyalty points from a guest's account.

**Endpoint:** `POST /api/v1/loyalty/{guestId}/redeem`

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| guestId | UUID | Guest identifier |

**Request Body:**

```json
{
  "points": 1000,
  "description": "Redeemed for room upgrade"
}
```

**Field Descriptions:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| points | Long | Yes | Number of points to redeem (min: 0) |
| description | String | No | Description/reason for redemption |

**Success Response: 200 OK**

```json
{
  "tier": "GOLD",
  "pointsBalance": 2000,
  "pointsExpiringSoon": 0
}
```

**Error Responses:**

- `400 Bad Request` - Validation errors or insufficient points
- `404 Not Found` - Guest not found or no loyalty profile
- `500 Internal Server Error` - Server processing error

**Validation Rules:**
- `points` must be 0 or greater
- Guest must have sufficient point balance

---

#### Get Loyalty Summary

Retrieve a guest's loyalty program summary.

**Endpoint:** `GET /api/v1/loyalty/{guestId}`

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| guestId | UUID | Guest identifier |

**Request Body:** None

**Success Response: 200 OK**

```json
{
  "tier": "GOLD",
  "pointsBalance": 2500,
  "pointsExpiringSoon": 0
}
```

**Error Responses:**

- `404 Not Found` - Guest not found or no loyalty profile
- `500 Internal Server Error` - Server processing error

---

### Reporting API

#### Generate Report

Generate operational reports in CSV format.

**Endpoint:** `GET /api/v1/reports`

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| type | Enum | Yes | Report type (see Report Types below) |
| propertyId | UUID | Yes | Property identifier |
| fromDate | Date (ISO-8601) | Yes | Report start date (YYYY-MM-DD) |
| toDate | Date (ISO-8601) | Yes | Report end date (YYYY-MM-DD) |

**Report Types:**
- `DAILY_OCCUPANCY` - Daily occupancy report
- `REVENUE_SUMMARY` - Revenue summary report
- `ADR_TREND` - Average Daily Rate trend analysis
- `HOUSEKEEPING_ROSTER` - Housekeeping assignments
- `LOYALTY_ACTIVITY` - Loyalty program activity

**Success Response: 200 OK**

```
Content-Type: text/csv
Content-Disposition: attachment; filename=abc123.csv

[CSV data payload]
```

**Error Responses:**

- `400 Bad Request` - Invalid parameters or date range
- `404 Not Found` - Property not found
- `500 Internal Server Error` - Report generation error

**Validation Rules:**
- `fromDate` must be before or equal to `toDate`
- Valid report type must be specified

---

## Data Models

### Common Types

#### UUID Format
```
00000000-0000-0000-0000-000000000001
```

#### Date Format (ISO-8601)
```
2025-11-01
```

#### Money Object
```json
{
  "amount": "89.00",
  "currency": "USD"
}
```

### Enumerations

#### BookingStatus
- `HOLD`
- `CONFIRMED`
- `CHECKED_IN`
- `CHECKED_OUT`
- `CANCELLED`
- `NO_SHOW`

#### PaymentMethod
- `CARD_PRESENT`
- `CARD_NOT_PRESENT`
- `CASH`
- `BANK_TRANSFER`
- `MOBILE_WALLET`

#### PaymentStatus
- `INITIATED`
- `AUTHORIZED`
- `CAPTURED`
- `REFUNDED`
- `VOIDED`
- `FAILED`

#### LoyaltyTier
- `STANDARD`
- `SILVER`
- `GOLD`
- `PLATINUM`

#### ReportType
- `DAILY_OCCUPANCY`
- `REVENUE_SUMMARY`
- `ADR_TREND`
- `HOUSEKEEPING_ROSTER`
- `LOYALTY_ACTIVITY`

---

## Error Handling

### Standard Error Response Format

While the system currently uses Spring Boot's default error responses, typical error responses follow this structure:

```json
{
  "timestamp": "2025-10-23T14:30:00.000+00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed for object='bookingCreateRequest'...",
  "path": "/api/v1/reservations"
}
```

### HTTP Status Codes

| Code | Description | Usage |
|------|-------------|-------|
| 200 | OK | Successful GET, POST, PUT operations |
| 400 | Bad Request | Validation errors, invalid request data |
| 404 | Not Found | Resource not found (booking, guest, property) |
| 500 | Internal Server Error | Unexpected server error |

### Common Error Scenarios

1. **Validation Errors** (400)
   - Missing required fields
   - Invalid field formats (dates, UUIDs)
   - Business rule violations (negative amounts, past dates)

2. **Not Found Errors** (404)
   - Invalid confirmation number
   - Guest not found
   - Property not found
   - Payment not found

3. **Server Errors** (500)
   - Database connectivity issues
   - Payment processor errors
   - Unexpected processing errors

---

## Sample Requests

### Scenario 1: Complete Booking Flow

#### Step 1: Search Availability

```bash
curl -X GET "http://localhost:8080/api/v1/availability?propertyId=00000000-0000-0000-0000-000000000001&startDate=2025-11-15&endDate=2025-11-17&adults=2&children=0" \
  -H "Content-Type: application/json"
```

#### Step 2: Create Reservation

```bash
curl -X POST "http://localhost:8080/api/v1/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "propertyId": "00000000-0000-0000-0000-000000000001",
    "guestId": "00000000-0000-0000-0000-000000000020",
    "checkIn": "2025-11-15",
    "checkOut": "2025-11-17",
    "adults": 2,
    "children": 0,
    "ratePlanId": "00000000-0000-0000-0000-000000000030",
    "roomTypeIds": ["00000000-0000-0000-0000-000000000010"],
    "paymentToken": "tok_visa_4242"
  }'
```

### Scenario 2: Payment Processing

#### Authorize Payment

```bash
curl -X POST "http://localhost:8080/api/v1/invoices/11111111-1111-1111-1111-111111111111/payments/authorize" \
  -H "Content-Type: application/json" \
  -d '{
    "method": "CARD_NOT_PRESENT",
    "paymentToken": "tok_visa_4242424242424242",
    "amount": {
      "amount": 278.00,
      "currency": "USD"
    },
    "initiatedBy": "john.doe@example.com"
  }'
```

#### Capture Payment

```bash
curl -X POST "http://localhost:8080/api/v1/payments/f1e2d3c4-b5a6-7890-cdef-123456789abc/capture" \
  -H "Content-Type: application/json"
```

### Scenario 3: Loyalty Management

#### Check Loyalty Balance

```bash
curl -X GET "http://localhost:8080/api/v1/loyalty/00000000-0000-0000-0000-000000000021" \
  -H "Content-Type: application/json"
```

#### Accrue Points

```bash
curl -X POST "http://localhost:8080/api/v1/loyalty/00000000-0000-0000-0000-000000000021/accrue" \
  -H "Content-Type: application/json" \
  -d '{
    "points": 500,
    "description": "Stay bonus - 2 nights"
  }'
```

### Scenario 4: Cancellation

```bash
curl -X POST "http://localhost:8080/api/v1/reservations/WBM-20251023-ABC123/cancel" \
  -H "Content-Type: application/json" \
  -d '{
    "reason": "Customer requested cancellation",
    "requestedBy": "john.doe@example.com"
  }'
```

### Scenario 5: Generate Report

```bash
curl -X GET "http://localhost:8080/api/v1/reports?type=DAILY_OCCUPANCY&propertyId=00000000-0000-0000-0000-000000000001&fromDate=2025-11-01&toDate=2025-11-30" \
  -H "Content-Type: application/json" \
  -o occupancy_report.csv
```

---

## Postman Collection

### Collection JSON

```json
{
  "info": {
    "name": "West Bethel Motel Booking System",
    "description": "Complete API collection for the West Bethel Motel Booking System",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Availability",
      "item": [
        {
          "name": "Search Availability",
          "request": {
            "method": "GET",
            "header": [],
            "url": {
              "raw": "{{baseUrl}}/availability?propertyId={{propertyId}}&startDate=2025-11-15&endDate=2025-11-17&adults=2&children=0",
              "host": ["{{baseUrl}}"],
              "path": ["availability"],
              "query": [
                {
                  "key": "propertyId",
                  "value": "{{propertyId}}"
                },
                {
                  "key": "startDate",
                  "value": "2025-11-15"
                },
                {
                  "key": "endDate",
                  "value": "2025-11-17"
                },
                {
                  "key": "adults",
                  "value": "2"
                },
                {
                  "key": "children",
                  "value": "0"
                }
              ]
            }
          }
        }
      ]
    },
    {
      "name": "Reservations",
      "item": [
        {
          "name": "Create Reservation",
          "request": {
            "method": "POST",
            "header": [
              {
                "key": "Content-Type",
                "value": "application/json"
              }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"propertyId\": \"{{propertyId}}\",\n  \"guestId\": \"{{guestJohnId}}\",\n  \"checkIn\": \"2025-11-15\",\n  \"checkOut\": \"2025-11-17\",\n  \"adults\": 2,\n  \"children\": 0,\n  \"ratePlanId\": \"{{ratePlanStandardId}}\",\n  \"roomTypeIds\": [\"{{roomTypeStandardId}}\"],\n  \"paymentToken\": \"tok_visa_4242\"\n}"
            },
            "url": {
              "raw": "{{baseUrl}}/reservations",
              "host": ["{{baseUrl}}"],
              "path": ["reservations"]
            }
          }
        },
        {
          "name": "Cancel Reservation",
          "request": {
            "method": "POST",
            "header": [
              {
                "key": "Content-Type",
                "value": "application/json"
              }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"reason\": \"Customer requested cancellation\",\n  \"requestedBy\": \"john.doe@example.com\"\n}"
            },
            "url": {
              "raw": "{{baseUrl}}/reservations/:confirmationNumber/cancel",
              "host": ["{{baseUrl}}"],
              "path": ["reservations", ":confirmationNumber", "cancel"],
              "variable": [
                {
                  "key": "confirmationNumber",
                  "value": "WBM-20251023-ABC123"
                }
              ]
            }
          }
        }
      ]
    },
    {
      "name": "Payments",
      "item": [
        {
          "name": "Authorize Payment",
          "request": {
            "method": "POST",
            "header": [
              {
                "key": "Content-Type",
                "value": "application/json"
              }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"method\": \"CARD_NOT_PRESENT\",\n  \"paymentToken\": \"tok_visa_4242424242424242\",\n  \"amount\": {\n    \"amount\": 278.00,\n    \"currency\": \"USD\"\n  },\n  \"initiatedBy\": \"john.doe@example.com\"\n}"
            },
            "url": {
              "raw": "{{baseUrl}}/invoices/:invoiceId/payments/authorize",
              "host": ["{{baseUrl}}"],
              "path": ["invoices", ":invoiceId", "payments", "authorize"],
              "variable": [
                {
                  "key": "invoiceId",
                  "value": "11111111-1111-1111-1111-111111111111"
                }
              ]
            }
          }
        },
        {
          "name": "Capture Payment",
          "request": {
            "method": "POST",
            "header": [],
            "url": {
              "raw": "{{baseUrl}}/payments/:paymentId/capture",
              "host": ["{{baseUrl}}"],
              "path": ["payments", ":paymentId", "capture"],
              "variable": [
                {
                  "key": "paymentId",
                  "value": "{{paymentId}}"
                }
              ]
            }
          }
        },
        {
          "name": "Refund Payment",
          "request": {
            "method": "POST",
            "header": [
              {
                "key": "Content-Type",
                "value": "application/json"
              }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"method\": \"CARD_NOT_PRESENT\",\n  \"amount\": {\n    \"amount\": 278.00,\n    \"currency\": \"USD\"\n  }\n}"
            },
            "url": {
              "raw": "{{baseUrl}}/payments/:paymentId/refund",
              "host": ["{{baseUrl}}"],
              "path": ["payments", ":paymentId", "refund"],
              "variable": [
                {
                  "key": "paymentId",
                  "value": "{{paymentId}}"
                }
              ]
            }
          }
        },
        {
          "name": "Void Payment",
          "request": {
            "method": "POST",
            "header": [],
            "url": {
              "raw": "{{baseUrl}}/payments/:paymentId/void",
              "host": ["{{baseUrl}}"],
              "path": ["payments", ":paymentId", "void"],
              "variable": [
                {
                  "key": "paymentId",
                  "value": "{{paymentId}}"
                }
              ]
            }
          }
        }
      ]
    },
    {
      "name": "Loyalty",
      "item": [
        {
          "name": "Get Loyalty Summary",
          "request": {
            "method": "GET",
            "header": [],
            "url": {
              "raw": "{{baseUrl}}/loyalty/:guestId",
              "host": ["{{baseUrl}}"],
              "path": ["loyalty", ":guestId"],
              "variable": [
                {
                  "key": "guestId",
                  "value": "{{guestJaneId}}"
                }
              ]
            }
          }
        },
        {
          "name": "Accrue Points",
          "request": {
            "method": "POST",
            "header": [
              {
                "key": "Content-Type",
                "value": "application/json"
              }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"points\": 500,\n  \"description\": \"Stay bonus - 2 nights\"\n}"
            },
            "url": {
              "raw": "{{baseUrl}}/loyalty/:guestId/accrue",
              "host": ["{{baseUrl}}"],
              "path": ["loyalty", ":guestId", "accrue"],
              "variable": [
                {
                  "key": "guestId",
                  "value": "{{guestJaneId}}"
                }
              ]
            }
          }
        },
        {
          "name": "Redeem Points",
          "request": {
            "method": "POST",
            "header": [
              {
                "key": "Content-Type",
                "value": "application/json"
              }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"points\": 1000,\n  \"description\": \"Redeemed for room upgrade\"\n}"
            },
            "url": {
              "raw": "{{baseUrl}}/loyalty/:guestId/redeem",
              "host": ["{{baseUrl}}"],
              "path": ["loyalty", ":guestId", "redeem"],
              "variable": [
                {
                  "key": "guestId",
                  "value": "{{guestJaneId}}"
                }
              ]
            }
          }
        }
      ]
    },
    {
      "name": "Reports",
      "item": [
        {
          "name": "Daily Occupancy Report",
          "request": {
            "method": "GET",
            "header": [],
            "url": {
              "raw": "{{baseUrl}}/reports?type=DAILY_OCCUPANCY&propertyId={{propertyId}}&fromDate=2025-11-01&toDate=2025-11-30",
              "host": ["{{baseUrl}}"],
              "path": ["reports"],
              "query": [
                {
                  "key": "type",
                  "value": "DAILY_OCCUPANCY"
                },
                {
                  "key": "propertyId",
                  "value": "{{propertyId}}"
                },
                {
                  "key": "fromDate",
                  "value": "2025-11-01"
                },
                {
                  "key": "toDate",
                  "value": "2025-11-30"
                }
              ]
            }
          }
        },
        {
          "name": "Revenue Summary Report",
          "request": {
            "method": "GET",
            "header": [],
            "url": {
              "raw": "{{baseUrl}}/reports?type=REVENUE_SUMMARY&propertyId={{propertyId}}&fromDate=2025-11-01&toDate=2025-11-30",
              "host": ["{{baseUrl}}"],
              "path": ["reports"],
              "query": [
                {
                  "key": "type",
                  "value": "REVENUE_SUMMARY"
                },
                {
                  "key": "propertyId",
                  "value": "{{propertyId}}"
                },
                {
                  "key": "fromDate",
                  "value": "2025-11-01"
                },
                {
                  "key": "toDate",
                  "value": "2025-11-30"
                }
              ]
            }
          }
        }
      ]
    }
  ],
  "variable": [
    {
      "key": "baseUrl",
      "value": "http://localhost:8080/api/v1"
    },
    {
      "key": "propertyId",
      "value": "00000000-0000-0000-0000-000000000001"
    },
    {
      "key": "roomTypeStandardId",
      "value": "00000000-0000-0000-0000-000000000010"
    },
    {
      "key": "roomTypeDeluxeId",
      "value": "00000000-0000-0000-0000-000000000011"
    },
    {
      "key": "roomTypeSuiteId",
      "value": "00000000-0000-0000-0000-000000000012"
    },
    {
      "key": "guestJohnId",
      "value": "00000000-0000-0000-0000-000000000020"
    },
    {
      "key": "guestJaneId",
      "value": "00000000-0000-0000-0000-000000000021"
    },
    {
      "key": "guestBobId",
      "value": "00000000-0000-0000-0000-000000000022"
    },
    {
      "key": "ratePlanStandardId",
      "value": "00000000-0000-0000-0000-000000000030"
    },
    {
      "key": "ratePlanWeekendId",
      "value": "00000000-0000-0000-0000-000000000031"
    },
    {
      "key": "paymentId",
      "value": "f1e2d3c4-b5a6-7890-cdef-123456789abc"
    }
  ]
}
```

### How to Import

1. Open Postman
2. Click **Import** button
3. Select **Raw text** tab
4. Paste the JSON above
5. Click **Import**
6. The collection will be available with all endpoints configured

### Environment Variables

The collection uses the following variables (pre-configured):

- `baseUrl`: http://localhost:8080/api/v1
- `propertyId`: 00000000-0000-0000-0000-000000000001
- `roomTypeStandardId`: 00000000-0000-0000-0000-000000000010
- `roomTypeDeluxeId`: 00000000-0000-0000-0000-000000000011
- `roomTypeSuiteId`: 00000000-0000-0000-0000-000000000012
- `guestJohnId`: 00000000-0000-0000-0000-000000000020
- `guestJaneId`: 00000000-0000-0000-0000-000000000021
- `ratePlanStandardId`: 00000000-0000-0000-0000-000000000030
- `paymentId`: (set this after creating a payment)

---

## API Design Assessment

### Strengths

#### 1. RESTful Design
- **Resource-Based URLs**: Endpoints follow RESTful conventions with clear resource hierarchies
  - `/api/v1/reservations` for bookings
  - `/api/v1/payments/{id}/capture` for payment operations
  - `/api/v1/loyalty/{guestId}` for loyalty operations

- **Appropriate HTTP Verbs**: Correct usage of GET for queries, POST for creation/actions
  - GET for availability search and loyalty summary
  - POST for creating reservations, processing payments, cancellations

- **Versioning**: API includes version prefix (`/api/v1`) enabling future evolution

#### 2. Clear Domain Separation
- Five distinct controllers with single responsibilities:
  - AvailabilityController - room search
  - BookingController - reservation management
  - PaymentController - payment processing
  - LoyaltyController - loyalty program
  - ReportingController - operational reporting

#### 3. Validation
- Comprehensive Jakarta Bean Validation annotations:
  - `@NotNull`, `@NotBlank`, `@NotEmpty` for required fields
  - `@Future` for date validation
  - `@Min` for numeric constraints
  - `@DecimalMin` for monetary amounts
  - `@Valid` for nested object validation

#### 4. Strong Type Safety
- UUID usage for identifiers (prevents enumeration attacks)
- Enums for status values and types (prevents invalid states)
- Explicit DTOs for request/response separation

#### 5. Proper Path Parameters
- Cancellation uses confirmation number in path: `/{confirmationNumber}/cancel`
- Payment operations use payment ID: `/{paymentId}/capture`
- Loyalty operations use guest ID: `/{guestId}/accrue`

### Areas for Improvement

#### 1. Missing Error Handling Layer
**Issue**: No global exception handler (@RestControllerAdvice)

**Impact**:
- Inconsistent error response formats
- Validation errors may expose internal details
- Difficult to troubleshoot for API consumers

**Recommendation**:
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        // Return consistent validation error format
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(EntityNotFoundException ex) {
        // Return 404 with clear message
    }
}
```

#### 2. Inconsistent HTTP Status Codes
**Issue**: All successful responses return 200 OK

**Impact**:
- POST for creating reservations should return 201 Created
- Cancellation (destructive operation) could return 204 No Content

**Recommendation**:
```java
@PostMapping
public ResponseEntity<BookingResponseDto> create(@Valid @RequestBody BookingCreateRequest request) {
    BookingResponse response = bookingService.create(mapper.toBookingRequest(request));
    return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toDto(response));
}
```

#### 3. Missing Pagination
**Issue**: No pagination support for potentially large result sets

**Impact**:
- Availability search could return many room types
- Reporting endpoints could return large datasets
- No way to limit or page through results

**Recommendation**:
- Add `Pageable` parameter to GET endpoints that return collections
- Return `Page<T>` or implement custom pagination response

#### 4. Limited Query Capabilities
**Issue**: Availability endpoint has basic filtering but no advanced queries

**Current**: Only filter by room types
**Missing**:
- Price range filtering
- Amenity filtering
- Sorting options (by price, capacity, etc.)

**Recommendation**:
```java
@GetMapping
public ResponseEntity<AvailabilityResult> searchAvailability(
    @RequestParam UUID propertyId,
    @RequestParam LocalDate startDate,
    @RequestParam LocalDate endDate,
    @RequestParam(required = false) BigDecimal maxPrice,
    @RequestParam(required = false) Set<String> amenities,
    @RequestParam(defaultValue = "price") String sortBy
) { ... }
```

#### 5. No GET Endpoint for Reservations
**Issue**: Cannot retrieve reservation details

**Impact**:
- No way to view a booking after creation
- Must store full response client-side
- Cannot verify booking status

**Recommendation**:
```java
@GetMapping("/{confirmationNumber}")
public ResponseEntity<BookingDetailDto> getBooking(
    @PathVariable String confirmationNumber
) { ... }

@GetMapping
public ResponseEntity<Page<BookingDto>> listBookings(
    @RequestParam(required = false) UUID guestId,
    Pageable pageable
) { ... }
```

#### 6. Payment API Design Concerns
**Issue**: Multiple concerns in payment endpoint design

**Problems**:
- Authorize endpoint requires invoice ID, but payment ID used for other operations
- No GET endpoint to retrieve payment status/details
- Refund uses hardcoded "SYSTEM" for initiatedBy field
- No support for partial refunds (validation needed)

**Recommendation**:
```java
// Add payment retrieval
@GetMapping("/payments/{paymentId}")
public ResponseEntity<PaymentDetailDto> getPayment(@PathVariable UUID paymentId) { ... }

// Fix refund to accept initiatedBy
@PostMapping("/payments/{paymentId}/refund")
public ResponseEntity<PaymentResult> refund(
    @PathVariable UUID paymentId,
    @Valid @RequestBody PaymentRefundRequest request  // Include initiatedBy
) { ... }
```

#### 7. Missing HATEOAS Links
**Issue**: No hypermedia controls in responses

**Impact**:
- Clients must construct URLs manually
- No discoverability of available actions
- Harder to evolve API

**Recommendation**:
```java
// Add links to booking response
{
  "bookingId": "...",
  "confirmationNumber": "...",
  "status": "CONFIRMED",
  "_links": {
    "self": { "href": "/api/v1/reservations/WBM-..." },
    "cancel": { "href": "/api/v1/reservations/WBM-.../cancel" },
    "invoice": { "href": "/api/v1/invoices/..." }
  }
}
```

#### 8. No Authentication/Authorization
**Issue**: No security layer (noted in documentation)

**Impact**:
- Anyone can access all endpoints
- No user context for operations
- Cannot restrict operations by role
- No audit trail of who performed actions

**Recommendation**: Implement Spring Security with JWT or OAuth2
- Add authentication filter
- Protect sensitive endpoints (payments, cancellations)
- Add role-based access control (GUEST, STAFF, ADMIN)

#### 9. Missing Rate Limiting
**Issue**: No protection against abuse

**Impact**:
- Vulnerable to DoS attacks
- No throttling for expensive operations (availability search, report generation)

**Recommendation**: Implement rate limiting using Spring Cloud Gateway or Bucket4j

#### 10. Reporting Content Negotiation
**Issue**: Reports only return CSV format

**Impact**:
- No flexibility for different export formats
- No option for JSON response
- Limited integration options

**Recommendation**:
```java
@GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE, "text/csv"})
public ResponseEntity<?> generateReport(
    @RequestHeader(value = "Accept", defaultValue = MediaType.APPLICATION_JSON_VALUE) String accept,
    @RequestParam ReportType type,
    // ... other params
) {
    if (accept.contains("csv")) {
        // Return CSV
    } else {
        // Return JSON
    }
}
```

### Best Practices Followed

1. **DTO Pattern**: Separate API DTOs from domain models
2. **Builder Pattern**: Lombok @Builder for immutable objects
3. **Validation**: Jakarta Bean Validation throughout
4. **REST Maturity Level 2**: Proper HTTP verbs and status codes
5. **ISO Standards**: ISO-8601 dates, ISO-4217 currencies
6. **Semantic Versioning**: `/api/v1` prefix

### Security Recommendations

1. **Add Authentication**: Implement JWT or OAuth2
2. **Input Validation**: Already implemented, maintain strict validation
3. **SQL Injection Protection**: Using JPA/Hibernate (parameterized queries)
4. **Rate Limiting**: Add throttling for public endpoints
5. **CORS Configuration**: Configure allowed origins
6. **HTTPS Only**: Enforce TLS in production
7. **Sensitive Data**: Mask payment tokens in logs

### Performance Recommendations

1. **Caching**: Add caching for availability searches (already using Redis)
2. **Async Processing**: Consider async for report generation
3. **Database Indexing**: Ensure indexes on frequently queried fields
4. **Connection Pooling**: Configure appropriate pool sizes
5. **Monitoring**: Add APM for performance tracking

---

## Testing the API

### Prerequisites

1. Start the application with dev profile:
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

2. Ensure PostgreSQL and Redis are running

3. Database will be seeded with test data automatically

### Quick Test Suite

```bash
# 1. Check availability
curl -X GET "http://localhost:8080/api/v1/availability?propertyId=00000000-0000-0000-0000-000000000001&startDate=2025-11-15&endDate=2025-11-17&adults=2"

# 2. Create a booking
curl -X POST "http://localhost:8080/api/v1/reservations" \
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

# 3. Check loyalty points
curl -X GET "http://localhost:8080/api/v1/loyalty/00000000-0000-0000-0000-000000000021"
```

---

## Changelog

### Version 1.0 (2025-10-23)
- Initial API documentation
- 5 controller modules documented
- 13 total endpoints
- Postman collection included
- API design assessment completed

---

## Support

For questions or issues:
- Email: info@westbethelmotel.com
- Phone: +1-207-555-0100

---

**Document Version:** 1.0
**Last Updated:** 2025-10-23
**Maintained By:** West Bethel Motel Development Team
