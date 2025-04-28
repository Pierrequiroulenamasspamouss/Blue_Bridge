import json
import os
import sqlite3
import math
from datetime import datetime, timedelta

# Database setup
DB_DIR = os.path.dirname(__file__)
DB_PATH = os.path.join(DB_DIR, 'wellconnect.db')

class NearbyServer:
    """Nearby users server for handling proximity searches"""
    
    def __init__(self, db_path=DB_PATH):
        self.db_path = db_path
        
    def get_db_connection(self):
        conn = sqlite3.connect(self.db_path)
        conn.row_factory = sqlite3.Row
        return conn
    
    def haversine(self, lat1, lon1, lat2, lon2):
        """
        Calculate the great circle distance between two points 
        on the earth (specified in decimal degrees)
        """
        # Convert decimal degrees to radians
        lat1, lon1, lat2, lon2 = map(math.radians, [lat1, lon1, lat2, lon2])
        
        # Haversine formula
        dlat = lat2 - lat1
        dlon = lon2 - lon1
        a = math.sin(dlat/2)**2 + math.cos(lat1) * math.cos(lat2) * math.sin(dlon/2)**2
        c = 2 * math.asin(math.sqrt(a))
        r = 6371  # Radius of earth in kilometers
        return c * r
    
    def get_nearby_users(self, query):
        """Find users within a specified radius"""
        if 'latitude' not in query or 'longitude' not in query:
            return json.dumps({
                'status': 'error',
                'message': 'Missing coordinates'
            }), 400
            
        try:
            lat = float(query.get('latitude', ['0'])[0])
            lon = float(query.get('longitude', ['0'])[0])
            radius_km = float(query.get('radius', ['50'])[0])
            requester_email = query.get('email', [''])[0]
        except ValueError:
            return json.dumps({
                'status': 'error',
                'message': 'Invalid coordinates'
            }), 400
            
        conn = self.get_db_connection()
        cursor = conn.cursor()
        
        # Get all users except the requester
        cursor.execute('''
        SELECT 
            id, email, first_name, last_name, username, role,
            latitude, longitude, last_updated, is_online
        FROM users
        WHERE email != ? AND is_online = 1
        ''', (requester_email,))
        
        users = cursor.fetchall()
        nearby_users = []
        
        # Filter users by distance
        for user in users:
            # Check if user has valid coordinates
            user_lat = user['latitude']
            user_lon = user['longitude']
            
            if user_lat is None or user_lon is None:
                continue
                
            # Calculate distance
            dist = self.haversine(lat, lon, user_lat, user_lon)
            
            # Only include users within the radius
            if dist <= radius_km:
                # Get water needs for this user
                cursor.execute('''
                SELECT amount, usage_type, description, priority
                FROM water_needs
                WHERE user_id = ?
                ''', (user['id'],))
                
                water_needs = [
                    {
                        'amount': need['amount'],
                        'usageType': need['usage_type'],
                        'description': need['description'],
                        'priority': need['priority']
                    } for need in cursor.fetchall()
                ]
                
                # Calculate online status (online if updated in last 10 minutes)
                is_recently_active = False
                if user['last_updated']:
                    try:
                        last_active = datetime.fromisoformat(user['last_updated'])
                        is_recently_active = (datetime.now() - last_active) < timedelta(minutes=10)
                    except (ValueError, TypeError):
                        pass
                
                nearby_users.append({
                    'firstName': user['first_name'],
                    'lastName': user['last_name'],
                    'username': user['username'],
                    'role': user['role'],
                    'distance': round(dist, 2),
                    'location': {
                        'latitude': user_lat,
                        'longitude': user_lon
                    },
                    'waterNeeds': water_needs,
                    'isOnline': is_recently_active
                })
        
        conn.close()
        
        response = {
            'status': 'success',
            'users': nearby_users,
            'timestamp': datetime.now().isoformat()
        }
        
        return json.dumps(response), 200 