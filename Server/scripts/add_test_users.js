const sqlite3 = require('sqlite3').verbose();
const path = require('path');
const crypto = require('crypto');

// Chemins des bases de données
const dbPaths = {
    users: path.join(__dirname, '../data/users.sqlite'),
    deviceTokens: path.join(__dirname, '../data/deviceTokens.sqlite')
};

// Connexions aux bases de données
const usersDb = new sqlite3.Database(dbPaths.users);
const tokensDb = new sqlite3.Database(dbPaths.deviceTokens);

// Utilisateurs de test
const testUsers = [
    {
        userId: 'bba26029-1c2f-4bae-9359-4e4e3d327fee',
        firstName: 'Test',
        lastName: 'User',
        username: 'testuser',
        email: 'test@example.com',
        password: 'hashed_password_123',
        role: 'user',
        allowLocationSharing: false,
        waterNeeds: '[]',
        notificationPreferences: '{}',
        isWellOwner: false
    },
    {
        userId: 'test_receiver',
        firstName: 'Receiver',
        lastName: 'User',
        username: 'receiver',
        email: 'receiver@example.com',
        password: 'hashed_password_456',
        role: 'user',
        allowLocationSharing: false,
        waterNeeds: '[]',
        notificationPreferences: '{}',
        isWellOwner: false
    }
];

// Token FCM de test
const testFCMToken = 'd9MrrVpSRL6ou_-Oq6FmW_:APA91bFnnfuJ25QyYuhDXYFsoI9iv-6nI3x7pXdqI0rrvdRKA74vtsUw5gyVy85uvOg2qggEq770K-F_WhqMEy_fd_HevEdLzTjGaYOZRRfj3Q75gyoOzqg';

// Fonction pour ajouter un utilisateur
function addUser(user) {
    return new Promise((resolve, reject) => {
        const query = `
            INSERT OR REPLACE INTO users (
                userId, firstName, lastName, username, email, password, role,
                allowLocationSharing, waterNeeds, notificationPreferences,
                isWellOwner, createdAt, updatedAt
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        `;
        
        const params = [
            user.userId, user.firstName, user.lastName, user.username,
            user.email, user.password, user.role, user.allowLocationSharing ? 1 : 0,
            user.waterNeeds, user.notificationPreferences, user.isWellOwner ? 1 : 0
        ];
        
        usersDb.run(query, params, function(err) {
            if (err) {
                console.error(`❌ Error adding user ${user.username}:`, err);
                reject(err);
            } else {
                console.log(`✅ User ${user.username} added/updated successfully`);
                resolve();
            }
        });
    });
}

// Fonction pour ajouter un token FCM
function addFCMToken(userId, token) {
    return new Promise((resolve, reject) => {
        const tokenId = crypto.randomUUID();
        
        const query = `
            INSERT OR REPLACE INTO device_tokens (
                tokenId, userId, token, deviceType, isActive, createdAt, updatedAt
            ) VALUES (?, ?, ?, 'android', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        `;
        
        const params = [tokenId, userId, token];
        
        tokensDb.run(query, params, function(err) {
            if (err) {
                console.error(`❌ Error adding FCM token for ${userId}:`, err);
                reject(err);
            } else {
                console.log(`✅ FCM token added/updated for user ${userId}`);
                resolve();
            }
        });
    });
}

// Fonction principale
async function main() {
    console.log('🚀 Starting to add test users...');
    
    try {
        // Ajouter les utilisateurs
        for (const user of testUsers) {
            await addUser(user);
        }
        
        // Ajouter les tokens FCM
        for (const user of testUsers) {
            await addFCMToken(user.userId, testFCMToken);
        }
        
        console.log('✅ All test users and FCM tokens added successfully!');
        
        // Afficher les utilisateurs ajoutés
        console.log('\n📋 Test users added:');
        testUsers.forEach(user => {
            console.log(`  - ${user.firstName} ${user.lastName} (${user.email}) - ID: ${user.userId}`);
        });
        
        console.log(`\n🔑 FCM Token: ${testFCMToken.substring(0, 50)}...`);
        
    } catch (error) {
        console.error('❌ Error in main:', error);
    } finally {
        // Fermer les connexions
        usersDb.close();
        tokensDb.close();
        console.log('🔒 Database connections closed');
    }
}

// Exécuter le script
if (require.main === module) {
    main();
}

module.exports = { addUser, addFCMToken }; 