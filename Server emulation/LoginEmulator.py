import json
import os
import traceback
from http.server import BaseHTTPRequestHandler, HTTPServer
from datetime import datetime

DATABASE_FILE = 'user_database.json'

class AuthServer(BaseHTTPRequestHandler):
    def do_OPTIONS(self):
        try:
            self.send_response(200)
            self.send_header('Access-Control-Allow-Origin', '*')
            self.send_header('Access-Control-Allow-Methods', 'POST, OPTIONS')
            self.send_header('Access-Control-Allow-Headers', 'Content-Type')
            self.end_headers()
        except Exception as e:
            print(f"Error in OPTIONS: {str(e)}")
            traceback.print_exc()

    def do_POST(self):
        try:
            print(f"\nReceived {self.command} request to {self.path}")
            print(f"Headers: {self.headers}")
            
            # Read request body
            content_length = int(self.headers.get('Content-Length', 0))
            body = self.rfile.read(content_length).decode('utf-8')
            print(f"Request body: {body}")
            
            try:
                request_data = json.loads(body) if body else {}
                print(f"Parsed request data: {request_data}")
            except json.JSONDecodeError as e:
                print(f"JSON decode error: {str(e)}")
                self.send_error_response(400, "Invalid JSON format")
                return

            if self.path == '/login':
                self.handle_login(request_data)
            elif self.path == '/register':
                self.handle_register(request_data)
            else:
                self.send_error_response(404, "Endpoint not found")
        except Exception as e:
            print(f"Error in POST: {str(e)}")
            traceback.print_exc()
            self.send_error_response(500, "Internal server error")

    def handle_login(self, credentials):
        try:
            if not self.validate_credentials(credentials):
                self.send_error_response(400, "Missing email or password")
                return

            # Load user database
            users = self.load_database()
            print(f"Looking for user: {credentials['email']}")
            user = users.get(credentials['email'])

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
                self.send_success_response(response)
            else:
                self.send_error_response(401, "Invalid credentials")
        except Exception as e:
            print(f"Error in handle_login: {str(e)}")
            traceback.print_exc()
            self.send_error_response(500, "Internal server error")

    def handle_register(self, user_data):
        try:
            print(f"Processing registration for: {user_data.get('email', 'unknown')}")
            required_fields = ['email', 'password', 'firstName', 'lastName', 'username']
            
            # Validate required fields
            missing_fields = [field for field in required_fields if field not in user_data]
            if missing_fields:
                print(f"Missing fields: {missing_fields}")
                self.send_error_response(400, f"Missing required fields: {', '.join(missing_fields)}")
                return

            # Validate email format
            if not '@' in user_data['email']:
                self.send_error_response(400, "Invalid email format")
                return

            users = self.load_database()
            
            # Check if user already exists
            if user_data['email'] in users:
                self.send_error_response(409, "User already exists")
                return

            # Add new user
            users[user_data['email']] = {
                'password': user_data['password'],
                'firstName': user_data['firstName'],
                'lastName': user_data['lastName'],
                'username': user_data['username'],
                'role': 'user',
                'registeredAt': datetime.now().isoformat()
            }

            # Save updated database
            self.save_database(users)
            print(f"Successfully registered user: {user_data['email']}")

            response = {
                'status': 'success',
                'timestamp': datetime.now().isoformat(),
                'message': 'Registration successful'
            }
            self.send_success_response(response)
        except Exception as e:
            print(f"Error in handle_register: {str(e)}")
            traceback.print_exc()
            self.send_error_response(500, "Internal server error")

    def validate_credentials(self, data):
        return data and 'email' in data and 'password' in data

    def load_database(self):
        try:
            if os.path.exists(DATABASE_FILE):
                with open(DATABASE_FILE, 'r') as f:
                    return json.load(f)
            return {}
        except Exception as e:
            print(f"Error loading database: {str(e)}")
            traceback.print_exc()
            return {}

    def save_database(self, data):
        try:
            with open(DATABASE_FILE, 'w') as f:
                json.dump(data, f, indent=4)
        except Exception as e:
            print(f"Error saving database: {str(e)}")
            traceback.print_exc()

    def send_success_response(self, data):
        try:
            self.send_response(200)
            self.send_common_headers()
            response_json = json.dumps(data)
            response_bytes = response_json.encode()
            self.wfile.write(response_bytes)
            print(f"Sent success response: {response_json}")
        except Exception as e:
            print(f"Error sending success response: {str(e)}")
            traceback.print_exc()

    def send_error_response(self, status_code, message):
        try:
            self.send_response(status_code)
            self.send_common_headers()
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
            self.wfile.write(response_bytes)
            print(f"Sent error response: {response_json}")
        except Exception as e:
            print(f"Error sending error response: {str(e)}")
            traceback.print_exc()

    def send_common_headers(self):
        try:
            self.send_header('Content-Type', 'application/json')
            self.send_header('Access-Control-Allow-Origin', '*')
            self.end_headers()
        except Exception as e:
            print(f"Error sending headers: {str(e)}")
            traceback.print_exc()

def run_server(port=8090, host=''):
    try:
        server_address = (host, port)
        httpd = HTTPServer(server_address, AuthServer)
        print(f'Starting auth server on {host or "localhost"}:{port}...')
        print(f'Server is running at http://{host or "localhost"}:{port}')
        httpd.serve_forever()
    except Exception as e:
        print(f"Error running server: {str(e)}")
        traceback.print_exc()

if __name__ == '__main__':
    run_server(host='0.0.0.0') 