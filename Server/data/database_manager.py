import sqlite3
import json
from datetime import datetime
from typing import Optional, List, Dict, Any, Union
from dataclasses import dataclass
from pathlib import Path

class DatabaseConfig:
    path: str
    schema: Dict[str, str]  # table_name -> create_table_sql

class DatabaseManager:
    def verify_schema():
        conn = sqlite3.connect('users.sqlite')
        cursor = conn.cursor()

        # Check if table exists
        cursor.execute("SELECT name FROM sqlite_master WHERE type='table' AND name='users'")
        if not cursor.fetchone():
            print("Users table doesn't exist!")
            return

        # Get all columns
        cursor.execute("PRAGMA table_info(users)")
        columns = cursor.fetchall()
        print("Current columns in users table:")
        for col in columns:
            print(f"{col[1]} ({col[2]})")

        conn.close()

    if __name__ == "__main__":
        verify_schema()
    def __init__(self):
        self.databases = {
            'users': DatabaseConfig(
                path='users.sqlite',
                schema={
                    'users': '''
                        CREATE TABLE IF NOT EXISTS users (
                            userId TEXT PRIMARY KEY,
                            email TEXT UNIQUE NOT NULL,
                            password TEXT NOT NULL,
                            firstName TEXT NOT NULL,
                            lastName TEXT NOT NULL,
                            username TEXT UNIQUE,
                            role TEXT NOT NULL DEFAULT 'user',
                            location TEXT,
                            waterNeeds TEXT,
                            lastActive TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            registrationDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            notificationPreferences TEXT,
                            loginToken TEXT UNIQUE,
                            lastLogin TIMESTAMP,
                            phoneNumber TEXT,
                            isWellOwner BOOLEAN DEFAULT 0,
                            themePreference INTEGER DEFAULT 0,
                            createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP, 
                        
                        )
                    '''
                }
            ),
            'deviceTokens': DatabaseConfig(
                path='deviceTokens.sqlite',
                schema={
                    'device_tokens': '''
                        CREATE TABLE IF NOT EXISTS device_tokens (
                            userId TEXT NOT NULL,
                            token TEXT NOT NULL UNIQUE,
                            deviceType TEXT NOT NULL DEFAULT 'android',
                            lastUsed TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            isActive BOOLEAN DEFAULT 1,
                            FOREIGN KEY (userId) REFERENCES users(userId)
                        )
                    '''
                }
            )
        }
        self._initialize_databases()

    def _initialize_databases(self):
        """Initialize all databases and create tables if they don't exist."""
        for db_name, config in self.databases.items():
            db_path = config.path
            print(f"\nInitializing database: {db_path}")

            # Delete existing database file to ensure clean slate
            if Path(db_path).exists():
                print(f"Removing existing database file: {db_path}")
                Path(db_path).unlink()

            with self._get_connection(db_name) as conn:
                cursor = conn.cursor()
                for table_name, schema in config.schema.items():
                    try:
                        cursor.execute(schema)
                        print(f"Created table {table_name} with schema:")
                        print(schema)
                    except sqlite3.Error as e:
                        print(f"Error creating table {table_name}: {e}")
                conn.commit()
    def _get_connection(self, db_name: str) -> sqlite3.Connection:
        """Get a connection to the specified database."""
        if db_name not in self.databases:
            raise ValueError(f"Unknown database: {db_name}")
        return sqlite3.connect(self.databases[db_name].path)

    # User-related methods
    def users(self):
        """Get the users database interface."""
        return UserDatabase(self._get_connection('users'))

    # Well-related methods
    def wells(self):
        """Get the wells database interface."""
        return WellDatabase(self._get_connection('wells'))

    # Device token-related methods
    def deviceTokens(self):
        """Get the device tokens database interface."""
        return DeviceTokenDatabase(self._get_connection('deviceTokens'))

class UserDatabase:
    def __init__(self, conn: sqlite3.Connection):
        self.conn = conn

    def get_user(self, user_id: str) -> Optional[Dict[str, Any]]:
        """Get a user by ID."""
        cursor = self.conn.cursor()
        cursor.execute('SELECT * FROM users WHERE userId = ?', (user_id,))
        row = cursor.fetchone()
        if not row:
            return None
        return dict(zip([col[0] for col in cursor.description], row))
    def get_all_users(self) -> List[Dict[str, Any]]:
        """Get all users."""
        cursor = self.conn.cursor()
        cursor.execute('SELECT * FROM users')
        return [dict(zip([col[0] for col in cursor.description], row)) 
                for row in cursor.fetchall()]
                    
    def get_user_by_email(email):
        try:
            user = db.users().get_user_by_email(email)
            if user:
                # Parse JSON fields
                for field in ['location', 'waterNeeds', 'notificationPreferences']:
                    if field in user and user[field]:
                        try:
                            user[field] = json.loads(user[field])
                        except json.JSONDecodeError:
                            user[field] = None
            return user
        except Exception as e:
            print(f"Error fetching user by email {email}: {str(e)}")
            return None

    def create_user(user_data):
        # Ensure JSON fields are properly stringified
        for field in ['location', 'waterNeeds', 'notificationPreferences']:
            if field in user_data and user_data[field] is not None:
                if not isinstance(user_data[field], str):
                    user_data[field] = json.dumps(user_data[field])
        return db.users().create_user(user_data)
    def update_user(self, user_id: str, updates: Dict[str, Any]) -> bool:
        """Update a user's information."""
        try:
            cursor = self.conn.cursor()
            set_clause = ', '.join([f'{k} = ?' for k in updates.keys()])
            query = f'UPDATE users SET {set_clause} WHERE userId = ?'
            cursor.execute(query, list(updates.values()) + [user_id])
            self.conn.commit()
            return True
        except sqlite3.Error:
            return False

    def delete_user(self, user_id: str) -> bool:
        """Delete a user."""
        try:
            cursor = self.conn.cursor()
            cursor.execute('DELETE FROM users WHERE userId = ?', (user_id,))
            self.conn.commit()
            return True
        except sqlite3.Error:
            return False

class WellDatabase:
    def __init__(self, conn: sqlite3.Connection):
        self.conn = conn

    def get_well(self, well_id: int) -> Optional[Dict[str, Any]]:
        """Get a well by ID."""
        cursor = self.conn.cursor()
        cursor.execute('SELECT * FROM wells WHERE id = ?', (well_id,))
        row = cursor.fetchone()
        if not row:
            return None
        return dict(zip([col[0] for col in cursor.description], row))

    def get_well_by_esp_id(self, esp_id: str) -> Optional[Dict[str, Any]]:
        """Get a well by ESP ID."""
        cursor = self.conn.cursor()
        cursor.execute('SELECT * FROM wells WHERE espId = ?', (esp_id,))
        row = cursor.fetchone()
        if not row:
            return None
        return dict(zip([col[0] for col in cursor.description], row))

    def create_well(self, well_data: Dict[str, Any]) -> bool:
        """Create a new well."""
        try:
            cursor = self.conn.cursor()
            columns = ', '.join(well_data.keys())
            placeholders = ', '.join(['?' for _ in well_data])
            query = f'INSERT INTO wells ({columns}) VALUES ({placeholders})'
            cursor.execute(query, list(well_data.values()))
            self.conn.commit()
            return True
        except sqlite3.Error:
            return False

    def update_well(self, well_id: int, updates: Dict[str, Any]) -> bool:
        """Update a well's information."""
        try:
            cursor = self.conn.cursor()
            set_clause = ', '.join([f'{k} = ?' for k in updates.keys()])
            query = f'UPDATE wells SET {set_clause} WHERE id = ?'
            cursor.execute(query, list(updates.values()) + [well_id])
            self.conn.commit()
            return True
        except sqlite3.Error:
            return False

    def delete_well(self, well_id: int) -> bool:
        """Delete a well."""
        try:
            cursor = self.conn.cursor()
            cursor.execute('DELETE FROM wells WHERE id = ?', (well_id,))
            self.conn.commit()
            return True
        except sqlite3.Error:
            return False

    def get_all_wells(self) -> List[Dict[str, Any]]:
        """Get all wells."""
        cursor = self.conn.cursor()
        cursor.execute('SELECT * FROM wells')
        return [dict(zip([col[0] for col in cursor.description], row)) 
                for row in cursor.fetchall()]

class DeviceTokenDatabase:
    def __init__(self, conn: sqlite3.Connection):
        self.conn = conn

    def get_all_tokens(self) -> List[Dict[str, Any]]:
        """Get all device tokens."""
        cursor = self.conn.cursor()
        cursor.execute('SELECT * FROM device_tokens')
        return [dict(zip([col[0] for col in cursor.description], row)) 
                for row in cursor.fetchall()]

    def get_tokens_by_user(self, user_id: str) -> List[Dict[str, Any]]:
        """Get all device tokens for a user."""
        cursor = self.conn.cursor()
        cursor.execute('SELECT * FROM device_tokens WHERE userId = ?', (user_id,))
        return [dict(zip([col[0] for col in cursor.description], row)) 
                for row in cursor.fetchall()]

    def add_token(self, user_id: str, token: str, device_type: str = 'android') -> bool:
        """Add a new device token."""
        try:
            cursor = self.conn.cursor()
            cursor.execute('''
                INSERT INTO device_tokens (userId, token, deviceType, lastUsed, isActive)
                VALUES (?, ?, ?, ?, ?)
            ''', (user_id, token, device_type, datetime.now().isoformat(), True))
            self.conn.commit()
            return True
        except sqlite3.Error:
            return False

    def update_token(self, user_id: str, old_token: str, new_token: str) -> bool:
        """Update a device token."""
        try:
            cursor = self.conn.cursor()
            cursor.execute('''
                UPDATE device_tokens 
                SET token = ?, lastUsed = ?
                WHERE userId = ? AND token = ?
            ''', (new_token, datetime.now().isoformat(), user_id, old_token))
            self.conn.commit()
            return cursor.rowcount > 0
        except sqlite3.Error:
            return False

    def delete_token(self, user_id: str, token: str) -> bool:
        """Delete a device token."""
        try:
            cursor = self.conn.cursor()
            cursor.execute('DELETE FROM device_tokens WHERE userId = ? AND token = ?', (user_id, token))
            self.conn.commit()
            return cursor.rowcount > 0
        except sqlite3.Error:
            return False

    def verify_token(self, user_id: str, token: str) -> bool:
        """Verify a device token."""
        try:
            cursor = self.conn.cursor()
            cursor.execute('''
                SELECT * FROM device_tokens 
                WHERE userId = ? AND token = ? AND isActive = ?
            ''', (user_id, token, True))
            result = cursor.fetchone()
            if result:
                # Update last used timestamp
                cursor.execute('''
                    UPDATE device_tokens 
                    SET lastUsed = ?
                    WHERE userId = ? AND token = ?
                ''', (datetime.now().isoformat(), user_id, token))
                self.conn.commit()
            return result is not None
        except sqlite3.Error:
            return False

# Example usage:
if __name__ == "__main__":
    db = DatabaseManager()
    
    # Example: Create a user
    user_data = {
        'userId': '123',
        'email': 'test@example.com',
        'password': 'hashed_password',
        'firstName': 'John',
        'lastName': 'Doe',
        'username': 'johndoe'
    }
    db.users().create_user(user_data)
    
    # Example: Create a well
    well_data = {
        'name': 'Test Well',
        'latitude': 12.34,
        'longitude': 56.78,
        'espId': 'ESP123'
    }
    db.wells().create_well(well_data)
    
    # Example: Set device token
    db.deviceTokens().set_token('123', 'device_token_123') 