# Exception Handling Guide

## Quick Reference for Developers

This guide shows how to use the custom exceptions in your service and controller code.

## Available Custom Exceptions

### 1. RoomNotAvailableException
**When to use**: Room is not available for the requested dates

```java
// In service layer
if (!isRoomAvailable(roomId, startDate, endDate)) {
    throw new RoomNotAvailableException(roomId, startDate, endDate);
}

// Simple version
throw new RoomNotAvailableException("Room is fully booked for these dates");
```

**Returns**: HTTP 409 Conflict with error code `ROOM_NOT_AVAILABLE`

---

### 2. InvalidDateRangeException
**When to use**: Invalid date range provided (e.g., end date before start date)

```java
// In service layer
if (endDate.isBefore(startDate)) {
    throw new InvalidDateRangeException(startDate, endDate);
}

// Simple version
throw new InvalidDateRangeException("Check-in date must be before check-out date");
```

**Returns**: HTTP 400 Bad Request with error code `INVALID_DATE_RANGE`

---

### 3. PaymentFailedException
**When to use**: Payment processing fails

```java
// In payment service
try {
    processPayment(command);
} catch (PaymentGatewayException e) {
    throw new PaymentFailedException(paymentId, "Gateway timeout", e);
}

// Simple version
throw new PaymentFailedException("Payment processing failed");
```

**Returns**: HTTP 402 Payment Required with error code `PAYMENT_FAILED`

---

### 4. InsufficientFundsException
**When to use**: Payment fails due to insufficient funds

```java
// In payment service
if (availableBalance.compareTo(requiredAmount) < 0) {
    throw new InsufficientFundsException(requiredAmount, availableBalance);
}

// Simple version
throw new InsufficientFundsException("Insufficient funds for this transaction");
```

**Returns**: HTTP 402 Payment Required with error code `INSUFFICIENT_FUNDS`

---

### 5. ResourceNotFoundException
**When to use**: Requested resource doesn't exist

```java
// In service layer
Booking booking = bookingRepository.findById(id)
    .orElseThrow(() -> new ResourceNotFoundException("Booking", id.toString()));

// For other resources
throw new ResourceNotFoundException("Guest", guestId);
throw new ResourceNotFoundException("Payment", paymentId);
```

**Returns**: HTTP 404 Not Found with error code `RESOURCE_NOT_FOUND`

---

### 6. InvalidCredentialsException
**When to use**: Login credentials are invalid

```java
// In authentication service
if (!passwordMatches(password, user.getPassword())) {
    throw new InvalidCredentialsException();
}

// With custom message
throw new InvalidCredentialsException("Account locked due to multiple failed attempts");
```

**Returns**: HTTP 401 Unauthorized with error code `INVALID_CREDENTIALS`

---

### 7. TokenExpiredException
**When to use**: JWT token has expired

```java
// In JWT service
if (isTokenExpired(token)) {
    throw new TokenExpiredException();
}

// With custom message
throw new TokenExpiredException("Session expired. Please login again");
```

**Returns**: HTTP 401 Unauthorized with error code `TOKEN_EXPIRED`

---

### 8. UnauthorizedException
**When to use**: User doesn't have permission for the action

```java
// In service layer
if (!user.hasRole("ADMIN")) {
    throw new UnauthorizedException("Admin access required");
}

// Simple version
throw new UnauthorizedException();
```

**Returns**: HTTP 403 Forbidden with error code `UNAUTHORIZED`

---

### 9. BookingException (Base Exception)
**When to use**: General booking-related errors

```java
// In service layer
throw new BookingException("BOOKING_CONFLICT", "Room is under maintenance");

// With cause
throw new BookingException("SYSTEM_ERROR", "Failed to create booking", cause);
```

**Returns**: HTTP 400 Bad Request with custom error code

---

## Validation Errors

For validation errors, use standard Jakarta Bean Validation annotations:

```java
public class BookingCreateRequest {

    @NotNull(message = "Guest email is required")
    @Email(message = "Invalid email format")
    private String guestEmail;

    @NotNull(message = "Check-in date is required")
    @FutureOrPresent(message = "Check-in date must be today or in the future")
    private LocalDate checkInDate;

    @Min(value = 1, message = "At least one adult is required")
    @Max(value = 10, message = "Maximum 10 adults per room")
    private Integer adults;
}
```

**Returns**: HTTP 400 Bad Request with error code `VALIDATION_FAILED` and field-level errors

---

## Security Audit Logging

Use `SecurityAuditService` for security-related operations:

```java
@Service
public class PaymentServiceImpl {

    private final SecurityAuditService auditService;

    public PaymentResult authorizePayment(PaymentCommand command) {
        try {
            PaymentResult result = gateway.authorize(command);

            // Audit successful operation
            auditService.auditPaymentOperation(
                "AUTHORIZE",
                result.getPaymentId(),
                command.getInitiatedBy(),
                "SUCCESS"
            );

            return result;

        } catch (Exception e) {
            // Audit failed operation
            auditService.auditPaymentOperation(
                "AUTHORIZE",
                command.getInvoiceId(),
                command.getInitiatedBy(),
                "FAILED"
            );

            throw new PaymentFailedException("Authorization failed", e);
        }
    }
}
```

### Available Audit Methods

```java
// Authentication events
auditService.auditSuccessfulLogin(username, ipAddress);
auditService.auditFailedLogin(username, ipAddress, reason);
auditService.auditLogout(username, ipAddress);

// Authorization events
auditService.auditAccessDenied(username, resource, action);

// Payment operations
auditService.auditPaymentOperation(operation, paymentId, performedBy, status);

// Booking operations
auditService.auditBookingOperation(operation, bookingId, performedBy);

// Sensitive data access
auditService.auditSensitiveDataAccess(dataType, dataId, performedBy, action);

// Security events
auditService.auditPasswordChange(username, successful);
auditService.auditTokenRefresh(username, ipAddress);
auditService.auditAccountLockout(username, reason);
auditService.auditSuspiciousActivity(activityType, details, ipAddress);
auditService.auditRateLimitExceeded(ipAddress, path);
```

---

## Best Practices

### 1. Don't Expose Sensitive Information

```java
// ❌ BAD - exposes database details
throw new BookingException("FK constraint violation on table bookings");

// ✅ GOOD - user-friendly message
throw new BookingException("Unable to create booking. Please try again");
```

### 2. Use Specific Exceptions

```java
// ❌ BAD - generic exception
if (room == null) {
    throw new RuntimeException("Room not found");
}

// ✅ GOOD - specific exception
if (room == null) {
    throw new ResourceNotFoundException("Room", roomId.toString());
}
```

### 3. Include Context in Exceptions

```java
// ❌ BAD - no context
throw new InvalidDateRangeException("Invalid dates");

// ✅ GOOD - includes context
throw new InvalidDateRangeException(startDate, endDate);
```

### 4. Log Errors Before Throwing

```java
// For unexpected errors
try {
    processBooking(request);
} catch (SQLException e) {
    logger.error("Database error while creating booking: {}", e.getMessage(), e);
    throw new BookingException("SYSTEM_ERROR", "Unable to process booking", e);
}
```

### 5. Don't Catch and Swallow Exceptions

```java
// ❌ BAD - swallows exception
try {
    processPayment(command);
} catch (Exception e) {
    // Ignoring error
}

// ✅ GOOD - handles or rethrows
try {
    processPayment(command);
} catch (PaymentGatewayException e) {
    auditService.auditPaymentOperation("PROCESS", id, user, "FAILED");
    throw new PaymentFailedException("Payment processing failed", e);
}
```

---

## Testing Exceptions

```java
@Test
void shouldThrowRoomNotAvailableException() {
    // Arrange
    UUID roomId = UUID.randomUUID();
    LocalDate startDate = LocalDate.now().plusDays(1);
    LocalDate endDate = LocalDate.now().plusDays(3);

    // Act & Assert
    assertThrows(RoomNotAvailableException.class, () -> {
        bookingService.createBooking(createRequest(roomId, startDate, endDate));
    });
}

@Test
void shouldReturnCorrectErrorResponse() {
    // Arrange
    BookingCreateRequest request = invalidRequest();

    // Act
    ResponseEntity<ErrorResponse> response = testRestTemplate.postForEntity(
        "/api/v1/reservations",
        request,
        ErrorResponse.class
    );

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().getCode()).isEqualTo("INVALID_DATE_RANGE");
    assertThat(response.getBody().getCorrelationId()).isNotNull();
}
```

---

## Error Response Format

All exceptions return this JSON structure:

```json
{
  "timestamp": "2025-10-23T20:30:45.123Z",
  "status": 404,
  "error": "Not Found",
  "code": "RESOURCE_NOT_FOUND",
  "message": "Booking with id abc-123 not found",
  "path": "/api/v1/reservations/abc-123",
  "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

With validation errors:

```json
{
  "timestamp": "2025-10-23T20:30:45.123Z",
  "status": 400,
  "error": "Bad Request",
  "code": "VALIDATION_FAILED",
  "message": "Validation failed for one or more fields",
  "path": "/api/v1/reservations",
  "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "validationErrors": [
    {
      "field": "guestEmail",
      "message": "Email is required"
    },
    {
      "field": "adults",
      "rejectedValue": 0,
      "message": "At least one adult is required"
    }
  ]
}
```

---

## Correlation IDs

Every request automatically gets a correlation ID:

1. **Client can provide**: Include `X-Correlation-ID` header in request
2. **Server generates**: If not provided, server creates a UUID
3. **Response includes**: Correlation ID is in response header and error body
4. **Logs include**: All logs include correlation ID in MDC

Use correlation IDs for debugging:

```bash
# Search logs by correlation ID
grep "a1b2c3d4-e5f6-7890-abcd-ef1234567890" logs/application.log
```

---

## Rate Limiting

Default: 100 requests per minute per IP address

Rate limit headers in response:
```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1729710000
```

When exceeded (HTTP 429):
```json
{
  "timestamp": "2025-10-23T20:30:45.123Z",
  "status": 429,
  "error": "Too Many Requests",
  "code": "RATE_LIMIT_EXCEEDED",
  "message": "Too many requests. Please try again later",
  "path": "/api/v1/reservations",
  "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

Response header:
```
Retry-After: 42
```

---

## Summary

1. **Use specific exceptions** for different error scenarios
2. **Include context** when throwing exceptions
3. **Audit sensitive operations** using SecurityAuditService
4. **Don't expose sensitive data** in error messages
5. **Log errors** before throwing for debugging
6. **Use correlation IDs** for tracking requests
7. **Test error scenarios** thoroughly
8. **Follow consistent patterns** across the codebase

All exceptions are handled by the GlobalExceptionHandler, ensuring consistent, secure error responses across the entire application.
