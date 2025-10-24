#!/bin/bash

# Post-start script for GitHub Codespaces
# Runs every time the container starts

set -e

echo "========================================="
echo "Codespaces Post-Start Setup..."
echo "========================================="

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Navigate to workspace
cd /workspaces/west-bethel-motel-booking-system || exit 1

# 1. Check service health
echo -e "${BLUE}Checking service health...${NC}"

# PostgreSQL
if pg_isready -h localhost -p 5432 -U postgres > /dev/null 2>&1; then
  echo -e "${GREEN}✓ PostgreSQL is running${NC}"
else
  echo -e "${YELLOW}⚠ PostgreSQL is starting...${NC}"
fi

# Redis
if redis-cli -h localhost -p 6379 -a devredispass ping > /dev/null 2>&1; then
  echo -e "${GREEN}✓ Redis is running${NC}"
else
  echo -e "${YELLOW}⚠ Redis is starting...${NC}"
fi

# 2. Display welcome message
echo ""
echo "========================================="
echo -e "${GREEN}Codespaces Environment Ready!${NC}"
echo "========================================="
echo ""
echo -e "${BLUE}To start the application:${NC}"
echo "  ./run-app.sh"
echo ""
echo -e "${BLUE}To run tests:${NC}"
echo "  ./run-tests.sh"
echo ""
echo -e "${BLUE}View full documentation:${NC}"
echo "  cat CODESPACES_README.md"
echo ""
echo "========================================="
