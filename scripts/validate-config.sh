#!/bin/bash
#
# Configuration Validation Script
# Validates that all required environment variables are set before deployment
#
# Usage:
#   ./scripts/validate-config.sh [environment]
#
# Arguments:
#   environment - Optional: dev, staging, prod (default: current profile)
#

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Load .env file if it exists
if [ -f .env ]; then
    echo -e "${BLUE}Loading .env file...${NC}"
    export $(grep -v '^#' .env | xargs)
fi

ENVIRONMENT=${1:-${SPRING_PROFILES_ACTIVE:-dev}}

echo -e "${BLUE}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║  Configuration Validation - ${ENVIRONMENT} Environment${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""

ERRORS=0
WARNINGS=0

# Function to check required variable
check_required() {
    local var_name=$1
    local var_value=${!var_name}

    if [ -z "$var_value" ]; then
        echo -e "${RED}✗ $var_name is not set (REQUIRED)${NC}"
        ((ERRORS++))
        return 1
    else
        echo -e "${GREEN}✓ $var_name is set${NC}"
        return 0
    fi
}

# Function to check optional variable
check_optional() {
    local var_name=$1
    local var_value=${!var_name}
    local default_value=$2

    if [ -z "$var_value" ]; then
        echo -e "${YELLOW}⚠ $var_name not set (using default: $default_value)${NC}"
        ((WARNINGS++))
    else
        echo -e "${GREEN}✓ $var_name is set${NC}"
    fi
}

echo -e "${BLUE}Database Configuration:${NC}"
check_required "DATABASE_USERNAME"
check_required "DATABASE_PASSWORD"
check_optional "DATABASE_URL" "jdbc:postgresql://localhost:5432/motel_booking"
echo ""

echo -e "${BLUE}Redis Configuration:${NC}"
if [ "$ENVIRONMENT" == "prod" ]; then
    check_required "REDIS_PASSWORD"
else
    check_optional "REDIS_PASSWORD" "devredispass"
fi
check_optional "REDIS_HOST" "localhost"
check_optional "REDIS_PORT" "6379"
echo ""

echo -e "${BLUE}JWT Configuration:${NC}"
check_required "JWT_SECRET"
if [ -n "$JWT_SECRET" ]; then
    # Validate JWT secret length
    SECRET_LENGTH=${#JWT_SECRET}
    if [ $SECRET_LENGTH -lt 32 ]; then
        echo -e "${RED}✗ JWT_SECRET is too short ($SECRET_LENGTH chars). Minimum 32 bytes required.${NC}"
        ((ERRORS++))
    else
        echo -e "${GREEN}✓ JWT_SECRET length is adequate${NC}"
    fi
fi
check_optional "JWT_EXPIRATION" "86400000"
echo ""

echo -e "${BLUE}Email Configuration:${NC}"
check_optional "MAIL_HOST" "smtp.gmail.com"
check_optional "MAIL_PORT" "587"
check_optional "MAIL_USERNAME" "noreply@westbethelmotel.com"
check_optional "MAIL_PASSWORD" "(not set)"
echo ""

echo -e "${BLUE}Server Configuration:${NC}"
check_optional "SERVER_PORT" "8080"
check_optional "SPRING_PROFILES_ACTIVE" "prod"
echo ""

# Summary
echo -e "${BLUE}════════════════════════════════════════════════════════════${NC}"
if [ $ERRORS -eq 0 ] && [ $WARNINGS -eq 0 ]; then
    echo -e "${GREEN}✓ Configuration validation PASSED${NC}"
    echo -e "${GREEN}  All required variables are set${NC}"
elif [ $ERRORS -eq 0 ]; then
    echo -e "${YELLOW}⚠ Configuration validation PASSED with warnings${NC}"
    echo -e "${YELLOW}  Errors: $ERRORS, Warnings: $WARNINGS${NC}"
    echo -e "${YELLOW}  Review warnings above${NC}"
else
    echo -e "${RED}✗ Configuration validation FAILED${NC}"
    echo -e "${RED}  Errors: $ERRORS, Warnings: $WARNINGS${NC}"
    echo -e "${RED}  Fix errors above before deployment${NC}"
    exit 1
fi
echo -e "${BLUE}════════════════════════════════════════════════════════════${NC}"

exit 0
