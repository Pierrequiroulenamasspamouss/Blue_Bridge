import json
from http.server import BaseHTTPRequestHandler, HTTPServer
import threading
import socket
import random


class ESP32SimulatedHandler(BaseHTTPRequestHandler):
    def __init__(self, *args, sample_data=None, **kwargs):
        self.sample_data = sample_data
        super().__init__(*args, **kwargs)

    def do_GET(self):
        if self.path == '/data':
            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.end_headers()
            self.wfile.write(json.dumps(self.sample_data).encode('utf-8'))
        else:
            self.send_response(404)
            self.end_headers()


def create_handler(data):
    def handler(*args, **kwargs):
        ESP32SimulatedHandler(*args, sample_data=data, **kwargs)
    return handler


def find_local_ip():
    # Get the local IP address (excluding localhost)
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
        s.close()
        return ip
    except Exception:
        return "127.0.0.1"


def run_server(port, data):
    server = HTTPServer(("0.0.0.0", port), create_handler(data))
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
        data = {
            "wellName": f"Simulated Well {i + 1}",
            "wellOwner": f"Owner {i + 1}",
            "wellLocation": f"Location {i + 1}",
            "wellWaterType": "Clean" if i % 2 == 0 else "Grey",
            "wellCapacity": random.randint(3000, 10000),
            "wellWaterLevel": random.randint(1000, 5000),
            "wellWaterConsumption": random.randint(100, 500),
            "espId": f"esp32-{i + 1:02d}",
        }

        thread = threading.Thread(target=run_server, args=(port, data), daemon=True)
        thread.start()

    print(f"\nSimulating {num_servers} ESP32 servers. Press Ctrl+C to stop.")
    try:
        while True:
            pass
    except KeyboardInterrupt:
        print("\nShutting down.")


if __name__ == "__main__":
    main()
