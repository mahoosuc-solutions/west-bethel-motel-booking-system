# Docker Deployment Guide

This guide provides instructions for deploying the West Bethel Motel Booking System using Docker and Docker Compose.

## Table of Contents
- [Prerequisites](#prerequisites)
- [Quick Start (Development)](#quick-start-development)
- [Configuration](#configuration)
- [Building the Image](#building-the-image)
- [Running with Docker Compose](#running-with-docker-compose)
- [Production Deployment](#production-deployment)
- [Monitoring and Maintenance](#monitoring-and-maintenance)
- [Troubleshooting](#troubleshooting)

---

## Prerequisites

### Required Software
- Docker 20.10+ ([Install Docker](https://docs.docker.com/get-docker/))
- Docker Compose 2.0+ ([Install Docker Compose](https://docs.docker.com/compose/install/))

### Verify Installation
```bash
docker --version
docker-compose --version
```

---

## Quick Start (Development)

### 1. Generate Secrets
```bash
./scripts/generate-secrets.sh dev
```

This creates a `.env` file with secure credentials.

### 2. Start All Services
```bash
docker-compose up -d
```

This starts:
- PostgreSQL database (port 5432)
- Redis cache (port 6379)
- Application (port 8080)
- MailHog for email testing (ports 1025, 8025)

### 3. View Logs
```bash
# All services
docker-compose logs -f

# Application only
docker-compose logs -f app

# Database only
docker-compose logs -f postgres
```

### 4. Access the Application
- **Application**: http://localhost:8080
- **Health Check**: http://localhost:8080/actuator/health
- **API Documentation**: http://localhost:8080/swagger-ui.html
- **MailHog UI**: http://localhost:8025

### 5. Stop Services
```bash
docker-compose down
```

To also remove volumes (database data):
```bash
docker-compose down -v
```

---

## Configuration

### Environment Variables

All configuration is done via environment variables. See `.env.example` for all available options.

#### Required Variables
```bash
DATABASE_USERNAME=motel_booking
DATABASE_PASSWORD=<secure-password>
REDIS_PASSWORD=<secure-password>
JWT_SECRET=<256-bit-base64-secret>
```

#### Optional Variables
```bash
SPRING_PROFILES_ACTIVE=dev
SERVER_PORT=8080
DB_POOL_MAX_SIZE=20
REDIS_HOST=localhost
```

See [docs/ENVIRONMENT_VARIABLES.md](docs/ENVIRONMENT_VARIABLES.md) for complete reference.

### Configuration Files

- `.env` - Local environment variables (DO NOT COMMIT)
- `.env.example` - Template for environment variables
- `docker-compose.yml` - Docker Compose configuration
- `Dockerfile` - Docker image definition

---

## Building the Image

### Development Build
```bash
docker build -t motel-booking-system:dev .
```

### Production Build
```bash
docker build \
  --build-arg SKIP_TESTS=false \
  --build-arg BUILD_VERSION=1.0.0 \
  -t motel-booking-system:1.0.0 \
  -t motel-booking-system:latest \
  .
```

### Verify Build
```bash
docker images | grep motel-booking-system
```

---

## Running with Docker Compose

### Start Services
```bash
# Start in foreground (see logs)
docker-compose up

# Start in background
docker-compose up -d

# Start specific services
docker-compose up -d postgres redis

# Start with rebuild
docker-compose up -d --build
```

### View Service Status
```bash
docker-compose ps
```

### Execute Commands in Containers
```bash
# Access application shell
docker-compose exec app sh

# Access PostgreSQL
docker-compose exec postgres psql -U motel_booking -d motel_booking

# Access Redis CLI
docker-compose exec redis redis-cli -a <REDIS_PASSWORD>
```

### Scale Services (if needed)
```bash
# Run multiple app instances
docker-compose up -d --scale app=3
```

### Optional Tools
Start pgAdmin for database administration:
```bash
docker-compose --profile tools up -d pgadmin
```

Access pgAdmin at http://localhost:5050

---

## Production Deployment

### Security Checklist

- [ ] Generate new secrets with `./scripts/generate-secrets.sh prod`
- [ ] Set `SPRING_PROFILES_ACTIVE=prod`
- [ ] Use strong passwords (16+ characters)
- [ ] Enable Redis SSL (`REDIS_SSL_ENABLED=true`)
- [ ] Use external secret management (not `.env` files)
- [ ] Configure production database with SSL
- [ ] Set up log aggregation
- [ ] Configure monitoring and alerting
- [ ] Enable HTTPS/TLS on reverse proxy
- [ ] Implement rate limiting
- [ ] Configure backup strategy

### Production Compose File

Create `docker-compose.prod.yml`:
```yaml
version: '3.8'

services:
  app:
    image: motel-booking-system:1.0.0
    restart: always
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DATABASE_URL: ${DATABASE_URL}
      DATABASE_USERNAME: ${DATABASE_USERNAME}
      DATABASE_PASSWORD: ${DATABASE_PASSWORD}
      REDIS_HOST: ${REDIS_HOST}
      REDIS_PASSWORD: ${REDIS_PASSWORD}
      REDIS_SSL_ENABLED: true
      JWT_SECRET: ${JWT_SECRET}
      MAIL_HOST: ${MAIL_HOST}
      MAIL_USERNAME: ${MAIL_USERNAME}
      MAIL_PASSWORD: ${MAIL_PASSWORD}
    ports:
      - "8080:8080"
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
    deploy:
      replicas: 2
      restart_policy:
        condition: on-failure
        max_attempts: 3
      resources:
        limits:
          cpus: '1'
          memory: 1G
        reservations:
          cpus: '0.5'
          memory: 512M
```

### Deploy to Production
```bash
docker-compose -f docker-compose.prod.yml up -d
```

### Kubernetes Deployment

For production, consider using Kubernetes. See example manifests in `k8s/` directory (to be created).

---

## Monitoring and Maintenance

### Health Checks
```bash
# Application health
curl http://localhost:8080/actuator/health

# Database connection
docker-compose exec postgres pg_isready

# Redis connection
docker-compose exec redis redis-cli ping
```

### View Metrics
```bash
# Prometheus metrics
curl http://localhost:8080/actuator/prometheus

# JVM metrics
curl http://localhost:8080/actuator/metrics
```

### Backup Database
```bash
# Backup
docker-compose exec postgres pg_dump -U motel_booking motel_booking > backup.sql

# Restore
docker-compose exec -T postgres psql -U motel_booking motel_booking < backup.sql
```

### View Logs
```bash
# Application logs
docker-compose logs -f app

# Last 100 lines
docker-compose logs --tail=100 app

# Since specific time
docker-compose logs --since 2024-01-01T00:00:00 app
```

### Update Application
```bash
# Pull new image
docker-compose pull app

# Restart with new image
docker-compose up -d app

# Or rebuild and restart
docker-compose up -d --build app
```

---

## Troubleshooting

### Application Won't Start

**Problem**: Application container exits immediately

**Solutions**:
1. Check logs: `docker-compose logs app`
2. Verify environment variables: `./scripts/validate-config.sh`
3. Check database connection: `docker-compose logs postgres`
4. Verify JWT secret is set and valid

### Database Connection Failed

**Problem**: Cannot connect to PostgreSQL

**Solutions**:
```bash
# Check if PostgreSQL is running
docker-compose ps postgres

# Check PostgreSQL logs
docker-compose logs postgres

# Verify credentials
docker-compose exec postgres psql -U motel_booking -d motel_booking

# Restart PostgreSQL
docker-compose restart postgres
```

### Redis Connection Failed

**Problem**: Cannot connect to Redis

**Solutions**:
```bash
# Check if Redis is running
docker-compose ps redis

# Check Redis logs
docker-compose logs redis

# Test Redis connection
docker-compose exec redis redis-cli -a <REDIS_PASSWORD> ping

# Should return: PONG
```

### Port Already in Use

**Problem**: Port 8080 (or other) already in use

**Solutions**:
```bash
# Find process using port
lsof -i :8080  # macOS/Linux
netstat -ano | findstr :8080  # Windows

# Change port in .env
SERVER_PORT=8081

# Restart services
docker-compose down && docker-compose up -d
```

### Out of Memory

**Problem**: Container crashes with OOM error

**Solutions**:
1. Increase Docker memory limit
2. Adjust JVM options in `docker-compose.yml`:
   ```yaml
   environment:
     JAVA_OPTS: "-Xms512m -Xmx1024m"
   ```

### Database Migration Failed

**Problem**: Flyway migration errors

**Solutions**:
```bash
# View migration status
docker-compose exec app sh
./mvnw flyway:info

# Repair failed migrations (use carefully!)
./mvnw flyway:repair

# Clean and re-migrate (development only!)
./mvnw flyway:clean flyway:migrate
```

### Configuration Validation Failed

**Problem**: Application fails with configuration errors

**Solutions**:
1. Run validation script: `./scripts/validate-config.sh`
2. Check all required variables are set
3. Verify JWT secret is at least 256 bits
4. Check password strength requirements

### Logs Not Appearing

**Problem**: Cannot see application logs

**Solutions**:
```bash
# Check log volume
docker volume inspect motel-booking-app-logs

# Access logs directly
docker-compose exec app sh
ls -la /var/log/motel-booking/

# Check if logs are going to stdout
docker-compose logs app
```

---

## Additional Resources

- [Environment Variables Reference](docs/ENVIRONMENT_VARIABLES.md)
- [Setup Guide](SETUP_GUIDE.md)
- [API Documentation](API_DOCUMENTATION.md)
- [Docker Documentation](https://docs.docker.com/)
- [Docker Compose Documentation](https://docs.docker.com/compose/)

---

## Support

For issues and questions:
- Check application logs: `docker-compose logs app`
- Validate configuration: `./scripts/validate-config.sh`
- Review environment variables: [docs/ENVIRONMENT_VARIABLES.md](docs/ENVIRONMENT_VARIABLES.md)

---

**Last Updated**: 2025-10-23
**Version**: 1.0.0
