const { usersDb, wellsDb, deviceTokensDb, User, Well, DeviceToken } = require('../models');
//TODO : fix that tool to work with the new database layout ( users, wells, deviceToken)
async function init() {
    try {
        // Sync all databases
        await Promise.all([
            usersDb.sync({ force: true }),
            wellsDb.sync({ force: true }),
            deviceTokensDb.sync({ force: true })
        ]);
        console.log('All databases synced (all tables dropped and recreated)');

        // Create example users
        const admin = await User.create({
            email: 'pierresluse@gmail.com',
            password: 'Uy6qvZV0iA2/drm4zACDLCCm7BE9aCKZVQ16bg80XiU=',
            firstName: 'Pierre',
            lastName: 'Sluse',
            username: 'P2R_Admin',
            role: 'admin',
            location: { latitude: 48.8589, longitude: 2.3469, lastUpdated: new Date().toISOString() },
            waterNeeds: [
                { amount: 50, usageType: 'Farming', description: 'Irrigation', priority: 2 },
                { amount: 9999999, usageType: 'ChatGPT', description: 'ChatGPT is kinda thirsty', priority: 0 }
            ]
        });

        const user1 = await User.create({
            email: 'user1@bluebridge.com',
            password: 'password1',
            firstName: 'Alice',
            lastName: 'Smith',
            username: 'alice',
            role: 'user',
            location: { latitude: 48.8589, longitude: 2.3469, lastUpdated: new Date().toISOString() },
            waterNeeds: [{ amount: 50, usageType: 'Farming', description: 'Irrigation', priority: 2 }]
        });

        const user2 = await User.create({
            email: 'owner1@bluebridge.com',
            password: 'password2',
            firstName: 'Bob',
            lastName: 'Johnson',
            username: 'bob',
            role: 'owner',
            location: { latitude: 48.857, longitude: 2.3504, lastUpdated: new Date().toISOString() },
            waterNeeds: [{ amount: 100, usageType: 'Drinking', description: 'Community', priority: 1 }]
        });

        // Create example device tokens
        await DeviceToken.create({
            userId: admin.userId,
            token: 'sample-device-token-1',
            deviceType: 'android',
            isActive: true
        });

        await DeviceToken.create({
            userId: user1.userId,
            token: 'sample-device-token-2',
            deviceType: 'ios',
            isActive: true
        });

        // Create example wells
        await Well.create({
            espId: 'esp32-001',
            wellName: 'Central Park Well',
            wellOwner: 'bob',
            wellLocation: { latitude: 48.8589, longitude: 2.3469 },
            wellWaterType: 'Clean',
            wellCapacity: 10000,
            wellWaterLevel: 8500,
            wellWaterConsumption: 100,
            extraData: {},
            wellStatus: 'Active',
            lastUpdated: new Date(),
            waterQuality: { ph: 7.2, turbidity: 0.5, tds: 120 },
            ownerId: user2.userId
        });

        await Well.create({
            espId: 'esp32-002',
            wellName: 'River Well',
            wellOwner: 'bob',
            wellLocation: { latitude: 48.857, longitude: 2.3504 },
            wellWaterType: 'Grey',
            wellCapacity: 8000,
            wellWaterLevel: 6000,
            wellWaterConsumption: 75,
            extraData: {},
            wellStatus: 'Active',
            lastUpdated: new Date(),
            waterQuality: { ph: 7.0, turbidity: 0.7, tds: 150 },
            ownerId: user2.userId
        });

        console.log('Example users, device tokens, and wells created!');
        process.exit(0);
    } catch (err) {
        console.error('Error initializing database:', err);
        process.exit(1);
    }
}

init();
