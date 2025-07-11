#!/bin/bash

# Configuration
USER_DB_PATH="/opt/bluebridge/data/users.sqlite"
DEVICE_TOKENS_DB_PATH="/opt/bluebridge/data/deviceTokens.sqlite"
WELLS_DB_PATH="/opt/bluebridge/data/wells.sqlite"
BACKUP_DIR="/opt/bluebridge/backups"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

USER_BACKUP_FILENAME="users_db_$TIMESTAMP.sqlite"
DEVICE_TOKENS_BACKUP_FILENAME="device_tokens_db_$TIMESTAMP.sqlite"
WELLS_BACKUP_FILENAME="wells_db_$TIMESTAMP.sqlite"

COMPRESSED_USER_BACKUP_FILE="$BACKUP_DIR/$USER_BACKUP_FILENAME.gz"
COMPRESSED_DEVICE_TOKENS_BACKUP_FILE="$BACKUP_DIR/$DEVICE_TOKENS_BACKUP_FILENAME.gz"
COMPRESSED_WELLS_BACKUP_FILE="$BACKUP_DIR/$WELLS_BACKUP_FILENAME.gz"

# Google Drive configuration
RCLONE_REMOTE="gdrive:bluebridge_backups"  # 'bluebridge_backups' is the folder in your Drive
MAX_CLOUD_BACKUPS=10

# Ensure backup directory exists
mkdir -p "$BACKUP_DIR"

# Create local backup
echo "Creating backup of users database..."
cp "$USER_DB_PATH" "$BACKUP_DIR/$USER_BACKUP_FILENAME"
gzip "$BACKUP_DIR/$USER_BACKUP_FILENAME"

echo "Creating backup of device tokens database..."
cp "$DEVICE_TOKENS_DB_PATH" "$BACKUP_DIR/$DEVICE_TOKENS_BACKUP_FILENAME"
gzip "$BACKUP_DIR/$DEVICE_TOKENS_BACKUP_FILENAME"

echo "Creating backup of wells database..."
cp "$WELLS_DB_PATH" "$BACKUP_DIR/$WELLS_BACKUP_FILENAME"
gzip "$BACKUP_DIR/$WELLS_BACKUP_FILENAME"

# Upload to Google Drive
echo "Uploading to Google Drive..."
rclone copy "$COMPRESSED_USER_BACKUP_FILE" "$RCLONE_REMOTE"
rclone copy "$COMPRESSED_DEVICE_TOKENS_BACKUP_FILE" "$RCLONE_REMOTE"
rclone copy "$COMPRESSED_WELLS_BACKUP_FILE" "$RCLONE_REMOTE"
# Clean up old cloud backups (keep only the last $MAX_CLOUD_BACKUPS)
echo "Pruning old backups from Google Drive..."
rclone ls "$RCLONE_REMOTE" | sort -k2 | head -n -"$MAX_CLOUD_BACKUPS" | awk '{print $2}' | while read -r file; do
    echo "Deleting $file from Google Drive..."
    rclone delete "$RCLONE_REMOTE/$file"
done

# Set permissions for local backup
chown -R bluebridge:bluebridge "$BACKUP_DIR"
chmod -R 644 "$BACKUP_DIR"/*.gz

echo "Backup completed: $(basename "$COMPRESSED_USER_BACKUP_FILE")"
echo "Backup completed: $(basename "$COMPRESSED_DEVICE_TOKENS_BACKUP_FILE")"
echo "Backup completed: $(basename "$COMPRESSED_WELLS_BACKUP_FILE")"

# List local backups
echo "Local backups:"
ls -lh "$BACKUP_DIR"
