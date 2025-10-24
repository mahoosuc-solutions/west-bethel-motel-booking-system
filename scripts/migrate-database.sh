#!/bin/bash
set -euo pipefail

# Database Migration Script
# Handles Flyway migrations with backup and rollback capabilities

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Default values
ENVIRONMENT="${1:-staging}"
DRY_RUN="${DRY_RUN:-false}"
BACKUP_BEFORE_MIGRATE="${BACKUP_BEFORE_MIGRATE:-true}"

# Load environment-specific configuration
if [ -f "$PROJECT_ROOT/config/environments/${ENVIRONMENT}.env" ]; then
    source "$PROJECT_ROOT/config/environments/${ENVIRONMENT}.env"
else
    echo -e "${RED}Error: Environment file not found: $PROJECT_ROOT/config/environments/${ENVIRONMENT}.env${NC}"
    exit 1
fi

echo "=================================="
echo "Database Migration Script"
echo "=================================="
echo "Environment: $ENVIRONMENT"
echo "Database: $DB_NAME"
echo "Dry Run: $DRY_RUN"
echo "=================================="

# Function to check database connectivity
check_database_connectivity() {
    echo -e "${YELLOW}Checking database connectivity...${NC}"
    if PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c '\q' 2>/dev/null; then
        echo -e "${GREEN}✓ Database connection successful${NC}"
        return 0
    else
        echo -e "${RED}✗ Failed to connect to database${NC}"
        return 1
    fi
}

# Function to backup database before migration
backup_database() {
    if [ "$BACKUP_BEFORE_MIGRATE" = "true" ]; then
        echo -e "${YELLOW}Creating pre-migration backup...${NC}"
        BACKUP_FILE="backup_${ENVIRONMENT}_$(date +%Y%m%d_%H%M%S).sql"
        PGPASSWORD="$DB_PASSWORD" pg_dump -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" \
            -F custom -f "/tmp/$BACKUP_FILE"

        if [ -f "/tmp/$BACKUP_FILE" ]; then
            echo -e "${GREEN}✓ Backup created: /tmp/$BACKUP_FILE${NC}"

            # Upload to cloud storage if configured
            if [ -n "${GCS_BACKUP_BUCKET:-}" ]; then
                gsutil cp "/tmp/$BACKUP_FILE" "gs://${GCS_BACKUP_BUCKET}/migrations/"
                echo -e "${GREEN}✓ Backup uploaded to GCS${NC}"
            elif [ -n "${S3_BACKUP_BUCKET:-}" ]; then
                aws s3 cp "/tmp/$BACKUP_FILE" "s3://${S3_BACKUP_BUCKET}/migrations/"
                echo -e "${GREEN}✓ Backup uploaded to S3${NC}"
            fi
        else
            echo -e "${RED}✗ Failed to create backup${NC}"
            exit 1
        fi
    fi
}

# Function to get current migration status
get_migration_status() {
    echo -e "${YELLOW}Getting current migration status...${NC}"
    cd "$PROJECT_ROOT"
    mvn flyway:info \
        -Dflyway.url="jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}" \
        -Dflyway.user="$DB_USER" \
        -Dflyway.password="$DB_PASSWORD" \
        -Dflyway.locations="filesystem:src/main/resources/db/migration"
}

# Function to validate migrations
validate_migrations() {
    echo -e "${YELLOW}Validating migrations...${NC}"
    cd "$PROJECT_ROOT"
    if mvn flyway:validate \
        -Dflyway.url="jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}" \
        -Dflyway.user="$DB_USER" \
        -Dflyway.password="$DB_PASSWORD" \
        -Dflyway.locations="filesystem:src/main/resources/db/migration"; then
        echo -e "${GREEN}✓ Migration validation successful${NC}"
        return 0
    else
        echo -e "${RED}✗ Migration validation failed${NC}"
        return 1
    fi
}

# Function to run migrations
run_migrations() {
    echo -e "${YELLOW}Running database migrations...${NC}"
    cd "$PROJECT_ROOT"

    if [ "$DRY_RUN" = "true" ]; then
        echo -e "${YELLOW}DRY RUN MODE - Migrations will not be applied${NC}"
        mvn flyway:info \
            -Dflyway.url="jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}" \
            -Dflyway.user="$DB_USER" \
            -Dflyway.password="$DB_PASSWORD" \
            -Dflyway.locations="filesystem:src/main/resources/db/migration"
    else
        if mvn flyway:migrate \
            -Dflyway.url="jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}" \
            -Dflyway.user="$DB_USER" \
            -Dflyway.password="$DB_PASSWORD" \
            -Dflyway.locations="filesystem:src/main/resources/db/migration" \
            -Dflyway.outOfOrder=false \
            -Dflyway.validateOnMigrate=true; then
            echo -e "${GREEN}✓ Migrations completed successfully${NC}"
            return 0
        else
            echo -e "${RED}✗ Migration failed${NC}"
            return 1
        fi
    fi
}

# Function to verify migration success
verify_migration() {
    echo -e "${YELLOW}Verifying migration...${NC}"
    cd "$PROJECT_ROOT"

    # Check schema version
    SCHEMA_VERSION=$(PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" \
        -t -c "SELECT MAX(installed_rank) FROM flyway_schema_history;" | xargs)

    echo -e "${GREEN}✓ Current schema version: $SCHEMA_VERSION${NC}"

    # Run database health checks
    if PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" \
        -c "SELECT COUNT(*) FROM rooms;" >/dev/null 2>&1; then
        echo -e "${GREEN}✓ Database health check passed${NC}"
        return 0
    else
        echo -e "${RED}✗ Database health check failed${NC}"
        return 1
    fi
}

# Main execution
main() {
    # Check database connectivity
    if ! check_database_connectivity; then
        echo -e "${RED}Cannot proceed without database connection${NC}"
        exit 1
    fi

    # Get current status
    get_migration_status

    # Validate migrations
    if ! validate_migrations; then
        echo -e "${RED}Migration validation failed. Please fix migration files.${NC}"
        exit 1
    fi

    # Backup database
    backup_database

    # Run migrations
    if run_migrations; then
        # Verify migration
        verify_migration
        echo -e "${GREEN}=================================="
        echo -e "Migration completed successfully!"
        echo -e "==================================${NC}"
        exit 0
    else
        echo -e "${RED}=================================="
        echo -e "Migration failed!"
        echo -e "==================================${NC}"
        echo -e "${YELLOW}To rollback, restore from backup:${NC}"
        echo "  ./scripts/restore-database.sh $ENVIRONMENT /tmp/backup_*.sql"
        exit 1
    fi
}

# Handle script arguments
case "${1:-}" in
    --help|-h)
        echo "Usage: $0 [ENVIRONMENT] [OPTIONS]"
        echo ""
        echo "Arguments:"
        echo "  ENVIRONMENT    Target environment (dev, staging, production)"
        echo ""
        echo "Environment Variables:"
        echo "  DRY_RUN=true              Run in dry-run mode"
        echo "  BACKUP_BEFORE_MIGRATE=false  Skip pre-migration backup"
        echo ""
        echo "Examples:"
        echo "  $0 staging"
        echo "  DRY_RUN=true $0 production"
        exit 0
        ;;
    *)
        main
        ;;
esac
