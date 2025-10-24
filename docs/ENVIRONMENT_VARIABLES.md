# Environment Variables Reference

This document provides a comprehensive reference for all environment variables used in the West Bethel Motel Booking System.

## Table of Contents
- [Required Variables (Production)](#required-variables-production)
- [Optional Variables (with Defaults)](#optional-variables-with-defaults)
- [Database Configuration](#database-configuration)
- [Redis Configuration](#redis-configuration)
- [JWT Configuration](#jwt-configuration)
- [Email Configuration](#email-configuration)
- [Server Configuration](#server-configuration)
- [Logging Configuration](#logging-configuration)
- [Security Best Practices](#security-best-practices)
- [Environment-Specific Configuration](#environment-specific-configuration)

---

## Required Variables (Production)

These variables MUST be set in production environments. The application will fail to start if these are not provided.

### Database Credentials
| Variable | Description | Example | Validation |
|----------|-------------|---------|------------|
| `DATABASE_USERNAME` | PostgreSQL database username | `motel_booking_prod` | Required, no default |
| `DATABASE_PASSWORD` | PostgreSQL database password | `SecureP@ssw0rd123!` | Required, min 12 chars recommended |

### Redis Credentials
| Variable | Description | Example | Validation |
|----------|-------------|---------|------------|
| `REDIS_PASSWORD` | Redis authentication password | `RedisSecure123!` | Required, min 16 chars recommended |

### JWT Configuration
| Variable | Description | Example | Validation |
|----------|-------------|---------|------------|
| `JWT_SECRET` | JWT signing secret (Base64 encoded) | `<256-bit base64 string>` | Required, min 256 bits (32 bytes) |

### Email Credentials
| Variable | Description | Example | Validation |
|----------|-------------|---------|------------|
| `MAIL_USERNAME` | SMTP server username | `noreply@westbethelmotel.com` | Required for email features |
| `MAIL_PASSWORD` | SMTP server password | `EmailP@ss123!` | Required for email features |

---

## Optional Variables (with Defaults)

These variables have sensible defaults but can be overridden as needed.

### Profile Selection
| Variable | Description | Default | Values |
|----------|-------------|---------|--------|
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `prod` | `prod`, `dev`, `test` |

---

## Database Configuration

### Connection Settings
| Variable | Description | Default | Notes |
|----------|-------------|---------|-------|
| `DATABASE_URL` | JDBC connection URL | `jdbc:postgresql://localhost:5432/motel_booking` | Include host, port, database name |
| `DATABASE_USERNAME` | Database username | (none) | **REQUIRED** |
| `DATABASE_PASSWORD` | Database password | (none) | **REQUIRED** |

### Connection Pool Settings (HikariCP)
| Variable | Description | Default | Recommended Range |
|----------|-------------|---------|-------------------|
| `DB_POOL_MAX_SIZE` | Maximum pool size | `20` | 10-50 depending on load |
| `DB_POOL_MIN_IDLE` | Minimum idle connections | `5` | 5-10 |
| `DB_POOL_TIMEOUT` | Connection timeout (ms) | `30000` | 20000-60000 |
| `DB_POOL_IDLE_TIMEOUT` | Idle connection timeout (ms) | `600000` | 300000-900000 |
| `DB_POOL_MAX_LIFETIME` | Max connection lifetime (ms) | `1800000` | 1200000-2400000 |
| `DB_LEAK_DETECTION` | Leak detection threshold (ms) | `60000` | 30000-120000 |
| `DB_VALIDATION_TIMEOUT` | Validation timeout (ms) | `5000` | 3000-10000 |

### Example Production Database Configuration
```bash
DATABASE_URL=jdbc:postgresql://prod-db.example.com:5432/motel_booking
DATABASE_USERNAME=motel_booking_prod
DATABASE_PASSWORD=VerySecureProductionPassword123!
DB_POOL_MAX_SIZE=30
DB_POOL_MIN_IDLE=10
```

---

## Redis Configuration

| Variable | Description | Default | Notes |
|----------|-------------|---------|-------|
| `REDIS_HOST` | Redis server host | `localhost` | Use internal DNS in production |
| `REDIS_PORT` | Redis server port | `6379` | Standard Redis port |
| `REDIS_PASSWORD` | Redis authentication | (none) | **REQUIRED** in production |
| `REDIS_SSL_ENABLED` | Enable SSL/TLS | `true` | Always true in production |
| `REDIS_TIMEOUT` | Connection timeout (ms) | `2000` | 1000-5000 |
| `REDIS_CONNECT_TIMEOUT` | Connect timeout (ms) | `2000` | 1000-5000 |
| `REDIS_CLIENT_TYPE` | Client implementation | `lettuce` | `lettuce` or `jedis` |

### Redis Connection Pool (Lettuce)
| Variable | Description | Default | Recommended Range |
|----------|-------------|---------|-------------------|
| `REDIS_POOL_MAX_ACTIVE` | Maximum active connections | `8` | 8-16 |
| `REDIS_POOL_MAX_IDLE` | Maximum idle connections | `8` | 8-16 |
| `REDIS_POOL_MIN_IDLE` | Minimum idle connections | `2` | 2-4 |
| `REDIS_POOL_MAX_WAIT` | Max wait time (ms) | `2000` | 1000-5000 |

### Example Production Redis Configuration
```bash
REDIS_HOST=redis.production.internal
REDIS_PORT=6379
REDIS_PASSWORD=SecureRedisPassword123!
REDIS_SSL_ENABLED=true
REDIS_POOL_MAX_ACTIVE=12
```

---

## JWT Configuration

| Variable | Description | Default | Notes |
|----------|-------------|---------|-------|
| `JWT_SECRET` | JWT signing secret | (none) | **REQUIRED**, min 256 bits |
| `JWT_EXPIRATION` | Access token expiration (ms) | `86400000` (24h) | 3600000-86400000 |
| `JWT_REFRESH_EXPIRATION` | Refresh token expiration (ms) | `604800000` (7d) | 604800000-2592000000 |

### Generating a Secure JWT Secret
Use the provided script to generate a cryptographically secure secret:
```bash
./scripts/generate-secrets.sh
```

Or manually:
```bash
# Generate 256-bit (32 byte) secret and encode as Base64
openssl rand -base64 32
```

### Example JWT Configuration
```bash
JWT_SECRET=YOUR_GENERATED_256_BIT_BASE64_SECRET_HERE
JWT_EXPIRATION=86400000
JWT_REFRESH_EXPIRATION=604800000
```

---

## Email Configuration

| Variable | Description | Default | Notes |
|----------|-------------|---------|-------|
| `MAIL_HOST` | SMTP server host | `smtp.gmail.com` | Provider-specific |
| `MAIL_PORT` | SMTP server port | `587` | 587 (TLS) or 465 (SSL) |
| `MAIL_USERNAME` | SMTP username | (none) | **REQUIRED** |
| `MAIL_PASSWORD` | SMTP password | (none) | **REQUIRED** |

### Example Email Configuration (Gmail)
```bash
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=noreply@westbethelmotel.com
MAIL_PASSWORD=YourAppPassword123
```

### Example Email Configuration (SendGrid)
```bash
MAIL_HOST=smtp.sendgrid.net
MAIL_PORT=587
MAIL_USERNAME=apikey
MAIL_PASSWORD=SG.your-sendgrid-api-key
```

---

## Server Configuration

| Variable | Description | Default | Notes |
|----------|-------------|---------|-------|
| `SERVER_PORT` | Application HTTP port | `8080` | 1024-65535 |
| `TOMCAT_CONNECTION_TIMEOUT` | Connection timeout (ms) | `20000` | 10000-30000 |
| `TOMCAT_MAX_THREADS` | Max worker threads | `200` | 100-500 |
| `TOMCAT_MIN_SPARE_THREADS` | Min spare threads | `10` | 10-50 |
| `TOMCAT_MAX_CONNECTIONS` | Max connections | `10000` | 5000-20000 |
| `TOMCAT_ACCEPT_COUNT` | Accept queue size | `100` | 50-200 |

---

## Logging Configuration

| Variable | Description | Default | Notes |
|----------|-------------|---------|-------|
| `LOG_FILE_PATH` | Log file location | `/var/log/motel-booking/application.log` | Ensure write permissions |
| `LOG_MAX_SIZE` | Max log file size | `50MB` | 10MB-100MB |
| `LOG_MAX_HISTORY` | Days to retain logs | `90` | 30-365 |
| `LOG_TOTAL_SIZE` | Total log size cap | `5GB` | 1GB-20GB |

---

## Security Best Practices

### 1. Credential Management
- **NEVER** commit credentials to version control
- Use different credentials for each environment (dev, staging, prod)
- Rotate credentials regularly (quarterly minimum)
- Use strong, randomly generated passwords (min 16 characters)
- Store production credentials in a secure vault (e.g., AWS Secrets Manager, HashiCorp Vault)

### 2. JWT Secret Requirements
- **Minimum 256 bits (32 bytes)** for adequate security
- Use cryptographically secure random generation
- Different secret for each environment
- Rotate JWT secrets periodically (requires user re-authentication)
- Base64 encode for safe storage and transmission

### 3. Database Security
- Use strong passwords (min 12 characters, mixed case, numbers, symbols)
- Limit database user permissions (principle of least privilege)
- Enable SSL/TLS for database connections in production
- Use connection pooling to prevent connection exhaustion
- Monitor for connection leaks

### 4. Redis Security
- Always set a password, even in development
- Enable SSL/TLS in production (`REDIS_SSL_ENABLED=true`)
- Use Redis ACL for fine-grained access control
- Isolate Redis instance (not publicly accessible)

### 5. Environment Variable Protection
- Use `.env` files locally (never commit)
- Use platform-specific secret management in production:
  - **Docker**: Use Docker secrets
  - **Kubernetes**: Use Kubernetes secrets
  - **AWS**: Use AWS Secrets Manager or Parameter Store
  - **Azure**: Use Azure Key Vault
  - **GCP**: Use Secret Manager

### 6. Monitoring and Auditing
- Enable connection leak detection in production
- Monitor actuator metrics for security events
- Set up alerts for failed authentication attempts
- Log all security-relevant events
- Regularly review logs for suspicious activity

---

## Environment-Specific Configuration

### Development Environment
```bash
# .env.development
SPRING_PROFILES_ACTIVE=dev
DATABASE_URL=jdbc:postgresql://localhost:5432/motel_booking
DATABASE_USERNAME=motel_booking
DATABASE_PASSWORD=devpassword123
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=devredispass
REDIS_SSL_ENABLED=false
JWT_SECRET=<dev-specific-secret>
MAIL_HOST=localhost
MAIL_PORT=1025
```

### Staging Environment
```bash
# Stored in secure vault, not in .env file
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=jdbc:postgresql://staging-db.internal:5432/motel_booking
DATABASE_USERNAME=motel_booking_staging
DATABASE_PASSWORD=<secure-staging-password>
REDIS_HOST=redis-staging.internal
REDIS_PORT=6379
REDIS_PASSWORD=<secure-redis-password>
REDIS_SSL_ENABLED=true
JWT_SECRET=<staging-specific-256-bit-secret>
MAIL_HOST=smtp.sendgrid.net
MAIL_PORT=587
MAIL_USERNAME=apikey
MAIL_PASSWORD=<sendgrid-api-key>
```

### Production Environment
```bash
# Stored in secure vault (AWS Secrets Manager, etc.)
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=jdbc:postgresql://prod-db.internal:5432/motel_booking
DATABASE_USERNAME=motel_booking_prod
DATABASE_PASSWORD=<secure-production-password>
DB_POOL_MAX_SIZE=50
DB_POOL_MIN_IDLE=10
REDIS_HOST=redis-prod.internal
REDIS_PORT=6379
REDIS_PASSWORD=<secure-redis-password>
REDIS_SSL_ENABLED=true
JWT_SECRET=<production-256-bit-secret>
JWT_EXPIRATION=3600000
MAIL_HOST=smtp.sendgrid.net
MAIL_PORT=587
MAIL_USERNAME=apikey
MAIL_PASSWORD=<sendgrid-api-key>
SERVER_PORT=8080
LOG_FILE_PATH=/var/log/motel-booking/application.log
```

---

## Validation and Troubleshooting

### Required Variables Check
The application includes a `ConfigurationValidator` that validates all required environment variables on startup. If any required variable is missing, the application will fail fast with a clear error message.

### Common Issues

#### "Failed to configure a DataSource"
- **Cause**: Missing `DATABASE_USERNAME` or `DATABASE_PASSWORD`
- **Solution**: Set both environment variables

#### "JWT secret must be at least 256 bits"
- **Cause**: JWT_SECRET is too short or not set
- **Solution**: Generate a proper secret using `./scripts/generate-secrets.sh`

#### "Unable to connect to Redis"
- **Cause**: Missing `REDIS_PASSWORD` or incorrect Redis configuration
- **Solution**: Verify Redis is running and credentials are correct

#### Connection pool exhausted
- **Cause**: `DB_POOL_MAX_SIZE` too small for load
- **Solution**: Increase pool size or investigate connection leaks

### Validation Script
Run the configuration validator:
```bash
# Check if all required variables are set
./scripts/validate-config.sh
```

---

## 12-Factor App Compliance

This configuration follows [12-Factor App](https://12factor.net/) principles:

1. **Codebase**: One codebase, many deploys
2. **Dependencies**: Explicitly declared in `pom.xml`
3. **Config**: Stored in environment (this document)
4. **Backing Services**: Treated as attached resources
5. **Build, Release, Run**: Strict separation
6. **Processes**: Stateless processes (session in Redis)
7. **Port Binding**: Self-contained (embedded Tomcat)
8. **Concurrency**: Scale via process model
9. **Disposability**: Fast startup and graceful shutdown
10. **Dev/Prod Parity**: Same backing services
11. **Logs**: Stream to stdout (configurable)
12. **Admin Processes**: Run as one-off processes

---

## Additional Resources

- [Spring Boot Externalized Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)
- [HikariCP Configuration](https://github.com/brettwooldridge/HikariCP#configuration-knobs-baby)
- [Redis Configuration Best Practices](https://redis.io/docs/management/security/)
- [JWT Best Practices](https://tools.ietf.org/html/rfc8725)

---

**Last Updated**: 2025-10-23
**Version**: 1.0.0
