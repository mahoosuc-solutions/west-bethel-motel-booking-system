# Security Features Quick Start Guide
## West Bethel Motel Booking System - Phase 2

---

## Quick Setup

### 1. Install Redis
```bash
# Docker
docker run -d -p 6379:6379 redis:latest

# Or native install
sudo apt-get install redis-server
sudo systemctl start redis
```

### 2. Configure Application
```yaml
# application.yml
spring:
  data:
    redis:
      host: localhost
      port: 6379

jwt:
  secret: ${JWT_SECRET}  # Base64-encoded 256-bit key
  expiration: 86400000
  refresh-expiration: 604800000

app:
  name: West Bethel Motel
```

### 3. Run Migrations
```bash
mvn flyway:migrate
```

---

## Feature Usage

### Password Reset Flow

**Request Reset:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/forgot-password \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com"
  }'
```

**Validate Token:**
```bash
curl http://localhost:8080/api/v1/auth/validate-reset-token?token=TOKEN_HERE
```

**Reset Password:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/reset-password \
  -H "Content-Type: application/json" \
  -d '{
    "token": "TOKEN_HERE",
    "newPassword": "NewSecure123!"
  }'
```

---

### Email Verification

**Verify Email:**
```bash
curl -X POST "http://localhost:8080/api/v1/auth/verify-email?token=TOKEN_HERE"
```

**Resend Verification:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/resend-verification \
  -H "Authorization: Bearer JWT_TOKEN"
```

---

### Multi-Factor Authentication

**1. Setup MFA:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/mfa/setup \
  -H "Authorization: Bearer JWT_TOKEN"

# Response includes:
# - secret (manual entry)
# - qrCodeUrl (otpauth://)
# - qrCodeImage (Base64 PNG)
# - backupCodes (save these!)
```

**2. Enable MFA:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/mfa/enable \
  -H "Authorization: Bearer JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "verificationCode": "123456",
    "backupCodes": ["ABCD-1234", "EFGH-5678", ...]
  }'
```

**3. Verify During Login:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/mfa/verify \
  -H "Authorization: Bearer JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "code": "123456"
  }'
```

**4. Disable MFA:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/mfa/disable \
  -H "Authorization: Bearer JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "password": "current_password"
  }'
```

**5. Regenerate Backup Codes:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/mfa/backup-codes \
  -H "Authorization: Bearer JWT_TOKEN"
```

---

### Session Management

**List Active Sessions:**
```bash
curl http://localhost:8080/api/v1/users/me/sessions \
  -H "Authorization: Bearer JWT_TOKEN"
```

**Logout Specific Session:**
```bash
curl -X DELETE http://localhost:8080/api/v1/users/me/sessions/SESSION_ID \
  -H "Authorization: Bearer JWT_TOKEN"
```

**Logout All Devices:**
```bash
curl -X DELETE http://localhost:8080/api/v1/users/me/sessions \
  -H "Authorization: Bearer JWT_TOKEN"
```

---

## Development Guide

### Using Token Blacklist in Code

```java
@Autowired
private TokenBlacklistService tokenBlacklistService;

// Blacklist a token on logout
tokenBlacklistService.blacklistToken(jwtToken, "LOGOUT");

// Check if token is blacklisted
if (tokenBlacklistService.isBlacklisted(jwtToken)) {
    throw new InvalidTokenException("Token has been revoked");
}

// Blacklist all user tokens (password change, etc.)
tokenBlacklistService.blacklistAllUserTokens(username, "PASSWORD_CHANGE");
```

### Password Reset Service

```java
@Autowired
private PasswordResetService passwordResetService;

// Request password reset
String token = passwordResetService.requestReset(email, ipAddress);
// Send token via email...

// Validate token
boolean valid = passwordResetService.validateToken(token);

// Reset password
passwordResetService.resetPassword(token, newPassword);
```

### MFA Service

```java
@Autowired
private MfaService mfaService;

// Setup MFA
MfaSetupResponse setup = mfaService.setupMfa(username);
// Display QR code to user

// Enable MFA
mfaService.enableMfa(username, verificationCode, backupCodes);

// Verify code during login
boolean verified = mfaService.verifyMfaCode(username, code);

// Disable MFA
mfaService.disableMfa(username, password);
```

### Session Management

```java
@Autowired
private SessionManagementService sessionService;

// Create session on login
UserSession session = sessionService.createSession(
    username, ipAddress, userAgent, accessToken
);

// Get active sessions
List<UserSession> sessions = sessionService.getActiveSessions(username);

// Invalidate session
sessionService.invalidateSession(sessionId, username);

// Invalidate all sessions
sessionService.invalidateAllSessions(username);
```

---

## Testing

### Run Unit Tests
```bash
mvn test -Dtest=TokenBlacklistServiceTest
mvn test -Dtest=PasswordResetServiceTest
mvn test -Dtest=TotpServiceTest
```

### Test MFA Flow
```bash
# 1. Register/Login
# 2. Setup MFA and save QR code
# 3. Scan QR with Google Authenticator
# 4. Enable MFA with code from app
# 5. Logout and login again
# 6. Verify with new TOTP code
```

---

## Security Considerations

### Rate Limiting
- Password reset: 5 requests/hour per email
- Email verification: 3 resends/hour per user
- Implemented via Redis with automatic expiry

### Token Expiry
- Password reset tokens: 1 hour
- Email verification tokens: 24 hours
- JWT tokens: 24 hours (configurable)
- Refresh tokens: 7 days (configurable)

### MFA Best Practices
- Always save backup codes securely
- Backup codes are single-use only
- Use strong passwords even with MFA enabled
- Disable MFA requires password verification

### Session Security
- Sessions expire after 24 hours of inactivity
- IP address changes detected as suspicious
- Logout all devices on password change recommended
- Session tokens stored in Redis with TTL

---

## Troubleshooting

### Redis Connection Issues
```bash
# Check Redis is running
redis-cli ping
# Should return: PONG

# Check connection
redis-cli
> keys *
> exit
```

### Token Not Being Blacklisted
```bash
# Check Redis contains blacklisted tokens
redis-cli
> keys blacklisted_tokens*
> ttl blacklisted_tokens::TOKEN_HERE
```

### MFA QR Code Not Generating
- Verify zxing dependencies in pom.xml
- Check logs for QR generation errors
- Ensure secret is Base32 encoded

### Email Not Sending
- Verify email service integration (Agent 2)
- Check SMTP configuration
- Review email service logs

---

## Configuration Reference

### Redis Configuration
```java
@Configuration
@EnableRedisRepositories(basePackages = {
    "com.westbethel.motel_booking.security.blacklist",
    "com.westbethel.motel_booking.security.session"
})
public class RedisConfig {
    // Configuration in config/RedisConfig.java
}
```

### Security Constants
```java
// Password Reset
private static final int TOKEN_EXPIRY_HOURS = 1;
private static final int MAX_REQUESTS_PER_HOUR = 5;

// Email Verification
private static final int TOKEN_EXPIRY_HOURS = 24;
private static final int MAX_RESEND_PER_HOUR = 3;

// MFA
private static final int BACKUP_CODE_LENGTH = 8;
private static final int BACKUP_CODE_COUNT = 10;

// Sessions
private static final long SESSION_TTL_SECONDS = 24 * 60 * 60;
```

---

## Monitoring

### Key Metrics to Track
1. **Token Blacklist Size**: Monitor Redis memory usage
2. **Password Reset Requests**: Track for abuse
3. **Failed MFA Attempts**: Security indicator
4. **Active Session Count**: Resource planning
5. **Rate Limit Violations**: Potential attacks

### Redis Monitoring
```bash
# Monitor Redis stats
redis-cli info stats

# Check memory usage
redis-cli info memory

# Monitor commands
redis-cli monitor
```

---

## Support and Resources

### Documentation
- Main Report: `PHASE2_SECURITY_IMPLEMENTATION_REPORT.md`
- API Documentation: Spring REST Docs (auto-generated)
- Code Documentation: Javadoc in source files

### External Resources
- [TOTP RFC 6238](https://tools.ietf.org/html/rfc6238)
- [Google Authenticator](https://github.com/google/google-authenticator)
- [OWASP Authentication Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html)
- [Redis Documentation](https://redis.io/documentation)

---

**Last Updated:** October 23, 2025
**Version:** 1.0
