#!/bin/bash

# Post-create script for GitHub Codespaces
# Runs once after the container is created

set -e

echo "========================================="
echo "Codespaces Post-Create Setup Starting..."
echo "========================================="

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Navigate to workspace
cd /workspaces/west-bethel-motel-booking-system || exit 1

# 1. Install Maven dependencies
echo -e "${BLUE}Installing Maven dependencies...${NC}"
mvn clean install -DskipTests -q
echo -e "${GREEN}✓ Maven dependencies installed${NC}"

# 2. Wait for PostgreSQL to be ready
echo -e "${BLUE}Waiting for PostgreSQL to be ready...${NC}"
max_attempts=30
attempt=0
until pg_isready -h localhost -p 5432 -U postgres > /dev/null 2>&1 || [ $attempt -eq $max_attempts ]; do
  attempt=$((attempt + 1))
  echo "  Waiting for PostgreSQL... (attempt $attempt/$max_attempts)"
  sleep 2
done

if [ $attempt -eq $max_attempts ]; then
  echo "ERROR: PostgreSQL did not become ready in time"
  exit 1
fi
echo -e "${GREEN}✓ PostgreSQL is ready${NC}"

# 3. Wait for Redis to be ready
echo -e "${BLUE}Waiting for Redis to be ready...${NC}"
max_attempts=30
attempt=0
until redis-cli -h localhost -p 6379 -a devredispass ping > /dev/null 2>&1 || [ $attempt -eq $max_attempts ]; do
  attempt=$((attempt + 1))
  echo "  Waiting for Redis... (attempt $attempt/$max_attempts)"
  sleep 2
done

if [ $attempt -eq $max_attempts ]; then
  echo "ERROR: Redis did not become ready in time"
  exit 1
fi
echo -e "${GREEN}✓ Redis is ready${NC}"

# 4. Run database migrations
echo -e "${BLUE}Running database migrations...${NC}"
# Migrations will run automatically when Spring Boot starts
# But we can verify the database is accessible
PGPASSWORD=devpassword psql -h localhost -U postgres -d motel_booking_dev -c "SELECT version();" > /dev/null 2>&1
echo -e "${GREEN}✓ Database connection verified${NC}"

# 5. Create application.properties for codespaces profile
echo -e "${BLUE}Creating Codespaces application properties...${NC}"
mkdir -p src/main/resources
cat > src/main/resources/application-codespaces.properties << 'EOF'
# Codespaces Development Configuration

# Application
spring.application.name=west-bethel-motel-booking
server.port=8080

# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/motel_booking_dev
spring.datasource.username=postgres
spring.datasource.password=devpassword
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# Redis
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.password=devredispass

# Email (Mailhog)
spring.mail.host=localhost
spring.mail.port=1025
spring.mail.username=
spring.mail.password=
spring.mail.properties.mail.smtp.auth=false
spring.mail.properties.mail.smtp.starttls.enable=false

# JWT
jwt.secret=dev-secret-key-change-in-production-minimum-256-bits-required
jwt.expiration=86400000

# CORS
cors.allowed-origins=*

# Actuator
management.endpoints.web.exposure.include=health,metrics,prometheus,info
management.endpoint.health.show-details=always

# Logging
logging.level.root=INFO
logging.level.com.westbethel.motel_booking=DEBUG
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} - %msg%n

# Feature Flags
feature.flags.enabled=true
feature.mfa.enabled=true
feature.email.notifications.enabled=true
feature.payment.processing.enabled=true
EOF
echo -e "${GREEN}✓ Codespaces application properties created${NC}"

# 6. Set up Git configuration
echo -e "${BLUE}Configuring Git...${NC}"
git config --global --add safe.directory /workspaces/west-bethel-motel-booking-system
echo -e "${GREEN}✓ Git configured${NC}"

# 7. Create quick reference scripts
echo -e "${BLUE}Creating quick reference scripts...${NC}"
cat > run-app.sh << 'EOF'
#!/bin/bash
echo "Starting West Bethel Motel Booking System..."
mvn spring-boot:run -Dspring-boot.run.profiles=codespaces
EOF
chmod +x run-app.sh

cat > run-tests.sh << 'EOF'
#!/bin/bash
echo "Running all tests..."
mvn clean test
EOF
chmod +x run-tests.sh

cat > check-health.sh << 'EOF'
#!/bin/bash
echo "Checking application health..."
curl -s http://localhost:8080/actuator/health | jq .
EOF
chmod +x check-health.sh

echo -e "${GREEN}✓ Quick reference scripts created${NC}"

# 8. Display helpful information
echo ""
echo "========================================="
echo -e "${GREEN}Codespaces Setup Complete!${NC}"
echo "========================================="
echo ""
echo "Quick Start Commands:"
echo "  ./run-app.sh         - Start the application"
echo "  ./run-tests.sh       - Run all tests"
echo "  ./check-health.sh    - Check application health"
echo ""
echo "Services Available:"
echo "  Application:    http://localhost:8080"
echo "  Mailhog UI:     http://localhost:8025"
echo "  Prometheus:     http://localhost:9090"
echo "  Grafana:        http://localhost:3000 (admin/admin)"
echo "  PostgreSQL:     localhost:5432 (postgres/devpassword)"
echo "  Redis:          localhost:6379 (devredispass)"
echo ""
echo "Documentation:"
echo "  API Docs:       /docs/API_DOCUMENTATION.md"
echo "  Quick Start:    /TESTING_QUICK_START.md"
echo "  Deployment:     /docs/deployment/DEPLOYMENT_GUIDE.md"
echo ""
echo "========================================="

# Create a welcome message file
cat > CODESPACES_README.md << 'EOF'
# GitHub Codespaces Development Environment

Welcome to the West Bethel Motel Booking System development environment!

## Quick Start

1. **Start the application:**
   ```bash
   ./run-app.sh
   ```

2. **Run tests:**
   ```bash
   ./run-tests.sh
   ```

3. **Check application health:**
   ```bash
   ./check-health.sh
   ```

## Available Services

| Service | URL | Credentials |
|---------|-----|-------------|
| Application | http://localhost:8080 | - |
| API Documentation | http://localhost:8080/swagger-ui.html | - |
| Mailhog (Email Testing) | http://localhost:8025 | - |
| Prometheus (Metrics) | http://localhost:9090 | - |
| Grafana (Dashboards) | http://localhost:3000 | admin/admin |
| PostgreSQL | localhost:5432 | postgres/devpassword |
| Redis | localhost:6379 | devredispass |

## Testing

### Run All Tests
```bash
mvn clean test
```

### Run Specific Test Suite
```bash
# Unit tests
mvn test -Dtest=*Test

# Integration tests
mvn test -Dtest=*IT

# E2E tests
mvn test -Dtest=*E2ETest
```

### Load Testing with K6
```bash
k6 run k6-scripts/load-test.js
```

## Database Management

### Connect to PostgreSQL
```bash
psql -h localhost -U postgres -d motel_booking_dev
```

### Run Migrations
Migrations run automatically on application startup

### Reset Database
```bash
./scripts/reset-database.sh
```

## Feature Flags

Feature flags are enabled by default in Codespaces:
- MFA: Enabled
- Email Notifications: Enabled
- Payment Processing: Enabled (test mode)

## Monitoring

- **Metrics**: http://localhost:8080/actuator/prometheus
- **Health**: http://localhost:8080/actuator/health
- **Info**: http://localhost:8080/actuator/info

## Troubleshooting

### Application won't start
1. Check if PostgreSQL is running: `pg_isready -h localhost`
2. Check if Redis is running: `redis-cli -h localhost -a devredispass ping`
3. Check logs: `mvn spring-boot:run -Dspring-boot.run.profiles=codespaces -X`

### Tests failing
1. Ensure database is accessible
2. Clear cache: `mvn clean`
3. Check test data: Review DATA_SEEDING.md

### Email not sending
1. Check Mailhog UI: http://localhost:8025
2. Verify SMTP settings in application-codespaces.properties

## Next Steps

1. Review the codebase structure
2. Run the test suite to ensure everything works
3. Explore the API documentation
4. Deploy to staging when ready

## Documentation

- **Phase 2 Completion Report**: PHASE_2_COMPLETION_REPORT.md
- **API Documentation**: docs/API_DOCUMENTATION.md
- **Deployment Guide**: docs/deployment/DEPLOYMENT_GUIDE.md
- **Security Guide**: SECURITY_IMPLEMENTATION_GUIDE.md

Happy coding! 🚀
EOF

echo -e "${GREEN}Created CODESPACES_README.md for quick reference${NC}"
echo ""
