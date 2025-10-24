#!/bin/bash

################################################################################
# Database Restore Script for West Bethel Motel Booking System
# Restore database from backup
################################################################################

set -euo pipefail

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
BACKUP_DIR="${BACKUP_DIR:-${PROJECT_ROOT}/backups}"

ENVIRONMENT="${1:-}"
BACKUP_FILE="${2:-}"

################################################################################
# Helper Functions
################################################################################

log() {
    echo -e "${BLUE}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $*"
}

success() {
    echo -e "${GREEN}[SUCCESS]${NC} $*"
}

error() {
    echo -e "${RED}[ERROR]${NC} $*"
}

warn() {
    echo -e "${YELLOW}[WARNING]${NC} $*"
}

usage() {
    cat << EOF
Usage: $0 ENVIRONMENT BACKUP_FILE

Restore database from a backup file.

Arguments:
    ENVIRONMENT    Target environment (dev|staging|production)
    BACKUP_FILE    Path to backup file or backup filename in backup directory

Environment Variables:
    DB_HOST        Database host (optional, defaults from environment)
    DB_PORT        Database port (optional, defaults to 5432)
    DB_NAME        Database name (optional, defaults from environment)
    DB_USER        Database user (optional, defaults from environment)
    DB_PASSWORD    Database password (required)
    BACKUP_DIR     Backup directory (default: PROJECT_ROOT/backups)

Examples:
    # Restore from a specific backup file
    $0 staging /path/to/backup.sql.gz

    # Restore from backup directory
    $0 production motel_booking_20240101_120000.sql.gz

EOF
    exit 1
}

validate_inputs() {
    if [[ -z "$ENVIRONMENT" ]]; then
        error "Environment is required"
        usage
    fi

    if [[ -z "$BACKUP_FILE" ]]; then
        error "Backup file is required"
        usage
    fi

    # Validate environment
    case "$ENVIRONMENT" in
        dev|staging|production)
            log "Environment: $ENVIRONMENT"
            ;;
        *)
            error "Invalid environment: $ENVIRONMENT"
            usage
            ;;
    esac
}

load_db_config() {
    log "Loading database configuration for $ENVIRONMENT..."

    # Set defaults based on environment
    case "$ENVIRONMENT" in
        dev)
            DB_HOST="${DB_HOST:-localhost}"
            DB_PORT="${DB_PORT:-5432}"
            DB_NAME="${DB_NAME:-motel_booking_dev}"
            DB_USER="${DB_USER:-postgres}"
            ;;
        staging)
            DB_HOST="${DB_HOST:-staging-db.internal}"
            DB_PORT="${DB_PORT:-5432}"
            DB_NAME="${DB_NAME:-motel_booking_staging}"
            DB_USER="${DB_USER:-moteluser}"
            ;;
        production)
            DB_HOST="${DB_HOST:-production-db.internal}"
            DB_PORT="${DB_PORT:-5432}"
            DB_NAME="${DB_NAME:-motel_booking}"
            DB_USER="${DB_USER:-moteluser}"
            ;;
    esac

    if [[ -z "${DB_PASSWORD:-}" ]]; then
        error "DB_PASSWORD environment variable is required"
        exit 1
    fi

    success "Database configuration loaded"
}

confirm_restore() {
    warn "=========================================="
    warn "DATABASE RESTORE WARNING"
    warn "=========================================="
    warn "Environment: $ENVIRONMENT"
    warn "Database: $DB_NAME"
    warn "Host: $DB_HOST"
    warn "Backup File: $BACKUP_FILE"
    warn "=========================================="
    warn "This will OVERWRITE the current database!"
    warn "All current data will be LOST!"
    warn "=========================================="

    if [[ "$ENVIRONMENT" == "production" ]]; then
        error "PRODUCTION DATABASE RESTORE!"
        echo -n "Type 'RESTORE PRODUCTION' to confirm: "
        read -r confirmation
        if [[ "$confirmation" != "RESTORE PRODUCTION" ]]; then
            error "Restore cancelled"
            exit 1
        fi
    else
        echo -n "Type 'RESTORE' to confirm: "
        read -r confirmation
        if [[ "$confirmation" != "RESTORE" ]]; then
            error "Restore cancelled"
            exit 1
        fi
    fi

    success "Restore confirmed"
}

locate_backup_file() {
    log "Locating backup file..."

    # Check if backup file is an absolute path
    if [[ -f "$BACKUP_FILE" ]]; then
        BACKUP_FILE_PATH="$BACKUP_FILE"
    # Check in backup directory
    elif [[ -f "${BACKUP_DIR}/${BACKUP_FILE}" ]]; then
        BACKUP_FILE_PATH="${BACKUP_DIR}/${BACKUP_FILE}"
    # Check in environment-specific backup directory
    elif [[ -f "${BACKUP_DIR}/${ENVIRONMENT}/${BACKUP_FILE}" ]]; then
        BACKUP_FILE_PATH="${BACKUP_DIR}/${ENVIRONMENT}/${BACKUP_FILE}"
    else
        error "Backup file not found: $BACKUP_FILE"
        error "Searched in:"
        error "  - $BACKUP_FILE"
        error "  - ${BACKUP_DIR}/${BACKUP_FILE}"
        error "  - ${BACKUP_DIR}/${ENVIRONMENT}/${BACKUP_FILE}"
        exit 1
    fi

    success "Backup file found: $BACKUP_FILE_PATH"
}

backup_current_database() {
    log "Creating safety backup of current database..."

    local safety_backup="${BACKUP_DIR}/${ENVIRONMENT}/safety-backup-$(date +%Y%m%d-%H%M%S).sql.gz"
    mkdir -p "$(dirname "$safety_backup")"

    export PGPASSWORD="$DB_PASSWORD"

    if pg_dump -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" \
        --no-owner --no-privileges | gzip > "$safety_backup"; then
        success "Safety backup created: $safety_backup"
    else
        error "Failed to create safety backup"
        exit 1
    fi

    unset PGPASSWORD
}

restore_database() {
    log "Restoring database from backup..."

    export PGPASSWORD="$DB_PASSWORD"

    # Terminate existing connections
    log "Terminating existing database connections..."
    psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres << EOF
SELECT pg_terminate_backend(pid)
FROM pg_stat_activity
WHERE datname = '$DB_NAME' AND pid <> pg_backend_pid();
EOF

    # Drop and recreate database
    log "Dropping and recreating database..."
    dropdb -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" "$DB_NAME" || {
        error "Failed to drop database"
        exit 1
    }

    createdb -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" "$DB_NAME" || {
        error "Failed to create database"
        exit 1
    }

    # Restore from backup
    log "Restoring data from backup file..."
    if [[ "$BACKUP_FILE_PATH" =~ \.gz$ ]]; then
        # Gzipped backup
        gunzip -c "$BACKUP_FILE_PATH" | psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" || {
            error "Failed to restore database from gzipped backup"
            exit 1
        }
    else
        # Plain SQL backup
        psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" < "$BACKUP_FILE_PATH" || {
            error "Failed to restore database from backup"
            exit 1
        }
    fi

    unset PGPASSWORD

    success "Database restored successfully"
}

verify_restore() {
    log "Verifying restored database..."

    export PGPASSWORD="$DB_PASSWORD"

    # Check table count
    local table_count
    table_count=$(psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -c \
        "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public';")

    log "Number of tables: $table_count"

    # Check if key tables exist
    local tables=("users" "rooms" "bookings" "payments")
    local missing_tables=0

    for table in "${tables[@]}"; do
        local exists
        exists=$(psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -c \
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = '$table';")

        if [[ "$exists" -eq 1 ]]; then
            log "Table '$table' exists"
        else
            warn "Table '$table' not found"
            missing_tables=$((missing_tables + 1))
        fi
    done

    unset PGPASSWORD

    if [[ $missing_tables -eq 0 ]]; then
        success "Database verification completed successfully"
    else
        warn "Some expected tables are missing ($missing_tables)"
        warn "This may be expected if the backup is from a different schema version"
    fi
}

main() {
    log "=========================================="
    log "Database Restore Process"
    log "=========================================="

    validate_inputs
    load_db_config
    locate_backup_file
    confirm_restore
    backup_current_database
    restore_database
    verify_restore

    success "=========================================="
    success "Database restore completed successfully!"
    success "Environment: $ENVIRONMENT"
    success "Database: $DB_NAME"
    success "Restored from: $BACKUP_FILE_PATH"
    success "=========================================="
}

main
