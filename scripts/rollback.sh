#!/bin/bash

################################################################################
# Rollback Script for West Bethel Motel Booking System
# Emergency rollback to previous deployment version
################################################################################

set -euo pipefail

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
LOG_FILE="${PROJECT_ROOT}/rollback-$(date +%Y%m%d-%H%M%S).log"

ENVIRONMENT="${ENVIRONMENT:-staging}"
NAMESPACE="${NAMESPACE:-$ENVIRONMENT}"
TARGET_VERSION="${TARGET_VERSION:-}"
REASON="${REASON:-Manual rollback}"
DRY_RUN="${DRY_RUN:-false}"

################################################################################
# Helper Functions
################################################################################

log() {
    echo -e "${BLUE}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $*" | tee -a "$LOG_FILE"
}

success() {
    echo -e "${GREEN}[SUCCESS]${NC} $*" | tee -a "$LOG_FILE"
}

error() {
    echo -e "${RED}[ERROR]${NC} $*" | tee -a "$LOG_FILE"
}

warn() {
    echo -e "${YELLOW}[WARNING]${NC} $*" | tee -a "$LOG_FILE"
}

usage() {
    cat << EOF
Usage: $0 [OPTIONS]

Rollback West Bethel Motel Booking System to a previous version.

OPTIONS:
    -e, --environment ENV    Target environment (dev|staging|production) [default: staging]
    -n, --namespace NS       Kubernetes namespace [default: environment name]
    -v, --version VERSION    Target version to rollback to (leave empty for previous)
    -r, --reason REASON      Reason for rollback [default: "Manual rollback"]
    -d, --dry-run            Perform dry run
    -h, --help               Show this help message

EXAMPLES:
    # Rollback to previous deployment
    $0 --environment production --reason "Critical bug found"

    # Rollback to specific version
    $0 -e staging -v v1.2.0

    # Dry run
    $0 -e production --dry-run

EOF
    exit 0
}

confirm_rollback() {
    warn "=========================================="
    warn "ROLLBACK CONFIRMATION"
    warn "=========================================="
    warn "Environment: $ENVIRONMENT"
    warn "Namespace: $NAMESPACE"
    warn "Target Version: ${TARGET_VERSION:-Previous deployment}"
    warn "Reason: $REASON"
    warn "=========================================="

    if [[ "$DRY_RUN" == "true" ]]; then
        log "DRY RUN MODE - No actual changes will be made"
        return 0
    fi

    if [[ "$ENVIRONMENT" == "production" ]]; then
        error "PRODUCTION ROLLBACK REQUESTED!"
        echo -n "Type 'ROLLBACK' to confirm: "
        read -r confirmation
        if [[ "$confirmation" != "ROLLBACK" ]]; then
            error "Rollback cancelled"
            exit 1
        fi
    else
        echo -n "Confirm rollback? (yes/no): "
        read -r confirmation
        if [[ "$confirmation" != "yes" ]]; then
            error "Rollback cancelled"
            exit 1
        fi
    fi

    success "Rollback confirmed"
}

check_prerequisites() {
    log "Checking prerequisites..."

    if ! command -v kubectl &> /dev/null; then
        error "kubectl is not installed"
        exit 1
    fi

    if ! kubectl cluster-info &> /dev/null; then
        error "Cannot connect to Kubernetes cluster"
        exit 1
    fi

    success "Prerequisites check passed"
}

backup_current_state() {
    log "Backing up current state..."

    local backup_dir="${PROJECT_ROOT}/rollback-backups/$(date +%Y%m%d-%H%M%S)"
    mkdir -p "$backup_dir"

    if [[ "$DRY_RUN" == "false" ]]; then
        # Save current deployment state
        kubectl get deployment motel-booking-app -n "$NAMESPACE" -o yaml > "${backup_dir}/deployment.yaml"
        kubectl get pods -n "$NAMESPACE" -l app=motel-booking-app -o yaml > "${backup_dir}/pods.yaml"
        kubectl describe deployment motel-booking-app -n "$NAMESPACE" > "${backup_dir}/deployment-describe.txt"

        # Backup database
        "${SCRIPT_DIR}/backup-database.sh" "$ENVIRONMENT" "pre-rollback-$(date +%Y%m%d-%H%M%S)" || {
            warn "Database backup failed, but continuing with rollback..."
        }

        success "Current state backed up to: $backup_dir"
    else
        log "DRY RUN: Would backup state to $backup_dir"
    fi
}

get_rollback_info() {
    log "Getting rollback information..."

    if [[ -z "$TARGET_VERSION" ]]; then
        log "No specific version provided, will rollback to previous deployment"

        # Get previous revision
        local revision_history
        revision_history=$(kubectl rollout history deployment/motel-booking-app -n "$NAMESPACE")

        log "Deployment history:"
        echo "$revision_history"
    else
        log "Will rollback to version: $TARGET_VERSION"
    fi
}

execute_rollback() {
    log "Executing rollback..."

    if [[ "$DRY_RUN" == "false" ]]; then
        if [[ -z "$TARGET_VERSION" ]]; then
            # Rollback to previous revision
            log "Rolling back to previous deployment..."
            kubectl rollout undo deployment/motel-booking-app -n "$NAMESPACE" || {
                error "Rollback command failed"
                exit 1
            }
        else
            # Rollback to specific version
            log "Rolling back to version: $TARGET_VERSION"
            kubectl set image deployment/motel-booking-app \
                motel-booking-app="${DOCKER_REGISTRY:-ghcr.io}/westbethel/motel-booking-system:$TARGET_VERSION" \
                -n "$NAMESPACE" || {
                error "Failed to set image version"
                exit 1
            }
        fi

        # Wait for rollback to complete
        log "Waiting for rollback to complete..."
        kubectl rollout status deployment/motel-booking-app -n "$NAMESPACE" --timeout=10m || {
            error "Rollback did not complete successfully"
            exit 1
        }

        success "Rollback executed successfully"
    else
        log "DRY RUN: Would execute rollback"
    fi
}

verify_rollback() {
    log "Verifying rollback..."

    if [[ "$DRY_RUN" == "true" ]]; then
        log "DRY RUN: Would verify rollback"
        return 0
    fi

    # Wait for pods to stabilize
    sleep 30

    # Check pod status
    log "Checking pod status..."
    kubectl get pods -n "$NAMESPACE" -l app=motel-booking-app

    # Check deployment status
    local ready_replicas
    ready_replicas=$(kubectl get deployment motel-booking-app -n "$NAMESPACE" -o jsonpath='{.status.readyReplicas}')
    local desired_replicas
    desired_replicas=$(kubectl get deployment motel-booking-app -n "$NAMESPACE" -o jsonpath='{.spec.replicas}')

    if [[ "$ready_replicas" -ge "$desired_replicas" ]]; then
        success "All replicas are ready ($ready_replicas/$desired_replicas)"
    else
        error "Not all replicas are ready ($ready_replicas/$desired_replicas)"
        exit 1
    fi

    # Get application URL
    local app_url
    case "$ENVIRONMENT" in
        production)
            app_url="https://www.westbethelmotel.com"
            ;;
        staging)
            app_url="https://staging.westbethelmotel.com"
            ;;
        *)
            app_url="http://localhost:8080"
            ;;
    esac

    # Health check
    log "Performing health check..."
    local max_attempts=30
    local attempt=0

    while [[ $attempt -lt $max_attempts ]]; do
        if curl -f -s "${app_url}/actuator/health" > /dev/null 2>&1; then
            success "Health check passed!"
            break
        fi

        attempt=$((attempt + 1))
        if [[ $attempt -lt $max_attempts ]]; then
            log "Health check attempt $attempt/$max_attempts failed, retrying..."
            sleep 10
        else
            error "Health check failed after $max_attempts attempts"
            exit 1
        fi
    done

    success "Rollback verification completed"
}

run_smoke_tests() {
    log "Running post-rollback smoke tests..."

    if [[ "$DRY_RUN" == "true" ]]; then
        log "DRY RUN: Would run smoke tests"
        return 0
    fi

    local app_url
    case "$ENVIRONMENT" in
        production)
            app_url="https://www.westbethelmotel.com"
            ;;
        staging)
            app_url="https://staging.westbethelmotel.com"
            ;;
        *)
            app_url="http://localhost:8080"
            ;;
    esac

    if [[ -f "${SCRIPT_DIR}/smoke-test.sh" ]]; then
        "${SCRIPT_DIR}/smoke-test.sh" "$app_url" || {
            warn "Smoke tests failed after rollback"
            error "Manual investigation required"
            exit 1
        }
        success "Smoke tests passed"
    else
        warn "Smoke test script not found"
    fi
}

send_notification() {
    log "Sending rollback notification..."

    # This would integrate with your notification system (Slack, email, etc.)
    warn "ROLLBACK COMPLETED"
    warn "Environment: $ENVIRONMENT"
    warn "Reason: $REASON"
    warn "Time: $(date)"

    # Log incident
    local incident_file="${PROJECT_ROOT}/incidents/rollback-$(date +%Y%m%d-%H%M%S).log"
    mkdir -p "$(dirname "$incident_file")"
    cat > "$incident_file" << EOF
Rollback Incident Report
========================
Date: $(date)
Environment: $ENVIRONMENT
Namespace: $NAMESPACE
Target Version: ${TARGET_VERSION:-Previous deployment}
Reason: $REASON
Status: Completed
Log File: $LOG_FILE
EOF

    success "Incident report saved to: $incident_file"
}

main() {
    log "=========================================="
    log "EMERGENCY ROLLBACK PROCEDURE"
    log "=========================================="

    confirm_rollback
    check_prerequisites
    backup_current_state
    get_rollback_info
    execute_rollback
    verify_rollback
    run_smoke_tests
    send_notification

    success "=========================================="
    success "ROLLBACK COMPLETED SUCCESSFULLY"
    success "Environment: $ENVIRONMENT"
    success "Reason: $REASON"
    success "=========================================="
}

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -e|--environment)
            ENVIRONMENT="$2"
            NAMESPACE="$2"
            shift 2
            ;;
        -n|--namespace)
            NAMESPACE="$2"
            shift 2
            ;;
        -v|--version)
            TARGET_VERSION="$2"
            shift 2
            ;;
        -r|--reason)
            REASON="$2"
            shift 2
            ;;
        -d|--dry-run)
            DRY_RUN="true"
            shift
            ;;
        -h|--help)
            usage
            ;;
        *)
            error "Unknown option: $1"
            usage
            ;;
    esac
done

main
