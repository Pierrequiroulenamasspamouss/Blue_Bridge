#!/bin/bash

# Configuration
DB_PATH="/opt/bluebridge/database.sqlite"
BACKUP_DIR="/opt/bluebridge/backups"
MAX_BACKUPS=7  # Keep a week's worth of backups
DATE=$(date +%Y%m%d_%H%M%S)

# Ensure backup directory exists
mkdir -p "$BACKUP_DIR"

# Create backup
echo "Creating backup of database..."
cp "$DB_PATH" "$BACKUP_DIR/database_$DATE.sqlite"
gzip "$BACKUP_DIR/database_$DATE.sqlite"

# Remove old backups (keep only the last MAX_BACKUPS files)
echo "Cleaning up old backups..."
ls -t "$BACKUP_DIR"/database_*.sqlite.gz | tail -n +$((MAX_BACKUPS + 1)) | xargs -r rm

# Set proper permissions
chown -R bluebridge:bluebridge "$BACKUP_DIR"
chmod -R 644 "$BACKUP_DIR"/*.gz

echo "Backup completed: database_$DATE.sqlite.gz"

# List current backups
echo "Current backups:"
ls -lh "$BACKUP_DIR" 