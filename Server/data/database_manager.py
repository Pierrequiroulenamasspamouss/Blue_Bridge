import sqlite3
import json
from datetime import datetime
from typing import Optional, List, Dict, Any, Union
from dataclasses import dataclass
from pathlib import Path
import uuid
import re
import random

@dataclass
class DatabaseConfig:
    path: str
    schema: Dict[str, str]  # table_name -> create_table_sql

class DatabaseManager:
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
                            lastActive TIMESTAMP,
                            registrationDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            notificationPreferences TEXT,
                            loginToken TEXT UNIQUE,
                            lastLogin TIMESTAMP,
                            phoneNumber TEXT,
                            isWellOwner BOOLEAN DEFAULT 0,
                            themePreference INTEGER DEFAULT 0,
                            createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        )
                    '''
                }
            ),
            'wells': DatabaseConfig(
                path='wells.sqlite',
                schema={
                    'wells': '''
                        CREATE TABLE IF NOT EXISTS wells (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            name TEXT NOT NULL,
                            description TEXT,
                            location TEXT NOT NULL,
                            latitude REAL NOT NULL,
                            longitude REAL NOT NULL,
                            water_level TEXT,
                            water_quality TEXT,
                            status TEXT,
                            owner TEXT,
                            contact_info TEXT,
                            access_info TEXT,
                            notes TEXT,
                            last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            espId TEXT UNIQUE,
                            wellWaterConsumption TEXT,
                            wellWaterType TEXT,
                            wellName TEXT,
                            wellOwner TEXT,
                            wellLocation TEXT,
                            wellCapacity REAL,
                            wellWaterLevel REAL,
                            wellStatus TEXT,
                            waterQuality TEXT,
                            extraData TEXT,
                            lastUpdated TIMESTAMP,
                            ownerId INTEGER
                        )
                    '''
                }
            ),
            'deviceTokens': DatabaseConfig(
                path='deviceTokens.sqlite',
                schema={
                    'device_tokens': '''
                        CREATE TABLE IF NOT EXISTS device_tokens (
                            tokenId TEXT PRIMARY KEY,
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
            try:
                with self._get_connection(db_name) as conn:
                    cursor = conn.cursor()
                    for table_name, schema in config.schema.items():
                        cursor.execute(schema)
                    conn.commit()
            except sqlite3.Error as e:
                print(f"Error initializing database {db_name}: {str(e)}")

    def _get_connection(self, db_name: str) -> sqlite3.Connection:
        """Get a connection to the specified database."""
        if db_name not in self.databases:
            raise ValueError(f"Unknown database: {db_name}")
        conn = sqlite3.connect(self.databases[db_name].path)
        conn.row_factory = sqlite3.Row  # Enable dictionary-like access
        return conn

    def users(self) -> 'UserDatabase':
        """Get the users database interface."""
        return UserDatabase(self._get_connection('users'))

    def wells(self) -> 'WellDatabase':
        """Get the wells database interface."""
        return WellDatabase(self._get_connection('wells'))

    def deviceTokens(self) -> 'DeviceTokenDatabase':
        """Get the device tokens database interface."""
        return DeviceTokenDatabase(self._get_connection('deviceTokens'))

class BaseDatabase:
    """Base class for database operations with common functionality."""

    def __init__(self, conn: sqlite3.Connection):
        self.conn = conn

    def _execute(self, query: str, params: tuple = ()) -> sqlite3.Cursor:
        """Execute a query with error handling."""
        try:
            cursor = self.conn.cursor()
            cursor.execute(query, params)
            return cursor
        except sqlite3.Error as e:
            print(f"Database error: {str(e)}")
            raise

    def _parse_json_fields(self, row: dict, json_fields: List[str]) -> dict:
        """Parse JSON fields in a row."""
        result = dict(row)
        for field in json_fields:
            if field in result and result[field]:
                try:
                    result[field] = json.loads(result[field])
                except json.JSONDecodeError:
                    result[field] = None
        return result

class UserDatabase(BaseDatabase):
    """Handles all user-related database operations."""

    JSON_FIELDS = ['location', 'waterNeeds', 'notificationPreferences']

    def get_user(self, user_id: str) -> Optional[Dict[str, Any]]:
        """Get a user by ID with parsed JSON fields."""
        cursor = self._execute('SELECT * FROM users WHERE userId = ?', (user_id,))
        row = cursor.fetchone()
        return self._parse_json_fields(row, self.JSON_FIELDS) if row else None

    def get_user_by_email(self, email: str) -> Optional[Dict[str, Any]]:
        """Get a user by email with parsed JSON fields."""
        cursor = self._execute('SELECT * FROM users WHERE email = ?', (email.lower().strip(),))
        row = cursor.fetchone()
        return self._parse_json_fields(row, self.JSON_FIELDS) if row else None

    def get_all_users(self) -> List[Dict[str, Any]]:
        """Get all users with parsed JSON fields."""
        cursor = self._execute('SELECT * FROM users')
        return [self._parse_json_fields(row, self.JSON_FIELDS) for row in cursor.fetchall()]

    def create_user(self, user_data: Dict[str, Any]) -> bool:
        """Create a new user with proper JSON serialization."""
        # Ensure required fields are present
        required_fields = ['userId', 'email', 'password', 'firstName', 'lastName']
        if not all(field in user_data for field in required_fields):
            raise ValueError("Missing required user fields")

        # Prepare data with JSON serialization
        prepared_data = user_data.copy()
        for field in self.JSON_FIELDS:
            if field in prepared_data and prepared_data[field] is not None:
                if not isinstance(prepared_data[field], str):
                    prepared_data[field] = json.dumps(prepared_data[field])

        # Generate timestamps if not provided
        if 'createdAt' not in prepared_data:
            prepared_data['createdAt'] = datetime.now().isoformat()
        if 'updatedAt' not in prepared_data:
            prepared_data['updatedAt'] = datetime.now().isoformat()

        # Execute insert
        columns = ', '.join(prepared_data.keys())
        placeholders = ', '.join(['?' for _ in prepared_data])
        query = f'INSERT INTO users ({columns}) VALUES ({placeholders})'

        try:
            self._execute(query, tuple(prepared_data.values()))
            self.conn.commit()
            return True
        except sqlite3.IntegrityError as e:
            print(f"User creation failed (possible duplicate): {str(e)}")
            return False

    def update_user(self, user_id: str, updates: Dict[str, Any]) -> bool:
        """Update a user's information with proper JSON serialization."""
        if not updates:
            return False

        # Prepare updates with JSON serialization
        prepared_updates = updates.copy()
        for field in self.JSON_FIELDS:
            if field in prepared_updates and prepared_updates[field] is not None:
                if not isinstance(prepared_updates[field], str):
                    prepared_updates[field] = json.dumps(prepared_updates[field])

        # Add updated timestamp
        prepared_updates['updatedAt'] = datetime.now().isoformat()

        # Build and execute update query
        set_clause = ', '.join([f'{k} = ?' for k in prepared_updates.keys()])
        query = f'UPDATE users SET {set_clause} WHERE userId = ?'
        params = list(prepared_updates.values()) + [user_id]

        try:
            cursor = self._execute(query, tuple(params))
            self.conn.commit()
            return cursor.rowcount > 0
        except sqlite3.Error as e:
            print(f"User update failed: {str(e)}")
            return False

    def delete_user(self, user_id: str) -> bool:
        """Delete a user by ID."""
        try:
            cursor = self._execute('DELETE FROM users WHERE userId = ?', (user_id,))
            self.conn.commit()
            return cursor.rowcount > 0
        except sqlite3.Error as e:
            print(f"User deletion failed: {str(e)}")
            return False

class WellDatabase(BaseDatabase):
    """Handles all well-related database operations."""

    def get_well(self, well_id: int) -> Optional[Dict[str, Any]]:
        """Get a well by ID."""
        cursor = self._execute('SELECT * FROM wells WHERE id = ?', (well_id,))
        row = cursor.fetchone()
        return dict(row) if row else None

    def get_well_by_esp_id(self, esp_id: str) -> Optional[Dict[str, Any]]:
        """Get a well by ESP ID."""
        cursor = self._execute('SELECT * FROM wells WHERE espId = ?', (esp_id,))
        row = cursor.fetchone()
        return dict(row) if row else None

    def get_all_wells(self) -> List[Dict[str, Any]]:
        """Get all wells."""
        cursor = self._execute('SELECT * FROM wells')
        return [dict(row) for row in cursor.fetchall()]

    def create_well(self, well_data: Dict[str, Any]) -> bool:
        """Create a new well with flexible field mapping."""

        # Define field mappings - map from input field names to database column names
        field_mappings = {
            'wellName': 'name',
            'wellOwner': 'owner',
            'wellLocation': 'location',
            'wellWaterLevel': 'water_level',
            'wellStatus': 'status',
            'waterQuality': 'water_quality',
            'wellWaterType': 'wellWaterType',
            'wellCapacity': 'wellCapacity',
            'wellWaterConsumption': 'wellWaterConsumption',
            'extraData': 'extraData',
            'lastUpdated': 'last_update',
            'ownerId': 'ownerId'
        }

        # Create a copy of the input data
        prepared_data = {}

        # Map fields from input to database schema
        for input_field, db_field in field_mappings.items():
            if input_field in well_data:
                prepared_data[db_field] = well_data[input_field]

        # Handle direct field mappings (where input field name = db field name)
        direct_fields = ['espId', 'description', 'latitude', 'longitude', 'contact_info', 'access_info', 'notes']
        for field in direct_fields:
            if field in well_data:
                prepared_data[field] = well_data[field]

        # Handle required fields with fallbacks
        # For 'name' field (required)
        if 'name' not in prepared_data:
            if 'wellName' in well_data:
                prepared_data['name'] = well_data['wellName']
            elif 'espId' in well_data:
                prepared_data['name'] = f"Well {well_data['espId']}"
            else:
                prepared_data['name'] = "Unnamed Well"

        # For 'latitude' and 'longitude' (required)
        if 'latitude' not in prepared_data:
            if 'wellLocation' in well_data and isinstance(well_data['wellLocation'], str):
                try:
                    location_data = json.loads(well_data['wellLocation'])
                    prepared_data['latitude'] = location_data.get('latitude', 0.0)
                    prepared_data['longitude'] = location_data.get('longitude', 0.0)
                except:
                    prepared_data['latitude'] = 0.0
                    prepared_data['longitude'] = 0.0
            else:
                prepared_data['latitude'] = well_data.get('latitude', 0.0)
                prepared_data['longitude'] = well_data.get('longitude', 0.0)

        if 'longitude' not in prepared_data:
            prepared_data['longitude'] = well_data.get('longitude', 0.0)

        # Handle location field (should be JSON string)
        if 'location' not in prepared_data:
            location_obj = {
                'latitude': prepared_data.get('latitude', 0.0),
                'longitude': prepared_data.get('longitude', 0.0)
            }
            prepared_data['location'] = json.dumps(location_obj)

        # Ensure JSON fields are properly serialized
        json_fields = ['water_quality', 'waterQuality', 'extraData', 'location']
        for field in json_fields:
            if field in prepared_data and prepared_data[field] is not None:
                if not isinstance(prepared_data[field], str):
                    prepared_data[field] = json.dumps(prepared_data[field])

        # Set default timestamp if not provided
        if 'last_update' not in prepared_data:
            prepared_data['last_update'] = datetime.now().isoformat()

        # Validate that we have the minimum required fields
        required_fields = ['name', 'latitude', 'longitude']
        missing_fields = [field for field in required_fields if field not in prepared_data or prepared_data[field] is None]
        if missing_fields:
            raise ValueError(f"Missing required well fields: {missing_fields}")

        # Execute insert
        columns = ', '.join(prepared_data.keys())
        placeholders = ', '.join(['?' for _ in prepared_data])
        query = f'INSERT INTO wells ({columns}) VALUES ({placeholders})'

        try:
            cursor = self._execute(query, tuple(prepared_data.values()))
            self.conn.commit()
            return cursor.lastrowid  # Return the ID of the created well
        except sqlite3.Error as e:
            print(f"Well creation failed: {str(e)}")
            print(f"Prepared data: {prepared_data}")
            return False

    def update_well(self, well_id: int, updates: Dict[str, Any]) -> bool:
        """Update a well's information."""
        if not updates:
            return False

        set_clause = ', '.join([f'{k} = ?' for k in updates.keys()])
        query = f'UPDATE wells SET {set_clause} WHERE id = ?'
        params = list(updates.values()) + [well_id]

        try:
            cursor = self._execute(query, tuple(params))
            self.conn.commit()
            return cursor.rowcount > 0
        except sqlite3.Error as e:
            print(f"Well update failed: {str(e)}")
            return False

    def delete_well(self, well_id: int) -> bool:
        """Delete a well by ID."""
        try:
            cursor = self._execute('DELETE FROM wells WHERE id = ?', (well_id,))
            self.conn.commit()
            return cursor.rowcount > 0
        except sqlite3.Error as e:
            print(f"Well deletion failed: {str(e)}")
            return False

class DeviceTokenDatabase(BaseDatabase):
    """Handles all device token-related database operations."""

    def get_tokens_by_user(self, user_id: str) -> List[Dict[str, Any]]:
        """Get all device tokens for a user."""
        cursor = self._execute('SELECT * FROM device_tokens WHERE userId = ?', (user_id,))
        return [dict(row) for row in cursor.fetchall()]

    def add_token(self, user_id: str, token: str, device_type: str = 'android') -> bool:
        """Add a new device token."""
        token_data = {
            'tokenId': str(uuid.uuid4()),
            'userId': user_id,
            'token': token,
            'deviceType': device_type,
            'lastUsed': datetime.now().isoformat(),
            'isActive': True
        }

        columns = ', '.join(token_data.keys())
        placeholders = ', '.join(['?' for _ in token_data])
        query = f'INSERT INTO device_tokens ({columns}) VALUES ({placeholders})'

        try:
            self._execute(query, tuple(token_data.values()))
            self.conn.commit()
            return True
        except sqlite3.Error as e:
            print(f"Token addition failed: {str(e)}")
            return False

    def update_token(self, token_id: str, updates: Dict[str, Any]) -> bool:
        """Update a device token."""
        if not updates:
            return False

        set_clause = ', '.join([f'{k} = ?' for k in updates.keys()])
        query = f'UPDATE device_tokens SET {set_clause} WHERE tokenId = ?'
        params = list(updates.values()) + [token_id]

        try:
            cursor = self._execute(query, tuple(params))
            self.conn.commit()
            return cursor.rowcount > 0
        except sqlite3.Error as e:
            print(f"Token update failed: {str(e)}")
            return False

    def delete_token(self, token_id: str) -> bool:
        """Delete a device token by ID."""
        try:
            cursor = self._execute('DELETE FROM device_tokens WHERE tokenId = ?', (token_id,))
            self.conn.commit()
            return cursor.rowcount > 0
        except sqlite3.Error as e:
            print(f"Token deletion failed: {str(e)}")
            return False

    def verify_token(self, user_id: str, token: str) -> bool:
        """Verify a device token and update last used timestamp."""
        try:
            # Check if token exists and is active
            cursor = self._execute('''
                SELECT tokenId FROM device_tokens 
                WHERE userId = ? AND token = ? AND isActive = 1
            ''', (user_id, token))

            if cursor.fetchone():
                # Update last used timestamp
                self._execute('''
                    UPDATE device_tokens 
                    SET lastUsed = ?
                    WHERE userId = ? AND token = ?
                ''', (datetime.now().isoformat(), user_id, token))
                self.conn.commit()
                return True
            return False
        except sqlite3.Error as e:
            print(f"Token verification failed: {str(e)}")
            return False

# Helper functions for user management
def generate_random_user() -> Dict[str, Any]:
    """Generate a random user with realistic test data."""
    first_names = ["John", "Jane", "Robert", "Emily", "Michael", "Sarah"]
    last_names = ["Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia"]

    return {
        'userId': str(uuid.uuid4()),
        'email': f"user_{uuid.uuid4().hex[:6]}@example.com",
        'password': str(uuid.uuid4()),
        'firstName': random.choice(first_names),
        'lastName': random.choice(last_names),
        'username': f"user_{uuid.uuid4().hex[:6]}",
        'role': random.choice(['user', 'admin', 'manager']),
        'location': {
            'latitude': round(random.uniform(-90, 90), 6),
            'longitude': round(random.uniform(-180, 180), 6)
        },
        'waterNeeds': {
            'type': random.choice(['drinking', 'irrigation', 'industrial']),
            'amount': random.randint(1, 100)
        },
        'notificationPreferences': {
            'email': random.choice([True, False]),
            'sms': random.choice([True, False]),
            'push': random.choice([True, False])
        },
        'phoneNumber': f"+1{random.randint(200, 999)}{random.randint(100, 999)}{random.randint(1000, 9999)}"
    }

def parse_indices(input_str: str, list_len: int) -> List[int]:
    """
    Parse user input for index selection with support for:
    - single numbers (1, 2, 3)
    - ranges (1-3)
    - 'all' keyword
    - 'last' and 'last-x' syntax
    """
    input_str = input_str.strip().lower()
    indices = set()

    if input_str == "all":
        return list(range(list_len))
    elif input_str.startswith("last"):
        if input_str == "last":
            return [list_len - 1] if list_len > 0 else []
        match = re.match(r"last-(\d+)", input_str)
        if match:
            count = int(match.group(1))
            return list(range(max(0, list_len - count), list_len))
        else:
            raise ValueError("Invalid 'last-x' format.")
    else:
        parts = input_str.split(',')
        for part in parts:
            part = part.strip()
            if '-' in part:
                start, end = map(int, part.split('-'))
                indices.update(range(start, end + 1))
            else:
                indices.add(int(part))
    return sorted(i for i in indices if 0 <= i < list_len)

def display_users(users: List[Dict[str, Any]]) -> None:
    """Display a formatted table of users with proper null handling."""
    if not users:
        print("No users to display.")
        return

    print("\nList of Users:")
    print("-" * 100)
    print(f"{'Index':<5} | {'Email':<30} | {'Name':<20} | {'Role':<10} | {'Last Active':<20} | {'ID'}")
    print("-" * 100)

    for idx, user in enumerate(users, 1):
        # Safely get values with defaults
        email = user.get('email', 'N/A')[:30]
        first_name = user.get('firstName', '')
        last_name = user.get('lastName', '')
        name = f"{first_name} {last_name}".strip()[:20] or 'N/A'
        role = user.get('role', 'N/A')[:10]

        # Handle lastActive safely
        last_active = user.get('lastActive')
        if last_active is None:
            last_active = 'Never'
        else:
            last_active = str(last_active)
            if len(last_active) > 20:
                last_active = last_active[:17] + "..."

        # Handle user ID safely
        user_id = user.get('userId', 'N/A')
        if user_id and len(user_id) > 8:
            user_id = user_id[:8] + "..."

        print(f"{idx:<5} | {email:<30} | {name:<20} | {role:<10} | {last_active:<20} | {user_id}")
    print("-" * 100)

# Initialize the database manager
db = DatabaseManager()

if __name__ == "__main__":
    # Example usage
    user_db = db.users()

    # Create a test user
    test_user = generate_random_user()
    if user_db.create_user(test_user):
        print(f"Created test user: {test_user['email']}")

    # Retrieve and display all users
    all_users = user_db.get_all_users()
    display_users(all_users)