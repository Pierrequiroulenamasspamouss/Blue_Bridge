#!/bin/bash

# Configuration
GITHUB_TOKEN="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"
GITHUB_USER="Pierrequiroulenamasspamouss"
REPO_NAME="Blue_Bridge"
BRANCH="master"
TARGET_DIR="/opt/bluebridge"
REPO_URL="https://${GITHUB_TOKEN}@github.com/${GITHUB_USER}/${REPO_NAME}.git"
SERVER_DIR="Server"

# Create staging directory
STAGING_DIR=$(mktemp -d)
echo "Using staging directory: $STAGING_DIR"

# Clone repository
echo "Cloning repository..."
git clone --branch $BRANCH --single-branch --depth 1 \
          "$REPO_URL" "$STAGING_DIR/repo"

# Verify Server directory exists
if [ ! -d "$STAGING_DIR/repo/$SERVER_DIR" ]; then
    echo "Error: Server directory not found!"
    rm -rf "$STAGING_DIR"
    exit 1
fi

# Backup critical files
echo "Backing up databases and configs..."
mkdir -p "$STAGING_DIR/backup"
cp -f "$TARGET_DIR"/*.sqlite "$STAGING_DIR/backup/" 2>/dev/null || true
cp -f "$TARGET_DIR"/*.env "$STAGING_DIR/backup/" 2>/dev/null || true

# Create protection file for node_modules
if [ -d "$TARGET_DIR/node_modules" ]; then
    echo "Protecting node_modules directory..."
    touch "$TARGET_DIR/node_modules/.protected"
fi

# Sync files with deletion, but protect node_modules
echo "Updating files..."
rsync -av --delete \
    --exclude='*.sqlite' \
    --exclude='*.env' \
    --exclude='.git' \
    --exclude='node_modules' \
    "$STAGING_DIR/repo/$SERVER_DIR/" "$TARGET_DIR/"

# Remove protection file if it exists
if [ -f "$TARGET_DIR/node_modules/.protected" ]; then
    rm "$TARGET_DIR/node_modules/.protected"
fi

# Restore backups
echo "Restoring databases..."
cp -f "$STAGING_DIR/backup"/*.sqlite "$TARGET_DIR/" 2>/dev/null || true
cp -f "$STAGING_DIR/backup"/*.env "$TARGET_DIR/" 2>/dev/null || true

# Set safe permissions
echo "Setting permissions..."
chown -R $(id -u):$(id -g) "$TARGET_DIR" 2>/dev/null || chown -R root:root "$TARGET_DIR"
chmod -R ugo+rwx "$TARGET_DIR"  # Give everyone read, write, and execute permissions
find "$TARGET_DIR" -name "*.sqlite" -exec chmod 666 {} \; # Give read/write to all for sqlite files

# Cleanup
rm -rf "$STAGING_DIR"

# Restart service
echo "Restarting service..."
cd /
systemctl restart bluebridge || service bluebridge restart

echo "Update completed successfully"
exit 0