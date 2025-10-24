#!/bin/bash

# Codespaces Pre-Deployment Validation Script
# Validates all configuration files before deploying to GitHub Codespaces

set -e

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "========================================="
echo "Codespaces Pre-Deployment Validation"
echo "========================================="
echo ""

ERRORS=0
WARNINGS=0

# Function to print status
print_status() {
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✅ $2${NC}"
    else
        echo -e "${RED}❌ $2${NC}"
        ERRORS=$((ERRORS + 1))
    fi
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
    WARNINGS=$((WARNINGS + 1))
}

print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

# 1. Check .devcontainer directory exists
echo -e "${BLUE}1. Checking .devcontainer directory...${NC}"
if [ -d ".devcontainer" ]; then
    print_status 0 ".devcontainer directory exists"
else
    print_status 1 ".devcontainer directory NOT found"
    exit 1
fi

# 2. Validate devcontainer.json
echo ""
echo -e "${BLUE}2. Validating devcontainer.json...${NC}"
if [ -f ".devcontainer/devcontainer.json" ]; then
    if cat .devcontainer/devcontainer.json | jq . > /dev/null 2>&1; then
        print_status 0 "devcontainer.json is valid JSON"
    else
        print_status 1 "devcontainer.json has syntax errors"
    fi
else
    print_status 1 "devcontainer.json NOT found"
fi

# 3. Validate docker-compose.yml
echo ""
echo -e "${BLUE}3. Validating docker-compose.yml...${NC}"
if [ -f ".devcontainer/docker-compose.yml" ]; then
    if command -v docker-compose &> /dev/null; then
        if docker-compose -f .devcontainer/docker-compose.yml config > /dev/null 2>&1; then
            print_status 0 "docker-compose.yml is valid"
        else
            print_status 1 "docker-compose.yml has syntax errors"
        fi
    else
        print_warning "docker-compose not installed, skipping validation"
    fi
else
    print_status 1 "docker-compose.yml NOT found"
fi

# 4. Check setup scripts
echo ""
echo -e "${BLUE}4. Checking setup scripts...${NC}"
if [ -f ".devcontainer/post-create.sh" ]; then
    if [ -x ".devcontainer/post-create.sh" ]; then
        print_status 0 "post-create.sh exists and is executable"
    else
        print_status 1 "post-create.sh exists but is NOT executable"
    fi
else
    print_status 1 "post-create.sh NOT found"
fi

if [ -f ".devcontainer/post-start.sh" ]; then
    if [ -x ".devcontainer/post-start.sh" ]; then
        print_status 0 "post-start.sh exists and is executable"
    else
        print_status 1 "post-start.sh exists but is NOT executable"
    fi
else
    print_status 1 "post-start.sh NOT found"
fi

# 5. Check database initialization
echo ""
echo -e "${BLUE}5. Checking database initialization...${NC}"
if [ -f ".devcontainer/init-db.sql" ]; then
    print_status 0 "init-db.sql exists"
else
    print_status 1 "init-db.sql NOT found"
fi

# 6. Check monitoring configuration
echo ""
echo -e "${BLUE}6. Checking monitoring configuration...${NC}"
if [ -f ".devcontainer/prometheus.yml" ]; then
    print_status 0 "prometheus.yml exists"
else
    print_warning "prometheus.yml NOT found"
fi

if [ -f ".devcontainer/grafana-datasources.yml" ]; then
    print_status 0 "grafana-datasources.yml exists"
else
    print_warning "grafana-datasources.yml NOT found"
fi

# 7. Check application properties
echo ""
echo -e "${BLUE}7. Checking application properties...${NC}"
if [ -f "src/main/resources/application.properties" ]; then
    print_status 0 "application.properties exists"
elif [ -f "src/main/resources/application.yml" ]; then
    print_status 0 "application.yml exists"
else
    print_warning "application.properties or application.yml NOT found"
fi

if [ -f "src/main/resources/application-codespaces.properties" ]; then
    print_status 0 "application-codespaces.properties exists"
elif [ -f "src/main/resources/application-codespaces.yml" ]; then
    print_status 0 "application-codespaces.yml exists"
else
    print_info "Codespaces-specific application config will be created by post-create.sh"
fi

# 8. Check Maven
echo ""
echo -e "${BLUE}8. Checking Maven build...${NC}"
if [ -f "pom.xml" ]; then
    print_status 0 "pom.xml exists"

    if command -v mvn &> /dev/null; then
        if command -v java &> /dev/null; then
            if [ -z "${JAVA_HOME}" ]; then
                JAVA_BIN=$(command -v java)
                if [ -n "${JAVA_BIN}" ]; then
                    JAVA_GUESS=$(dirname "$(dirname "${JAVA_BIN}")")
                    export JAVA_HOME="${JAVA_GUESS}"
                    print_info "JAVA_HOME not set; temporarily using ${JAVA_HOME}"
                fi
            fi

            if [ -n "${JAVA_HOME}" ]; then
                echo "  Running Maven validation (compile only, no tests)..."
                if mvn clean compile -DskipTests -q > /dev/null 2>&1; then
                    print_status 0 "Maven compile successful"
                else
                    print_status 1 "Maven compile failed"
                fi
            else
                print_warning "JAVA_HOME could not be determined; skipping Maven compile check"
            fi
        else
            print_warning "Java runtime not available; skipping Maven compile check"
        fi
    else
        print_info "Maven not installed locally, will be available in Codespaces"
    fi
else
    print_status 1 "pom.xml NOT found"
fi

# 9. Check feature flags
echo ""
echo -e "${BLUE}9. Checking feature flags implementation...${NC}"
if [ -f "src/main/java/com/westbethel/motel_booking/config/FeatureFlags.java" ]; then
    print_status 0 "FeatureFlags.java exists"
else
    print_status 1 "FeatureFlags.java NOT found"
fi

if [ -f "src/main/java/com/westbethel/motel_booking/service/FeatureFlagService.java" ]; then
    print_status 0 "FeatureFlagService.java exists"
else
    print_status 1 "FeatureFlagService.java NOT found"
fi

# 10. Check documentation
echo ""
echo -e "${BLUE}10. Checking documentation...${NC}"
if [ -f "FAANG_RELEASE_PLAN_SUMMARY.md" ]; then
    print_status 0 "FAANG_RELEASE_PLAN_SUMMARY.md exists"
else
    print_warning "FAANG_RELEASE_PLAN_SUMMARY.md NOT found"
fi

if [ -f "docs/PRODUCTION_READINESS_REVIEW.md" ]; then
    print_status 0 "PRODUCTION_READINESS_REVIEW.md exists"
else
    print_warning "PRODUCTION_READINESS_REVIEW.md NOT found"
fi

if [ -f "docs/LAUNCH_READINESS_REVIEW.md" ]; then
    print_status 0 "LAUNCH_READINESS_REVIEW.md exists"
else
    print_warning "LAUNCH_READINESS_REVIEW.md NOT found"
fi

if [ -f "docs/PCI_DSS_COMPLIANCE_CHECKLIST.md" ]; then
    print_status 0 "PCI_DSS_COMPLIANCE_CHECKLIST.md exists"
else
    print_warning "PCI_DSS_COMPLIANCE_CHECKLIST.md NOT found"
fi

# Summary
echo ""
echo "========================================="
echo "Validation Summary"
echo "========================================="
if [ $ERRORS -eq 0 ] && [ $WARNINGS -eq 0 ]; then
    echo -e "${GREEN}✅ All checks passed! Ready for Codespaces deployment.${NC}"
    exit 0
elif [ $ERRORS -eq 0 ]; then
    echo -e "${YELLOW}⚠️  Validation passed with $WARNINGS warning(s).${NC}"
    echo "   You can proceed, but review warnings above."
    exit 0
else
    echo -e "${RED}❌ Validation failed with $ERRORS error(s) and $WARNINGS warning(s).${NC}"
    echo "   Please fix errors before deploying to Codespaces."
    exit 1
fi
