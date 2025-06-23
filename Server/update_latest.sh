#!/bin/bash

# Configuration
GITHUB_TOKEN="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"
GITHUB_USER="Pierrequiroulenamasspamouss"
REPO_NAME="Blue_Bridge"
BRANCH="master"
TARGET_DIR="/opt/bluebridge"
REPO_URL="https://${GITHUB_TOKEN}@github.com/${GITHUB_USER}/${REPO_NAME}.git"
SERVER_DIR="Server"

# Create a temporary clone location
TMP_CLONE=$(mktemp -d)

# Clone fresh copy (shallow, sparse)
echo "Cloning repository..."
git clone --branch $BRANCH --single-branch --depth 1 \
          --filter=blob:none --sparse "$REPO_URL" "$TMP_CLONE"

cd "$TMP_CLONE"
git sparse-checkout set "$SERVER_DIR"

# Clean target directory except config files
echo "Preparing target directory..."
mkdir -p "$TARGET_DIR"
find "$TARGET_DIR" -mindepth 1 -maxdepth 1 ! -name '*.env' ! -name '*.json' ! -name 'config*' -exec rm -rf {} +

# Deploy only Server directory contents
echo "Deploying files..."
rsync -a "$TMP_CLONE/$SERVER_DIR/" "$TARGET_DIR/"

# Cleanup
rm -rf "$TMP_CLONE"

# Set permissions
chown -R bluebridge:bluebridge "$TARGET_DIR"
chmod -R 750 "$TARGET_DIR"

# Notification and restart
if [ "$1" == "-prod" ]; then
    python3 "$TARGET_DIR/scripts/new_update_available.py"
fi

service bluebridge restart
echo "Update completed successfully"