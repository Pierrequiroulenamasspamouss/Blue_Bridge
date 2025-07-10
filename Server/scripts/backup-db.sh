#!/bin/bash

# Configuration
USER_DB_PATH="/opt/bluebridge/database.sqlite"
BACKUP_DIR="/opt/bluebridge/backups"
DATE=$(date +%Y%m%d_%H%M%S)
FILENAME="database_$DATE.sqlite"
BACKUP_FILE="$BACKUP_DIR/$FILENAME"
COMPRESSED_FILE="$BACKUP_FILE.gz"

# Google Drive configuration
RCLONE_REMOTE="gdrive:bluebridge_backups"  # 'bluebridge_backups' is the folder in your Drive
MAX_CLOUD_BACKUPS=10

# Ensure backup directory exists
mkdir -p "$BACKUP_DIR"

# Create local backup
echo "Creating backup of database..."
cp "$USER_DB_PATH" "$BACKUP_FILE"
gzip "$BACKUP_FILE"

# Upload to Google Drive
echo "Uploading to Google Drive..."
rclone copy "$COMPRESSED_FILE" "$RCLONE_REMOTE"

# Clean up old cloud backups (keep only the last $MAX_CLOUD_BACKUPS)
echo "Pruning old backups from Google Drive..."
rclone ls "$RCLONE_REMOTE" | sort -k2 | head -n -"$MAX_CLOUD_BACKUPS" | awk '{print $2}' | while read -r file; do
    echo "Deleting $file from Google Drive..."
    rclone delete "$RCLONE_REMOTE/$file"
done

# Set permissions for local backup
chown -R bluebridge:bluebridge "$BACKUP_DIR"
chmod -R 644 "$BACKUP_DIR"/*.gz

echo "Backup completed: $(basename "$COMPRESSED_FILE")"

# List local backups
echo "Local backups:"
ls -lh "$BACKUP_DIR"
