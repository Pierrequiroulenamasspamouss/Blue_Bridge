import json
import math
import os
from http.server import BaseHTTPRequestHandler, HTTPServer
from datetime import datetime
import traceback
from urllib.parse import parse_qs, urlparse
import logging

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s [%(levelname)s] %(message)s',
    datefmt='%Y-%m-%d %H:%M:%S'
)
logger = logging.getLogger('NearbyUsersServer')

DATABASE_FILE = 'user_database.json'
EARTH_RADIUS = 6371  # Earth's radius in kilometers

class NearbyUsersServer(BaseHTTPRequestHandler):
    protocol_version = 'HTTP/1.1'  # Use HTTP/1.1 to prevent unexpected end of stream

    def log_message(self, format, *args):
        """Override to use our custom logger"""
        logger.info(f"{self.address_string()} - {format%args}")

    def do_OPTIONS(self):
        try:
            logger.info(f"Received OPTIONS request from {self.client_address[0]}")
            self.send_response(200)
            self.send_header('Access-Control-Allow-Origin', '*')
            self.send_header('Access-Control-Allow-Methods', 'GET, POST, OPTIONS')
            self.send_header('Access-Control-Allow-Headers', 'Content-Type')
            self.end_headers()
        except Exception as e:
            logger.error(f"Error in OPTIONS: {str(e)}")
            traceback.print_exc()

    def do_GET(self):
        try:
            logger.info(f"Received GET request from {self.client_address[0]} to {self.path}")
            
            if self.path.startswith('/nearby-users'):
                parsed_url = urlparse(self.path)
                params = parse_qs(parsed_url.query)
                logger.info(f"Query parameters: {params}")
                
                try:
                    latitude = float(params.get('latitude', [0])[0])
                    longitude = float(params.get('longitude', [0])[0])
                    radius = float(params.get('radius', [50])[0])  # Default 50km radius
                    email = params.get('email', [''])[0]
                    
                    logger.info(f"Processing nearby users request for {email} at ({latitude}, {longitude}) within {radius}km")
                    self.handle_nearby_users(latitude, longitude, radius, email)
                except (ValueError, KeyError) as e:
                    logger.error(f"Invalid parameters: {str(e)}")
                    self.send_error_response(400, f"Invalid parameters: {str(e)}")
            else:
                logger.warning(f"Endpoint not found: {self.path}")
                self.send_error_response(404, "Endpoint not found")
        except Exception as e:
            logger.error(f"Error in GET: {str(e)}")
            traceback.print_exc()
            self.send_error_response(500, "Internal server error")

    def do_POST(self):
        try:
            logger.info(f"Received POST request from {self.client_address[0]} to {self.path}")
            
            content_length = int(self.headers.get('Content-Length', 0))
            body = self.rfile.read(content_length).decode('utf-8')
            request_data = json.loads(body) if body else {}
            logger.info(f"Request body: {request_data}")
            
            if self.path == '/update-location':
                self.handle_update_location(request_data)
            elif self.path == '/update-water-needs':
                self.handle_update_water_needs(request_data)
            else:
                logger.warning(f"Endpoint not found: {self.path}")
                self.send_error_response(404, "Endpoint not found")
        except Exception as e:
            logger.error(f"Error in POST: {str(e)}")
            traceback.print_exc()
            self.send_error_response(500, "Internal server error")

    def handle_nearby_users(self, latitude, longitude, radius, requester_email):
        try:
            users = self.load_database()
            nearby_users = []
            logger.info(f"Finding users near ({latitude}, {longitude}) within {radius}km for {requester_email}")
            
            for email, user_data in users.items():
                if email == requester_email:
                    continue
                
                # Simulate random location near the requested coordinates
                user_lat = latitude + (hash(email) % 100) / 1000.0
                user_lon = longitude + (hash(email[::-1]) % 100) / 1000.0
                
                distance = self.calculate_distance(latitude, longitude, user_lat, user_lon)
                
                if distance <= radius:
                    user = {
                        "firstName": user_data.get('firstName', ''),
                        "lastName": user_data.get('lastName', ''),
                        #"username": user_data.get('username', ''),
                        "distance": round(distance, 2),
                        #"location": {
                        #    "latitude": user_lat,
                        #    "longitude": user_lon
                        #},
                        "waterNeeds": user_data.get('waterNeeds', []),
                        "isOnline": self.is_user_online(user_data)
                    }
                    nearby_users.append(user)
                    logger.info(f"Found nearby user: {user['firstName'] + ' ' + user['lastName']} at {distance:.2f}km")
            
            response = {
                "status": "success",
                "users": nearby_users,
                "timestamp": datetime.now().isoformat()
            }
            logger.info(f"Found {len(nearby_users)} nearby users")
            self.send_success_response(response)
        except Exception as e:
            logger.error(f"Error in handle_nearby_users: {str(e)}")
            traceback.print_exc()
            self.send_error_response(500, "Internal server error")

    def handle_update_location(self, data):
        try:
            logger.info(f"Updating location for user: {data.get('email')}")
            if not all(key in data for key in ['email', 'latitude', 'longitude']):
                logger.warning("Missing required fields in location update")
                self.send_error_response(400, "Missing required fields")
                return
            
            users = self.load_database()
            if data['email'] not in users:
                logger.warning(f"User not found: {data.get('email')}")
                self.send_error_response(404, "User not found")
                return
            
            users[data['email']]['location'] = {
                'latitude': data['latitude'],
                'longitude': data['longitude'],
                'lastUpdated': datetime.now().isoformat()
            }
            
            self.save_database(users)
            logger.info(f"Successfully updated location for {data['email']}")
            self.send_success_response({
                "status": "success",
                "message": "Location updated successfully",
                "timestamp": datetime.now().isoformat()
            })
        except Exception as e:
            logger.error(f"Error in handle_update_location: {str(e)}")
            traceback.print_exc()
            self.send_error_response(500, "Internal server error")

    def handle_update_water_needs(self, data):
        try:
            logger.info(f"Updating water needs for user: {data.get('email')}")
            if not all(key in data for key in ['email', 'waterNeeds']):
                logger.warning("Missing required fields in water needs update")
                self.send_error_response(400, "Missing required fields")
                return
            
            users = self.load_database()
            if data['email'] not in users:
                logger.warning(f"User not found: {data.get('email')}")
                self.send_error_response(404, "User not found")
                return
            
            users[data['email']]['waterNeeds'] = data['waterNeeds']
            self.save_database(users)
            
            logger.info(f"Successfully updated water needs for {data['email']}")
            self.send_success_response({
                "status": "success",
                "message": "Water needs updated successfully",
                "timestamp": datetime.now().isoformat()
            })
        except Exception as e:
            logger.error(f"Error in handle_update_water_needs: {str(e)}")
            traceback.print_exc()
            self.send_error_response(500, "Internal server error")

    def calculate_distance(self, lat1, lon1, lat2, lon2):
        """Calculate distance between two points using the Haversine formula"""
        lat1, lon1, lat2, lon2 = map(math.radians, [lat1, lon1, lat2, lon2])
        dlat = lat2 - lat1
        dlon = lon2 - lon1
        
        a = math.sin(dlat/2)**2 + math.cos(lat1) * math.cos(lat2) * math.sin(dlon/2)**2
        c = 2 * math.asin(math.sqrt(a))
        return EARTH_RADIUS * c

    def is_user_online(self, user_data):
        """Simulate online status based on last activity"""
        last_updated = user_data.get('location', {}).get('lastUpdated')
        if not last_updated:
            return False
        
        try:
            last_updated_time = datetime.fromisoformat(last_updated)
            time_diff = (datetime.now() - last_updated_time).total_seconds()
            return time_diff < 900  # Consider online if active in the last 15 minutes
        except Exception as e:
            logger.error(f"Error checking online status: {str(e)}")
            return False

    def load_database(self):
        try:
            if os.path.exists(DATABASE_FILE):
                with open(DATABASE_FILE, 'r') as f:
                    return json.load(f)
            logger.warning(f"Database file not found: {DATABASE_FILE}")
            return {}
        except Exception as e:
            logger.error(f"Error loading database: {str(e)}")
            traceback.print_exc()
            return {}

    def save_database(self, data):
        try:
            with open(DATABASE_FILE, 'w') as f:
                json.dump(data, f, indent=4)
            logger.info("Database saved successfully")
        except Exception as e:
            logger.error(f"Error saving database: {str(e)}")
            traceback.print_exc()

    def send_success_response(self, data):
        try:
            self.send_response(200)
            response_json = json.dumps(data)
            response_bytes = response_json.encode()
            self.send_header('Content-Type', 'application/json')
            self.send_header('Content-Length', str(len(response_bytes)))
            self.send_header('Access-Control-Allow-Origin', '*')
            self.send_header('Connection', 'keep-alive')
            self.end_headers()
            self.wfile.write(response_bytes)
            logger.info(f"Sent success response: {response_json[:200]}...")  # Log first 200 chars
        except Exception as e:
            logger.error(f"Error sending success response: {str(e)}")
            traceback.print_exc()

    def send_error_response(self, status_code, message):
        try:
            self.send_response(status_code)
            error_response = {
                'status': 'error',
                'timestamp': datetime.now().isoformat(),
                'error': {
                    'code': status_code,
                    'message': message
                }
            }
            response_json = json.dumps(error_response)
            response_bytes = response_json.encode()
            self.send_header('Content-Type', 'application/json')
            self.send_header('Content-Length', str(len(response_bytes)))
            self.send_header('Access-Control-Allow-Origin', '*')
            self.send_header('Connection', 'keep-alive')
            self.end_headers()
            self.wfile.write(response_bytes)
            logger.info(f"Sent error response: {response_json}")
        except Exception as e:
            logger.error(f"Error sending error response: {str(e)}")
            traceback.print_exc()

def run_server(port=8090, host=''):
    try:
        server_address = (host, port)
        httpd = HTTPServer(server_address, NearbyUsersServer)
        logger.info(f"Starting nearby users server on {host}:{port}...")
        logger.info("Server is ready to accept connections")
        httpd.serve_forever()
    except Exception as e:
        logger.error(f"Error starting server: {str(e)}")
        traceback.print_exc()

if __name__ == '__main__':
    run_server()