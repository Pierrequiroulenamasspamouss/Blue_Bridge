#!/bin/bash

# Configuration
GITHUB_TOKEN="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"
GITHUB_USER="Pierrequiroulenamasspamouss"
REPO_NAME="Blue_Bridge"
BRANCH="master"
TARGET_DIR="/opt/bluebridge"
REPO_URL="https://${GITHUB_TOKEN}@github.com/${GITHUB_USER}/${REPO_NAME}.git"
SERVER_DIR="Server"

# Create target dir if needed
mkdir -p "$TARGET_DIR"

# Check if we already have a clone
if [ -d "$TARGET_DIR/.git" ]; then
    echo "Updating existing repository..."
    cd "$TARGET_DIR"
    git fetch origin
    git reset --hard origin/$BRANCH
    git clean -fd
else
    echo "Cloning repository for the first time..."
    git clone --branch $BRANCH --single-branch --depth 1 \
              --filter=blob:none --sparse "$REPO_URL" "$TARGET_DIR"
    cd "$TARGET_DIR"
    git sparse-checkout set "$SERVER_DIR"
fi

# Only copy the Server directory contents
echo "Deploying Server files..."
rsync -a --delete "$TARGET_DIR/$SERVER_DIR/" "$TARGET_DIR/"

# Clean up Git files if you don't need them in production
rm -rf "$TARGET_DIR/.git"

# Notification and restart
if [ "$1" == "-prod" ]; then
    python3 "$TARGET_DIR/scripts/new_update_available.py"
fi

service bluebridge restart