import json
import os
import random
import threading
import socket
import time
from http.server import BaseHTTPRequestHandler, HTTPServer
from datetime import datetime
from urllib.parse import urlparse, parse_qs

DATABASE_FILE = 'user_database.json'
start_time = time.time()

class LoginRequestHandler(BaseHTTPRequestHandler):
    def do_POST(self):
        print(f"Connection received from {self.client_address}")
        if '/login' in self.path:
            credentials = {}
            
            # Try to get credentials from query parameters first
            parsed_url = urlparse(self.path)
            query_params = parse_qs(parsed_url.query)
            
            if 'email' in query_params and 'password' in query_params:
                credentials = {
                    'email': query_params['email'][0],
                    'password': query_params['password'][0]
                }
            else:
                # If not in query params, try to get from JSON body
                try:
                    content_length = int(self.headers.get('Content-Length', 0))
                    if content_length > 0:
                        post_data = self.rfile.read(content_length)
                        credentials = json.loads(post_data)
                except (ValueError, json.JSONDecodeError) as e:
                    print(f"Error parsing request body: {e}")
                    self.send_error(400, "Invalid request format")
                    return

            if not credentials or 'email' not in credentials or 'password' not in credentials:
                self.send_error(400, "Missing credentials")
                return

            # Load user database
            if os.path.exists(DATABASE_FILE):
                with open(DATABASE_FILE, 'r') as db_file:
                    user_db = json.load(db_file)
            else:
                user_db = {}

            # Check credentials
            user = user_db.get(credentials['email'])
            if user and user['password'] == credentials['password']:
                response = {
                    'status': 'success',
                    'timestamp': datetime.now().isoformat(),
                    'data': {
                        'user': {
                            'email': credentials['email'],
                            'firstName': user.get('firstName', ''),
                            'lastName': user.get('lastName', ''),
                            'username': user.get('username', ''),
                            'role': user.get('role', 'user'),
                            'lastLogin': datetime.now().isoformat()
                        }
                    },
                    'message': 'Login successful'
                }
                self.send_response(200)
            else:
                response = {
                    'status': 'error',
                    'timestamp': datetime.now().isoformat(),
                    'error': {
                        'code': 'AUTH_FAILED',
                        'message': 'Invalid credentials'
                    }
                }
                self.send_response(401)

            response_data = json.dumps(response).encode('utf-8')
            self.send_header('Content-type', 'application/json')
            self.send_header('Access-Control-Allow-Origin', '*')
            self.send_header('Content-Length', str(len(response_data)))
            self.end_headers()
            self.wfile.write(response_data)
        else:
            self.send_error(404, "Endpoint not found")

    def do_OPTIONS(self):
        self.send_response(200)
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'POST, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', 'Content-Type')
        self.end_headers()

    def do_GET(self):
        print(f"GET request received from {self.client_address}")
        if self.path == '/login':
            response = {
                'status': 'error',
                'timestamp': datetime.now().isoformat(),
                'error': {
                    'code': 'METHOD_NOT_ALLOWED',
                    'message': 'Use POST method for login'
                }
            }
            response_data = json.dumps(response).encode('utf-8')
            self.send_response(405)
            self.send_header('Content-type', 'application/json')
            self.send_header('Content-Length', str(len(response_data)))
            self.end_headers()
            self.wfile.write(response_data)
        else:
            response = {
                'status': 'error',
                'timestamp': datetime.now().isoformat(),
                'error': {
                    'code': 'NOT_FOUND',
                    'message': 'Endpoint not found'
                }
            }
            response_data = json.dumps(response).encode('utf-8')
            self.send_response(404)
            self.send_header('Content-type', 'application/json')
            self.send_header('Content-Length', str(len(response_data)))
            self.end_headers()
            self.wfile.write(response_data)

    def log_message(self, format, *args):
        return  # Disable logging


class ESP32SimulatedHandler(BaseHTTPRequestHandler):
    wells_data = {}  # Class variable to store wells data
    
    @classmethod
    def initialize_wells(cls):
        if not cls.wells_data:  # Only initialize if not already initialized
            print("Initializing wells data...")
            for i in range(1, 11):  # Create 10 wells
                well_id = f"WELL_{i}"
                cls.wells_data[well_id] = {
                    "id": well_id,
                    "water_level": random.uniform(0, 100),
                    "consumption": random.uniform(0, 1000),
                    "battery_level": random.uniform(0, 100),
                    "signal_strength": random.randint(0, 5),
                    "last_update": datetime.now().isoformat()
                }
    
    def do_GET(self):
        # Initialize wells if not already done
        ESP32SimulatedHandler.initialize_wells()
        
        # Set response headers
        self.send_response(200)
        self.send_header('Content-type', 'application/json')
        self.send_header('Access-Control-Allow-Origin', '*')
        self.end_headers()
        
        # Return the current wells data
        response = {
            "wells": list(self.wells_data.values()),
            "timestamp": datetime.now().isoformat()
        }
        self.wfile.write(json.dumps(response).encode())

    def log_message(self, format, *args):
        # Suppress logging for cleaner output
        pass


def create_handler(shared_data):
    def handler(*args, **kwargs):
        ESP32SimulatedHandler(*args, shared_data=shared_data, **kwargs)
    return handler


def find_local_ip():
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
        s.close()
        return ip
    except Exception:
        return "127.0.0.1"


def generate_base_data(well_id):
    return {
        "wellId": f"WELL_{well_id:03d}",
        "metadata": {
            "deviceId": f"ESP32_{well_id:03d}",
            "firmwareVersion": "v1.2.3",
            "lastUpdate": datetime.now().isoformat(),
            "installationDate": "2024-01-01"
        },
        "wellData": {
            "wellWaterLevel": random.randint(1000, 5000),
            "wellWaterConsumption": random.randint(100, 500),
            "wellDepth": random.randint(5000, 10000),
            "wellDiameter": random.randint(100, 300)
        },
        "status": {
            "isOnline": True,
            "batteryLevel": random.randint(0, 100),
            "signalStrength": random.randint(-100, -50),
            "lastMaintenance": "2024-01-01"
        },
        "location": {
            "latitude": random.uniform(-1.5, 1.5),
            "longitude": random.uniform(30.0, 33.0),
            "altitude": random.randint(1000, 2000)
        }
    }


def add_random_extra_fields(data):
    # Randomly add optional fields
    if random.random() > 0.5:
        data["wellData"]["waterQuality"] = {
            "pH": round(random.uniform(6.5, 8.5), 2),
            "turbidity": round(random.uniform(0, 5), 2),
            "dissolvedSolids": round(random.uniform(100, 500), 2)
        }
    
    if random.random() > 0.7:
        data["alerts"] = []
        if random.random() > 0.8:
            data["alerts"].append({
                "type": "WARNING",
                "message": "Low water level detected",
                "timestamp": datetime.now().isoformat()
            })
    
    return data


def update_data_loop(shared_data, base_data, delay=1):
    while True:
        updated_data = base_data.copy()

        # Change dynamic values
        updated_data["wellData"]["wellWaterLevel"] = random.randint(1000, 5000)
        updated_data["wellData"]["wellWaterConsumption"] = random.randint(100, 500)

        # Add extra fields
        updated_data = add_random_extra_fields(updated_data)

        with shared_data['lock']:
            shared_data['data'] = updated_data

        time.sleep(delay)


def run_login_server(port=8090):
    server_address = ('192.168.0.98', port)
    httpd = HTTPServer(server_address, LoginRequestHandler)
    print(f'Login server running at http://{server_address[0]}:{port}/login')
    httpd.serve_forever()


def run_data_servers(num_servers):
    base_port = 8081
    for i in range(num_servers):
        port = base_port + i
        base_data = generate_base_data(i)

        shared_data = {
            'data': base_data,
            'lock': threading.Lock()
        }

        # Start data updater thread
        updater_thread = threading.Thread(
            target=update_data_loop,
            args=(shared_data, base_data),
            daemon=True
        )
        updater_thread.start()

        # Start HTTP server thread
        server_thread = threading.Thread(
            target=run_server,
            args=(port, shared_data),
            daemon=True
        )
        server_thread.start()

    print(f"\nSimulating {num_servers} ESP32 servers. Press Ctrl+C to stop.")
    try:
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        print("\nShutting down.")


def run_server(port, shared_data):
    server = HTTPServer(("0.0.0.0", port), create_handler(shared_data))
    print(f"Simulated ESP32 running at http://{find_local_ip()}:{port}/data")
    server.serve_forever()


def update_data_in_loop():
    while True:
        # Initialize wells if not already done
        ESP32SimulatedHandler.initialize_wells()
        
        # Update data for each well
        for well_id, well_data in ESP32SimulatedHandler.wells_data.items():
            # Update water level (slight random change)
            well_data["water_level"] = max(0, min(100, 
                well_data["water_level"] + random.uniform(-5, 5)))
            
            # Update consumption (add some random amount)
            well_data["consumption"] += random.uniform(0, 10)
            
            # Update battery level (slight decrease)
            well_data["battery_level"] = max(0, 
                well_data["battery_level"] - random.uniform(0, 0.5))
            
            # Update signal strength (occasional random changes)
            if random.random() < 0.1:  # 10% chance to change
                well_data["signal_strength"] = random.randint(0, 5)
            
            # Update timestamp
            well_data["last_update"] = datetime.now().isoformat()
        
        time.sleep(10)  # Update every 10 seconds


def main():
    port = 8090
    server_address = ('', port)
    
    # Create and start the data update thread
    update_thread = threading.Thread(target=update_data_in_loop, daemon=True)
    update_thread.start()
    
    # Start the server
    print(f"Starting server on port {port}...")
    httpd = HTTPServer(server_address, ESP32SimulatedHandler)
    
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        print("\nShutting down server...")
        httpd.server_close()


if __name__ == "__main__":
    main() 