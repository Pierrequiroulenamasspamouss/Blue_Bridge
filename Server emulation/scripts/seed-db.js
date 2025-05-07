const { sequelize, User, Well } = require('../models');
const { v4: uuidv4 } = require('uuid');

async function seed() {
    console.log('Starting database seeding...');
    
    try {
        // Sync database
        await sequelize.sync({ force: true });
        console.log('Database synchronized');
        
        // Create sample users
        const users = [
            {
                userId: uuidv4(),
                email: 'pierresluse@gmail.com',
                password: 'Uy6qvZV0iA2/drm4zACDLCCm7BE9aCKZVQ16bg80XiU=',
                firstName: 'Pierre',
                lastName: 'Sluse',
                username: 'pierresluse',
                role: 'admin',
                themePreference: 0,
                location: { latitude: 48.8566, longitude: 2.3522, lastUpdated: new Date().toISOString() },
                latitude: 48.8566,
                longitude: 2.3522,
                waterNeeds: [
                    { amount: 100, usageType: 'Drinking', priority: 2, description: 'Daily drinking water' },
                    { amount: 200, usageType: 'Farming', priority: 3, description: 'Small garden irrigation' }
                ],
                isWellOwner: true,
                lastActive: new Date(),
                loginToken: 'sample-token-123',
                deviceTokens: JSON.stringify(['sample-device-token-123']),
                profileImageUrl: 'https://randomuser.me/api/portraits/men/1.jpg',
                bio: 'Administrator and water management specialist',
                registrationDate: new Date(),
                accountStatus: 'active',
                notificationPreferences: { waterAlerts: true, communityUpdates: true }
            },
            {
                userId: uuidv4(),
                email: 'user@example.com',
                password: 'Uy6qvZV0iA2/drm4zACDLCCm7BE9aCKZVQ16bg80XiU=',
                firstName: 'Test',
                lastName: 'User',
                username: 'testuser',
                role: 'user',
                themePreference: 0,
                location: { latitude: 48.85, longitude: 2.35, lastUpdated: new Date().toISOString() },
                latitude: 48.85,
                longitude: 2.35,
                waterNeeds: [
                    { amount: 50, usageType: 'Drinking', priority: 2, description: 'Daily drinking water' }
                ],
                isWellOwner: false,
                lastActive: new Date(),
                loginToken: null,
                deviceTokens: JSON.stringify(['sample-device-token-456']),
                profileImageUrl: 'https://randomuser.me/api/portraits/women/2.jpg',
                bio: 'Regular community member',
                registrationDate: new Date(),
                accountStatus: 'active',
                notificationPreferences: { waterAlerts: true, communityUpdates: false }
            },
            {
                userId: uuidv4(),
                email: 'wellowner@example.com',
                password: 'Uy6qvZV0iA2/drm4zACDLCCm7BE9aCKZVQ16bg80XiU=',
                firstName: 'Well',
                lastName: 'Owner',
                username: 'wellowner',
                role: 'user',
                themePreference: 0,
                location: { latitude: 48.86, longitude: 2.36, lastUpdated: new Date().toISOString() },
                latitude: 48.86,
                longitude: 2.36,
                waterNeeds: [],
                isWellOwner: true,
                lastActive: new Date(),
                loginToken: null,
                deviceTokens: JSON.stringify(['sample-device-token-789']),
                profileImageUrl: 'https://randomuser.me/api/portraits/men/3.jpg',
                bio: 'Well owner and maintainer',
                registrationDate: new Date(),
                accountStatus: 'active',
                notificationPreferences: { waterAlerts: true, communityUpdates: true }
            }
        ];
        
        await User.bulkCreate(users);
        console.log(`Created ${users.length} sample users`);
        
        // Get users for well ownership
        const allUsers = await User.findAll();
        const wellOwner = allUsers.find(u => u.isWellOwner);
        
        // Create sample wells
        const wells = [
            {
                espId: '1001',
                wellName: 'Community Well A',
                wellOwner: wellOwner?.email || 'pierresluse@gmail.com',
                ownerId: wellOwner?.userId,
                wellLocation: { latitude: 48.8566, longitude: 2.3522 },
                wellWaterType: 'Clean',
                wellCapacity: 1000,
                wellWaterLevel: 750,
                wellWaterConsumption: 10,
                extraData: { description: 'Main community well near town center' },
                wellStatus: 'Active',
                lastUpdated: new Date(),
                waterQuality: { ph: 7.2, turbidity: 0.5, tds: 150 }
            },
            {
                espId: '1002',
                wellName: 'Agricultural Well B',
                wellOwner: wellOwner?.email || 'pierresluse@gmail.com',
                ownerId: wellOwner?.userId,
                wellLocation: { latitude: 48.85, longitude: 2.35 },
                wellWaterType: 'Irrigation',
                wellCapacity: 2000,
                wellWaterLevel: 1200,
                wellWaterConsumption: 35,
                extraData: { description: 'Used primarily for crop irrigation' },
                wellStatus: 'Active',
                lastUpdated: new Date(),
                waterQuality: { ph: 6.8, turbidity: 1.2, tds: 220 }
            },
            {
                espId: '1003',
                wellName: 'Emergency Well C',
                wellOwner: wellOwner?.email || 'pierresluse@gmail.com',
                ownerId: wellOwner?.userId,
                wellLocation: { latitude: 48.86, longitude: 2.36 },
                wellWaterType: 'Emergency',
                wellCapacity: 800,
                wellWaterLevel: 800,
                wellWaterConsumption: 0,
                extraData: { description: 'Reserved for emergency use only' },
                wellStatus: 'Standby',
                lastUpdated: new Date(),
                waterQuality: { ph: 7.0, turbidity: 0.3, tds: 120 }
            },
            {
                espId: '1004',
                wellName: 'Maintenance Well D',
                wellOwner: wellOwner?.email || 'pierresluse@gmail.com',
                ownerId: wellOwner?.userId,
                wellLocation: { latitude: 48.87, longitude: 2.37 },
                wellWaterType: 'Clean',
                wellCapacity: 1200,
                wellWaterLevel: 100,
                wellWaterConsumption: 0,
                extraData: { description: 'Currently under maintenance' },
                wellStatus: 'Maintenance',
                lastUpdated: new Date(),
                waterQuality: { ph: 7.1, turbidity: 0.8, tds: 180 }
            }
        ];
        
        await Well.bulkCreate(wells);
        console.log(`Created ${wells.length} sample wells`);
        
        console.log('Database seeding completed successfully');
    } catch (error) {
        console.error('Error seeding database:', error);
    } finally {
        await sequelize.close();
    }
}

// Run the seed function
seed(); 