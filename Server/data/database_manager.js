const sqlite3 = require('sqlite3').verbose();
const path = require('path');

// Configuration des chemins de base de données
const dbPaths = {
    users: path.join(__dirname, 'users.sqlite'),
    deviceTokens: path.join(__dirname, 'deviceTokens.sqlite'),
    wells: path.join(__dirname, 'wells.sqlite')
};

// Pool de connexions SQLite
let connections = {};

// Initialiser les connexions SQLite
function initializeConnections() {
    if (Object.keys(connections).length === 0) {
        connections.users = new sqlite3.Database(dbPaths.users);
        connections.deviceTokens = new sqlite3.Database(dbPaths.deviceTokens);
        connections.wells = new sqlite3.Database(dbPaths.wells);
        
        console.log('✅ SQLite connections initialized');
    }
    return connections;
}

// Récupérer un utilisateur par ID
async function getUserById(userId) {
    return new Promise((resolve, reject) => {
        try {
            const db = initializeConnections().users;
            
            const query = `
                SELECT userId, firstName, lastName, username, email, role, 
                       location, allowLocationSharing, waterNeeds, 
                       notificationPreferences, phoneNumber, themePreference,
                       lastActive, isActive, registrationDate, isWellOwner,
                       createdAt, updatedAt
                FROM users 
                WHERE userId = ?
            `;
            
            db.get(query, [userId], (err, row) => {
                if (err) {
                    console.error('❌ Database error getting user:', err);
                    reject(err);
                    return;
                }
                
                if (row) {
                    // Parser les champs JSON
                    try {
                        if (row.location) row.location = JSON.parse(row.location);
                        if (row.waterNeeds) row.waterNeeds = JSON.parse(row.waterNeeds);
                        if (row.notificationPreferences) row.notificationPreferences = JSON.parse(row.notificationPreferences);
                    } catch (e) {
                        console.warn('⚠️ Error parsing JSON fields for user:', e.message);
                    }
                    
                    console.log(`✅ User found: ${row.firstName} ${row.lastName} (${row.email})`);
                    resolve(row);
                } else {
                    console.log(`❌ User not found: ${userId}`);
                    resolve(null);
                }
            });
        } catch (error) {
            console.error('❌ Error in getUserById:', error);
            reject(error);
        }
    });
}

// Récupérer un utilisateur par email
async function getUserByEmail(email) {
    return new Promise((resolve, reject) => {
        try {
            const db = initializeConnections().users;
            
            const query = `
                SELECT userId, firstName, lastName, username, email, role, 
                       location, allowLocationSharing, waterNeeds, 
                       notificationPreferences, phoneNumber, themePreference,
                       lastActive, isActive, registrationDate, isWellOwner,
                       createdAt, updatedAt
                FROM users 
                WHERE email = ?
            `;
            
            db.get(query, [email.toLowerCase().trim()], (err, row) => {
                if (err) {
                    console.error('❌ Database error getting user by email:', err);
                    reject(err);
                    return;
                }
                
                if (row) {
                    // Parser les champs JSON
                    try {
                        if (row.location) row.location = JSON.parse(row.location);
                        if (row.waterNeeds) row.waterNeeds = JSON.parse(row.waterNeeds);
                        if (row.notificationPreferences) row.notificationPreferences = JSON.parse(row.notificationPreferences);
                    } catch (e) {
                        console.warn('⚠️ Error parsing JSON fields for user:', e.message);
                    }
                    
                    console.log(`✅ User found by email: ${row.firstName} ${row.lastName}`);
                    resolve(row);
                } else {
                    console.log(`❌ User not found by email: ${email}`);
                    resolve(null);
                }
            });
        } catch (error) {
            console.error('❌ Error in getUserByEmail:', error);
            reject(error);
        }
    });
}

// Récupérer le token FCM d'un utilisateur
async function getUserFCMToken(userId) {
    return new Promise((resolve, reject) => {
        try {
            const db = initializeConnections().deviceTokens;
            
            const query = `
                SELECT token 
                FROM device_tokens 
                WHERE userId = ? AND isActive = 1 
                ORDER BY lastUsed DESC 
                LIMIT 1
            `;
            
            db.get(query, [userId], (err, row) => {
                if (err) {
                    console.error('❌ Database error getting FCM token:', err);
                    reject(err);
                    return;
                }
                
                if (row && row.token) {
                    console.log(`✅ FCM token found for ${userId}: ${row.token.substring(0, 20)}...`);
                    resolve(row.token);
                } else {
                    console.log(`❌ No FCM token found for user: ${userId}`);
                    resolve(null);
                }
            });
        } catch (error) {
            console.error('❌ Error in getUserFCMToken:', error);
            reject(error);
        }
    });
}

// Mettre à jour le token FCM d'un utilisateur
async function updateUserFCMToken(userId, fcmToken) {
    return new Promise((resolve, reject) => {
        try {
            const db = initializeConnections().deviceTokens;
            
            // Vérifier si un token existe déjà pour cet utilisateur
            const checkQuery = 'SELECT tokenId FROM device_tokens WHERE userId = ? AND isActive = 1';
            
            db.get(checkQuery, [userId], (err, row) => {
                if (err) {
                    console.error('❌ Database error checking existing token:', err);
                    reject(err);
                    return;
                }
                
                if (row) {
                    // Mettre à jour le token existant
                    const updateQuery = `
                        UPDATE device_tokens 
                        SET token = ?, lastUsed = CURRENT_TIMESTAMP, updatedAt = CURRENT_TIMESTAMP 
                        WHERE userId = ? AND isActive = 1
                    `;
                    
                    db.run(updateQuery, [fcmToken, userId], function(err) {
                        if (err) {
                            console.error('❌ Database error updating FCM token:', err);
                            reject(err);
                            return;
                        }
                        
                        if (this.changes > 0) {
                            console.log(`✅ FCM token updated for user: ${userId}`);
                            resolve(true);
                        } else {
                            console.log(`❌ Failed to update FCM token for user: ${userId}`);
                            resolve(false);
                        }
                    });
                } else {
                    // Créer un nouveau token
                    const insertQuery = `
                        INSERT INTO device_tokens (tokenId, userId, token, deviceType, isActive, createdAt, updatedAt) 
                        VALUES (?, ?, ?, 'android', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    `;
                    
                    const tokenId = require('crypto').randomUUID();
                    
                    db.run(insertQuery, [tokenId, userId, fcmToken], function(err) {
                        if (err) {
                            console.error('❌ Database error creating FCM token:', err);
                            reject(err);
                            return;
                        }
                        
                        if (this.lastID) {
                            console.log(`✅ FCM token created for user: ${userId}`);
                            resolve(true);
                        } else {
                            console.log(`❌ Failed to create FCM token for user: ${userId}`);
                            resolve(false);
                        }
                    });
                }
            });
        } catch (error) {
            console.error('❌ Error in updateUserFCMToken:', error);
            reject(error);
        }
    });
}

// Créer un nouvel utilisateur
async function createUser(userData) {
    return new Promise((resolve, reject) => {
        try {
            const db = initializeConnections().users;
            
            const {
                userId, firstName, lastName, email, username, password,
                role = 'user', location = null, allowLocationSharing = false,
                waterNeeds = '[]', notificationPreferences = '{}',
                phoneNumber = null, themePreference = 0, isWellOwner = false
            } = userData;
            
            const query = `
                INSERT INTO users (
                    userId, firstName, lastName, email, username, password, role,
                    location, allowLocationSharing, waterNeeds, notificationPreferences,
                    phoneNumber, themePreference, isWellOwner, createdAt, updatedAt
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            `;
            
            const params = [
                userId, firstName, lastName, email.toLowerCase().trim(), username,
                password, role, location ? JSON.stringify(location) : null,
                allowLocationSharing ? 1 : 0, waterNeeds, notificationPreferences,
                phoneNumber, themePreference, isWellOwner ? 1 : 0
            ];
            
            db.run(query, params, function(err) {
                if (err) {
                    console.error('❌ Database error creating user:', err);
                    reject(err);
                    return;
                }
                
                if (this.lastID) {
                    console.log(`✅ User created with ID: ${userId}`);
                    resolve(userId);
                } else {
                    console.log(`❌ Failed to create user`);
                    resolve(null);
                }
            });
        } catch (error) {
            console.error('❌ Error in createUser:', error);
            reject(error);
        }
    });
}

// Fermer toutes les connexions
async function closeConnections() {
    return new Promise((resolve) => {
        const dbNames = Object.keys(connections);
        let closedCount = 0;
        
        if (dbNames.length === 0) {
            resolve();
            return;
        }
        
        dbNames.forEach(dbName => {
            connections[dbName].close((err) => {
                if (err) {
                    console.error(`❌ Error closing ${dbName} connection:`, err);
                } else {
                    console.log(`✅ ${dbName} connection closed`);
                }
                
                closedCount++;
                if (closedCount === dbNames.length) {
                    connections = {};
                    resolve();
                }
            });
        });
    });
}

module.exports = {
    initializeConnections,
    getUserById,
    getUserByEmail,
    getUserFCMToken,
    updateUserFCMToken,
    createUser,
    closeConnections
}; 