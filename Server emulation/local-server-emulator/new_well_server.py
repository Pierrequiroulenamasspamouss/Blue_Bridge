import json
import os
import sqlite3
import random
import re
from datetime import datetime, timedelta

# Database setup
DB_DIR = os.path.dirname(__file__)
DB_PATH = os.path.join(DB_DIR, 'wellconnect.db')

class WellServer:
    """Well data server for handling well information and updates"""
    
    def __init__(self, db_path=DB_PATH):
        self.db_path = db_path
        
    def get_db_connection(self):
        conn = sqlite3.connect(self.db_path)
        conn.row_factory = sqlite3.Row
        return conn
    
    def sanitize_input(self, input_str):
        """Sanitize input to prevent injection attacks"""
        if not input_str:
            return ""
        # Remove potential SQL injection characters
        return re.sub(r'[\'";\\]', '', str(input_str))
    
    def update_well_data(self):
        """Simulate real-time updates to well data"""
        conn = self.get_db_connection()
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
    
    def get_all_wells(self):
        """Get all wells in the shortened format"""
        self.update_well_data()  # Update well data before returning
        
        conn = self.get_db_connection()
        cursor = conn.cursor()
        
        cursor.execute('''
        SELECT 
            id, esp_id, well_name, well_owner, latitude, longitude, 
            water_type, capacity, water_level, water_consumption, status
        FROM wells
        ''')
        wells = cursor.fetchall()
        
        # Convert to the format expected by the client
        wells_list = []
        for well in wells:
            wells_list.append({
                "id": well['id'],  # Include well ID for proper reference
                "wellName": well['well_name'],
                "wellLocation": f"Location:\nlat: {well['latitude']}\nlon: {well['longitude']}",
                "wellWaterType": well['water_type'],
                "espId": well['esp_id'],
                "wellStatus": well['status'],
                "wellOwner": well['well_owner'],
                "wellCapacity": str(well['capacity']),
                "wellWaterLevel": str(well['water_level']),
                "wellWaterConsumption": str(well['water_consumption'])
            })
        
        conn.close()
        return json.dumps(wells_list), 200
    
    def get_wells_filtered(self, query):
        """Get wells with optional filtering"""
        self.update_well_data()  # Update well data before returning
        
        water_type = self.sanitize_input(query.get('waterType', [None])[0])
        status = self.sanitize_input(query.get('status', [None])[0])
        
        # Safely parse numeric parameters
        try:
            min_capacity = float(query.get('minCapacity', [None])[0]) if query.get('minCapacity', [None])[0] else None
        except (ValueError, TypeError):
            min_capacity = None
            
        try:
            max_capacity = float(query.get('maxCapacity', [None])[0]) if query.get('maxCapacity', [None])[0] else None
        except (ValueError, TypeError):
            max_capacity = None
        
        conn = self.get_db_connection()
        cursor = conn.cursor()
        
        # Build query dynamically based on filters
        query_parts = ["SELECT * FROM wells"]
        params = []
        
        where_clauses = []
        if water_type:
            where_clauses.append("water_type = ?")
            params.append(water_type)
        
        if status:
            where_clauses.append("status = ?")
            params.append(status)
        
        if min_capacity is not None:
            where_clauses.append("capacity >= ?")
            params.append(min_capacity)
        
        if max_capacity is not None:
            where_clauses.append("capacity <= ?")
            params.append(max_capacity)
        
        if where_clauses:
            query_parts.append("WHERE " + " AND ".join(where_clauses))
        
        cursor.execute(" ".join(query_parts), params)
        wells = cursor.fetchall()
        
        # Convert to dictionary format with ESP ID as key
        wells_dict = {}
        for well in wells:
            # Get latest water quality data
            cursor.execute('''
            SELECT ph, turbidity, tds 
            FROM water_quality 
            WHERE well_id = ? 
            ORDER BY timestamp DESC 
            LIMIT 1
            ''', (well['id'],))
            quality = cursor.fetchone()
            
            wells_dict[well['esp_id']] = {
                "id": well['id'],  # Include well ID for proper reference
                "wellName": well['well_name'],
                "wellOwner": well['well_owner'],
                "wellLocation": f"Location:\nlat: {well['latitude']}\nlon: {well['longitude']}",
                "wellWaterType": well['water_type'],
                "wellCapacity": well['capacity'],
                "wellWaterLevel": well['water_level'],
                "wellWaterConsumption": well['water_consumption'],
                "espId": well['esp_id'],
                "wellStatus": well['status'],
                "lastUpdated": well['last_updated'],
                "waterQuality": {
                    "ph": quality['ph'] if quality else 7.0,
                    "turbidity": quality['turbidity'] if quality else 1.0,
                    "tds": quality['tds'] if quality else 250
                }
            }
        
        conn.close()
        return json.dumps(wells_dict), 200
    
    def get_well_details(self, esp_id):
        """Get detailed information about a specific well"""
        self.update_well_data()  # Update well data before returning
        
        # Sanitize input
        sanitized_esp_id = self.sanitize_input(esp_id)
        
        conn = self.get_db_connection()
        cursor = conn.cursor()
        
        cursor.execute("SELECT * FROM wells WHERE esp_id = ?", (sanitized_esp_id,))
        well = cursor.fetchone()
        
        if not well:
            conn.close()
            return json.dumps({"error": "Well not found"}), 404
        
        # Get latest water quality data
        cursor.execute('''
        SELECT ph, turbidity, tds 
        FROM water_quality 
        WHERE well_id = ? 
        ORDER BY timestamp DESC 
        LIMIT 1
        ''', (well['id'],))
        quality = cursor.fetchone()
        
        well_data = {
            "id": well['id'],  # Include well ID for proper reference
            "wellName": well['well_name'],
            "wellOwner": well['well_owner'],
            "wellLocation": f"Location:\nlat: {well['latitude']}\nlon: {well['longitude']}",
            "wellWaterType": well['water_type'],
            "wellCapacity": well['capacity'],
            "wellWaterLevel": well['water_level'],
            "wellWaterConsumption": well['water_consumption'],
            "espId": well['esp_id'],
            "wellStatus": well['status'],
            "lastUpdated": well['last_updated'],
            "waterQuality": {
                "ph": quality['ph'] if quality else 7.0,
                "turbidity": quality['turbidity'] if quality else 1.0,
                "tds": quality['tds'] if quality else 250
            }
        }
        
        conn.close()
        return json.dumps(well_data), 200
    
    def get_well_stats(self):
        """Get statistics about all wells"""
        self.update_well_data()  # Update well data before returning
        
        conn = self.get_db_connection()
        cursor = conn.cursor()
        
        try:
            # Get total count
            cursor.execute("SELECT COUNT(*) FROM wells")
            total_wells = cursor.fetchone()[0]
            
            # Get capacity and levels
            cursor.execute("SELECT SUM(capacity) FROM wells")
            total_capacity = cursor.fetchone()[0] or 0
            
            cursor.execute("SELECT SUM(water_level) FROM wells")
            total_current_level = cursor.fetchone()[0] or 0
            
            cursor.execute("SELECT SUM(water_consumption) FROM wells")
            total_consumption = cursor.fetchone()[0] or 0
            
            # Get counts by status
            cursor.execute("SELECT status, COUNT(*) FROM wells GROUP BY status")
            status_counts = cursor.fetchall()
            
            # Get counts by water type
            cursor.execute("SELECT water_type, COUNT(*) FROM wells GROUP BY water_type")
            type_counts = cursor.fetchall()
            
            # Process status counts
            status_dict = {}
            for row in status_counts:
                status_dict[row[0]] = row[1]
            
            # Ensure all statuses are represented
            for status in ['Active', 'Maintenance', 'Inactive']:
                if status not in status_dict:
                    status_dict[status] = 0
            
            # Process water type counts
            type_dict = {}
            for row in type_counts:
                if row[0]:  # Skip null water types
                    type_dict[row[0]] = row[1]
            
            # Ensure common water types are represented
            for water_type in ['Clean', 'Grey']:
                if water_type not in type_dict:
                    type_dict[water_type] = 0
            
            # Calculate percentage of capacity currently filled
            capacity_percentage = (total_current_level / total_capacity * 100) if total_capacity > 0 else 0
            
            # Get average well fill level (as a percentage)
            cursor.execute("SELECT AVG(water_level / capacity * 100) FROM wells WHERE capacity > 0")
            avg_fill_level = cursor.fetchone()[0] or 0
            
            stats = {
                "totalWells": total_wells,
                "totalCapacity": round(total_capacity, 2),
                "totalCurrentLevel": round(total_current_level, 2),
                "totalConsumption": round(total_consumption, 2),
                "capacityPercentage": round(capacity_percentage, 2),
                "averageFillLevel": round(avg_fill_level, 2),
                "wellsByStatus": status_dict,
                "wellsByType": type_dict,
                "timestamp": datetime.now().isoformat()
            }
            
            conn.close()
            return json.dumps(stats), 200
            
        except Exception as e:
            conn.close()
            return json.dumps({
                "error": f"Failed to retrieve statistics: {str(e)}",
                "timestamp": datetime.now().isoformat()
            }), 500 