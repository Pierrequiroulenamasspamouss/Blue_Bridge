import sqlite3
import json
from datetime import datetime
from dataclasses import dataclass
import uuid
from typing import Dict, List, Optional, Any

@dataclass
class DBConfig:
    path: str
    schema: str

class DatabaseManager:
    def __init__(self):
        self.dbs = {
            'users': DBConfig('users.sqlite', '''
                CREATE TABLE IF NOT EXISTS users (
                    userId TEXT PRIMARY KEY, email TEXT UNIQUE NOT NULL, password TEXT NOT NULL,
                    firstName TEXT NOT NULL, lastName TEXT NOT NULL, username TEXT UNIQUE,
                    role TEXT DEFAULT 'user', location TEXT, allowLocationSharing BOOLEAN DEFAULT 0,
                    waterNeeds TEXT DEFAULT '[]', notificationPreferences TEXT DEFAULT '{}',
                    loginToken TEXT UNIQUE, phoneNumber TEXT, themePreference INTEGER DEFAULT 0,
                    lastActive TIMESTAMP, isActive BOOLEAN DEFAULT 1, registrationDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    isWellOwner BOOLEAN DEFAULT 0, createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )'''),
            'wells': DBConfig('wells.sqlite', '''
                CREATE TABLE IF NOT EXISTS Wells (
                    wellId TEXT PRIMARY KEY, wellName TEXT NOT NULL,
                    wellOwner TEXT, wellLocation TEXT, wellWaterType TEXT DEFAULT 'Clean', 
                    wellCapacity REAL DEFAULT 0.0, wellWaterLevel REAL DEFAULT 0.0, 
                    wellWaterConsumption REAL DEFAULT 0.0, wellStatus TEXT DEFAULT 'Unknown', 
                    waterQuality TEXT, wellImages TEXT DEFAULT '[]',
                    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )'''),
            'deviceTokens': DBConfig('deviceTokens.sqlite', '''
                CREATE TABLE IF NOT EXISTS device_tokens (
                    tokenId TEXT PRIMARY KEY, userId TEXT NOT NULL, token TEXT UNIQUE NOT NULL,
                    deviceType TEXT DEFAULT 'android', lastUsed TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    isActive BOOLEAN DEFAULT 1, createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )''')
        }
        self._init_dbs()

    def _init_dbs(self):
        for name, config in self.dbs.items():
            with self._conn(name) as conn:
                conn.execute(config.schema)

    def _conn(self, db_name):
        conn = sqlite3.connect(self.dbs[db_name].path)
        conn.row_factory = sqlite3.Row
        return conn

    def users(self):
        return UserDB(self._conn('users'))

    def wells(self):
        return WellDB(self._conn('wells'))

    def deviceTokens(self):
        return TokenDB(self._conn('deviceTokens'))

class BaseDB:
    def __init__(self, conn):
        self.conn = conn

    def _execute(self, query, params=()):
        cursor = self.conn.cursor()
        cursor.execute(query, params)
        return cursor

    def _parse_json(self, row, fields):
        if not row: return None
        data = dict(row)
        for field in fields:
            if field in data and data[field]:
                try: data[field] = json.loads(data[field])
                except: data[field] = None
        return data

class UserDB(BaseDB):
    json_fields = ['location', 'waterNeeds', 'notificationPreferences']

    def get_user(self, user_id):
        row = self._execute('SELECT * FROM users WHERE userId = ?', (user_id,)).fetchone()
        return self._parse_json(row, self.json_fields)

    def get_user_by_email(self, email):
        row = self._execute('SELECT * FROM users WHERE email = ?', (email.lower().strip(),)).fetchone()
        return self._parse_json(row, self.json_fields)

    def get_all_users(self):
        return [self._parse_json(row, self.json_fields)
                for row in self._execute('SELECT * FROM users').fetchall()]

    def create_user(self, data):
        data = data.copy()
        for field in self.json_fields:
            if field in data and not isinstance(data[field], str):
                data[field] = json.dumps(data[field])

        if 'createdAt' not in data: data['createdAt'] = datetime.now().isoformat()
        if 'updatedAt' not in data: data['updatedAt'] = datetime.now().isoformat()

        cols, vals = zip(*data.items())
        try:
            self._execute(f'INSERT INTO users ({",".join(cols)}) VALUES ({",".join("?"*len(vals))})', vals)
            self.conn.commit()
            return True
        except sqlite3.IntegrityError:
            return False

    def update_user(self, user_id, updates):
        if not updates: return False
        updates['updatedAt'] = datetime.now().isoformat()
        for field in self.json_fields:
            if field in updates and not isinstance(updates[field], str):
                updates[field] = json.dumps(updates[field])

        set_clause = ','.join(f'{k}=?' for k in updates)
        params = list(updates.values()) + [user_id]
        cursor = self._execute(f'UPDATE users SET {set_clause} WHERE userId=?', params)
        self.conn.commit()
        return cursor.rowcount > 0

    def delete_user(self, user_id):
        cursor = self._execute('DELETE FROM users WHERE userId=?', (user_id,))
        self.conn.commit()
        return cursor.rowcount > 0

class WellDB(BaseDB):
    def get_well(self, well_id):
        row = self._execute('SELECT * FROM Wells WHERE wellId=?', (well_id,)).fetchone()
        return dict(row) if row else None

    def get_all_wells(self):
        return [dict(row) for row in self._execute('SELECT * FROM wells').fetchall()]

    def create_well(self, data):
        defaults = {
            'wellWaterType': 'Clean', 'wellCapacity': 0.0, 'wellWaterLevel': 0.0,
            'wellWaterConsumption': 0.0, 'wellStatus': 'Unknown',
            'wellImages': [], 'createdAt': datetime.now().isoformat(),
            'updatedAt': datetime.now().isoformat()
        }
        data = {**defaults, **data}

        if 'wellImages' in data and not isinstance(data['wellImages'], str):
            data['wellImages'] = json.dumps(data['wellImages'])

        cols, vals = zip(*data.items())
        cursor = self._execute(f'INSERT INTO Wells ({",".join(cols)}) VALUES ({",".join("?"*len(vals))})', vals)
        self.conn.commit()
        return cursor.lastrowid

    def update_well(self, well_id, updates):
        if not updates: return False
        set_clause = ','.join(f'{k}=?' for k in updates)
        params = list(updates.values()) + [well_id]
        cursor = self._execute(f'UPDATE Wells SET {set_clause} WHERE welId=?', params)
        self.conn.commit()
        return cursor.rowcount > 0

    def delete_well(self, well_id):
        cursor = self._execute('DELETE FROM wells WHERE wellId=?', (well_id,))
        self.conn.commit()
        return cursor.rowcount > 0

    def get_well_images(self, well_id):
        well = self.get_well(well_id)
        if not well or not well.get('wellImages'): return []
        try: return json.loads(well['wellImages'])
        except: return []

    def add_well_image(self, well_id, image_data):
        images = self.get_well_images(well_id)
        if len(images) >= 10: return False
        image_data['imageNumber'] = len(images)
        images.append(image_data)
        return self.update_well(well_id, {'wellImages': json.dumps(images)})

    def delete_well_image(self, well_id, image_number):
        images = self.get_well_images(well_id)
        if image_number >= len(images): return False
        images.pop(image_number)
        for i, img in enumerate(images): img['imageNumber'] = i
        return self.update_well(well_id, {'wellImages': json.dumps(images)})

class TokenDB(BaseDB):
    def get_tokens_by_user(self, user_id):
        return [dict(row) for row in
                self._execute('SELECT * FROM device_tokens WHERE userId=?', (user_id,)).fetchall()]

    def add_token(self, user_id, token, device_type='android'):
        data = {
            'tokenId': str(uuid.uuid4()), 'userId': user_id, 'token': token,
            'deviceType': device_type, 'lastUsed': datetime.now().isoformat(),
            'isActive': True
        }
        cols, vals = zip(*data.items())
        try:
            self._execute(f'INSERT INTO device_tokens ({",".join(cols)}) VALUES ({",".join("?"*len(vals))})', vals)
            self.conn.commit()
            return True
        except sqlite3.Error:
            return False

    def update_token(self, token_id, updates):
        if not updates: return False
        set_clause = ','.join(f'{k}=?' for k in updates)
        params = list(updates.values()) + [token_id]
        cursor = self._execute(f'UPDATE device_tokens SET {set_clause} WHERE tokenId=?', params)
        self.conn.commit()
        return cursor.rowcount > 0

    def delete_token(self, token_id):
        cursor = self._execute('DELETE FROM device_tokens WHERE tokenId=?', (token_id,))
        self.conn.commit()
        return cursor.rowcount > 0

    def verify_token(self, user_id, token):
        row = self._execute('''
            SELECT tokenId FROM device_tokens 
            WHERE userId=? AND token=? AND isActive=1
        ''', (user_id, token)).fetchone()
        if row:
            self._execute('UPDATE device_tokens SET lastUsed=? WHERE userId=? AND token=?',
                          (datetime.now().isoformat(), user_id, token))
            self.conn.commit()
            return True
        return False