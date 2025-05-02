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
        if self.path == '/data':
            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.end_headers()
            # Read latest data
            with self.shared_data['lock']:
                current_data = json.dumps(self.shared_data['data'])
            self.wfile.write(current_data.encode('utf-8'))
        else:
            self.send_response(404)
            self.end_headers()


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
    return {
        "wellName": f"Simulated Well {i + 1}",
        "wellOwner": f"Owner {i + 1}",
        "wellLocation": f"Location {i + 1}",
        "wellWaterType": "Clean" if i % 2 == 0 else "Grey",
        "wellCapacity": random.randint(3000, 10000),
        "wellWaterLevel": random.randint(1000, 5000),
        "wellWaterConsumption": random.randint(100, 500),
        "espId": f"esp32-{i + 1:02d}",
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


def update_data_loop(shared_data, base_data, delay=1):
    while True:
        updated_data = base_data.copy()

        # Change dynamic values
        updated_data["wellWaterLevel"] = random.randint(1000, 5000)
        updated_data["wellWaterConsumption"] = random.randint(100, 500)

        # Add extra fields
        updated_data = add_random_extra_fields(updated_data)

        with shared_data['lock']:
            shared_data['data'] = updated_data

        time.sleep(delay)


def run_server(port, shared_data):
    server = HTTPServer(("0.0.0.0", port), create_handler(shared_data))
    print(f"Simulated ESP32 running at http://{find_local_ip()}:{port}/data")
    server.serve_forever()


def main():
    try:
        num_servers = int(input("How many ESP32 servers would you like to simulate? "))
    except ValueError:
        print("Invalid input. Please enter a number.")
        return

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

        # Start HTTP server_crt thread
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


if __name__ == "__main__":
    main()
