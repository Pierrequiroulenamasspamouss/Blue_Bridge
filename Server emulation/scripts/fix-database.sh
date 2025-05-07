#!/bin/bash

# Exit on error
set -e

echo "WellConnect Database Schema Fix Tool"
echo "==================================="

DB_PATH="./data/database.sqlite"

# Check if SQLite is installed
if ! command -v sqlite3 &> /dev/null; then
    echo "Error: sqlite3 command not found. Please install SQLite."
    exit 1
fi

# Check if database file exists
if [ ! -f "$DB_PATH" ]; then
    echo "Error: Database file not found at $DB_PATH"
    exit 1
fi

echo "Checking database schema..."

# Create function to add column if it doesn't exist
add_column_if_not_exists() {
    local table=$1
    local column=$2
    local type=$3
    local default=$4
    
    # Check if column exists
    local column_check=$(sqlite3 "$DB_PATH" "PRAGMA table_info($table);" | grep "$column" || true)
    
    if [ -z "$column_check" ]; then
        if [ -n "$default" ]; then
            echo "Adding missing $column column to $table table with default value..."
            sqlite3 "$DB_PATH" "ALTER TABLE $table ADD COLUMN $column $type DEFAULT $default;"
        else
            echo "Adding missing $column column to $table table..."
            sqlite3 "$DB_PATH" "ALTER TABLE $table ADD COLUMN $column $type;"
        fi
        echo "Column $column added successfully!"
    else
        echo "Column $column already exists in $table table."
    fi
}

# Add all potentially missing columns
add_column_if_not_exists "Users" "deviceTokens" "TEXT" "NULL"
add_column_if_not_exists "Users" "profileImageUrl" "TEXT" "NULL"
add_column_if_not_exists "Users" "bio" "TEXT" "NULL"

# Add registrationDate column without a default value first
add_column_if_not_exists "Users" "registrationDate" "DATETIME" "NULL"

# Then update it with the current timestamp in a separate step
if [[ "$(sqlite3 "$DB_PATH" "SELECT COUNT(*) FROM pragma_table_info('Users') WHERE name='registrationDate';")" -eq "1" ]]; then
    echo "Updating registrationDate with current timestamp..."
    sqlite3 "$DB_PATH" "UPDATE Users SET registrationDate = datetime('now') WHERE registrationDate IS NULL;"
    echo "registrationDate updated successfully!"
fi

add_column_if_not_exists "Users" "accountStatus" "TEXT" "'active'"
add_column_if_not_exists "Users" "notificationPreferences" "TEXT" "NULL"

echo ""
echo "Setting sample device tokens and profile data for testing..."

# Update some users with sample tokens for testing
sqlite3 "$DB_PATH" "UPDATE Users SET 
    deviceTokens = '[\"sample-fcm-token-1\", \"sample-fcm-token-2\"]',
    profileImageUrl = 'https://randomuser.me/api/portraits/men/1.jpg',
    bio = 'Administrator and water management specialist',
    accountStatus = 'active',
    notificationPreferences = '{\"waterAlerts\": true, \"communityUpdates\": true}'
WHERE email = 'pierresluse@gmail.com';"

sqlite3 "$DB_PATH" "UPDATE Users SET 
    deviceTokens = '[\"sample-fcm-token-3\"]',
    profileImageUrl = 'https://randomuser.me/api/portraits/women/2.jpg',
    bio = 'Regular community member',
    accountStatus = 'active',
    notificationPreferences = '{\"waterAlerts\": true, \"communityUpdates\": false}'
WHERE email = 'user@example.com';"

echo "Sample data added for test users."
echo ""
echo "Database schema fix completed!" 