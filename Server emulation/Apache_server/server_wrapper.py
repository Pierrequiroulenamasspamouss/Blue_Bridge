import json
import os
import time
import threading
import logging
from http.server import HTTPServer, BaseHTTPRequestHandler
from urllib.parse import urlparse, parse_qs, unquote
from datetime import datetime

# Import the individual server modules
from new_auth_server import LoginServer
from new_well_server import WellServer
from new_nearby_server import NearbyServer
from db_init import init_db

# Setup logging
LOG_DIR = os.path.dirname(__file__)
LOG_PATH = os.path.join(LOG_DIR, 'server_logs.txt')

# Configure logging
logging.basicConfig(
    filename=LOG_PATH,
    level=logging.INFO,
    format='%(asctime)s [%(levelname)s] %(message)s',
    datefmt='%Y-%m-%d %H:%M:%S'
)

# Global flags for active services
active_services = {
    'login': True,
    'data': True,
    'nearby': True,
    'wells': True,
    'stats': True
}

# Mock database of wells
wells_db = [
    {
        "wellId": 1,
        "wellName": "North Well",
        "latitude": 37.785834,
        "longitude": -122.406417,
        "waterLevel": "75%",
        "capacity": "1000 gallons",
        "waterType": "Fresh",
        "status": "Operational"
    },
    {
        "wellId": 2,
        "wellName": "South Well",
        "latitude": 37.780834,
        "longitude": -122.408417,
        "waterLevel": "45%",
        "capacity": "750 gallons",
        "waterType": "Brackish",
        "status": "Needs Maintenance"
    },
    {
        "wellId": 3,
        "wellName": "East Well",
        "latitude": 37.787834,
        "longitude": -122.404417,
        "waterLevel": "90%",
        "capacity": "1200 gallons",
        "waterType": "Fresh",
        "status": "Operational"
    }
]

# Mock user data
users = {
    "demo": {
        "password": "password123",
        "token": "demo_token_12345"
    }
}

class ServerWrapper(BaseHTTPRequestHandler):
    """
    Server wrapper that integrates all services and logs requests/responses
    """
    
    def __init__(self, *args, **kwargs):
        # Initialize the service instances
        self.login_server = LoginServer()
        self.well_server = WellServer()
        self.nearby_server = NearbyServer()
        super().__init__(*args, **kwargs)
    
    def log_request_data(self, method, path, query, body=None):
        """Log incoming request details"""
        log_message = f"\n{'=' * 80}\n"
        log_message += f"REQUEST: {method} {path}\n"
        log_message += f"TIME: {datetime.now().isoformat()}\n"
        
        if query:
            log_message += "QUERY PARAMS:\n"
            for key, value in query.items():
                log_message += f"  {key}: {value}\n"
        
        if body:
            log_message += f"BODY: {json.dumps(body, indent=2)}\n"
        
        logging.info(log_message)
    
    def log_response_data(self, status_code, response_body):
        """Log outgoing response details"""
        log_message = f"RESPONSE: {status_code}\n"
        
        if isinstance(response_body, str):
            try:
                # Try to pretty-print if it's JSON
                pretty_body = json.dumps(json.loads(response_body), indent=2)
                log_message += f"BODY: {pretty_body}\n"
            except:
                # If not JSON, log as is (truncated if too large)
                if len(response_body) > 1000:
                    log_message += f"BODY: {response_body[:1000]}... [truncated]\n"
                else:
                    log_message += f"BODY: {response_body}\n"
        else:
            log_message += f"BODY: {response_body}\n"
        
        log_message += f"{'=' * 80}\n"
        logging.info(log_message)
    
    def send_response_with_logging(self, status_code, content_type, content):
        """Send a response and log it"""
        self.send_response(status_code)
        self.send_header('Content-Type', content_type)
        self.send_header('Access-Control-Allow-Origin', '*')
        self.end_headers()
        
        if isinstance(content, str):
            response_bytes = content.encode('utf-8')
        else:
            response_bytes = content
        
        self.wfile.write(response_bytes)
        self.log_response_data(status_code, content)
    
    def do_OPTIONS(self):
        """Handle OPTIONS requests for CORS"""
        self.send_response(200)
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, POST, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', 'Content-Type')
        self.end_headers()
    
    def do_GET(self):
        """Handle GET requests"""
        parsed = urlparse(self.path)
        path = parsed.path
        query = parse_qs(parsed.query)
        
        # Remove /api prefix if present
        if path.startswith('/api/'):
            path = path[4:]  # Strip the /api prefix
        
        # Log the request
        self.log_request_data('GET', path, query)
        
        # Check if the requested service is active
        if path.startswith('/wells') or path.startswith('/data/wells'):
            if not active_services['wells']:
                self.send_response_with_logging(503, 'application/json', 
                    json.dumps({"error": "Wells service unavailable"}))
                return
        elif path.startswith('/nearby-users'):
            if not active_services['nearby']:
                self.send_response_with_logging(503, 'application/json', 
                    json.dumps({"error": "Nearby service unavailable"}))
                return
        
        # Route to appropriate handler
        if path == '/wells' or path == '/data/wells':
            # Get all wells
            response, status_code = self.well_server.get_all_wells()
            self.send_response_with_logging(status_code, 'application/json', response)
        
        elif path == '/wells/index':
            # Get filtered wells
            response, status_code = self.well_server.get_wells_filtered(query)
            self.send_response_with_logging(status_code, 'application/json', response)
        
        elif path.startswith('/wells/') and not path.endswith('/stats'):
            # Get specific well details
            esp_id = path.split('/')[-1]
            response, status_code = self.well_server.get_well_details(esp_id)
            self.send_response_with_logging(status_code, 'application/json', response)
        
        elif path.startswith('/data/wells/'):
            # Get specific well details (alternative endpoint)
            esp_id = path.split('/')[-1]
            response, status_code = self.well_server.get_well_details(esp_id)
            self.send_response_with_logging(status_code, 'application/json', response)
        
        elif path == '/wells/stats':
            # Get well statistics
            if not active_services['stats']:
                self.send_response_with_logging(503, 'application/json', 
                    json.dumps({"error": "Stats service unavailable"}))
                return
            
            response, status_code = self.well_server.get_well_stats()
            self.send_response_with_logging(status_code, 'application/json', response)
        
        elif path.startswith('/nearby-users'):
            # Get nearby users
            response, status_code = self.nearby_server.get_nearby_users(query)
            self.send_response_with_logging(status_code, 'application/json', response)
        
        else:
            # Invalid endpoint
            self.send_response_with_logging(404, 'application/json', 
                json.dumps({"error": "Invalid GET endpoint"}))
    
    def do_POST(self):
        """Handle POST requests"""
        parsed = urlparse(self.path)
        path = parsed.path
        
        # Remove /api prefix if present
        if path.startswith('/api/'):
            path = path[4:]  # Strip the /api prefix
        
        # Read request body
        content_length = int(self.headers.get('Content-Length', 0))
        body = self.rfile.read(content_length).decode('utf-8')
        
        try:
            body_json = json.loads(body) if body else {}
        except json.JSONDecodeError:
            body_json = {}
        
        # Log the request
        self.log_request_data('POST', path, {}, body_json)
        
        # Check if the requested service is active
        if path in ['/login', '/register']:
            if not active_services['login']:
                self.send_response_with_logging(503, 'application/json', 
                    json.dumps({"error": "Auth service unavailable"}))
                return
        elif path in ['/update-location', '/update-water-needs']:
            if not active_services['nearby']:
                self.send_response_with_logging(503, 'application/json', 
                    json.dumps({"error": "Nearby service unavailable"}))
                return
        
        # Route to appropriate handler
        if path == '/login':
            # Handle login
            response, status_code = self.login_server.login(body_json)
            self.send_response_with_logging(status_code, 'application/json', response)
        
        elif path == '/register':
            # Handle registration
            response, status_code = self.login_server.register(body_json)
            self.send_response_with_logging(status_code, 'application/json', response)
        
        elif path == '/update-location':
            # Handle location update
            response, status_code = self.login_server.update_location(body_json)
            self.send_response_with_logging(status_code, 'application/json', response)
        
        elif path == '/update-water-needs':
            # Handle water needs update
            response, status_code = self.login_server.update_water_needs(body_json)
            self.send_response_with_logging(status_code, 'application/json', response)
        
        else:
            # Invalid endpoint
            self.send_response_with_logging(404, 'application/json', 
                json.dumps({"error": "Invalid POST endpoint"}))


def clear_logs_periodically():
    """Clear log file every 5 minutes"""
    while True:
        time.sleep(300)  # 300 seconds = 5 minutes
        try:
            # Clear the log file
            with open(LOG_PATH, 'w') as f:
                f.write(f"Logs cleared at {datetime.now().isoformat()}\n")
            logging.info("Log file cleared automatically")
        except Exception as e:
            logging.error(f"Error clearing log file: {str(e)}")


def interactive_console():
    """Interactive console for server management"""
    print("Interactive mode: type 'help' for available commands")
    
    while True:
        cmd = input().strip().lower().split()
        
        if not cmd:
            continue
        
        if cmd[0] == 'help':
            print("\nAvailable commands:")
            print("  status - Show status of all services")
            print("  shutdown <service> - Disable a service")
            print("  boot <service> - Enable a service")
            print("  clearlog - Clear the log file")
            print("  exit - Exit the server")
            print("\nAvailable services:", list(active_services.keys()))
        
        elif cmd[0] == 'status':
            print("\nService Status:")
            for service, status in active_services.items():
                print(f"  {service}: {'active' if status else 'inactive'}")
        
        elif cmd[0] == 'shutdown' and len(cmd) > 1:
            if cmd[1] in active_services:
                active_services[cmd[1]] = False
                print(f"Service '{cmd[1]}' is now inactive")
            else:
                print(f"Unknown service: {cmd[1]}")
        
        elif cmd[0] == 'boot' and len(cmd) > 1:
            if cmd[1] in active_services:
                active_services[cmd[1]] = True
                print(f"Service '{cmd[1]}' is now active")
            else:
                print(f"Unknown service: {cmd[1]}")
        
        elif cmd[0] == 'clearlog':
            try:
                with open(LOG_PATH, 'w') as f:
                    f.write(f"Logs cleared manually at {datetime.now().isoformat()}\n")
                print("Log file cleared")
            except Exception as e:
                print(f"Error clearing log file: {str(e)}")
        
        elif cmd[0] == 'exit':
            print("Exiting server...")
            os._exit(0)
        
        else:
            print(f"Unknown command: {' '.join(cmd)}")
            print("Type 'help' for available commands")


def run_server(host='', port=8090):
    """Run the server"""
    # Initialize the database
    init_db()
    
    # Start log cleaner thread
    log_cleaner = threading.Thread(target=clear_logs_periodically, daemon=True)
    log_cleaner.start()
    
    # Start interactive console thread
    console = threading.Thread(target=interactive_console, daemon=True)
    console.start()
    
    # Start the server
    server = HTTPServer((host, port), ServerWrapper)
    print(f"Server running on port {port}")
    logging.info(f"Server started on port {port}")
    
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("Shutting down server...")
        logging.info("Server shutting down")
        server.server_close()


if __name__ == "__main__":
    run_server() 