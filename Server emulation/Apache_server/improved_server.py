import json
import os
import threading
import sqlite3
import time
from http.server import BaseHTTPRequestHandler, HTTPServer
from urllib.parse import urlparse, parse_qs, unquote
from datetime import datetime, timedelta
import math
import hashlib
import base64
import random

# Database setup
DB_DIR = os.path.dirname(__file__)
DB_PATH = os.path.join(DB_DIR, 'wellconnect.db')

# Create database connection
def get_db_connection():
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    return conn

# Initialize database
def init_db():
    conn = get_db_connection()
    cursor = conn.cursor()
    
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
        # Add sample users
        sample_users = [
            ('admin@wellconnect.com', base64.b64encode(hashlib.sha256('admin123'.encode()).digest()).decode(), 
             'Admin', 'User', 'admin', 'admin', 0, 1.2345, 2.3456, 1, datetime.now().isoformat(), 1),
            ('user@wellconnect.com', base64.b64encode(hashlib.sha256('user123'.encode()).digest()).decode(), 
             'Regular', 'User', 'user', 'user', 0, 1.3456, 2.4567, 0, datetime.now().isoformat(), 1),
            ('wellowner@wellconnect.com', base64.b64encode(hashlib.sha256('owner123'.encode()).digest()).decode(), 
             'Well', 'Owner', 'wellowner', 'well_owner', 0, 1.4567, 2.5678, 1, datetime.now().isoformat(), 1)
        ]
        
        cursor.executemany('''
        INSERT INTO users (email, password, first_name, last_name, username, role, 
                         theme_preference, latitude, longitude, is_well_owner, last_updated, is_online)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
        INSERT INTO water_needs (user_id, amount, usage_type, description, priority)
        VALUES (?, ?, ?, ?, ?)
        ''', water_needs)
        
        # Add sample wells
        sample_wells = [
            ('esp32-01', 'Simulated Well 1', 'Owner A', 0.0, 25.0, 'Clean', 5000.0, 2500.0, 300.0, 'Active', 
             (datetime.now() - timedelta(minutes=5)).isoformat(), 3),
            ('esp32-02', 'Simulated Well 2', 'Owner B', 1.234567, 26.234567, 'Grey', 6000.0, 3000.0, 350.0, 'Maintenance', 
             (datetime.now() - timedelta(hours=1)).isoformat(), 3),
            ('esp32-03', 'Simulated Well 3', 'Owner C', -0.987654, 24.987654, 'Clean', 4500.0, 2000.0, 250.0, 'Inactive', 
             (datetime.now() - timedelta(days=1)).isoformat(), 3)
        ]
        
        for well in sample_wells:
            cursor.execute('''
            INSERT INTO wells (esp_id, well_name, well_owner, latitude, longitude, water_type,
                            capacity, water_level, water_consumption, status, last_updated, owner_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
            INSERT INTO water_quality (well_id, ph, turbidity, tds, timestamp)
            VALUES (?, ?, ?, ?, ?)
            ''', water_quality)
    
    conn.commit()
    conn.close()

# Initialize database at startup
init_db()

# Active services flags
active_services = {
    'login': True,
    'data': True,
    'nearby': True,
    'wells': True,
    'stats': True
}

def update_well_data():
    """Simulate real-time updates to well data"""
    if not active_services['data']:
        return
    
    conn = get_db_connection()
    cursor = conn.cursor()
    
    # Get all active wells
    cursor.execute("SELECT id, water_level, capacity, water_consumption FROM wells WHERE status = 'Active'")
    active_wells = cursor.fetchall()
    
    for well in active_wells:
        well_id = well['id']
        water_level = well['water_level']
        capacity = well['capacity']
        consumption = well['water_consumption']
        
        # Simulate water level changes
        consumption_rate = random.uniform(0.8, 1.2) * consumption
        new_water_level = max(0, min(
            capacity,
            water_level - consumption_rate/24  # Consumption per hour
        ))
        
        # Update water level and timestamp
        cursor.execute('''
        UPDATE wells 
        SET water_level = ?, last_updated = ?
        WHERE id = ?
        ''', (new_water_level, datetime.now().isoformat(), well_id))
        
        # Update water quality metrics with small variations
        cursor.execute("SELECT ph, turbidity, tds FROM water_quality WHERE well_id = ? ORDER BY timestamp DESC LIMIT 1", (well_id,))
        last_quality = cursor.fetchone()
        
        if last_quality:
            new_ph = max(6.0, min(8.5, last_quality['ph'] + random.uniform(-0.1, 0.1)))
            new_turbidity = max(0.1, min(5.0, last_quality['turbidity'] + random.uniform(-0.05, 0.05)))
            new_tds = max(100, min(1000, last_quality['tds'] + random.uniform(-5, 5)))
            
            cursor.execute('''
            INSERT INTO water_quality (well_id, ph, turbidity, tds, timestamp)
            VALUES (?, ?, ?, ?, ?)
            ''', (well_id, new_ph, new_turbidity, new_tds, datetime.now().isoformat()))
            
        # Set to maintenance if water level is too low
        if new_water_level < 0.1 * capacity:
            cursor.execute("UPDATE wells SET status = 'Maintenance' WHERE id = ?", (well_id,))
    
    conn.commit()
    conn.close()

# Function to update user's last activity timestamp
def update_user_last_activity(email):
    """Update the lastUpdated timestamp for a user if they exist in the database"""
    if not email:
        return False
    
    conn = get_db_connection()
    cursor = conn.cursor()
    
    cursor.execute("SELECT id FROM users WHERE email = ?", (email,))
    user = cursor.fetchone()
    
    if user:
        cursor.execute("UPDATE users SET last_updated = ?, is_online = 1 WHERE id = ?", 
                      (datetime.now().isoformat(), user['id']))
        conn.commit()
        conn.close()
        return True
    
    conn.close()
    return False

class ImprovedHandler(BaseHTTPRequestHandler):
    def send_common_headers(self, code=200):
        self.send_response(code)
        self.send_header('Content-Type', 'application/json')
        self.send_header('Access-Control-Allow-Origin', '*')
        self.end_headers()

    def do_OPTIONS(self):
        self.send_response(200)
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, POST, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', 'Content-Type')
        self.end_headers()

    def do_GET(self):
        parsed = urlparse(self.path)
        path = parsed.path
        query = parse_qs(parsed.query)
        
        # Check if email is provided in the query params and update lastUpdated
        if 'email' in query:
            email = unquote(query['email'][0])
            update_user_last_activity(email)

        # Handle the different GET endpoints
        if path == '/wells':
            self.handle_get_wells()
        elif path == '/wells/index':
            self.handle_get_wells_filtered(query)
        elif path.startswith('/wells/') and not path.endswith('/stats'):
            self.handle_get_well_detail(path.split('/')[-1])
        elif path == '/wells/stats':
            self.handle_get_well_stats()
        elif path.startswith('/data/wells/'):
            self.handle_get_well_detail(path.split('/')[-1])
        elif path == '/data/wells':
            self.handle_get_wells()
        elif path.startswith('/nearby-users'):
            self.handle_get_nearby_users(query)
        else:
            self.send_common_headers(404)
            self.wfile.write(json.dumps({'error': 'Invalid GET endpoint'}).encode())

    def do_POST(self):
        parsed = urlparse(self.path)
        path = parsed.path
        length = int(self.headers.get('Content-Length', 0))
        body = self.rfile.read(length).decode()
        try:
            data = json.loads(body) if body else {}
        except Exception:
            data = {}
            
        # Check if email is provided in the request body and update lastUpdated
        if 'email' in data:
            update_user_last_activity(data['email'])

        # Handle the different POST endpoints
        if path == '/login':
            self.handle_login(data)
        elif path == '/register':
            self.handle_register(data)
        elif path == '/update-location':
            self.handle_update_location(data)
        elif path == '/update-water-needs':
            self.handle_update_water_needs(data)
        else:
            self.send_common_headers(404)
            self.wfile.write(json.dumps({'error': 'Invalid POST endpoint'}).encode())

    # Detailed handler methods follow in the next part 