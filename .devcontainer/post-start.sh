#!/bin/bash

# Post-start script for GitHub Codespaces
# Runs every time the container starts

set -e

echo "========================================="
echo "Codespaces Post-Start: Starting Services"
echo "========================================="

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

# Navigate to workspace
cd /workspaces/west-bethel-motel-booking-system || exit 1

# Start PostgreSQL
echo -e "${BLUE}Starting PostgreSQL...${NC}"
sudo service postgresql start > /dev/null 2>&1
pg_isready -h localhost -p 5432 > /dev/null 2>&1 && echo -e "${GREEN}✓ PostgreSQL started${NC}"

# Start Redis
echo -e "${BLUE}Starting Redis...${NC}"
sudo service redis-server start > /dev/null 2>&1
redis-cli -a devredispass ping > /dev/null 2>&1 && echo -e "${GREEN}✓ Redis started${NC}"

echo "========================================="
echo -e "${GREEN}All Services Started!${NC}"
echo "========================================="
echo ""
echo "Quick Start:"
echo "  ./check-services.sh  - Verify all services"
echo "  ./run-app.sh         - Start Spring Boot"
echo "  ./run-tests.sh       - Run 792+ tests"
echo ""
