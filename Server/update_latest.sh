#!/bin/bash

# Configuration
GITHUB_TOKEN="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"
GITHUB_USER="Pierrequiroulenamasspamouss"
REPO_NAME="Blue_Bridge"
BRANCH="master"
TARGET_DIR="/opt/bluebridge"
REPO_URL="https://${GITHUB_TOKEN}@github.com/${GITHUB_USER}/${REPO_NAME}.git"
SERVER_DIR="Server"

# Create temporary working directory
WORK_DIR=$(mktemp -d)
echo "Using temporary directory: $WORK_DIR"

# Clone fresh copy
echo "Cloning repository..."
git clone --branch $BRANCH --single-branch --depth 1 \
          --filter=blob:none "$REPO_URL" "$WORK_DIR/repo"

# Verify Server directory exists
if [ ! -d "$WORK_DIR/repo/$SERVER_DIR" ]; then
    echo "Error: Server directory not found in repository!"
    rm -rf "$WORK_DIR"
    exit 1
fi

# Backup existing config files
echo "Backing up configuration files..."
mkdir -p "$WORK_DIR/backup"
shopt -s dotglob
cp -r "$TARGET_DIR"/*.env "$WORK_DIR/backup/" 2>/dev/null || true
cp -r "$TARGET_DIR"/*.json "$WORK_DIR/backup/" 2>/dev/null || true
cp -r "$TARGET_DIR"/config* "$WORK_DIR/backup/" 2>/dev/null || true

# Clean target directory
echo "Cleaning target directory..."
find "$TARGET_DIR" -mindepth 1 -delete

# Copy new files
echo "Deploying new version..."
cp -r "$WORK_DIR/repo/$SERVER_DIR"/* "$TARGET_DIR/"

# Restore config files
echo "Restoring configuration files..."
cp -r "$WORK_DIR/backup"/* "$TARGET_DIR/" 2>/dev/null || true

# Set permissions
echo "Setting permissions..."
chown -R bluebridge:bluebridge "$TARGET_DIR"
chmod -R 750 "$TARGET_DIR"

# Cleanup
rm -rf "$WORK_DIR"

# Notification and restart
if [ "$1" == "-prod" ]; then
    echo "Sending update notification..."
    python3 "$TARGET_DIR/scripts/new_update_available.py"
fi

echo "Restarting service..."
service bluebridge restart

echo "Update completed successfully"
exit 0