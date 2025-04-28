import os
import sqlite3
import random
import hashlib
import base64
from datetime import datetime, timedelta

# Database setup
DB_DIR = os.path.dirname(__file__)
DB_PATH = os.path.join(DB_DIR, 'wellconnect.db')

# Sample data for random generation
FIRST_NAMES = ["James", "Mary", "John", "Patricia", "Robert", "Jennifer", "Michael", "Linda", "William", "Elizabeth", 
               "David", "Barbara", "Richard", "Susan", "Joseph", "Jessica", "Thomas", "Sarah", "Christopher", "Karen"]
LAST_NAMES = ["Smith", "Johnson", "Williams", "Jones", "Brown", "Davis", "Miller", "Wilson", "Moore", "Taylor", 
              "Anderson", "Thomas", "Jackson", "White", "Harris", "Martin", "Thompson", "Garcia", "Martinez", "Robinson"]
LOCATIONS = [
    {"name": "San Francisco", "lat": 37.7749, "lon": -122.4194},
    {"name": "Los Angeles", "lat": 34.0522, "lon": -118.2437},
    {"name": "New York", "lat": 40.7128, "lon": -74.0060},
    {"name": "Chicago", "lat": 41.8781, "lon": -87.6298},
    {"name": "Houston", "lat": 29.7604, "lon": -95.3698},
    {"name": "Phoenix", "lat": 33.4484, "lon": -112.0740},
    {"name": "Philadelphia", "lat": 39.9526, "lon": -75.1652},
    {"name": "San Antonio", "lat": 29.4241, "lon": -98.4936},
    {"name": "San Diego", "lat": 32.7157, "lon": -117.1611},
    {"name": "Dallas", "lat": 32.7767, "lon": -96.7970}
]
WELL_TYPES = ["Clean", "Grey", "Fresh", "Brackish", "Potable", "Non-Potable"]
WELL_STATUSES = ["Active", "Maintenance", "Inactive", "Operational", "Needs Maintenance"]
USAGE_TYPES = ["Drinking", "Cooking", "Bathing", "Irrigation", "Livestock", "Industrial", "Cleaning"]

def encrypt_password(password):
    """Encrypt a password using SHA-256 and base64 encoding"""
    return base64.b64encode(hashlib.sha256(password.encode()).digest()).decode()

def generate_random_user(user_id):
    """Generate a random user"""
    first_name = random.choice(FIRST_NAMES)
    last_name = random.choice(LAST_NAMES)
    username = f"{first_name.lower()}{user_id}"
    email = f"{username}@example.com"
    password = encrypt_password(f"password{user_id}")
    
    location = random.choice(LOCATIONS)
    # Add some randomness to location
    lat = location["lat"] + random.uniform(-0.1, 0.1)
    lon = location["lon"] + random.uniform(-0.1, 0.1)
    
    is_well_owner = random.random() < 0.3  # 30% chance of being a well owner
    role = "well_owner" if is_well_owner else "user"
    
    last_updated = (datetime.now() - timedelta(minutes=random.randint(1, 1000))).isoformat()
    is_online = random.random() < 0.7  # 70% chance of being online
    
    return (
        email, password, first_name, last_name, username, role, 
        0, lat, lon, 1 if is_well_owner else 0, last_updated, 1 if is_online else 0
    )

def generate_random_water_needs(user_id):
    """Generate random water needs for a user"""
    num_needs = random.randint(1, 4)
    water_needs = []
    
    for _ in range(num_needs):
        usage_type = random.choice(USAGE_TYPES)
        amount = random.randint(10, 500)
        description = f"{usage_type} water for {random.choice(['daily', 'weekly', 'monthly'])} use"
        priority = random.randint(1, 3)
        
        water_needs.append((user_id, amount, usage_type, description, priority))
    
    return water_needs

def generate_random_well(well_id, owner_id):
    """Generate a random well"""
    location = random.choice(LOCATIONS)
    # Add some randomness to location
    lat = location["lat"] + random.uniform(-0.1, 0.1)
    lon = location["lon"] + random.uniform(-0.1, 0.1)
    
    esp_id = f"esp32-{well_id:03d}"
    well_name = f"Well {well_id} at {location['name']}"
    well_owner = f"Owner {well_id}"
    water_type = random.choice(WELL_TYPES)
    capacity = random.randint(1000, 10000)
    water_level = random.uniform(0.1, 1.0) * capacity
    water_consumption = random.uniform(50, 500)
    status = random.choice(WELL_STATUSES)
    last_updated = (datetime.now() - timedelta(minutes=random.randint(1, 1000))).isoformat()
    
    return (
        esp_id, well_name, well_owner, lat, lon, water_type,
        capacity, water_level, water_consumption, status, last_updated, owner_id
    )

def generate_random_water_quality(well_id):
    """Generate random water quality data for a well"""
    ph = random.uniform(6.0, 8.5)
    turbidity = random.uniform(0.1, 5.0)
    tds = random.uniform(100, 1000)
    timestamp = datetime.now().isoformat()
    
    return (well_id, ph, turbidity, tds, timestamp)

def create_database(num_users=30, num_wells=50):
    """Create a new database with sample data"""
    # Delete existing database if it exists
    if os.path.exists(DB_PATH):
        os.remove(DB_PATH)
        print(f"Deleted existing database at {DB_PATH}")
    
    # Create new database
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    
    print(f"Creating new database at {DB_PATH}")
    
    # Create tables
    cursor.execute('''
    CREATE TABLE users (
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
    CREATE TABLE water_needs (
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
    CREATE TABLE wells (
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
    CREATE TABLE water_quality (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        well_id INTEGER,
        ph REAL,
        turbidity REAL,
        tds REAL,
        timestamp TEXT,
        FOREIGN KEY (well_id) REFERENCES wells (id)
    )
    ''')
    
    # Add admin user
    admin_user = ('admin@wellconnect.com', encrypt_password('admin123'), 
                  'Admin', 'User', 'admin', 'admin', 
                  0, LOCATIONS[0]["lat"], LOCATIONS[0]["lon"], 1, datetime.now().isoformat(), 1)
    
    cursor.execute('''
    INSERT INTO users (
        email, password, first_name, last_name, username, role, 
        theme_preference, latitude, longitude, is_well_owner, last_updated, is_online
    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    ''', admin_user)
    
    # Add demo user
    demo_user = ('demo@wellconnect.com', encrypt_password('demo123'), 
                 'Demo', 'User', 'demo', 'user', 
                 0, LOCATIONS[1]["lat"], LOCATIONS[1]["lon"], 0, datetime.now().isoformat(), 1)
    
    cursor.execute('''
    INSERT INTO users (
        email, password, first_name, last_name, username, role, 
        theme_preference, latitude, longitude, is_well_owner, last_updated, is_online
    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    ''', demo_user)
    
    # Add demo water needs
    demo_water_needs = [
        (2, 50.0, 'Drinking', 'Daily drinking water for family of 4', 1),
        (2, 100.0, 'Irrigation', 'Small garden irrigation', 2),
        (2, 30.0, 'Cooking', 'Water for daily cooking needs', 1)
    ]
    
    for need in demo_water_needs:
        cursor.execute('''
        INSERT INTO water_needs (
            user_id, amount, usage_type, description, priority
        ) VALUES (?, ?, ?, ?, ?)
        ''', need)
    
    # Generate random users
    for i in range(3, num_users + 3):  # Start from 3 (after admin and demo)
        user = generate_random_user(i)
        
        cursor.execute('''
        INSERT INTO users (
            email, password, first_name, last_name, username, role, 
            theme_preference, latitude, longitude, is_well_owner, last_updated, is_online
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ''', user)
        
        # Generate water needs for this user
        water_needs = generate_random_water_needs(i)
        for need in water_needs:
            cursor.execute('''
            INSERT INTO water_needs (
                user_id, amount, usage_type, description, priority
            ) VALUES (?, ?, ?, ?, ?)
            ''', need)
    
    # Get well owner IDs
    cursor.execute("SELECT id FROM users WHERE is_well_owner = 1")
    well_owner_ids = [row[0] for row in cursor.fetchall()]
    
    # Generate wells
    for i in range(1, num_wells + 1):
        owner_id = random.choice(well_owner_ids)
        well = generate_random_well(i, owner_id)
        
        cursor.execute('''
        INSERT INTO wells (
            esp_id, well_name, well_owner, latitude, longitude, water_type,
            capacity, water_level, water_consumption, status, last_updated, owner_id
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ''', well)
        
        # Get well ID
        well_id = cursor.lastrowid
        
        # Generate history of water quality measurements (3-5 entries per well)
        for _ in range(random.randint(3, 5)):
            quality = generate_random_water_quality(well_id)
            
            cursor.execute('''
            INSERT INTO water_quality (
                well_id, ph, turbidity, tds, timestamp
            ) VALUES (?, ?, ?, ?, ?)
            ''', quality)
    
    conn.commit()
    conn.close()
    
    print(f"Database created with {num_users} users and {num_wells} wells")
    print(f"Admin User: admin@wellconnect.com / admin123")
    print(f"Demo User: demo@wellconnect.com / demo123")

if __name__ == "__main__":
    import argparse
    
    parser = argparse.ArgumentParser(description='Generate sample database for WellConnect')
    parser.add_argument('--users', type=int, default=30, help='Number of users to generate')
    parser.add_argument('--wells', type=int, default=50, help='Number of wells to generate')
    
    args = parser.parse_args()
    
    create_database(args.users, args.wells) 