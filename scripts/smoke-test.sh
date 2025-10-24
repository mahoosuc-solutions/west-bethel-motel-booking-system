#!/bin/bash

################################################################################
# Smoke Test Script for West Bethel Motel Booking System
# Quick validation of critical functionality after deployment
################################################################################

set -euo pipefail

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Configuration
APP_URL="${1:-http://localhost:8080}"
TIMEOUT="${TIMEOUT:-10}"

# Test counters
PASSED=0
FAILED=0
SKIPPED=0

################################################################################
# Helper Functions
################################################################################

log() {
    echo -e "${BLUE}[TEST]${NC} $*"
}

success() {
    echo -e "${GREEN}[PASS]${NC} $*"
    PASSED=$((PASSED + 1))
}

fail() {
    echo -e "${RED}[FAIL]${NC} $*"
    FAILED=$((FAILED + 1))
}

skip() {
    echo -e "${YELLOW}[SKIP]${NC} $*"
    SKIPPED=$((SKIPPED + 1))
}

################################################################################
# Smoke Tests
################################################################################

test_application_available() {
    log "Test: Application is available"

    local http_code
    http_code=$(curl -s -o /dev/null -w "%{http_code}" --max-time "$TIMEOUT" "$APP_URL/actuator/health" 2>/dev/null || echo "000")

    if [[ "$http_code" == "200" ]]; then
        success "Application is available (HTTP $http_code)"
        return 0
    else
        fail "Application is not available (HTTP $http_code)"
        return 1
    fi
}

test_health_endpoint() {
    log "Test: Health endpoint returns UP status"

    local response
    response=$(curl -s --max-time "$TIMEOUT" "$APP_URL/actuator/health" 2>/dev/null || echo "{}")
    local status=$(echo "$response" | jq -r '.status' 2>/dev/null || echo "UNKNOWN")

    if [[ "$status" == "UP" ]]; then
        success "Health endpoint status: UP"
        return 0
    else
        fail "Health endpoint status: $status"
        return 1
    fi
}

test_database_connection() {
    log "Test: Database connection is healthy"

    local response
    response=$(curl -s --max-time "$TIMEOUT" "$APP_URL/actuator/health" 2>/dev/null || echo "{}")
    local db_status=$(echo "$response" | jq -r '.components.db.status' 2>/dev/null || echo "UNKNOWN")

    if [[ "$db_status" == "UP" ]]; then
        success "Database connection: UP"
        return 0
    else
        fail "Database connection: $db_status"
        return 1
    fi
}

test_api_authentication() {
    log "Test: API authentication endpoint is accessible"

    local http_code
    http_code=$(curl -s -o /dev/null -w "%{http_code}" --max-time "$TIMEOUT" \
        -X POST "$APP_URL/api/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"email":"invalid@test.com","password":"wrong"}' 2>/dev/null || echo "000")

    # Should return 401 Unauthorized for invalid credentials
    if [[ "$http_code" == "401" ]]; then
        success "Authentication endpoint is working (HTTP $http_code)"
        return 0
    elif [[ "$http_code" == "400" || "$http_code" == "403" ]]; then
        success "Authentication endpoint is working (HTTP $http_code)"
        return 0
    else
        fail "Authentication endpoint returned unexpected status (HTTP $http_code)"
        return 1
    fi
}

test_rooms_api() {
    log "Test: Rooms API endpoint is accessible"

    local http_code
    http_code=$(curl -s -o /dev/null -w "%{http_code}" --max-time "$TIMEOUT" \
        "$APP_URL/api/rooms" 2>/dev/null || echo "000")

    # Should return 200 or 401 (if auth required)
    if [[ "$http_code" == "200" || "$http_code" == "401" ]]; then
        success "Rooms API endpoint is accessible (HTTP $http_code)"
        return 0
    else
        fail "Rooms API endpoint is not accessible (HTTP $http_code)"
        return 1
    fi
}

test_bookings_api() {
    log "Test: Bookings API endpoint exists"

    local http_code
    http_code=$(curl -s -o /dev/null -w "%{http_code}" --max-time "$TIMEOUT" \
        "$APP_URL/api/bookings" 2>/dev/null || echo "000")

    # Should return 200, 401, or 403 (depending on auth requirements)
    if [[ "$http_code" == "200" || "$http_code" == "401" || "$http_code" == "403" ]]; then
        success "Bookings API endpoint exists (HTTP $http_code)"
        return 0
    else
        fail "Bookings API endpoint not found (HTTP $http_code)"
        return 1
    fi
}

test_api_documentation() {
    log "Test: API documentation is accessible"

    local http_code
    http_code=$(curl -s -o /dev/null -w "%{http_code}" --max-time "$TIMEOUT" \
        "$APP_URL/swagger-ui/index.html" 2>/dev/null || echo "000")

    if [[ "$http_code" == "200" ]]; then
        success "API documentation is accessible (HTTP $http_code)"
        return 0
    else
        skip "API documentation not accessible (HTTP $http_code) - may be disabled in production"
        return 0
    fi
}

test_metrics_endpoint() {
    log "Test: Metrics endpoint is accessible"

    local http_code
    http_code=$(curl -s -o /dev/null -w "%{http_code}" --max-time "$TIMEOUT" \
        "$APP_URL/actuator/metrics" 2>/dev/null || echo "000")

    if [[ "$http_code" == "200" ]]; then
        success "Metrics endpoint is accessible (HTTP $http_code)"
        return 0
    else
        skip "Metrics endpoint not accessible (HTTP $http_code) - may require authentication"
        return 0
    fi
}

test_prometheus_metrics() {
    log "Test: Prometheus metrics endpoint"

    local http_code
    http_code=$(curl -s -o /dev/null -w "%{http_code}" --max-time "$TIMEOUT" \
        "$APP_URL/actuator/prometheus" 2>/dev/null || echo "000")

    if [[ "$http_code" == "200" ]]; then
        success "Prometheus metrics endpoint is accessible (HTTP $http_code)"
        return 0
    else
        skip "Prometheus metrics not accessible (HTTP $http_code)"
        return 0
    fi
}

test_response_time() {
    log "Test: API response time is acceptable"

    local start_time
    local end_time
    local duration

    start_time=$(date +%s%N)
    curl -s --max-time "$TIMEOUT" "$APP_URL/actuator/health" > /dev/null 2>&1
    end_time=$(date +%s%N)

    duration=$(( (end_time - start_time) / 1000000 ))

    if [[ $duration -lt 2000 ]]; then
        success "Response time is acceptable: ${duration}ms"
        return 0
    else
        fail "Response time is too slow: ${duration}ms (should be < 2000ms)"
        return 1
    fi
}

test_cors_headers() {
    log "Test: CORS headers are configured"

    local headers
    headers=$(curl -s -I --max-time "$TIMEOUT" "$APP_URL/actuator/health" 2>/dev/null || echo "")

    if echo "$headers" | grep -i "Access-Control-Allow-Origin" > /dev/null; then
        success "CORS headers are configured"
        return 0
    else
        skip "CORS headers not present (may not be required)"
        return 0
    fi
}

test_security_headers() {
    log "Test: Security headers are present"

    local headers
    headers=$(curl -s -I --max-time "$TIMEOUT" "$APP_URL/actuator/health" 2>/dev/null || echo "")

    local headers_found=0

    if echo "$headers" | grep -i "X-Content-Type-Options" > /dev/null; then
        headers_found=$((headers_found + 1))
    fi

    if echo "$headers" | grep -i "X-Frame-Options" > /dev/null; then
        headers_found=$((headers_found + 1))
    fi

    if [[ $headers_found -gt 0 ]]; then
        success "Security headers are configured ($headers_found found)"
        return 0
    else
        skip "Security headers not detected"
        return 0
    fi
}

test_error_handling() {
    log "Test: Error handling for invalid endpoints"

    local http_code
    http_code=$(curl -s -o /dev/null -w "%{http_code}" --max-time "$TIMEOUT" \
        "$APP_URL/api/nonexistent-endpoint-12345" 2>/dev/null || echo "000")

    if [[ "$http_code" == "404" ]]; then
        success "Proper error handling for invalid endpoints (HTTP $http_code)"
        return 0
    else
        fail "Unexpected response for invalid endpoint (HTTP $http_code)"
        return 1
    fi
}

################################################################################
# Main Execution
################################################################################

main() {
    echo "=========================================="
    echo "Smoke Tests for West Bethel Motel Booking System"
    echo "=========================================="
    echo "Target: $APP_URL"
    echo "Timeout: ${TIMEOUT}s"
    echo "=========================================="
    echo

    # Critical tests (must pass)
    test_application_available || exit 1
    test_health_endpoint || exit 1
    test_database_connection || exit 1

    # Important functionality tests
    test_api_authentication
    test_rooms_api
    test_bookings_api

    # Nice-to-have tests
    test_api_documentation
    test_metrics_endpoint
    test_prometheus_metrics
    test_response_time
    test_cors_headers
    test_security_headers
    test_error_handling

    # Summary
    echo
    echo "=========================================="
    echo "Smoke Test Summary"
    echo "=========================================="
    echo -e "${GREEN}Passed:${NC} $PASSED"
    echo -e "${RED}Failed:${NC} $FAILED"
    echo -e "${YELLOW}Skipped:${NC} $SKIPPED"
    echo "=========================================="

    if [[ $FAILED -gt 0 ]]; then
        echo -e "${RED}Smoke tests FAILED${NC}"
        echo "Critical issues detected. Deployment may need to be rolled back."
        exit 1
    else
        echo -e "${GREEN}All critical smoke tests PASSED${NC}"
        if [[ $SKIPPED -gt 0 ]]; then
            echo -e "${YELLOW}$SKIPPED tests were skipped (non-critical)${NC}"
        fi
        exit 0
    fi
}

# Parse arguments
if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
    cat << EOF
Usage: $0 [APP_URL]

Run smoke tests against the deployed application.

Arguments:
    APP_URL    Base URL of the application (default: http://localhost:8080)

Environment Variables:
    TIMEOUT    Request timeout in seconds (default: 10)

Examples:
    $0 https://staging.westbethelmotel.com
    TIMEOUT=30 $0 https://www.westbethelmotel.com

EOF
    exit 0
fi

# Run smoke tests
main
