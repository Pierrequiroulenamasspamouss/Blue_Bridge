#!/usr/bin/env python3
"""
Usage:
    python generate_users.py COUNT [--lat LATITUDE] [--lon LONGITUDE] [--radius RADIUS] [--file PATH]
Generates COUNT fake users around the given lat/lon (in degrees) within the radius (in degrees) and saves them to the user database JSON file.
python generate_users.py 10 --lat 50.632 --lon 5.579 --radius 0.1 --file user_database.json
"""
import json
import os
import sys
import argparse
import random
import hashlib
import base64
from datetime import datetime

# Try importing Faker for realistic names
try:
    from faker import Faker
except ImportError:
    print("Faker library not found. Please install with: pip install faker")
    sys.exit(1)

# Default database file
DEFAULT_DB = os.path.join(os.path.dirname(__file__), 'user_database.json')

# Parse arguments
parser = argparse.ArgumentParser(description="Generate fake users for testing")
parser.add_argument('count', type=int, help='Number of users to generate')
parser.add_argument('--lat', type=float, default=50.632, help='Center latitude (default Liège)')
parser.add_argument('--lon', type=float, default=5.579, help='Center longitude (default Liège)')
parser.add_argument('--radius', type=float, default=0.1, help='Degrees radius around center (default ±0.1)')
parser.add_argument('--file', type=str, default=DEFAULT_DB, help='Path to user_database.json')
args = parser.parse_args()

# Load or initialize database
if os.path.exists(args.file):
    with open(args.file, 'r') as f:
        try:
            db = json.load(f)
        except json.JSONDecodeError:
            db = {}
else:
    db = {}

fake = Faker()

for i in range(args.count):
    # Generate unique email
    username = fake.user_name() + str(i)
    email = f"{username}@example.com"
    # Generate name and password
    first = fake.first_name()
    last = fake.last_name()
    pwd = fake.password(length=12)
    # Hash password (SHA-256 + Base64)
    digest = hashlib.sha256(pwd.encode()).digest()
    hashed = base64.b64encode(digest).decode()
    # Random location within radius
    lat = args.lat + random.uniform(-args.radius, args.radius)
    lon = args.lon + random.uniform(-args.radius, args.radius)
    # Assign water needs
    amount = random.randint(50, 500)
    usageType = random.choice(['drinking', 'industry', 'gardening', 'farming'])
    description = fake.sentence()
    priority = random.randint(0, 5)
    # Build user entry
    db[email] = {
        'password': hashed,
        'firstName': first,
        'lastName': last,
        'username': username,
        'role': 'user',
        'location': {
            'latitude': lat,
            'longitude': lon,
            'lastUpdated': datetime.now().isoformat()
        },
        'waterNeeds': [
            {
            'amount': amount,
            'usageType': usageType,
            'description': description,
            'priority': priority
        }
        ]
    }

# Write back to file
with open(args.file, 'w') as f:
    json.dump(db, f, indent=4)

print(f"Generated {args.count} users in {args.file}")
print("Sample user credentials (first 5):")
for idx, (email, user) in enumerate(db.items()):
    if idx >= 5:
        break
    print(f"{email} -> password (plain text hidden in script log)") 