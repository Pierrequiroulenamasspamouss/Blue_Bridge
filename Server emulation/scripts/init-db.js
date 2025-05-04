const { sequelize } = require('../models');
const db = require('../models');
const { User, Well } = db;

async function init() {
    try {
        // Sync database (create tables if not present)
        await sequelize.sync({ force: true });
        console.log('Database synced (all tables dropped and recreated)');
        // Create example users
        const admin = await User.create({
                    email: 'pierresluse@gmail.com',
                    password: 'Uy6qvZV0iA2/drm4zACDLCCm7BE9aCKZVQ16bg80XiU=',
                    firstName: 'Pierre',
                    lastName: 'Sluse',
                    username: 'P2R_Admin',
                    role: 'admin',
                    themePreference: 0,
                    latitude: 48.8589,
                    longitude: 2.3469,
                    location: { latitude: 48.8589, longitude: 2.3469, lastUpdated: new Date().toISOString() },
                    waterNeeds: [{ amount: 50, usageType: 'Farming', description: 'Irrigation', priority: 2 },{ amount: 9999999, usageType: 'ChatGPT', description: 'ChatGPT is kinda thirsty', priority: 0 }],
                    isWellOwner: true
                });

        const user1 = await User.create({
            email: 'user1@wellconnect.com',
            password: 'password1',
            firstName: 'Alice',
            lastName: 'Smith',
            username: 'alice',
            role: 'user',
            themePreference: 1,
            latitude: 48.8589,
            longitude: 2.3469,
            location: { latitude: 48.8589, longitude: 2.3469, lastUpdated: new Date().toISOString() },
            waterNeeds: [{ amount: 50, usageType: 'Farming', description: 'Irrigation', priority: 2 }],
            isWellOwner: false
        });
        const user2 = await User.create({
            email: 'owner1@wellconnect.com',
            password: 'password2',
            firstName: 'Bob',
            lastName: 'Johnson',
            username: 'bob',
            role: 'owner',
            themePreference: 2,
            latitude: 48.857,
            longitude: 2.3504,
            location: { latitude: 48.857, longitude: 2.3504, lastUpdated: new Date().toISOString() },
            waterNeeds: [{ amount: 100, usageType: 'Drinking', description: 'Community', priority: 1 }],
            isWellOwner: true
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
            ownerId: user2.id
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
            ownerId: user2.id
        });

        console.log('Example users and wells created!');
        process.exit(0);
    } catch (err) {
        console.error('Error initializing database:', err);
        process.exit(1);
    }
}

init();
