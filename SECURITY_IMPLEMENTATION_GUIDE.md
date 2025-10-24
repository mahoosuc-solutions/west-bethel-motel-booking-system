# Security Implementation Guide - Agent 2

## Overview
This document describes the CSRF protection, exception handling, and security enhancements implemented by Security Agent 2 for the West Bethel Motel Booking System.

## Implementation Summary

### 1. CSRF Protection
**Status**: Disabled (for stateless JWT authentication)

The SecurityConfig has been configured to disable CSRF for the stateless JWT-based API. However, the infrastructure is in place if CSRF protection is needed in the future:

- **CsrfController** (`/api/v1/csrf`): Endpoint to retrieve CSRF tokens
- CSRF token endpoint is publicly accessible
- Cookie-based token repository ready for SPA compatibility

**Note**: CSRF is typically disabled for stateless JWT authentication as recommended by OWASP. If you need to add CSRF protection for specific endpoints, modify `SecurityConfig.java` to enable it with `CookieCsrfTokenRepository.withHttpOnlyFalse()`.

### 2. Global Exception Handler
**Location**: `/src/main/java/com/westbethel/motel_booking/exception/GlobalExceptionHandler.java`

**Features**:
- Catches all exceptions application-wide
- Returns consistent error responses with correlation IDs
- Never exposes stack traces to clients
- Logs detailed errors server-side
- Sanitizes error messages to prevent sensitive data leakage

**Handles**:
- Custom booking exceptions
- Validation errors
- Authentication/Authorization failures
- JWT token errors
- Database integrity violations
- Generic runtime exceptions

### 3. Custom Exception Classes

All custom exceptions are located in `/src/main/java/com/westbethel/motel_booking/exception/`:

1. **BookingException** - Base exception for all booking-related errors
2. **RoomNotAvailableException** - Room not available for requested dates
3. **InvalidDateRangeException** - Invalid date range provided
4. **InsufficientFundsException** - Insufficient funds for payment
5. **PaymentFailedException** - Payment operation failed
6. **InvalidCredentialsException** - Invalid login credentials
7. **TokenExpiredException** - Authentication token expired
8. **UnauthorizedException** - User not authorized for action
9. **ResourceNotFoundException** - Requested resource not found

### 4. Error Response DTOs

Located in `/src/main/java/com/westbethel/motel_booking/common/dto/`:

- **ErrorResponse**: Standard error response structure
- **ValidationError**: Field-level validation errors
- **ErrorDetails**: Additional error context

### 5. Security Filters

#### LoggingFilter
**Location**: `/src/main/java/com/westbethel/motel_booking/security/filter/LoggingFilter.java`

**Features**:
- Logs all incoming requests and responses
- Adds correlation IDs for request tracking
- Sanitizes sensitive data (passwords, tokens, credit cards)
- Uses MDC for correlation ID propagation
- Logs response times and status codes

#### RateLimitingFilter
**Location**: `/src/main/java/com/westbethel/motel_booking/security/filter/RateLimitingFilter.java`

**Features**:
- In-memory rate limiting (100 requests/minute per IP by default)
- Configurable via application.yml
- Returns HTTP 429 with Retry-After header
- Automatic cleanup of expired entries
- Can be replaced with Redis for distributed systems

**Configuration**:
```yaml
security:
  rate-limit:
    enabled: true
    requests-per-minute: 100
```

### 6. Security Headers

**Location**: `/src/main/java/com/westbethel/motel_booking/config/SecurityHeadersConfig.java`

**Headers Added**:
- `X-Content-Type-Options: nosniff` - Prevents MIME sniffing
- `X-Frame-Options: DENY` - Prevents clickjacking
- `X-XSS-Protection: 1; mode=block` - Enables XSS protection
- `Strict-Transport-Security` - Enforces HTTPS (1 year)
- `Content-Security-Policy` - Restricts resource loading
- `Referrer-Policy: strict-origin-when-cross-origin` - Controls referrer info
- `Permissions-Policy` - Disables unnecessary browser features
- Cache control headers for sensitive pages

### 7. Security Audit Logging

#### SecurityAuditService
**Location**: `/src/main/java/com/westbethel/motel_booking/common/audit/SecurityAuditService.java`

**Features**:
- Audits all security-related events
- Logs to both database (audit_entries) and application logs
- Sanitizes all sensitive data
- Never logs passwords, tokens, or credit card numbers
- Includes correlation IDs and IP addresses

**Events Audited**:
- Successful/failed login attempts
- Logout events
- Access denied events
- Payment operations
- Booking operations
- Sensitive data access
- Password changes
- Token refresh
- Account lockouts
- Suspicious activity
- Rate limit violations

#### AuthenticationEventListener
**Location**: `/src/main/java/com/westbethel/motel_booking/security/listener/AuthenticationEventListener.java`

**Features**:
- Automatically listens for Spring Security authentication events
- Triggers audit logging for authentication events
- Captures IP addresses from requests

### 8. Application Configuration

Updated `/src/main/resources/application.yml`:

```yaml
# Server error configuration - never expose sensitive info
server:
  error:
    include-message: never
    include-binding-errors: never
    include-stacktrace: never
    include-exception: false

# Logging configuration with correlation IDs
logging:
  level:
    com.westbethel.motel_booking: INFO
    org.springframework.security: INFO
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} [%X{correlationId}] - %msg%n"
  file:
    name: logs/application.log
    max-size: 10MB
    max-history: 30

# Security configuration
security:
  rate-limit:
    enabled: true
    requests-per-minute: 100
```

## Usage Examples

### Using Custom Exceptions in Services

```java
@Service
public class BookingServiceImpl implements BookingService {

    public Booking createBooking(BookingRequest request) {
        // Validate date range
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new InvalidDateRangeException(
                request.getStartDate(),
                request.getEndDate()
            );
        }

        // Check room availability
        if (!isRoomAvailable(request.getRoomId(), request.getStartDate(), request.getEndDate())) {
            throw new RoomNotAvailableException(
                request.getRoomId(),
                request.getStartDate(),
                request.getEndDate()
            );
        }

        // Create booking
        return bookingRepository.save(booking);
    }
}
```

### Using Security Audit Service

```java
@Service
public class PaymentServiceImpl implements PaymentService {

    private final SecurityAuditService auditService;

    public PaymentResult processPayment(PaymentCommand command) {
        try {
            PaymentResult result = paymentGateway.process(command);

            // Audit successful payment
            auditService.auditPaymentOperation(
                "PROCESS",
                result.getPaymentId(),
                command.getInitiatedBy(),
                "SUCCESS"
            );

            return result;
        } catch (Exception e) {
            // Audit failed payment
            auditService.auditPaymentOperation(
                "PROCESS",
                command.getInvoiceId(),
                command.getInitiatedBy(),
                "FAILED"
            );

            throw new PaymentFailedException("Payment processing failed", e);
        }
    }
}
```

### Error Response Format

All errors return a consistent JSON structure:

```json
{
  "timestamp": "2025-10-23T20:30:45.123Z",
  "status": 400,
  "error": "Bad Request",
  "code": "INVALID_DATE_RANGE",
  "message": "Invalid date range: start date 2025-10-25 must be before end date 2025-10-20",
  "path": "/api/v1/reservations",
  "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

Validation errors include field details:

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
      "field": "email",
      "message": "Email must be valid"
    }
  ]
}
```

## Integration with Security Agent 1

The implementation coordinates with Security Agent 1's JWT authentication:

1. **SecurityConfig**: CSRF disabled for stateless JWT auth
2. **CSRF endpoint**: Added to permitted endpoints list
3. **Filters**: LoggingFilter and RateLimitingFilter work alongside JwtAuthenticationFilter
4. **Audit**: AuthenticationEventListener captures JWT authentication events

## Security Best Practices Implemented

1. **No Stack Traces**: Never exposed to clients
2. **Sanitized Messages**: All error messages sanitized
3. **Correlation IDs**: Track requests across the application
4. **Rate Limiting**: Prevents abuse (100 req/min per IP)
5. **Security Headers**: Protection against common web vulnerabilities
6. **Audit Logging**: All security events logged
7. **Sensitive Data**: Never logged (passwords, tokens, cards)
8. **Input Validation**: Comprehensive validation with user-friendly messages
9. **Thread Safety**: All filters and services are thread-safe
10. **Configurable**: Security settings configurable via environment variables

## Testing Recommendations for Security Agent 4

1. **Exception Handling Tests**:
   - Test all custom exceptions are caught correctly
   - Verify error response format
   - Ensure no stack traces in responses
   - Test correlation ID propagation

2. **CSRF Tests** (if enabled):
   - Test CSRF token retrieval
   - Test POST requests with/without CSRF token
   - Verify CSRF exclusions work

3. **Rate Limiting Tests**:
   - Test rate limit enforcement
   - Verify headers (X-RateLimit-*)
   - Test cleanup of expired buckets
   - Test disable/enable configuration

4. **Security Headers Tests**:
   - Verify all security headers present
   - Test CSP restrictions
   - Verify HSTS configuration

5. **Audit Logging Tests**:
   - Test all audit events are logged
   - Verify sensitive data is sanitized
   - Test correlation ID in audit logs
   - Verify database persistence

6. **Logging Filter Tests**:
   - Test request/response logging
   - Verify correlation ID generation
   - Test sensitive data sanitization
   - Verify MDC cleanup

## Known Issues and Considerations

1. **Rate Limiting**: Currently uses in-memory storage. For distributed systems, replace with Redis-based implementation.

2. **CSRF**: Currently disabled for JWT authentication. Enable if adding traditional session-based authentication.

3. **Security Headers**: HSTS header enforces HTTPS. Comment out in development if using HTTP.

4. **Audit Storage**: Consider archiving old audit entries for long-term storage.

5. **Logging**: Application logs can grow large. Monitor disk space and adjust retention as needed.

## Next Steps

1. **Security Agent 3**: Implement input validation, password policies, and additional security features
2. **Security Agent 4**: Create comprehensive tests for all security components
3. **Consider**: Implementing distributed rate limiting with Redis
4. **Consider**: Adding request/response body logging for debugging (with sensitive data masking)
5. **Consider**: Implementing circuit breakers for external service calls

## Files Created by Security Agent 2

### Exception Classes (10 files)
- `/src/main/java/com/westbethel/motel_booking/exception/BookingException.java`
- `/src/main/java/com/westbethel/motel_booking/exception/GlobalExceptionHandler.java`
- `/src/main/java/com/westbethel/motel_booking/exception/InsufficientFundsException.java`
- `/src/main/java/com/westbethel/motel_booking/exception/InvalidCredentialsException.java`
- `/src/main/java/com/westbethel/motel_booking/exception/InvalidDateRangeException.java`
- `/src/main/java/com/westbethel/motel_booking/exception/PaymentFailedException.java`
- `/src/main/java/com/westbethel/motel_booking/exception/ResourceNotFoundException.java`
- `/src/main/java/com/westbethel/motel_booking/exception/RoomNotAvailableException.java`
- `/src/main/java/com/westbethel/motel_booking/exception/TokenExpiredException.java`
- `/src/main/java/com/westbethel/motel_booking/exception/UnauthorizedException.java`

### DTOs (3 files)
- `/src/main/java/com/westbethel/motel_booking/common/dto/ErrorDetails.java`
- `/src/main/java/com/westbethel/motel_booking/common/dto/ErrorResponse.java`
- `/src/main/java/com/westbethel/motel_booking/common/dto/ValidationError.java`

### Security Components (4 files)
- `/src/main/java/com/westbethel/motel_booking/security/api/CsrfController.java`
- `/src/main/java/com/westbethel/motel_booking/security/filter/LoggingFilter.java`
- `/src/main/java/com/westbethel/motel_booking/security/filter/RateLimitingFilter.java`
- `/src/main/java/com/westbethel/motel_booking/security/listener/AuthenticationEventListener.java`

### Configuration (1 file)
- `/src/main/java/com/westbethel/motel_booking/config/SecurityHeadersConfig.java`

### Audit (1 file)
- `/src/main/java/com/westbethel/motel_booking/common/audit/SecurityAuditService.java`

### Modified Files (2 files)
- `/src/main/java/com/westbethel/motel_booking/config/SecurityConfig.java` (added CSRF endpoint to permitted list)
- `/src/main/resources/application.yml` (added security and logging configuration)

**Total: 19 new files + 2 modified files**
