#!/bin/bash

# Save your GitHub token safely
GITHUB_TOKEN="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"
GITHUB_USER="Pierrequiroulenamasspamouss"
REPO_NAME="Blue_Bridge"
BRANCH="master"
TARGET_DIR="/opt/bluebridge"

# Temp download folder
TMP_DIR=$(mktemp -d)

# Download ZIP
curl -L -H "Authorization: token $GITHUB_TOKEN" \
  -o "$TMP_DIR/repo.zip" \
  "https://api.github.com/repos/$GITHUB_USER/$REPO_NAME/zipball/$BRANCH"

# Extract only the Server/ folder
unzip -q "$TMP_DIR/repo.zip" -d "$TMP_DIR/unzipped"

# Find the exact path (GitHub puts a hash in folder name)
SOURCE_DIR=$(find "$TMP_DIR/unzipped" -type d -name "Server")

# Create target dir if needed
mkdir -p "$TARGET_DIR"

# Copy contents
cp -r "$SOURCE_DIR"/* "$TARGET_DIR"

# Cleanup
rm -rf "$TMP_DIR"
