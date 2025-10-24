#!/bin/bash

################################################################################
# Health Check Script for West Bethel Motel Booking System
# Comprehensive health verification for deployment validation
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
MAX_ATTEMPTS="${MAX_ATTEMPTS:-30}"
TIMEOUT="${TIMEOUT:-5}"
VERBOSE="${VERBOSE:-false}"

# Counters
PASSED=0
FAILED=0

################################################################################
# Helper Functions
################################################################################

log() {
    if [[ "$VERBOSE" == "true" ]]; then
        echo -e "${BLUE}[INFO]${NC} $*"
    fi
}

success() {
    echo -e "${GREEN}[PASS]${NC} $*"
    PASSED=$((PASSED + 1))
}

fail() {
    echo -e "${RED}[FAIL]${NC} $*"
    FAILED=$((FAILED + 1))
}

warn() {
    echo -e "${YELLOW}[WARN]${NC} $*"
}

################################################################################
# Health Check Functions
################################################################################

check_actuator_health() {
    log "Checking actuator health endpoint..."

    local url="${APP_URL}/actuator/health"
    local response
    local http_code

    response=$(curl -s -w "\n%{http_code}" --max-time "$TIMEOUT" "$url" 2>/dev/null || echo "ERROR\n000")
    http_code=$(echo "$response" | tail -n1)
    local body=$(echo "$response" | head -n-1)

    if [[ "$http_code" == "200" ]]; then
        local status=$(echo "$body" | jq -r '.status' 2>/dev/null || echo "UNKNOWN")
        if [[ "$status" == "UP" ]]; then
            success "Actuator health check: $status"
            return 0
        else
            fail "Actuator health check: $status (expected UP)"
            return 1
        fi
    else
        fail "Actuator health check: HTTP $http_code"
        return 1
    fi
}

check_readiness() {
    log "Checking readiness endpoint..."

    local url="${APP_URL}/actuator/health/readiness"
    local response
    local http_code

    response=$(curl -s -w "\n%{http_code}" --max-time "$TIMEOUT" "$url" 2>/dev/null || echo "ERROR\n000")
    http_code=$(echo "$response" | tail -n1)
    local body=$(echo "$response" | head -n-1)

    if [[ "$http_code" == "200" ]]; then
        local status=$(echo "$body" | jq -r '.status' 2>/dev/null || echo "UNKNOWN")
        success "Readiness check: $status"
        return 0
    else
        fail "Readiness check: HTTP $http_code"
        return 1
    fi
}

check_liveness() {
    log "Checking liveness endpoint..."

    local url="${APP_URL}/actuator/health/liveness"
    local response
    local http_code

    response=$(curl -s -w "\n%{http_code}" --max-time "$TIMEOUT" "$url" 2>/dev/null || echo "ERROR\n000")
    http_code=$(echo "$response" | tail -n1)

    if [[ "$http_code" == "200" ]]; then
        success "Liveness check: UP"
        return 0
    else
        fail "Liveness check: HTTP $http_code"
        return 1
    fi
}

check_database_connectivity() {
    log "Checking database connectivity..."

    local url="${APP_URL}/actuator/health"
    local response

    response=$(curl -s --max-time "$TIMEOUT" "$url" 2>/dev/null || echo "{}")
    local db_status=$(echo "$response" | jq -r '.components.db.status' 2>/dev/null || echo "UNKNOWN")

    if [[ "$db_status" == "UP" ]]; then
        success "Database connectivity: $db_status"
        return 0
    else
        fail "Database connectivity: $db_status"
        return 1
    fi
}

check_redis_connectivity() {
    log "Checking Redis connectivity..."

    local url="${APP_URL}/actuator/health"
    local response

    response=$(curl -s --max-time "$TIMEOUT" "$url" 2>/dev/null || echo "{}")
    local redis_status=$(echo "$response" | jq -r '.components.redis.status' 2>/dev/null || echo "UNKNOWN")

    if [[ "$redis_status" == "UP" ]]; then
        success "Redis connectivity: $redis_status"
        return 0
    else
        warn "Redis connectivity: $redis_status (cache may be degraded)"
        return 0  # Don't fail on Redis issues
    fi
}

check_api_endpoints() {
    log "Checking critical API endpoints..."

    # Check API documentation endpoint
    local swagger_url="${APP_URL}/swagger-ui/index.html"
    local http_code
    http_code=$(curl -s -o /dev/null -w "%{http_code}" --max-time "$TIMEOUT" "$swagger_url" 2>/dev/null || echo "000")

    if [[ "$http_code" == "200" ]]; then
        success "API documentation: Available"
    else
        warn "API documentation: HTTP $http_code"
    fi

    # Check health endpoint exists
    local metrics_url="${APP_URL}/actuator/metrics"
    http_code=$(curl -s -o /dev/null -w "%{http_code}" --max-time "$TIMEOUT" "$metrics_url" 2>/dev/null || echo "000")

    if [[ "$http_code" == "200" ]]; then
        success "Metrics endpoint: Available"
    else
        warn "Metrics endpoint: HTTP $http_code"
    fi
}

check_response_time() {
    log "Checking response time..."

    local url="${APP_URL}/actuator/health"
    local start_time
    local end_time
    local duration

    start_time=$(date +%s%N)
    curl -s --max-time "$TIMEOUT" "$url" > /dev/null 2>&1
    end_time=$(date +%s%N)

    duration=$(( (end_time - start_time) / 1000000 ))  # Convert to milliseconds

    if [[ $duration -lt 1000 ]]; then
        success "Response time: ${duration}ms (excellent)"
    elif [[ $duration -lt 2000 ]]; then
        success "Response time: ${duration}ms (good)"
    elif [[ $duration -lt 5000 ]]; then
        warn "Response time: ${duration}ms (acceptable)"
    else
        fail "Response time: ${duration}ms (too slow)"
    fi
}

check_ssl_certificate() {
    if [[ "$APP_URL" =~ ^https:// ]]; then
        log "Checking SSL certificate..."

        local domain=$(echo "$APP_URL" | sed 's|https://||' | sed 's|/.*||')
        local cert_info

        cert_info=$(echo | openssl s_client -servername "$domain" -connect "${domain}:443" 2>/dev/null | openssl x509 -noout -dates 2>/dev/null || echo "")

        if [[ -n "$cert_info" ]]; then
            local expiry_date=$(echo "$cert_info" | grep "notAfter" | cut -d= -f2)
            success "SSL certificate: Valid (expires: $expiry_date)"
        else
            warn "SSL certificate: Could not verify"
        fi
    else
        log "Skipping SSL check (not HTTPS)"
    fi
}

wait_for_health() {
    echo "Waiting for application to become healthy..."

    local attempt=0

    while [[ $attempt -lt $MAX_ATTEMPTS ]]; do
        if curl -f -s "${APP_URL}/actuator/health" > /dev/null 2>&1; then
            success "Application is healthy after $attempt attempts"
            return 0
        fi

        attempt=$((attempt + 1))
        if [[ $attempt -lt $MAX_ATTEMPTS ]]; then
            log "Attempt $attempt/$MAX_ATTEMPTS failed, waiting..."
            sleep 10
        fi
    done

    fail "Application did not become healthy after $MAX_ATTEMPTS attempts"
    return 1
}

################################################################################
# Main Execution
################################################################################

main() {
    echo "=========================================="
    echo "Health Check for West Bethel Motel Booking System"
    echo "=========================================="
    echo "Target: $APP_URL"
    echo "Timeout: ${TIMEOUT}s"
    echo "=========================================="
    echo

    # Wait for application to be available
    wait_for_health || exit 1

    echo
    echo "Running comprehensive health checks..."
    echo

    # Run all checks
    check_actuator_health
    check_readiness
    check_liveness
    check_database_connectivity
    check_redis_connectivity
    check_api_endpoints
    check_response_time
    check_ssl_certificate

    # Summary
    echo
    echo "=========================================="
    echo "Health Check Summary"
    echo "=========================================="
    echo -e "${GREEN}Passed:${NC} $PASSED"
    echo -e "${RED}Failed:${NC} $FAILED"
    echo "=========================================="

    if [[ $FAILED -gt 0 ]]; then
        echo -e "${RED}Health check FAILED${NC}"
        exit 1
    else
        echo -e "${GREEN}All health checks PASSED${NC}"
        exit 0
    fi
}

# Run main function
main
