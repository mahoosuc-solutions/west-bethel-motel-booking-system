#!/bin/bash

################################################################################
# West Bethel Motel Booking System - Development Setup Script
# Version: 1.0
# Purpose: Complete project setup from scratch
################################################################################

set -e  # Exit on error

echo "=========================================="
echo "West Bethel Motel Booking System Setup"
echo "=========================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Function to print status
print_status() {
    echo -e "${GREEN}✓${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

# Get script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
cd "$SCRIPT_DIR"

echo "1. Checking prerequisites..."
echo "----------------------------"

# Check Java
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -ge 17 ]; then
        print_status "Java $JAVA_VERSION found"
    else
        print_warning "Java 17+ required, found Java $JAVA_VERSION"
    fi
else
    print_error "Java not found. Please install Java 17+"
    echo "  Ubuntu/Debian: sudo apt install openjdk-17-jdk"
    echo "  macOS: brew install openjdk@17"
fi

# Check Maven
if command -v mvn &> /dev/null; then
    MVN_VERSION=$(mvn -version | head -n 1 | awk '{print $3}')
    print_status "Maven $MVN_VERSION found"
else
    print_error "Maven not found. Please install Maven 3.9+"
    echo "  Ubuntu/Debian: sudo apt install maven"
    echo "  macOS: brew install maven"
fi

# Check Docker
if command -v docker &> /dev/null; then
    DOCKER_VERSION=$(docker --version | awk '{print $3}' | tr -d ',')
    print_status "Docker $DOCKER_VERSION found"
else
    print_warning "Docker not found (optional for containerized deployment)"
fi

# Check PostgreSQL
if command -v psql &> /dev/null; then
    PSQL_VERSION=$(psql --version | awk '{print $3}')
    print_status "PostgreSQL $PSQL_VERSION found"
else
    print_warning "PostgreSQL not found (required for production, optional for dev with H2)"
fi

# Check Redis
if command -v redis-cli &> /dev/null; then
    REDIS_VERSION=$(redis-cli --version | awk '{print $2}')
    print_status "Redis $REDIS_VERSION found"
else
    print_warning "Redis not found (required for caching)"
fi

echo ""
echo "2. Cleaning previous builds..."
echo "------------------------------"
if [ -d "target" ]; then
    rm -rf target
    print_status "Removed target directory"
fi

echo ""
echo "3. Setting up environment files..."
echo "----------------------------------"

# Create .env.example if it doesn't exist
if [ ! -f ".env.example" ]; then
    cat > .env.example << 'EOF'
# Database Configuration
DATABASE_URL=jdbc:postgresql://localhost:5432/motel_booking
DATABASE_USERNAME=motel_booking
DATABASE_PASSWORD=your-secure-password-here

# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=your-redis-password

# Mail Configuration (Optional)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=noreply@westbethelmotel.com
MAIL_PASSWORD=your-mail-password

# Security Configuration (Future)
JWT_SECRET=your-base64-encoded-secret-key-here
JWT_EXPIRATION=86400000

# Application Settings
SPRING_PROFILES_ACTIVE=dev
SERVER_PORT=8080
EOF
    print_status "Created .env.example"
else
    print_status ".env.example already exists"
fi

# Create local .env if it doesn't exist
if [ ! -f ".env" ]; then
    cp .env.example .env
    print_status "Created .env from template"
    print_warning "Please update .env with your actual credentials"
else
    print_status ".env already exists"
fi

echo ""
echo "4. Verifying project structure..."
echo "---------------------------------"

REQUIRED_DIRS=(
    "src/main/java"
    "src/main/resources"
    "src/main/resources/db/migration"
    "src/test/java"
    "src/test/resources"
)

for dir in "${REQUIRED_DIRS[@]}"; do
    if [ -d "$dir" ]; then
        print_status "$dir exists"
    else
        print_error "$dir missing!"
    fi
done

echo ""
echo "5. Building the project..."
echo "--------------------------"

if command -v mvn &> /dev/null; then
    echo "Running: mvn clean compile..."
    if mvn clean compile -DskipTests > build.log 2>&1; then
        print_status "Build successful!"
        echo "  Log: build.log"
    else
        print_error "Build failed. Check build.log for details"
        tail -20 build.log
        exit 1
    fi
else
    print_warning "Skipping build (Maven not available)"
fi

echo ""
echo "6. Setting up local database (Optional)..."
echo "-------------------------------------------"

read -p "Do you want to set up a local PostgreSQL database? (y/N) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    if command -v psql &> /dev/null; then
        echo "Creating database..."
        sudo -u postgres psql -c "CREATE DATABASE motel_booking;" 2>/dev/null || echo "Database may already exist"
        sudo -u postgres psql -c "CREATE USER motel_booking WITH PASSWORD 'change-me';" 2>/dev/null || echo "User may already exist"
        sudo -u postgres psql -c "GRANT ALL PRIVILEGES ON DATABASE motel_booking TO motel_booking;" 2>/dev/null
        print_status "Database setup complete"
    else
        print_error "PostgreSQL not installed"
    fi
else
    print_status "Skipping database setup (you can use H2 in-memory for development)"
fi

echo ""
echo "7. Running tests..."
echo "-------------------"

read -p "Do you want to run the test suite? (y/N) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    if command -v mvn &> /dev/null; then
        echo "Running tests..."
        if mvn test > test.log 2>&1; then
            print_status "All tests passed!"
            grep "Tests run:" test.log | tail -1
        else
            print_warning "Some tests failed. Check test.log for details"
            grep "Tests run:" test.log | tail -1
        fi
    else
        print_warning "Maven not available, skipping tests"
    fi
else
    print_status "Skipping tests"
fi

echo ""
echo "=========================================="
echo "Setup Complete!"
echo "=========================================="
echo ""
echo "Next steps:"
echo "1. Update .env with your actual credentials"
echo "2. Review the documentation:"
echo "   - API_DOCUMENTATION.md"
echo "   - DATA_MODEL_VALIDATION_REPORT.md"
echo "   - DATA_SEEDING.md"
echo ""
echo "To start the application:"
echo "  mvn spring-boot:run -Dspring-boot.run.profiles=dev"
echo ""
echo "To run with seeded test data:"
echo "  SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run"
echo ""
echo "API will be available at: http://localhost:8080"
echo "Health check: http://localhost:8080/actuator/health"
echo ""
