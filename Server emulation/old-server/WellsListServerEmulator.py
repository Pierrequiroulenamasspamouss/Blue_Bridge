import json
import os
import threading
from http.server import BaseHTTPRequestHandler, HTTPServer
from urllib.parse import urlparse, parse_qs
from datetime import datetime, timedelta
import random
import math

# Load static data
DATA_DIR = os.path.dirname(__file__)
WELL_LIST_FILE = os.path.join(DATA_DIR, 'well_list.json')

# Active services flags
active_services = {
    'wells': True,
    'data': True,
    'stats': True
}

def load_or_create_well_data():

    with open(WELL_LIST_FILE, 'r') as f:
        return json.load(f)


# Initialize well data
WELLS_DATA = load_or_create_well_data()

def update_well_data():
    """Simulate real-time updates to well data"""
    if not active_services['data']:
        return
        
    for well_id in WELLS_DATA:
        well = WELLS_DATA[well_id]
        if well["wellStatus"] == "Active":
            # Simulate water level changes
            consumption = random.uniform(0.8, 1.2) * well["wellWaterConsumption"]
            well["wellWaterLevel"] = max(0, min(
                well["wellCapacity"],
                well["wellWaterLevel"] - consumption/24  # Consumption per hour
            ))
            
            # Make sure waterQuality exists in well data
            if "waterQuality" not in well:
                well["waterQuality"] = {
                    "ph": 7.0,
                    "turbidity": 1.0,
                    "tds": 200
                }
            
            # Update water quality metrics
            well["waterQuality"]["ph"] = max(6.0, min(8.5, we   ll["waterQuality"]["ph"] + random.uniform(-0.1, 0.1)))
            well["waterQuality"]["turbidity"] = max(0.1, min(5.0, well["waterQuality"]["turbidity"] + random.uniform(-0.05, 0.05)))
            well["waterQuality"]["tds"] = max(100, min(1000, well["waterQuality"]["tds"] + random.uniform(-5, 5)))
            
            # Update timestamp
            well["lastUpdated"] = datetime.now().isoformat()
            
            # Automatically set to maintenance if water level is too low
            if well["wellWaterLevel"] < 0.1 * well["wellCapacity"]:
                well["wellStatus"] = "Maintenance"
                
    # Save updated data back to file
    with open(WELL_LIST_FILE, 'w') as f:
        json.dump(WELLS_DATA, f, indent=4)

class WellsHandler(BaseHTTPRequestHandler):
    def send_json_response(self, data, status=200):
        self.send_response(status)
        self.send_header('Content-Type', 'application/json')
        self.send_header('Access-Control-Allow-Origin', '*')
        self.end_headers()
        self.wfile.write(json.dumps(data).encode())

    def do_OPTIONS(self):
        self.send_response(200)
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', 'Content-Type')
        self.end_headers()

    def do_GET(self):
        parsed = urlparse(self.path)
        path = parsed.path
        query = parse_qs(parsed.query)

        if path == '/wells':
            if not active_services['wells']:
                self.send_json_response({"error": "Wells service unavailable"}, 503)
                return

            update_well_data()
            
            # Apply filters
            filtered_wells = WELLS_DATA.copy()
            
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
            
            # Return only simplified well data format
            simplified_wells = []
            for esp_id, well in filtered_wells.items():
                simplified_wells.append({
                    "wellName": well["wellName"],
                    "wellLocation": well["wellLocation"],
                    "wellWaterType": well["wellWaterType"],
                    "espId": esp_id
                })
            
            self.send_json_response(simplified_wells)

        elif path.startswith('/wells/') and path != '/wells/stats':
            if not active_services['data']:
                self.send_json_response({"error": "Data service unavailable"}, 503)
                return

            esp_id = path.split('/')[-1]
            if esp_id in WELLS_DATA:
                update_well_data()
                self.send_json_response(WELLS_DATA[esp_id])
            else:
                self.send_json_response({"error": "Well not found"}, 404)

        elif path == '/wells/stats':
            if not active_services['stats']:
                self.send_json_response({"error": "Stats service unavailable"}, 503)
                return

            update_well_data()
            
            stats = {
                "totalWells": len(WELLS_DATA),
                "totalCapacity": sum(well['wellCapacity'] for well in WELLS_DATA.values()),
                "totalCurrentLevel": sum(well['wellWaterLevel'] for well in WELLS_DATA.values()),
                "totalConsumption": sum(well['wellWaterConsumption'] for well in WELLS_DATA.values()),
                "wellsByStatus": {
                    "Active": sum(1 for well in WELLS_DATA.values() if well['wellStatus'] == 'Active'),
                    "Maintenance": sum(1 for well in WELLS_DATA.values() if well['wellStatus'] == 'Maintenance'),
                    "Inactive": sum(1 for well in WELLS_DATA.values() if well['wellStatus'] == 'Inactive')
                },
                "wellsByType": {
                    "Clean": sum(1 for well in WELLS_DATA.values() if well['wellWaterType'] == 'Clean'),
                    "Grey": sum(1 for well in WELLS_DATA.values() if well['wellWaterType'] == 'Grey')
                },
                "timestamp": datetime.now().isoformat()
            }
            self.send_json_response(stats)
        else:
            self.send_json_response({"error": "Invalid endpoint"}, 404)

def console_thread():
    """Interactive console for controlling services"""
    print("Interactive mode: type 'shutdown <service>' or 'boot <service>' (wells, data, stats)")
    while True:
        try:
            cmd = input().strip().split()
            if len(cmd) == 2 and cmd[0] in ('shutdown', 'boot') and cmd[1] in active_services:
                active_services[cmd[1]] = (cmd[0] == 'boot')
                print(f"Service '{cmd[1]}' now {'active' if active_services[cmd[1]] else 'inactive'}")
        except Exception as e:
            print(f"Error processing command: {e}")

if __name__ == '__main__':
    port = 8090
    server = HTTPServer(('', port), WellsHandler)
    
    # Start console thread
    thread = threading.Thread(target=console_thread, daemon=True)
    thread.start()
    
    print(f"Wells server running on port {port}")
    print("\nAvailable endpoints:")
    print("- GET /wells/index - List all wells (with optional filters)")
    print("- GET /wells/<esp_id> - Get specific well details")
    print("- GET /wells/stats - Get well statistics")
    print("\nAvailable services:", list(active_services.keys()))
    
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nShutting down server...")

