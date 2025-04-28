#!/usr/bin/env python

import json
import os
import time
import re
import threading
import logging
from datetime import datetime
from flask import Flask, request, jsonify, make_response

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

# Initialize service instances
login_server = LoginServer()
well_server = WellServer()
nearby_server = NearbyServer()

# Create Flask application
app = Flask(__name__)

def log_request_data(method, path, query=None, body=None):
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

def log_response_data(status_code, response_body):
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

@app.before_request
def before_request():
    """Log all requests"""
    query_params = request.args.to_dict(flat=False)
    body = request.get_json(silent=True)
    log_request_data(request.method, request.path, query_params, body)

@app.after_request
def after_request(response):
    """Log all responses"""
    log_response_data(response.status_code, response.get_data(as_text=True))
    return response

@app.route('/api/<path:subpath>', methods=['GET', 'POST', 'OPTIONS'])
def api_route(subpath):
    """Main API route handler"""
    path = f"/{subpath}"
    
    # Handle OPTIONS request (CORS)
    if request.method == 'OPTIONS':
        response = make_response()
        response.headers.add('Access-Control-Allow-Origin', '*')
        response.headers.add('Access-Control-Allow-Methods', 'GET, POST, OPTIONS')
        response.headers.add('Access-Control-Allow-Headers', 'Content-Type')
        return response
    
    # Check if the requested service is active
    if path.startswith('/wells') or path.startswith('/data/wells'):
        if not active_services['wells']:
            return jsonify({"error": "Wells service unavailable"}), 503
    elif path.startswith('/nearby-users'):
        if not active_services['nearby']:
            return jsonify({"error": "Nearby service unavailable"}), 503
    elif path in ['/login', '/register']:
        if not active_services['login']:
            return jsonify({"error": "Auth service unavailable"}), 503
    
    # GET request handlers
    if request.method == 'GET':
        if path == '/wells' or path == '/data/wells':
            # Get all wells
            response, status_code = well_server.get_all_wells()
            return app.response_class(response=response, status=status_code, mimetype='application/json')
        
        elif path == '/wells/index':
            # Get filtered wells
            response, status_code = well_server.get_wells_filtered(request.args.to_dict(flat=False))
            return app.response_class(response=response, status=status_code, mimetype='application/json')
        
        elif path.startswith('/wells/') and not path.endswith('/stats'):
            # Get specific well details
            esp_id = path.split('/')[-1]
            response, status_code = well_server.get_well_details(esp_id)
            return app.response_class(response=response, status=status_code, mimetype='application/json')
        
        elif path.startswith('/data/wells/'):
            # Get specific well details (alternative endpoint)
            esp_id = path.split('/')[-1]
            response, status_code = well_server.get_well_details(esp_id)
            return app.response_class(response=response, status=status_code, mimetype='application/json')
        
        elif path == '/wells/stats':
            # Get well statistics
            if not active_services['stats']:
                return jsonify({"error": "Stats service unavailable"}), 503
            
            response, status_code = well_server.get_well_stats()
            return app.response_class(response=response, status=status_code, mimetype='application/json')
        
        elif path.startswith('/nearby-users'):
            # Get nearby users
            response, status_code = nearby_server.get_nearby_users(request.args.to_dict(flat=False))
            return app.response_class(response=response, status=status_code, mimetype='application/json')
        
        else:
            # Invalid endpoint
            return jsonify({"error": "Invalid GET endpoint"}), 404
    
    # POST request handlers
    elif request.method == 'POST':
        body = request.get_json()
        
        if path == '/login':
            # Handle login
            response, status_code = login_server.login(body)
            return app.response_class(response=response, status=status_code, mimetype='application/json')
        
        elif path == '/register':
            # Handle registration
            response, status_code = login_server.register(body)
            return app.response_class(response=response, status=status_code, mimetype='application/json')
        
        elif path == '/update-location':
            # Handle location update
            response, status_code = login_server.update_location(body)
            return app.response_class(response=response, status=status_code, mimetype='application/json')
        
        elif path == '/update-water-needs':
            # Handle water needs update
            response, status_code = login_server.update_water_needs(body)
            return app.response_class(response=response, status=status_code, mimetype='application/json')
        
        else:
            # Invalid endpoint
            return jsonify({"error": "Invalid POST endpoint"}), 404

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

# Start the log cleaner in a separate thread when running directly
if __name__ == "__main__":
    # Initialize the database
    init_db()
    
    # Start log cleaner thread
    log_cleaner = threading.Thread(target=clear_logs_periodically, daemon=True)
    log_cleaner.start()
    
    # Run Flask development server
    app.run(host='0.0.0.0', port=8090, debug=True)
else:
    # When running under Apache, just initialize the database
    init_db()

# This is the WSGI application object that Apache mod_wsgi will use
application = app 