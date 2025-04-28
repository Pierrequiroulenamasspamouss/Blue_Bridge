import json
import os
import threading
from http.server import BaseHTTPRequestHandler, HTTPServer
from urllib.parse import urlparse, parse_qs, unquote
from datetime import datetime, timedelta
import math
import hashlib
import base64
import random

# Load static data
DATA_DIR = os.path.dirname(__file__)
WELL_LIST_FILE = os.path.join(DATA_DIR, 'well_list.json')
USER_DB_FILE = os.path.join(DATA_DIR, 'user_database.json')

with open(USER_DB_FILE, 'r') as f:
    users = json.load(f)

def load_or_create_well_data():
    try:
        with open(WELL_LIST_FILE, 'r') as f:
            return json.load(f)
    except FileNotFoundError:
        # Create sample data
        sample_data = {
            "esp32-01": {
                "wellName": "Simulated Well 1",
                "wellOwner": "Owner A",
                "wellLocation": "Location:\nlat: 0.000000\nlon: 25.000000",
                "wellWaterType": "Clean",
                "wellCapacity": 5000,
                "wellWaterLevel": 2500,
                "wellWaterConsumption": 300,
                "espId": "esp32-01",
                "wellStatus": "Active",
                "lastUpdated": (datetime.now() - timedelta(minutes=5)).isoformat(),
                "waterQuality": {
                    "ph": 7.2,
                    "turbidity": 1.2,
                    "tds": 250
                }
            },
            "esp32-02": {
                "wellName": "Simulated Well 2",
                "wellOwner": "Owner B",
                "wellLocation": "Location:\nlat: 1.234567\nlon: 26.234567",
                "wellWaterType": "Grey",
                "wellCapacity": 6000,
                "wellWaterLevel": 3000,
                "wellWaterConsumption": 350,
                "espId": "esp32-02",
                "wellStatus": "Maintenance",
                "lastUpdated": (datetime.now() - timedelta(hours=1)).isoformat(),
                "waterQuality": {
                    "ph": 6.8,
                    "turbidity": 2.5,
                    "tds": 450
                }
            },
            "esp32-03": {
                "wellName": "Simulated Well 3",
                "wellOwner": "Owner C",
                "wellLocation": "Location:\nlat: -0.987654\nlon: 24.987654",
                "wellWaterType": "Clean",
                "wellCapacity": 4500,
                "wellWaterLevel": 2000,
                "wellWaterConsumption": 250,
                "espId": "esp32-03",
                "wellStatus": "Inactive",
                "lastUpdated": (datetime.now() - timedelta(days=1)).isoformat(),
                "waterQuality": {
                    "ph": 7.5,
                    "turbidity": 0.8,
                    "tds": 180
                }
            }
        }
        with open(WELL_LIST_FILE, 'w') as f:
            json.dump(sample_data, f, indent=4)
        return sample_data

wells = load_or_create_well_data()

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
        
    for well_id in wells:
        well = wells[well_id]
        if well["wellStatus"] == "Active":
            # Simulate water level changes
            consumption = random.uniform(0.8, 1.2) * well["wellWaterConsumption"]
            well["wellWaterLevel"] = max(0, min(
                well["wellCapacity"],
                well["wellWaterLevel"] - consumption/24  # Consumption per hour
            ))
            
            # Update water quality metrics
            well["waterQuality"]["ph"] = max(6.0, min(8.5, well["waterQuality"]["ph"] + random.uniform(-0.1, 0.1)))
            well["waterQuality"]["turbidity"] = max(0.1, min(5.0, well["waterQuality"]["turbidity"] + random.uniform(-0.05, 0.05)))
            well["waterQuality"]["tds"] = max(100, min(1000, well["waterQuality"]["tds"] + random.uniform(-5, 5)))
            
            # Update timestamp
            well["lastUpdated"] = datetime.now().isoformat()
            
            # Automatically set to maintenance if water level is too low
            if well["wellWaterLevel"] < 0.1 * well["wellCapacity"]:
                well["wellStatus"] = "Maintenance"
                
    # Save updated data back to file
    with open(WELL_LIST_FILE, 'w') as f:
        json.dump(wells, f, indent=4)

# Add a function to update user's lastUpdated timestamp
def update_user_last_activity(email):
    """Update the lastUpdated timestamp for a user if they exist in the database"""
    if email and email in users:
        users[email]['lastUpdated'] = datetime.now().isoformat()
        # Save the updated data
        with open(USER_DB_FILE, 'w') as f:
            json.dump(users, f, indent=4)
        return True
    return False

class CombinedHandler(BaseHTTPRequestHandler):
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

        # Wells endpoints
        if path == '/wells':
            if not active_services['wells']:
                self.send_common_headers(503)
                self.wfile.write(json.dumps({"error": "Wells service unavailable"}).encode())
                return

            update_well_data()
            # Convert to format expected by EspApiService.getAllWells()
            wells_list = []
            for well_id, well_data in wells.items():
                # Create shortened well data with only the required fields
                shortened_well = {
                    "wellName": well_data.get("wellName", ""),
                    "wellLocation": well_data.get("wellLocation", ""),
                    "wellWaterType": well_data.get("wellWaterType", ""),
                    "espId": well_id,  # Use the well_id as the espId
                    "wellStatus": well_data.get("wellStatus", "Unknown"),
                    "wellOwner": well_data.get("wellOwner", ""),
                    "wellCapacity": str(well_data.get("wellCapacity", "")),
                    "wellWaterLevel": str(well_data.get("wellWaterLevel", "")),
                    "wellWaterConsumption": str(well_data.get("wellWaterConsumption", ""))
                }
                wells_list.append(shortened_well)
            
            self.send_common_headers()
            self.wfile.write(json.dumps(wells_list).encode())
            return

        elif path == '/wells/index':
            if not active_services['wells']:
                self.send_common_headers(503)
                self.wfile.write(json.dumps({"error": "Wells service unavailable"}).encode())
                return

            update_well_data()
            filtered_wells = wells.copy()
            
            if 'waterType' in query:
                water_type = query['waterType'][0]
                filtered_wells = {k: v for k, v in filtered_wells.items() 
                                if v['wellWaterType'].lower() == water_type.lower()}
            
            if 'status' in query:
                status = query['status'][0]
                filtered_wells = {k: v for k, v in filtered_wells.items() 
                                if v['wellStatus'].lower() == status.lower()}
            
            if 'minCapacity' in query or 'maxCapacity' in query:
                min_cap = float(query.get('minCapacity', [0])[0])
                max_cap = float(query.get('maxCapacity', [float('inf')])[0])
                filtered_wells = {k: v for k, v in filtered_wells.items() 
                                if min_cap <= v['wellCapacity'] <= max_cap}
            
            self.send_common_headers()
            self.wfile.write(json.dumps(filtered_wells).encode())
            return

        elif path.startswith('/wells/') and not path.endswith('/stats'):
            if not active_services['data']:
                self.send_common_headers(503)
                self.wfile.write(json.dumps({"error": "Data service unavailable"}).encode())
                return

            esp_id = path.split('/')[-1]
            if esp_id in wells:
                update_well_data()
                self.send_common_headers()
                self.wfile.write(json.dumps(wells[esp_id]).encode())
            else:
                self.send_common_headers(404)
                self.wfile.write(json.dumps({"error": "Well not found"}).encode())
            return

        elif path == '/wells/stats':
            if not active_services['stats']:
                self.send_common_headers(503)
                self.wfile.write(json.dumps({"error": "Stats service unavailable"}).encode())
                return

            update_well_data()
            stats = {
                "totalWells": len(wells),
                "totalCapacity": sum(well['wellCapacity'] for well in wells.values()),
                "totalCurrentLevel": sum(well['wellWaterLevel'] for well in wells.values()),
                "totalConsumption": sum(well['wellWaterConsumption'] for well in wells.values()),
                "wellsByStatus": {
                    "Active": sum(1 for well in wells.values() if well['wellStatus'] == 'Active'),
                    "Maintenance": sum(1 for well in wells.values() if well['wellStatus'] == 'Maintenance'),
                    "Inactive": sum(1 for well in wells.values() if well['wellStatus'] == 'Inactive')
                },
                "wellsByType": {
                    "Clean": sum(1 for well in wells.values() if well['wellWaterType'] == 'Clean'),
                    "Grey": sum(1 for well in wells.values() if well['wellWaterType'] == 'Grey')
                },
                "timestamp": datetime.now().isoformat()
            }
            self.send_common_headers()
            self.wfile.write(json.dumps(stats).encode())
            return

        # Existing endpoints
        elif path.startswith('/data/wells/'):
            if not active_services['data']:
                self.send_common_headers(503)
                self.wfile.write(json.dumps({'error':'Data service unavailable'}).encode())
                return
            parts = path.split('/')
            if len(parts) >= 4 and parts[3] in wells:
                esp_id = parts[3]
                self.send_common_headers(200)
                self.wfile.write(json.dumps(wells[esp_id]).encode())
            else:
                self.send_common_headers(404)
                self.wfile.write(json.dumps({'error':'Well not found'}).encode())
        
        # Add endpoint for getting all wells at /data/wells - return as ShortenedWellData format
        elif path == '/data/wells':
            if not active_services['data']:
                self.send_common_headers(503)
                self.wfile.write(json.dumps({'error':'Data service unavailable'}).encode())
                return
            
            update_well_data()
            # Convert to format expected by client (list of ShortenedWellData)
            wells_list = []
            for well_id, well_data in wells.items():
                # Create shortened well data with only the required fields
                shortened_well = {
                    "wellName": well_data.get("wellName", ""),
                    "wellLocation": well_data.get("wellLocation", ""),
                    "wellWaterType": well_data.get("wellWaterType", ""),
                    "espId": well_id,  # Use the well_id as the espId
                    "wellStatus": well_data.get("wellStatus", "Unknown"),
                    "wellOwner": well_data.get("wellOwner", ""),
                    "wellCapacity": str(well_data.get("wellCapacity", "")),
                    "wellWaterLevel": str(well_data.get("wellWaterLevel", "")),
                    "wellWaterConsumption": str(well_data.get("wellWaterConsumption", ""))
                }
                wells_list.append(shortened_well)
            
            self.send_common_headers(200)
            self.wfile.write(json.dumps(wells_list).encode())
            return

        elif path.startswith('/nearby-users'):
            if not active_services['nearby']:
                self.send_common_headers(503)
                self.wfile.write(json.dumps({'error':'Nearby service unavailable'}).encode())
                return

            # Parse params
            params = parse_qs(parsed.query)
            try:
                lat = float(params.get('latitude', ['0'])[0])
                lon = float(params.get('longitude', ['0'])[0])
                radius_km = float(params.get('radius', ['50'])[0])
                requester = unquote(params.get('email', [''])[0])
            except Exception:
                self.send_common_headers(400)
                self.wfile.write(json.dumps({'error':'Invalid parameters'}).encode())
                return

            # Haversine helper
            def haversine(lat1, lon1, lat2, lon2):
                R = 6371.0
                dlat = math.radians(lat2 - lat1)
                dlon = math.radians(lon2 - lon1)
                a = math.sin(dlat/2)**2 + math.cos(math.radians(lat1))*math.cos(math.radians(lat2))*math.sin(dlon/2)**2
                return R * 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))

            results = []
            for email, u in users.items():
                if email == requester:
                    continue
                loc = u.get('location')
                if not loc:
                    continue
                ulat = loc.get('latitude')
                ulon = loc.get('longitude')
                if ulat is None or ulon is None:
                    continue
                dist = haversine(lat, lon, ulat, ulon)
                if dist <= radius_km:
                    user_info = {
                        'firstName': u.get('firstName',''),
                        'lastName': u.get('lastName',''),
                        'username': u.get('username',''),
                        'role': u.get('role','user'),
                        'distance': round(dist,2),
                        'location': {'latitude': ulat, 'longitude': ulon},
                        'waterNeeds': u.get('waterNeeds', 0),
                        'isOnline': True
                    }
                    results.append(user_info)

            resp = {'status':'success', 'users': results, 'timestamp': datetime.now().isoformat()}
            self.send_common_headers(200)
            self.wfile.write(json.dumps(resp).encode())
        else:
            self.send_common_headers(404)
            self.wfile.write(json.dumps({'error':'Invalid GET endpoint'}).encode())

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

        if path in ['/login', '/register']:
            if not active_services['login']:
                self.send_common_headers(503)
                self.wfile.write(json.dumps({'error':'Auth service unavailable'}).encode())
                return
            if path == '/login':
                return self.handle_login(data)
            else:
                return self.handle_register(data)
        elif path in ['/update-location', '/update-water-needs']:
            if not active_services['nearby']:
                self.send_common_headers(503)
                self.wfile.write(json.dumps({'error':'Nearby service unavailable'}).encode())
                return
            if path == '/update-location':
                return self.handle_update_location(data)
            else:
                return self.handle_update_water_needs(data)
        else:
            self.send_common_headers(404)
            self.wfile.write(json.dumps({'error':'Invalid POST endpoint'}).encode())

    def handle_login(self, creds):
        email = creds.get('email')
        pwd = creds.get('password')
        if not email or not pwd:
            self.send_common_headers(400)
            self.wfile.write(json.dumps({'status':'error','message':'Missing credentials'}).encode())
            return

        user = users.get(email)
        if not user:
            self.send_common_headers(401)
            self.wfile.write(json.dumps({'status':'error','message':'Invalid credentials'}).encode())
            return

        # Hash the provided password for comparison
        hashed_pwd = base64.b64encode(hashlib.sha256(pwd.encode()).digest()).decode()
        if user.get('password') != hashed_pwd:
            self.send_common_headers(401)
            self.wfile.write(json.dumps({'status':'error','message':'Invalid credentials'}).encode())
            return

        # Build user object for response
        user_resp = {
            'email': email,
            'firstName': user.get('firstName', ''),
            'lastName': user.get('lastName', ''),
            'username': user.get('username', ''),
            'role': user.get('role', 'user'),
            'lastLogin': datetime.now().isoformat(),
        }
        resp = {
            'status': 'success',
            'timestamp': datetime.now().isoformat(),
            'data': {'user': user_resp},
            'message': 'Login successful'
        }
        self.send_common_headers(200)
        self.wfile.write(json.dumps(resp).encode())

    def handle_register(self, user_data):
        required_fields = ['email', 'password', 'firstName', 'lastName', 'username', 'location', 'waterNeeds']
        
        # Check for missing fields
        for field in required_fields:
            if field not in user_data:
                self.send_common_headers(400)
                self.wfile.write(json.dumps({'status':'error','message':f'Missing field: {field}'}).encode())
                return

        email = user_data.get('email')
        if email in users:
            self.send_common_headers(409)
            self.wfile.write(json.dumps({'status':'error','message':'User exists'}).encode())
            return

        # Hash the user's password before saving
        pwd = user_data.get('password', '')
        hashed_pwd = base64.b64encode(hashlib.sha256(pwd.encode()).digest()).decode()
        user_data['password'] = hashed_pwd
        
        # Set default role if not provided
        user_data['role'] = user_data.get('role', 'user')
        
        users[email] = user_data
        with open(USER_DB_FILE, 'w') as f:
            json.dump(users, f, indent=4)
        
        self.send_common_headers(200)
        self.wfile.write(json.dumps({'status':'success','timestamp':datetime.now().isoformat(),'message':'Registered'}).encode())
    def handle_update_location(self, data):
        email = data.get('email')
        if not email or email not in users:
            self.send_common_headers(404)
            self.wfile.write(json.dumps({'status':'error','message':'User not found'}).encode())
            return
        users[email]['location'] = data.get('location', {})
        with open(USER_DB_FILE, 'w') as f:
            json.dump(users, f, indent=4)
        self.send_common_headers(200)
        self.wfile.write(json.dumps({'status':'success','message':'Location updated'}).encode())

    def handle_update_water_needs(self, data):
        email = data.get('email')
        if not email or email not in users:
            self.send_common_headers(404)
            self.wfile.write(json.dumps({'status':'error','message':'User not found'}).encode())
            return
        users[email]['waterNeeds'] = data.get('waterNeeds')
        with open(USER_DB_FILE, 'w') as f:
            json.dump(users, f, indent=4)
        self.send_common_headers(200)
        self.wfile.write(json.dumps({'status':'success','message':'Water needs updated'}).encode())


def console_thread():
    print("Interactive mode: type 'shutdown <service>' or 'boot <service>' (login, data, nearby, wells, stats)")
    while True:
        cmd = input().strip().split()
        if len(cmd) == 2 and cmd[0] in ('shutdown','boot') and cmd[1] in active_services:
            active_services[cmd[1]] = (cmd[0] == 'boot')
            print(f"Service '{cmd[1]}' now {'active' if active_services[cmd[1]] else 'inactive'}")

if __name__ == '__main__':
    port = 8090
    server = HTTPServer(('', port), CombinedHandler)
    # Start console for interactive commands
    thread = threading.Thread(target=console_thread, daemon=True)
    thread.start()
    print(f"Server running on port {port}")
    # List available endpoints
    print("Available endpoints:")
    print("\nWell endpoints:")
    print("- GET /wells - Get all wells as ShortenedWellData format (for EspApiService.getAllWells)")
    print("- GET /wells/index - List all wells (with optional filters)")
    print("- GET /wells/<esp_id> - Get specific well details")
    print("- GET /wells/stats - Get well statistics")
    print("\nData endpoints:")
    print("- GET /data/wells - Get all wells as ShortenedWellData format (alternative)")
    for esp_id in wells.keys():
        print(f"- GET /data/wells/{esp_id}")
    print("\nAvailable services:", list(active_services.keys()))
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("Shutting down server...") 