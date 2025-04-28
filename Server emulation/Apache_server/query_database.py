import os
import sqlite3
import json
from datetime import datetime
from tabulate import tabulate

# Database setup
DB_DIR = os.path.dirname(__file__)
DB_PATH = os.path.join(DB_DIR, 'wellconnect.db')

def get_db_connection():
    """Get a database connection with row factory"""
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    return conn

def print_table(data, headers):
    """Print tabular data"""
    print(tabulate(data, headers=headers, tablefmt="grid"))

def list_users(limit=10):
    """List users in the database"""
    conn = get_db_connection()
    cursor = conn.cursor()
    
    cursor.execute('''
    SELECT id, email, first_name, last_name, username, role, is_well_owner, is_online
    FROM users
    ORDER BY id
    LIMIT ?
    ''', (limit,))
    
    users = cursor.fetchall()
    
    print(f"\n=== USERS (showing {len(users)} of {count_records('users')}) ===")
    
    user_data = []
    for user in users:
        user_data.append([
            user['id'],
            user['email'],
            user['first_name'] + ' ' + user['last_name'],
            user['username'],
            user['role'],
            'Yes' if user['is_well_owner'] == 1 else 'No',
            'Yes' if user['is_online'] == 1 else 'No'
        ])
    
    headers = ["ID", "Email", "Name", "Username", "Role", "Well Owner", "Online"]
    print_table(user_data, headers)
    
    conn.close()

def list_wells(limit=10):
    """List wells in the database"""
    conn = get_db_connection()
    cursor = conn.cursor()
    
    cursor.execute('''
    SELECT w.id, w.esp_id, w.well_name, w.latitude, w.longitude, 
           w.water_type, w.capacity, w.water_level, w.status,
           u.first_name || ' ' || u.last_name as owner_name
    FROM wells w
    LEFT JOIN users u ON w.owner_id = u.id
    ORDER BY w.id
    LIMIT ?
    ''', (limit,))
    
    wells = cursor.fetchall()
    
    print(f"\n=== WELLS (showing {len(wells)} of {count_records('wells')}) ===")
    
    well_data = []
    for well in wells:
        # Calculate percentage of water level
        capacity = well['capacity'] or 0
        water_level = well['water_level'] or 0
        percentage = round((water_level / capacity * 100) if capacity > 0 else 0, 1)
        
        well_data.append([
            well['id'],
            well['esp_id'],
            well['well_name'],
            f"({well['latitude']:.4f}, {well['longitude']:.4f})",
            well['water_type'],
            f"{water_level:.1f}/{capacity:.1f} ({percentage}%)",
            well['status'],
            well['owner_name']
        ])
    
    headers = ["ID", "ESP ID", "Name", "Location", "Type", "Water Level", "Status", "Owner"]
    print_table(well_data, headers)
    
    conn.close()

def list_water_needs(limit=10):
    """List water needs in the database"""
    conn = get_db_connection()
    cursor = conn.cursor()
    
    cursor.execute('''
    SELECT wn.id, wn.user_id, u.first_name || ' ' || u.last_name as user_name,
           wn.amount, wn.usage_type, wn.description, wn.priority
    FROM water_needs wn
    JOIN users u ON wn.user_id = u.id
    ORDER BY wn.user_id, wn.priority
    LIMIT ?
    ''', (limit,))
    
    needs = cursor.fetchall()
    
    print(f"\n=== WATER NEEDS (showing {len(needs)} of {count_records('water_needs')}) ===")
    
    need_data = []
    for need in needs:
        need_data.append([
            need['id'],
            need['user_id'],
            need['user_name'],
            need['amount'],
            need['usage_type'],
            need['description'],
            need['priority']
        ])
    
    headers = ["ID", "User ID", "User Name", "Amount", "Usage Type", "Description", "Priority"]
    print_table(need_data, headers)
    
    conn.close()

def list_water_quality(limit=10):
    """List water quality measurements in the database"""
    conn = get_db_connection()
    cursor = conn.cursor()
    
    cursor.execute('''
    SELECT wq.id, wq.well_id, w.well_name, wq.ph, wq.turbidity, wq.tds, wq.timestamp
    FROM water_quality wq
    JOIN wells w ON wq.well_id = w.id
    ORDER BY wq.well_id, wq.timestamp DESC
    LIMIT ?
    ''', (limit,))
    
    qualities = cursor.fetchall()
    
    print(f"\n=== WATER QUALITY (showing {len(qualities)} of {count_records('water_quality')}) ===")
    
    quality_data = []
    for quality in qualities:
        quality_data.append([
            quality['id'],
            quality['well_id'],
            quality['well_name'],
            quality['ph'],
            quality['turbidity'],
            quality['tds'],
            quality['timestamp']
        ])
    
    headers = ["ID", "Well ID", "Well Name", "pH", "Turbidity", "TDS", "Timestamp"]
    print_table(quality_data, headers)
    
    conn.close()

def count_records(table_name):
    """Count records in a table"""
    conn = get_db_connection()
    cursor = conn.cursor()
    
    cursor.execute(f"SELECT COUNT(*) FROM {table_name}")
    count = cursor.fetchone()[0]
    
    conn.close()
    return count

def check_database():
    """Check if the database exists and show basic stats"""
    if not os.path.exists(DB_PATH):
        print(f"Database file not found at {DB_PATH}")
        print("Run generate_sample_data.py first to create the database.")
        return False
    
    conn = get_db_connection()
    cursor = conn.cursor()
    
    # Get table list
    cursor.execute("SELECT name FROM sqlite_master WHERE type='table'")
    tables = cursor.fetchall()
    
    print(f"Database found at {DB_PATH}")
    print(f"Tables: {', '.join([table['name'] for table in tables if not table['name'].startswith('sqlite_')])}")
    
    # Get record counts
    print("\nRecord counts:")
    for table in [table['name'] for table in tables if not table['name'].startswith('sqlite_')]:
        cursor.execute(f"SELECT COUNT(*) FROM {table}")
        count = cursor.fetchone()[0]
        print(f"  {table}: {count} records")
    
    conn.close()
    return True

def show_user_details(user_id):
    """Show details for a specific user"""
    conn = get_db_connection()
    cursor = conn.cursor()
    
    # Get user info
    cursor.execute('''
    SELECT * FROM users WHERE id = ?
    ''', (user_id,))
    
    user = cursor.fetchone()
    
    if not user:
        print(f"No user found with ID {user_id}")
        conn.close()
        return
    
    print(f"\n=== USER DETAILS: {user['first_name']} {user['last_name']} (ID: {user['id']}) ===")
    
    # Convert to dict for pretty printing
    user_dict = dict(user)
    
    # Get water needs
    cursor.execute('''
    SELECT * FROM water_needs WHERE user_id = ?
    ''', (user_id,))
    
    needs = cursor.fetchall()
    water_needs = [dict(need) for need in needs]
    
    # Get wells if user is a well owner
    owned_wells = []
    if user['is_well_owner'] == 1:
        cursor.execute('''
        SELECT * FROM wells WHERE owner_id = ?
        ''', (user_id,))
        
        wells = cursor.fetchall()
        owned_wells = [dict(well) for well in wells]
    
    # Format output
    print(f"Email: {user['email']}")
    print(f"Name: {user['first_name']} {user['last_name']}")
    print(f"Username: {user['username']}")
    print(f"Role: {user['role']}")
    print(f"Well Owner: {'Yes' if user['is_well_owner'] == 1 else 'No'}")
    print(f"Location: ({user['latitude']}, {user['longitude']})")
    print(f"Online: {'Yes' if user['is_online'] == 1 else 'No'}")
    print(f"Last Updated: {user['last_updated']}")
    
    if water_needs:
        print("\nWater Needs:")
        for need in water_needs:
            print(f"  - {need['amount']} units of {need['usage_type']} (Priority: {need['priority']})")
            if need['description']:
                print(f"    Description: {need['description']}")
    
    if owned_wells:
        print("\nOwned Wells:")
        for well in owned_wells:
            print(f"  - {well['well_name']} (ESP ID: {well['esp_id']})")
            print(f"    Type: {well['water_type']}, Status: {well['status']}")
            capacity = well['capacity'] or 0
            water_level = well['water_level'] or 0
            percentage = round((water_level / capacity * 100) if capacity > 0 else 0, 1)
            print(f"    Water Level: {water_level}/{capacity} ({percentage}%)")
    
    conn.close()

def show_well_details(well_id):
    """Show details for a specific well"""
    conn = get_db_connection()
    cursor = conn.cursor()
    
    # Get well info
    cursor.execute('''
    SELECT w.*, u.first_name || ' ' || u.last_name as owner_name, u.email as owner_email
    FROM wells w
    LEFT JOIN users u ON w.owner_id = u.id
    WHERE w.id = ?
    ''', (well_id,))
    
    well = cursor.fetchone()
    
    if not well:
        print(f"No well found with ID {well_id}")
        conn.close()
        return
    
    print(f"\n=== WELL DETAILS: {well['well_name']} (ID: {well['id']}) ===")
    
    # Get water quality
    cursor.execute('''
    SELECT * FROM water_quality 
    WHERE well_id = ? 
    ORDER BY timestamp DESC
    LIMIT 5
    ''', (well_id,))
    
    qualities = cursor.fetchall()
    water_quality = [dict(quality) for quality in qualities]
    
    # Format output
    print(f"ESP ID: {well['esp_id']}")
    print(f"Name: {well['well_name']}")
    print(f"Owner: {well['owner_name']} ({well['owner_email']})")
    print(f"Location: ({well['latitude']}, {well['longitude']})")
    print(f"Water Type: {well['water_type']}")
    
    capacity = well['capacity'] or 0
    water_level = well['water_level'] or 0
    percentage = round((water_level / capacity * 100) if capacity > 0 else 0, 1)
    print(f"Capacity: {capacity}")
    print(f"Water Level: {water_level} ({percentage}%)")
    print(f"Water Consumption: {well['water_consumption']} units/day")
    print(f"Status: {well['status']}")
    print(f"Last Updated: {well['last_updated']}")
    
    if water_quality:
        print("\nLatest Water Quality Measurements:")
        for quality in water_quality:
            print(f"  [{quality['timestamp']}]")
            print(f"    pH: {quality['ph']}")
            print(f"    Turbidity: {quality['turbidity']}")
            print(f"    TDS: {quality['tds']} ppm")
    
    conn.close()

def main():
    """Main function to query the database"""
    import argparse
    
    parser = argparse.ArgumentParser(description='Query WellConnect database')
    parser.add_argument('--check', action='store_true', help='Check database and show basic stats')
    parser.add_argument('--users', action='store_true', help='List users')
    parser.add_argument('--wells', action='store_true', help='List wells')
    parser.add_argument('--water-needs', action='store_true', help='List water needs')
    parser.add_argument('--water-quality', action='store_true', help='List water quality measurements')
    parser.add_argument('--user', type=int, help='Show details for a specific user ID')
    parser.add_argument('--well', type=int, help='Show details for a specific well ID')
    parser.add_argument('--limit', type=int, default=10, help='Limit the number of records shown')
    
    args = parser.parse_args()
    
    # If no arguments, show basic help
    if not any(vars(args).values()):
        parser.print_help()
        return
    
    # Check if database exists
    if args.check or not os.path.exists(DB_PATH):
        if not check_database():
            return
    
    if args.users:
        list_users(args.limit)
    
    if args.wells:
        list_wells(args.limit)
    
    if args.water_needs:
        list_water_needs(args.limit)
    
    if args.water_quality:
        list_water_quality(args.limit)
    
    if args.user is not None:
        show_user_details(args.user)
    
    if args.well is not None:
        show_well_details(args.well)

if __name__ == "__main__":
    main() 