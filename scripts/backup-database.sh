#!/bin/bash
set -euo pipefail

# Automated Database Backup Script
# Creates compressed backups and uploads to cloud storage

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Arguments
ENVIRONMENT="${1:-production}"
BACKUP_TYPE="${2:-scheduled}"  # scheduled, pre-deployment, manual

# Load environment configuration
if [ -f "$PROJECT_ROOT/config/environments/${ENVIRONMENT}.env" ]; then
    source "$PROJECT_ROOT/config/environments/${ENVIRONMENT}.env"
else
    echo -e "${RED}Error: Environment file not found${NC}"
    exit 1
fi

# Backup configuration
BACKUP_DIR="${BACKUP_DIR:-/tmp/backups}"
RETENTION_DAYS="${RETENTION_DAYS:-30}"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="${ENVIRONMENT}_${BACKUP_TYPE}_${TIMESTAMP}.backup"
BACKUP_PATH="${BACKUP_DIR}/${BACKUP_FILE}"

echo "=================================="
echo "Database Backup Script"
echo "=================================="
echo "Environment: $ENVIRONMENT"
echo "Type: $BACKUP_TYPE"
echo "Database: $DB_NAME"
echo "Timestamp: $TIMESTAMP"
echo "=================================="

# Create backup directory
mkdir -p "$BACKUP_DIR"

# Function to create backup
create_backup() {
    echo -e "${YELLOW}Creating database backup...${NC}"

    # Use pg_dump with custom format for better compression and restore options
    if PGPASSWORD="$DB_PASSWORD" pg_dump \
        -h "$DB_HOST" \
        -p "$DB_PORT" \
        -U "$DB_USER" \
        -d "$DB_NAME" \
        -F custom \
        -b \
        -v \
        -f "$BACKUP_PATH"; then
        echo -e "${GREEN}✓ Backup created successfully: $BACKUP_PATH${NC}"

        # Get backup size
        BACKUP_SIZE=$(du -h "$BACKUP_PATH" | cut -f1)
        echo -e "${GREEN}  Size: $BACKUP_SIZE${NC}"
        return 0
    else
        echo -e "${RED}✗ Backup creation failed${NC}"
        return 1
    fi
}

# Function to verify backup
verify_backup() {
    echo -e "${YELLOW}Verifying backup integrity...${NC}"

    # List backup contents to verify
    if pg_restore --list "$BACKUP_PATH" > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Backup verification successful${NC}"

        # Count tables in backup
        TABLE_COUNT=$(pg_restore --list "$BACKUP_PATH" | grep -c "TABLE DATA" || true)
        echo -e "${GREEN}  Tables backed up: $TABLE_COUNT${NC}"
        return 0
    else
        echo -e "${RED}✗ Backup verification failed${NC}"
        return 1
    fi
}

# Function to upload to cloud storage
upload_to_cloud() {
    echo -e "${YELLOW}Uploading backup to cloud storage...${NC}"

    # GCS Upload
    if [ -n "${GCS_BACKUP_BUCKET:-}" ]; then
        if gsutil cp "$BACKUP_PATH" "gs://${GCS_BACKUP_BUCKET}/database-backups/${ENVIRONMENT}/${BACKUP_FILE}"; then
            echo -e "${GREEN}✓ Backup uploaded to GCS${NC}"
            gsutil ls -lh "gs://${GCS_BACKUP_BUCKET}/database-backups/${ENVIRONMENT}/${BACKUP_FILE}"
        else
            echo -e "${RED}✗ GCS upload failed${NC}"
            return 1
        fi
    fi

    # AWS S3 Upload
    if [ -n "${S3_BACKUP_BUCKET:-}" ]; then
        if aws s3 cp "$BACKUP_PATH" "s3://${S3_BACKUP_BUCKET}/database-backups/${ENVIRONMENT}/${BACKUP_FILE}"; then
            echo -e "${GREEN}✓ Backup uploaded to S3${NC}"
            aws s3 ls --human-readable "s3://${S3_BACKUP_BUCKET}/database-backups/${ENVIRONMENT}/${BACKUP_FILE}"
        else
            echo -e "${RED}✗ S3 upload failed${NC}"
            return 1
        fi
    fi
}

# Function to cleanup old backups
cleanup_old_backups() {
    echo -e "${YELLOW}Cleaning up old backups...${NC}"

    # Local cleanup
    find "$BACKUP_DIR" -name "${ENVIRONMENT}_*.backup" -type f -mtime +${RETENTION_DAYS} -delete
    echo -e "${GREEN}✓ Local backups older than ${RETENTION_DAYS} days deleted${NC}"

    # GCS cleanup
    if [ -n "${GCS_BACKUP_BUCKET:-}" ]; then
        CUTOFF_DATE=$(date -d "${RETENTION_DAYS} days ago" +%Y-%m-%d)
        gsutil ls -l "gs://${GCS_BACKUP_BUCKET}/database-backups/${ENVIRONMENT}/" | \
            awk -v cutoff="$CUTOFF_DATE" '$2 < cutoff {print $3}' | \
            xargs -r gsutil rm
        echo -e "${GREEN}✓ Old GCS backups cleaned up${NC}"
    fi

    # S3 cleanup with lifecycle policy (preferred)
    if [ -n "${S3_BACKUP_BUCKET:-}" ]; then
        echo -e "${YELLOW}  S3 cleanup handled by lifecycle policy${NC}"
    fi
}

# Function to create backup metadata
create_metadata() {
    METADATA_FILE="${BACKUP_PATH}.json"

    cat > "$METADATA_FILE" << EOF
{
  "environment": "$ENVIRONMENT",
  "backup_type": "$BACKUP_TYPE",
  "timestamp": "$TIMESTAMP",
  "database_name": "$DB_NAME",
  "database_host": "$DB_HOST",
  "backup_file": "$BACKUP_FILE",
  "backup_size": "$(stat -f%z "$BACKUP_PATH" 2>/dev/null || stat -c%s "$BACKUP_PATH")",
  "created_by": "${USER:-automated}",
  "hostname": "$(hostname)",
  "pg_version": "$(PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -t -c 'SELECT version();' | xargs)"
}
EOF

    echo -e "${GREEN}✓ Metadata file created${NC}"

    # Upload metadata
    if [ -n "${GCS_BACKUP_BUCKET:-}" ]; then
        gsutil cp "$METADATA_FILE" "gs://${GCS_BACKUP_BUCKET}/database-backups/${ENVIRONMENT}/${BACKUP_FILE}.json"
    fi

    if [ -n "${S3_BACKUP_BUCKET:-}" ]; then
        aws s3 cp "$METADATA_FILE" "s3://${S3_BACKUP_BUCKET}/database-backups/${ENVIRONMENT}/${BACKUP_FILE}.json"
    fi
}

# Function to send notification
send_notification() {
    STATUS=$1
    MESSAGE=$2

    if [ -n "${SLACK_WEBHOOK_URL:-}" ]; then
        curl -X POST "$SLACK_WEBHOOK_URL" \
            -H 'Content-Type: application/json' \
            -d "{
                \"text\": \"Database Backup $STATUS\",
                \"blocks\": [{
                    \"type\": \"section\",
                    \"text\": {
                        \"type\": \"mrkdwn\",
                        \"text\": \"*Database Backup $STATUS*\n*Environment:* $ENVIRONMENT\n*Type:* $BACKUP_TYPE\n*Message:* $MESSAGE\"
                    }
                }]
            }" 2>/dev/null || true
    fi
}

# Main execution
main() {
    # Create backup
    if ! create_backup; then
        send_notification "FAILED" "Backup creation failed"
        exit 1
    fi

    # Verify backup
    if ! verify_backup; then
        send_notification "FAILED" "Backup verification failed"
        exit 1
    fi

    # Create metadata
    create_metadata

    # Upload to cloud
    if ! upload_to_cloud; then
        send_notification "WARNING" "Backup created but cloud upload failed"
        exit 1
    fi

    # Cleanup old backups
    cleanup_old_backups

    # Success
    echo -e "${GREEN}=================================="
    echo -e "Backup completed successfully!"
    echo -e "File: $BACKUP_FILE"
    echo -e "==================================${NC}"

    send_notification "SUCCESS" "Backup completed successfully: $BACKUP_FILE"
}

# Run main
main
