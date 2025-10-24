# West Bethel Motel Booking System - Setup Guide

## Quick Start

### Prerequisites

- **Java 17+** (required)
- **Maven 3.9+** (required)
- **PostgreSQL 14+** (optional, can use H2 for dev)
- **Redis 6+** (optional for caching)
- **Docker** (optional for containerized deployment)

### 1. Clone and Setup

```bash
cd /home/webemo-aaron/projects/west-bethel-motel-booking-system

# Make scripts executable
chmod +x setup.sh cleanup.sh

# Run setup
./setup.sh
```

The setup script will:
- ✅ Check prerequisites
- ✅ Clean previous builds
- ✅ Create environment files
- ✅ Verify project structure
- ✅ Build the project
- ✅ Optionally set up database
- ✅ Optionally run tests

### 2. Configure Environment

Edit `.env` with your actual credentials:

```bash
# Database Configuration
DATABASE_URL=jdbc:postgresql://localhost:5432/motel_booking
DATABASE_USERNAME=motel_booking
DATABASE_PASSWORD=your-actual-password

# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=your-redis-password

# Application
SPRING_PROFILES_ACTIVE=dev
SERVER_PORT=8080
```

### 3. Build the Project

```bash
# Clean and compile
mvn clean compile

# Run tests
mvn test

# Package application
mvn package
```

### 4. Run the Application

#### Option A: Development Mode (with test data)

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

This will:
- Start on port 8080
- Seed test data automatically
- Use development logging
- Enable SQL logging

#### Option B: Default Mode

```bash
mvn spring-boot:run
```

#### Option C: With Docker

```bash
# Build image
docker build -t west-bethel-motel .

# Run container
docker run -p 8080:8080 \
  -e DATABASE_URL=jdbc:postgresql://host.docker.internal:5432/motel_booking \
  -e DATABASE_PASSWORD=your-password \
  west-bethel-motel
```

### 5. Verify Installation

Once running, test these endpoints:

```bash
# Health check
curl http://localhost:8080/actuator/health

# Search availability (requires dev profile with seeded data)
curl "http://localhost:8080/api/v1/availability?propertyId=00000000-0000-0000-0000-000000000001&startDate=2025-12-01&endDate=2025-12-03"
```

## Database Setup

### Option 1: PostgreSQL (Production-like)

```bash
# Install PostgreSQL (Ubuntu/Debian)
sudo apt update
sudo apt install postgresql postgresql-contrib

# Create database and user
sudo -u postgres psql << EOF
CREATE DATABASE motel_booking;
CREATE USER motel_booking WITH PASSWORD 'change-me';
GRANT ALL PRIVILEGES ON DATABASE motel_booking TO motel_booking;
\q
EOF

# Update application.yml or .env with credentials
```

### Option 2: H2 (Development Only)

H2 is automatically used for tests. To use for development:

1. Update `src/main/resources/application-dev.yml`:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:devdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    username: sa
    password:
```

2. Add H2 dependency to `pom.xml` (already in test scope):

```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>
```

## Development Workflow

### Initial Setup
```bash
./setup.sh
```

### Daily Development
```bash
# Start with hot reload
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# In another terminal, run tests on changes
mvn test -Dtest=YourTest
```

### Before Committing
```bash
# Clean build
mvn clean verify

# Check test coverage
mvn test jacoco:report
# Open target/site/jacoco/index.html
```

### Reset Environment
```bash
./cleanup.sh
./setup.sh
```

## Troubleshooting

### Issue: "Cannot connect to database"

**Solution:**
- Verify PostgreSQL is running: `sudo systemctl status postgresql`
- Check credentials in `.env`
- Test connection: `psql -U motel_booking -d motel_booking -h localhost`

### Issue: "Port 8080 already in use"

**Solution:**
- Change port in `.env`: `SERVER_PORT=8081`
- Or kill process: `lsof -ti:8080 | xargs kill -9`

### Issue: "Tests failing"

**Solution:**
- Check test logs: `tail -100 test.log`
- Run specific test: `mvn test -Dtest=AvailabilityControllerTest`
- Ensure H2 is in test scope in `pom.xml`

### Issue: "Maven build fails"

**Solution:**
- Check Java version: `java -version` (must be 17+)
- Clear Maven cache: `rm -rf ~/.m2/repository/com/westbethel`
- Re-run: `mvn clean install -U`

### Issue: "Redis connection refused"

**Solution:**
- Install Redis: `sudo apt install redis-server`
- Start Redis: `sudo systemctl start redis`
- Or disable cache in application.yml: `spring.cache.type: none`

## Test Data

When running with `dev` profile, the following test data is seeded:

### Property
- **West Bethel Motel** (UUID: `00000000-0000-0000-0000-000000000001`)

### Room Types
- **STANDARD** - $89/night (UUID: `00000000-0000-0000-0000-000000000010`)
- **DELUXE** - $129/night (UUID: `00000000-0000-0000-0000-000000000011`)
- **SUITE** - $199/night (UUID: `00000000-0000-0000-0000-000000000012`)

### Rooms
- 101-105 (STANDARD)
- 201-203 (DELUXE)
- 301-302 (SUITE)

### Guests
- **john.doe@example.com** (UUID: `00000000-0000-0000-0000-000000000020`)
- **jane.smith@example.com** (UUID: `00000000-0000-0000-0000-000000000021`) - GOLD loyalty, 2500 points
- **bob.jones@example.com** (UUID: `00000000-0000-0000-0000-000000000022`)

### Rate Plans
- **Standard Rate** (UUID: `00000000-0000-0000-0000-000000000030`)
- **Weekend Special** (UUID: `00000000-0000-0000-0000-000000000031`)

## API Testing

See `API_DOCUMENTATION.md` for complete API reference.

### Sample Requests

```bash
# Search availability
curl "http://localhost:8080/api/v1/availability?\
propertyId=00000000-0000-0000-0000-000000000001&\
startDate=2025-12-01&\
endDate=2025-12-03"

# Create booking
curl -X POST http://localhost:8080/api/v1/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "propertyId": "00000000-0000-0000-0000-000000000001",
    "guestId": "00000000-0000-0000-0000-000000000020",
    "checkIn": "2025-12-01",
    "checkOut": "2025-12-03",
    "adults": 2,
    "children": 0,
    "ratePlanId": "00000000-0000-0000-0000-000000000030",
    "roomTypeIds": ["00000000-0000-0000-0000-000000000010"]
  }'
```

## Documentation

- **API_DOCUMENTATION.md** - Complete API reference
- **DATA_MODEL_VALIDATION_REPORT.md** - Database schema and entity analysis
- **DATA_SEEDING.md** - Test data guide
- **TEST_DATA_REFERENCE.md** - Quick UUID reference
- **API_QUICK_REFERENCE.md** - Developer cheat sheet

## Next Steps

1. Review validation reports for production readiness
2. Implement security (JWT/OAuth2)
3. Create production configuration
4. Set up CI/CD pipeline
5. Deploy to staging environment

## Support

For issues and questions:
- Check `TROUBLESHOOTING.md`
- Review GitHub Issues: https://github.com/anthropics/claude-code/issues
- See comprehensive documentation in project root
