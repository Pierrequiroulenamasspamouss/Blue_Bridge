import json
import os
import sqlite3
import hashlib
import base64
import re
from datetime import datetime

# Database setup
DB_DIR = os.path.dirname(__file__)
DB_PATH = os.path.join(DB_DIR, 'wellconnect.db')

class LoginServer:
    """Authentication server for handling login and registration requests"""
    
    def __init__(self, db_path=DB_PATH):
        self.db_path = db_path
        
    def get_db_connection(self):
        conn = sqlite3.connect(self.db_path)
        conn.row_factory = sqlite3.Row
        return conn
    
    def sanitize_input(self, input_str):
        """Sanitize input to prevent injection attacks"""
        if not input_str:
            return ""
        # Remove potential SQL injection characters
        return re.sub(r'[\'";\\]', '', input_str)
    
    def normalize_email(self, email):
        """Normalize email to lowercase for consistent comparison"""
        if not email:
            return ""
        return email.lower().strip()
    
    def login(self, data):
        """Handle user login requests"""
        if not data or 'email' not in data or 'password' not in data:
            return json.dumps({
                'status': 'error',
                'message': 'Missing credentials'
            }), 400
            
        # Normalize email to lowercase and sanitize inputs
        email = self.normalize_email(data.get('email'))
        password = self.sanitize_input(data.get('password'))
        
        conn = self.get_db_connection()
        cursor = conn.cursor()
        
        # Find user with matching email
        cursor.execute(
            "SELECT * FROM users WHERE LOWER(email) = ?", 
            (email,)
        )
        user = cursor.fetchone()
        
        if not user:
            conn.close()
            return json.dumps({
                'status': 'error',
                'message': 'Invalid credentials'
            }), 401
            
        # Verify password
        if user['password'] != password:
            conn.close()
            return json.dumps({
                'status': 'error',
                'message': 'Invalid credentials'
            }), 401
            
        # Update last login timestamp
        current_time = datetime.now().isoformat()
        cursor.execute(
            "UPDATE users SET last_updated = ?, is_online = 1 WHERE id = ?",
            (current_time, user['id'])
        )
        conn.commit()
        
        # Get water needs
        cursor.execute(
            "SELECT amount, usage_type, description, priority FROM water_needs WHERE user_id = ?",
            (user['id'],)
        )
        water_needs = cursor.fetchall()
        
        # Build response
        response = {
            'status': 'success',
            'timestamp': current_time,
            'message': 'Login successful',
            'data': {
                'user': {
                    'email': user['email'],
                    'firstName': user['first_name'],
                    'lastName': user['last_name'],
                    'username': user['username'],
                    'role': user['role']
                },
                'location': {
                    'latitude': user['latitude'],
                    'longitude': user['longitude']
                },
                'waterNeeds': [
                    {
                        'amount': need['amount'],
                        'usageType': need['usage_type'],
                        'description': need['description'],
                        'priority': need['priority']
                    } for need in water_needs
                ]
            }
        }
        
        conn.close()
        return json.dumps(response), 200
        
    def register(self, data):
        """Handle user registration requests"""
        required_fields = ['email', 'password', 'firstName', 'lastName', 'username', 'location', 'waterNeeds']
        
        # Check for missing fields
        for field in required_fields:
            if field not in data:
                return json.dumps({
                    'status': 'error',
                    'message': f'Missing field: {field}'
                }), 400
        
        # Normalize email to lowercase and sanitize inputs
        email = self.normalize_email(data.get('email'))
        password = self.sanitize_input(data.get('password'))
        firstName = self.sanitize_input(data.get('firstName'))
        lastName = self.sanitize_input(data.get('lastName'))
        username = self.sanitize_input(data.get('username'))
        location = data.get('location', {})
        waterNeeds = data.get('waterNeeds', [])
        isWellOwner = data.get('isWellOwner', False)
        role = self.sanitize_input(data.get('role', 'user'))
        
        # Validate email format
        if not re.match(r'^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$', email):
            return json.dumps({
                'status': 'error',
                'message': 'Invalid email format'
            }), 400
        
        conn = self.get_db_connection()
        cursor = conn.cursor()
        
        # Check if email exists (case-insensitive)
        cursor.execute("SELECT id FROM users WHERE LOWER(email) = ?", (email,))
        if cursor.fetchone():
            conn.close()
            return json.dumps({
                'status': 'error',
                'message': 'User exists'
            }), 409
            
        try:
            # Insert user
            cursor.execute('''
            INSERT INTO users (
                email, password, first_name, last_name, username, role, 
                latitude, longitude, is_well_owner, last_updated, is_online
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ''', (
                email, password, firstName, lastName, username, role,
                location.get('latitude', 0.0), location.get('longitude', 0.0),
                1 if isWellOwner else 0, datetime.now().isoformat(), 1
            ))
            
            user_id = cursor.lastrowid
            
            # Insert water needs with sanitized inputs
            for need in waterNeeds:
                cursor.execute('''
                INSERT INTO water_needs (
                    user_id, amount, usage_type, description, priority
                ) VALUES (?, ?, ?, ?, ?)
                ''', (
                    user_id, 
                    need.get('amount', 0), 
                    self.sanitize_input(need.get('usageType', '')), 
                    self.sanitize_input(need.get('description', '')),
                    need.get('priority', 1)
                ))
            
            conn.commit()
            conn.close()
            
            return json.dumps({
                'status': 'success',
                'timestamp': datetime.now().isoformat(),
                'message': 'Registration successful'
            }), 200
        except Exception as e:
            conn.rollback()
            conn.close()
            return json.dumps({
                'status': 'error',
                'message': f'Registration failed: {str(e)}'
            }), 500
            
    def update_location(self, data):
        """Update user location"""
        if not data or 'email' not in data:
            return json.dumps({
                'status': 'error',
                'message': 'Missing email'
            }), 400
            
        email = data.get('email')
        latitude = data.get('latitude', 0.0)
        longitude = data.get('longitude', 0.0)
        
        conn = self.get_db_connection()
        cursor = conn.cursor()
        
        # Find user
        cursor.execute("SELECT id FROM users WHERE email = ?", (email,))
        user = cursor.fetchone()
        
        if not user:
            conn.close()
            return json.dumps({
                'status': 'error',
                'message': 'User not found'
            }), 404
            
        # Update location
        cursor.execute(
            "UPDATE users SET latitude = ?, longitude = ?, last_updated = ? WHERE id = ?",
            (latitude, longitude, datetime.now().isoformat(), user['id'])
        )
        conn.commit()
        conn.close()
        
        return json.dumps({
            'status': 'success',
            'message': 'Location updated',
            'timestamp': datetime.now().isoformat()
        }), 200
        
    def update_water_needs(self, data):
        """Update user water needs"""
        if not data or 'email' not in data or 'waterNeeds' not in data:
            return json.dumps({
                'status': 'error',
                'message': 'Missing data'
            }), 400
            
        email = data.get('email')
        waterNeeds = data.get('waterNeeds', [])
        
        conn = self.get_db_connection()
        cursor = conn.cursor()
        
        # Find user
        cursor.execute("SELECT id FROM users WHERE email = ?", (email,))
        user = cursor.fetchone()
        
        if not user:
            conn.close()
            return json.dumps({
                'status': 'error',
                'message': 'User not found'
            }), 404
            
        try:
            # Delete existing water needs
            cursor.execute("DELETE FROM water_needs WHERE user_id = ?", (user['id'],))
            
            # Insert new water needs
            for need in waterNeeds:
                cursor.execute('''
                INSERT INTO water_needs (
                    user_id, amount, usage_type, description, priority
                ) VALUES (?, ?, ?, ?, ?)
                ''', (
                    user['id'], 
                    need.get('amount', 0), 
                    need.get('usageType', ''), 
                    need.get('description', ''),
                    need.get('priority', 1)
                ))
            
            conn.commit()
            conn.close()
            
            return json.dumps({
                'status': 'success',
                'message': 'Water needs updated',
                'timestamp': datetime.now().isoformat()
            }), 200
        except Exception as e:
            conn.rollback()
            conn.close()
            return json.dumps({
                'status': 'error',
                'message': f'Update failed: {str(e)}'
            }), 500 