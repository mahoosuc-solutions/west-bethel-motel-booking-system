#!/bin/bash

################################################################################
# Deployment Script for West Bethel Motel Booking System
# This script automates the deployment process to Kubernetes or ECS
################################################################################

set -euo pipefail

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Script configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
LOG_FILE="${PROJECT_ROOT}/deployment-$(date +%Y%m%d-%H%M%S).log"

# Default values
ENVIRONMENT="${ENVIRONMENT:-staging}"
ORCHESTRATOR="${ORCHESTRATOR:-kubernetes}"
IMAGE_TAG="${IMAGE_TAG:-latest}"
NAMESPACE="${NAMESPACE:-$ENVIRONMENT}"
REPLICAS="${REPLICAS:-2}"
DRY_RUN="${DRY_RUN:-false}"
SKIP_TESTS="${SKIP_TESTS:-false}"
SKIP_MIGRATION="${SKIP_MIGRATION:-false}"

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

Deploy West Bethel Motel Booking System to cloud infrastructure.

OPTIONS:
    -e, --environment ENV       Target environment (dev|staging|production) [default: staging]
    -o, --orchestrator TYPE     Orchestration platform (kubernetes|ecs) [default: kubernetes]
    -t, --tag TAG              Docker image tag [default: latest]
    -n, --namespace NAMESPACE  Kubernetes namespace [default: environment name]
    -r, --replicas COUNT       Number of replicas [default: 2]
    -d, --dry-run              Perform dry run without actual deployment
    --skip-tests               Skip pre-deployment tests
    --skip-migration           Skip database migration
    -h, --help                 Show this help message

EXAMPLES:
    # Deploy to staging
    $0 --environment staging --tag v1.2.3

    # Deploy to production with 5 replicas
    $0 -e production -r 5 -t v1.2.3

    # Dry run to production
    $0 -e production --dry-run

EOF
    exit 0
}

check_prerequisites() {
    log "Checking prerequisites..."

    local required_tools=("kubectl" "docker" "curl" "jq")

    if [[ "$ORCHESTRATOR" == "ecs" ]]; then
        required_tools+=("aws")
    fi

    for tool in "${required_tools[@]}"; do
        if ! command -v "$tool" &> /dev/null; then
            error "$tool is not installed or not in PATH"
            exit 1
        fi
    done

    success "All prerequisites met"
}

validate_environment() {
    log "Validating environment: $ENVIRONMENT"

    case "$ENVIRONMENT" in
        dev|staging|production)
            success "Environment validated: $ENVIRONMENT"
            ;;
        *)
            error "Invalid environment: $ENVIRONMENT"
            error "Must be one of: dev, staging, production"
            exit 1
            ;;
    esac

    # Verify cluster connectivity
    if [[ "$ORCHESTRATOR" == "kubernetes" ]]; then
        if ! kubectl cluster-info &> /dev/null; then
            error "Cannot connect to Kubernetes cluster"
            exit 1
        fi
        success "Connected to Kubernetes cluster"
    elif [[ "$ORCHESTRATOR" == "ecs" ]]; then
        if ! aws sts get-caller-identity &> /dev/null; then
            error "Cannot authenticate with AWS"
            exit 1
        fi
        success "Connected to AWS"
    fi
}

run_pre_deployment_tests() {
    if [[ "$SKIP_TESTS" == "true" ]]; then
        warn "Skipping pre-deployment tests"
        return 0
    fi

    log "Running pre-deployment tests..."

    cd "$PROJECT_ROOT"

    # Run unit tests
    log "Running unit tests..."
    if [[ "$DRY_RUN" == "false" ]]; then
        mvn test -Dtest=!*IntegrationTest,!*IT || {
            error "Unit tests failed"
            exit 1
        }
    else
        log "DRY RUN: Would run unit tests"
    fi

    success "Pre-deployment tests passed"
}

build_and_push_image() {
    log "Building and pushing Docker image..."

    local image_name="${DOCKER_REGISTRY:-ghcr.io}/westbethel/motel-booking-system"
    local full_image="${image_name}:${IMAGE_TAG}"

    cd "$PROJECT_ROOT"

    if [[ "$DRY_RUN" == "false" ]]; then
        # Build application
        log "Building application..."
        mvn clean package -DskipTests -Pprod

        # Build Docker image
        log "Building Docker image: $full_image"
        docker build -f Dockerfile.production -t "$full_image" .

        # Push to registry
        log "Pushing image to registry..."
        docker push "$full_image"

        success "Image built and pushed: $full_image"
    else
        log "DRY RUN: Would build and push $full_image"
    fi

    echo "$full_image"
}

run_database_migration() {
    if [[ "$SKIP_MIGRATION" == "true" ]]; then
        warn "Skipping database migration"
        return 0
    fi

    log "Running database migration..."

    if [[ "$DRY_RUN" == "false" ]]; then
        # Backup database first
        "${SCRIPT_DIR}/backup-database.sh" "$ENVIRONMENT" "pre-deployment-$(date +%Y%m%d-%H%M%S)" || {
            error "Database backup failed"
            exit 1
        }

        # Run migration
        "${SCRIPT_DIR}/migrate-database.sh" "$ENVIRONMENT" || {
            error "Database migration failed"
            exit 1
        }

        success "Database migration completed"
    else
        log "DRY RUN: Would run database migration"
    fi
}

deploy_to_kubernetes() {
    local image=$1

    log "Deploying to Kubernetes..."
    log "Namespace: $NAMESPACE"
    log "Image: $image"
    log "Replicas: $REPLICAS"

    cd "$PROJECT_ROOT/k8s"

    if [[ "$DRY_RUN" == "false" ]]; then
        # Create namespace if it doesn't exist
        kubectl create namespace "$NAMESPACE" --dry-run=client -o yaml | kubectl apply -f -

        # Apply configurations
        kubectl apply -f namespace.yaml -n "$NAMESPACE"
        kubectl apply -f configmap.yaml -n "$NAMESPACE"
        kubectl apply -f secret.yaml -n "$NAMESPACE"
        kubectl apply -f serviceaccount.yaml -n "$NAMESPACE"

        # Update deployment with new image
        kubectl set image deployment/motel-booking-app \
            motel-booking-app="$image" \
            -n "$NAMESPACE"

        # Scale deployment
        kubectl scale deployment/motel-booking-app \
            --replicas="$REPLICAS" \
            -n "$NAMESPACE"

        # Apply other resources
        kubectl apply -f service.yaml -n "$NAMESPACE"
        kubectl apply -f ingress.yaml -n "$NAMESPACE"
        kubectl apply -f hpa.yaml -n "$NAMESPACE"
        kubectl apply -f pdb.yaml -n "$NAMESPACE"
        kubectl apply -f networkpolicy.yaml -n "$NAMESPACE"

        # Wait for rollout
        log "Waiting for deployment to complete..."
        kubectl rollout status deployment/motel-booking-app -n "$NAMESPACE" --timeout=10m || {
            error "Deployment rollout failed"
            warn "Rolling back..."
            kubectl rollout undo deployment/motel-booking-app -n "$NAMESPACE"
            exit 1
        }

        success "Deployment completed successfully"
    else
        log "DRY RUN: Would deploy to Kubernetes namespace $NAMESPACE"
    fi
}

deploy_to_ecs() {
    local image=$1

    log "Deploying to ECS..."
    error "ECS deployment not yet fully implemented"
    error "Please use Kubernetes deployment or implement ECS-specific logic"
    exit 1
}

verify_deployment() {
    log "Verifying deployment..."

    if [[ "$DRY_RUN" == "true" ]]; then
        log "DRY RUN: Would verify deployment"
        return 0
    fi

    sleep 30  # Wait for pods to stabilize

    if [[ "$ORCHESTRATOR" == "kubernetes" ]]; then
        # Check pod status
        log "Checking pod status..."
        kubectl get pods -n "$NAMESPACE" -l app=motel-booking-app

        # Get deployment info
        local ready_replicas
        ready_replicas=$(kubectl get deployment motel-booking-app -n "$NAMESPACE" -o jsonpath='{.status.readyReplicas}')

        if [[ "$ready_replicas" -ge "$REPLICAS" ]]; then
            success "All replicas are ready ($ready_replicas/$REPLICAS)"
        else
            error "Not all replicas are ready ($ready_replicas/$REPLICAS)"
            exit 1
        fi
    fi
}

run_smoke_tests() {
    log "Running smoke tests..."

    if [[ "$DRY_RUN" == "true" ]]; then
        log "DRY RUN: Would run smoke tests"
        return 0
    fi

    # Get application URL based on environment
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
            error "Smoke tests failed"
            exit 1
        }
    else
        warn "Smoke test script not found, skipping..."
    fi

    success "Smoke tests passed"
}

cleanup() {
    log "Cleaning up temporary files..."
    # Add cleanup logic here if needed
}

main() {
    log "=========================================="
    log "West Bethel Motel Booking System Deployment"
    log "=========================================="
    log "Environment: $ENVIRONMENT"
    log "Orchestrator: $ORCHESTRATOR"
    log "Image Tag: $IMAGE_TAG"
    log "Namespace: $NAMESPACE"
    log "Replicas: $REPLICAS"
    log "Dry Run: $DRY_RUN"
    log "=========================================="

    # Deployment steps
    check_prerequisites
    validate_environment
    run_pre_deployment_tests

    local image
    image=$(build_and_push_image)

    run_database_migration

    if [[ "$ORCHESTRATOR" == "kubernetes" ]]; then
        deploy_to_kubernetes "$image"
    elif [[ "$ORCHESTRATOR" == "ecs" ]]; then
        deploy_to_ecs "$image"
    else
        error "Unknown orchestrator: $ORCHESTRATOR"
        exit 1
    fi

    verify_deployment
    run_smoke_tests
    cleanup

    success "=========================================="
    success "Deployment completed successfully!"
    success "Environment: $ENVIRONMENT"
    success "Image: $image"
    success "=========================================="
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -e|--environment)
            ENVIRONMENT="$2"
            shift 2
            ;;
        -o|--orchestrator)
            ORCHESTRATOR="$2"
            shift 2
            ;;
        -t|--tag)
            IMAGE_TAG="$2"
            shift 2
            ;;
        -n|--namespace)
            NAMESPACE="$2"
            shift 2
            ;;
        -r|--replicas)
            REPLICAS="$2"
            shift 2
            ;;
        -d|--dry-run)
            DRY_RUN="true"
            shift
            ;;
        --skip-tests)
            SKIP_TESTS="true"
            shift
            ;;
        --skip-migration)
            SKIP_MIGRATION="true"
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

# Run main deployment
trap cleanup EXIT
main
