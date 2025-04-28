import json
import random
import threading
import socket
import time
from http.server import BaseHTTPRequestHandler, HTTPServer

class ESP32SimulatedHandler(BaseHTTPRequestHandler):
    def __init__(self, *args, shared_data=None, **kwargs):
        self.shared_data = shared_data
        super().__init__(*args, **kwargs)

    def do_GET(self):
        # Extract well ID from path (e.g., /data/well1 -> well1)
        path_parts = self.path.split('/')
        if len(path_parts) >= 3 and path_parts[1] == 'data':
            well_id = path_parts[2]
            if well_id in self.shared_data['wells']:
                self.send_response(200)
                self.send_header('Content-type', 'application/json')
                self.end_headers()
                # Read latest data for specific well
                with self.shared_data['lock']:
                    current_data = json.dumps(self.shared_data['wells'][well_id])
                self.wfile.write(current_data.encode('utf-8'))
            else:
                self.send_response(404)
                self.send_header('Content-type', 'application/json')
                self.end_headers()
                error_msg = json.dumps({"error": f"Well {well_id} not found"})
                self.wfile.write(error_msg.encode('utf-8'))
        else:
            self.send_response(404)
            self.send_header('Content-type', 'application/json')
            self.end_headers()
            error_msg = json.dumps({"error": "Invalid endpoint"})
            self.wfile.write(error_msg.encode('utf-8'))


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


def generate_base_data(i):
    # Generate random coordinates within a reasonable range
    # This example uses coordinates roughly in central Africa
    base_lat = 0.0  # Equator
    base_lon = 25.0  # Roughly central Africa
    
    # Add some random variation (approximately within 1000km)
    lat = base_lat + random.uniform(-5, 5)  # ±5 degrees latitude
    lon = base_lon + random.uniform(-5, 5)  # ±5 degrees longitude
    
    return {
        "wellName": f"Simulated Well {i + 1}",
        "wellOwner": f"Owner {i + 1}",
        "wellLocation": f"Location:\nlat: {lat:.6f}\nlon: {lon:.6f}",
        "wellWaterType": "Clean" if i % 2 == 0 else "Grey",
        "wellCapacity": random.randint(3000, 10000),
        "wellWaterLevel": random.randint(1000, 5000),
        "wellWaterConsumption": random.randint(100, 500),
        "espId": f"esp32-{i + 1:02d}",
        "wellStatus": random.choice(["Active", "Inactive", "Maintenance", "Low Water"])
    }


def add_random_extra_fields(data):
    optional_fields = {
        "wellPrice": lambda: round(random.uniform(1000, 5000), 2),
        "accessibility": lambda: random.choice(["Easy", "Moderate", "Difficult"]),
        "wellDepth": lambda: random.randint(10, 100),
        "pHLevel": lambda: round(random.uniform(6.5, 8.5), 2),
        "temperature": lambda: round(random.uniform(15, 30), 1),
    }

    extra_keys = random.sample(list(optional_fields.keys()), random.randint(1, 3))
    for key in extra_keys:
        data[key] = optional_fields[key]()
    return data


def update_data_loop(shared_data, num_wells, delay=1):
    # Initialize wells data
    wells = {}
    for i in range(num_wells):
        well_id = f"well{i+1}"
        wells[well_id] = generate_base_data(i)

    # Store wells in shared data
    with shared_data['lock']:
        shared_data['wells'] = wells

    while True:
        for well_id in wells:
            updated_data = wells[well_id].copy()

            # Change dynamic values
            updated_data["wellWaterLevel"] = random.randint(1000, 5000)
            updated_data["wellWaterConsumption"] = random.randint(100, 500)

            # Add extra fields
            updated_data = add_random_extra_fields(updated_data)

            with shared_data['lock']:
                shared_data['wells'][well_id] = updated_data

        time.sleep(delay)


def main():
    try:
        num_wells = int(input("How many wells would you like to simulate? "))
        if num_wells <= 0:
            print("Please enter a positive number.")
            return
    except ValueError:
        print("Invalid input. Please enter a number.")
        return

    port = 8090
    shared_data = {
        'wells': {},
        'lock': threading.Lock()
    }

    # Start data updater thread
    updater_thread = threading.Thread(
        target=update_data_loop,
        args=(shared_data, num_wells),
        daemon=True
    )
    updater_thread.start()

    # Create and start HTTP server
    handler = lambda *args, **kwargs: ESP32SimulatedHandler(*args, shared_data=shared_data, **kwargs)
    server = HTTPServer(('', port), handler)
    print(f"\nServer running on port {port}")
    print(f"Simulating {num_wells} wells. Available endpoints:")
    for i in range(num_wells):
        print(f"http://localhost:{port}/data/well{i+1}")
    print("\nPress Ctrl+C to stop.")

    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nShutting down server...")
        server.server_close()


if __name__ == "__main__":
    main()
