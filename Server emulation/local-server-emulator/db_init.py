import os
import sqlite3
import hashlib
import base64
import random
from datetime import datetime, timedelta

# Database setup
DB_DIR = os.path.dirname(__file__)
DB_PATH = os.path.join(DB_DIR, 'wellconnect.db')

def init_db():
    """Initialize the database with tables and sample data"""
    # Create a new database if it doesn't exist
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    cursor = conn.cursor()
    
    print(f"Initializing database at {DB_PATH}")
    
    # Create tables
    cursor.execute('''
    CREATE TABLE IF NOT EXISTS users (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        email TEXT UNIQUE NOT NULL,
        password TEXT NOT NULL,
        first_name TEXT NOT NULL,
        last_name TEXT NOT NULL,
        username TEXT NOT NULL,
        role TEXT DEFAULT 'user',
        theme_preference INTEGER DEFAULT 0,
        latitude REAL DEFAULT 0.0,
        longitude REAL DEFAULT 0.0,
        is_well_owner BOOLEAN DEFAULT 0,
        last_updated TEXT,
        is_online BOOLEAN DEFAULT 0
    )
    ''')
    
    cursor.execute('''
    CREATE TABLE IF NOT EXISTS water_needs (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        user_id INTEGER,
        amount REAL NOT NULL,
        usage_type TEXT NOT NULL,
        description TEXT,
        priority INTEGER DEFAULT 1,
        FOREIGN KEY (user_id) REFERENCES users (id)
    )
    ''')
    
    cursor.execute('''
    CREATE TABLE IF NOT EXISTS wells (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        esp_id TEXT UNIQUE NOT NULL,
        well_name TEXT NOT NULL,
        well_owner TEXT,
        latitude REAL,
        longitude REAL,
        water_type TEXT,
        capacity REAL,
        water_level REAL,
        water_consumption REAL,
        status TEXT DEFAULT 'Unknown',
        last_updated TEXT,
        owner_id INTEGER,
        FOREIGN KEY (owner_id) REFERENCES users (id)
    )
    ''')
    
    cursor.execute('''
    CREATE TABLE IF NOT EXISTS water_quality (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        well_id INTEGER,
        ph REAL,
        turbidity REAL,
        tds REAL,
        timestamp TEXT,
        FOREIGN KEY (well_id) REFERENCES wells (id)
    )
    ''')
    
    # Add sample data if database is empty
    cursor.execute("SELECT COUNT(*) FROM users")
    if cursor.fetchone()[0] == 0:
        print("Adding sample data...")
        
        # Add sample users
        sample_users = [
            ('admin@wellconnect.com', encrypt_password('admin123'), 
             'Admin', 'User', 'admin', 'admin', 0, 1.2345, 2.3456, 1, datetime.now().isoformat(), 1),
            ('user@wellconnect.com', encrypt_password('user123'), 
             'Regular', 'User', 'user', 'user', 0, 1.3456, 2.4567, 0, datetime.now().isoformat(), 1),
            ('wellowner@wellconnect.com', encrypt_password('owner123'), 
             'Well', 'Owner', 'wellowner', 'well_owner', 0, 1.4567, 2.5678, 1, datetime.now().isoformat(), 1)
        ]
        
        cursor.executemany('''
        INSERT INTO users (
            email, password, first_name, last_name, username, role, 
            theme_preference, latitude, longitude, is_well_owner, last_updated, is_online
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ''', sample_users)
        
        # Add sample water needs
        cursor.execute("SELECT id FROM users")
        user_ids = cursor.fetchall()
        
        water_needs = []
        for user_id in user_ids:
            water_needs.extend([
                (user_id[0], 50.0, 'Drinking', 'Daily drinking water', 1),
                (user_id[0], 100.0, 'Irrigation', 'Garden irrigation', 2),
                (user_id[0], 30.0, 'Cooking', 'Water for cooking', 1)
            ])
        
        cursor.executemany('''
        INSERT INTO water_needs (
            user_id, amount, usage_type, description, priority
        ) VALUES (?, ?, ?, ?, ?)
        ''', water_needs)
        
        # Add well owner reference
        cursor.execute("SELECT id FROM users WHERE email = 'wellowner@wellconnect.com'")
        owner_id = cursor.fetchone()[0]
        
        # Add sample wells
        sample_wells = [
            ('esp32-01', 'Simulated Well 1', 'Owner A', 0.0, 25.0, 'Clean', 5000.0, 2500.0, 300.0, 'Active', 
             (datetime.now() - timedelta(minutes=5)).isoformat(), owner_id),
            ('esp32-02', 'Simulated Well 2', 'Owner B', 1.234567, 26.234567, 'Grey', 6000.0, 3000.0, 350.0, 'Maintenance', 
             (datetime.now() - timedelta(hours=1)).isoformat(), owner_id),
            ('esp32-03', 'Simulated Well 3', 'Owner C', -0.987654, 24.987654, 'Clean', 4500.0, 2000.0, 250.0, 'Inactive', 
             (datetime.now() - timedelta(days=1)).isoformat(), owner_id)
        ]
        
        for well in sample_wells:
            cursor.execute('''
            INSERT INTO wells (
                esp_id, well_name, well_owner, latitude, longitude, water_type,
                capacity, water_level, water_consumption, status, last_updated, owner_id
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ''', well)
            
            # Get well ID for water quality data
            well_id = cursor.lastrowid
            
            # Add water quality data
            water_quality = (
                well_id, 
                random.uniform(6.0, 8.5),  # pH 
                random.uniform(0.1, 5.0),  # turbidity
                random.uniform(100, 1000), # TDS
                datetime.now().isoformat()
            )
            
            cursor.execute('''
            INSERT INTO water_quality (
                well_id, ph, turbidity, tds, timestamp
            ) VALUES (?, ?, ?, ?, ?)
            ''', water_quality)
        
        print("Sample data added successfully!")
    
    conn.commit()
    conn.close()
    print("Database initialization complete!")

def encrypt_password(password):
    """Encrypt a password using SHA-256 and base64 encoding"""
    return base64.b64encode(hashlib.sha256(password.encode()).digest()).decode()

if __name__ == "__main__":
    init_db() 