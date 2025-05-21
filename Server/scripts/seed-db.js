const { usersDb, wellsDb, deviceTokensDb, User, Well, DeviceToken } = require('../models');
const { v4: uuidv4 } = require('uuid');
//TODO : fix that tool to work with the new database layout ( users, wells, deviceToken)
async function seed() {
    console.log('Starting database seeding...');
    
    try {
        // Sync all databases
        await Promise.all([
            usersDb.sync({ force: true }),
            wellsDb.sync({ force: true }),
            deviceTokensDb.sync({ force: true })
        ]);
        console.log('All databases synchronized');
        
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
                profileImageUrl: 'https://randomuser.me/api/portraits/women/2.jpg',
                bio: 'Regular community member',
                registrationDate: new Date(),
                accountStatus: 'active',
                notificationPreferences: { waterAlerts: true, communityUpdates: false }
            }
        ];
        
        const createdUsers = await User.bulkCreate(users);
        console.log(`Created ${users.length} sample users`);

        // Create device tokens for users
        const deviceTokens = createdUsers.map(user => ({
            userId: user.userId,
            token: `sample-device-token-${user.userId}`,
            deviceType: 'android',
            lastUsed: new Date()
        }));

        await DeviceToken.bulkCreate(deviceTokens);
        console.log(`Created ${deviceTokens.length} device tokens`);
        
        // Create sample wells
        const wells = [
            {
                espId: '1001',
                wellName: 'Community Well A',
                wellOwner: createdUsers[0].email,
                ownerId: createdUsers[0].userId,
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
                wellOwner: createdUsers[0].email,
                ownerId: createdUsers[0].userId,
                wellLocation: { latitude: 48.85, longitude: 2.35 },
                wellWaterType: 'Irrigation',
                wellCapacity: 2000,
                wellWaterLevel: 1200,
                wellWaterConsumption: 35,
                extraData: { description: 'Used primarily for crop irrigation' },
                wellStatus: 'Active',
                lastUpdated: new Date(),
                waterQuality: { ph: 6.8, turbidity: 1.2, tds: 220 }
            }
        ];
        
        await Well.bulkCreate(wells);
        console.log(`Created ${wells.length} sample wells`);
        
        console.log('Database seeding completed successfully');
    } catch (error) {
        console.error('Error seeding database:', error);
    } finally {
        await Promise.all([
            usersDb.close(),
            wellsDb.close(),
            deviceTokensDb.close()
        ]);
    }
}

// Run the seed function
seed(); 