#!/bin/bash

################################################################################
# West Bethel Motel Booking System - Cleanup Script
# Version: 1.0
# Purpose: Clean build artifacts and reset to fresh state
################################################################################

set -e

echo "=========================================="
echo "West Bethel Motel Booking System Cleanup"
echo "=========================================="
echo ""

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

print_status() {
    echo -e "${GREEN}✓${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

# Get script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
cd "$SCRIPT_DIR"

echo "This will remove:"
echo "  - target/ (compiled classes)"
echo "  - build.log, test.log"
echo "  - docker-build.log"
echo "  - Docker images and containers"
echo "  - Maven local cache (optional)"
echo ""
read -p "Continue? (y/N) " -n 1 -r
echo

if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Cleanup cancelled"
    exit 0
fi

echo ""
echo "1. Removing build artifacts..."
echo "------------------------------"

if [ -d "target" ]; then
    rm -rf target
    print_status "Removed target/"
fi

if [ -f "build.log" ]; then
    rm -f build.log
    print_status "Removed build.log"
fi

if [ -f "test.log" ]; then
    rm -f test.log
    print_status "Removed test.log"
fi

if [ -f "docker-build.log" ]; then
    rm -f docker-build.log
    print_status "Removed docker-build.log"
fi

echo ""
echo "2. Cleaning Docker resources..."
echo "--------------------------------"

if command -v docker &> /dev/null; then
    # Remove project containers
    CONTAINERS=$(docker ps -a -q --filter "ancestor=west-bethel-build" 2>/dev/null)
    if [ ! -z "$CONTAINERS" ]; then
        docker rm -f $CONTAINERS
        print_status "Removed project containers"
    fi

    # Remove project images
    IMAGES=$(docker images -q west-bethel-build 2>/dev/null)
    if [ ! -z "$IMAGES" ]; then
        docker rmi -f $IMAGES
        print_status "Removed project images"
    fi

    read -p "Remove dangling Docker images? (y/N) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        docker image prune -f
        print_status "Removed dangling images"
    fi
else
    print_warning "Docker not found, skipping Docker cleanup"
fi

echo ""
echo "3. Cleaning Maven cache (optional)..."
echo "--------------------------------------"

read -p "Remove Maven local repository cache? (y/N) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    if [ -d "$HOME/.m2/repository/com/westbethel" ]; then
        rm -rf "$HOME/.m2/repository/com/westbethel"
        print_status "Removed project from Maven cache"
    fi
fi

echo ""
echo "4. Cleaning IDE files (optional)..."
echo "------------------------------------"

read -p "Remove IDE metadata (.idea, .vscode, *.iml)? (y/N) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    find . -type d -name ".idea" -exec rm -rf {} + 2>/dev/null || true
    find . -type d -name ".vscode" -exec rm -rf {} + 2>/dev/null || true
    find . -type f -name "*.iml" -delete 2>/dev/null || true
    print_status "Removed IDE files"
fi

echo ""
echo "5. Preserving important files..."
echo "---------------------------------"

PRESERVED_FILES=(
    "src/"
    "pom.xml"
    "Dockerfile"
    ".gitignore"
    "README.md"
    "*.md"
    ".env"
    ".env.example"
)

print_status "Source code and documentation preserved"

echo ""
echo "=========================================="
echo "Cleanup Complete!"
echo "=========================================="
echo ""
echo "Preserved:"
echo "  ✓ Source code (src/)"
echo "  ✓ Configuration files (pom.xml, Dockerfile)"
echo "  ✓ Documentation (*.md)"
echo "  ✓ Environment files (.env)"
echo ""
echo "To rebuild the project, run:"
echo "  ./setup.sh"
echo ""
