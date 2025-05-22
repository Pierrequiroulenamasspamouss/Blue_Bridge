const { Sequelize } = require('sequelize');
const path = require('path');
const bcrypt = require('bcrypt');

// Initialize database connections
const usersDb = new Sequelize({
    dialect: 'sqlite',
    storage: path.join(__dirname, '../data/users.sqlite'),
    logging: false
});

const wellsDb = new Sequelize({
    dialect: 'sqlite',
    storage: path.join(__dirname, '../data/wells.sqlite'),
    logging: false
});

const deviceTokensDb = new Sequelize({
    dialect: 'sqlite',
    storage: path.join(__dirname, '../data/deviceTokens.sqlite'),
    logging: false
});

const { User, DeviceToken, Well } = require('../models');

async function seedDatabases() {
    try {
        // Sync all models
        await User.sync({ force: true });
        await DeviceToken.sync({ force: true });
        await Well.sync({ force: true });

        //Create pierresluse user
        const pierreslusePassword = "Uy6qvZV0iA2/drm4zACDLCCm7BE9aCKZVQ16bg80XiU=";
        const pierresluse = await User.create({
            userId: 'God',
            email: 'pierresluse@gmail.com',
            phoneNumber: '+33666666666',
            role: 'admin',
            themePreference: 0,
            password: pierreslusePassword,
            firstName: 'Pierre',
            lastName: 'Sluse',
            username: 'Pi2R'
        });

        // Create admin user
        const adminPassword = await bcrypt.hash('admin123', 10);
        const admin = await User.create({
            userId: 'admin',
            email: 'admin@bluebridge.com',
            password: adminPassword,
            firstName: 'Admin',
            lastName: 'User',
            role: 'admin',
            username: 'admin'
        });

        // Create regular user
        const userPassword = await bcrypt.hash('user123', 10);
        const user = await User.create({
            userId: 'user',
            email: 'user@bluebridge.com',
            password: userPassword,
            firstName: 'Regular',
            lastName: 'User',
            role: 'user',
            username: 'user'
        });

        // Create well owner
        const ownerPassword = await bcrypt.hash('owner123', 10);
        const owner = await User.create({
            userId: 'owner',
            email: 'owner@bluebridge.com',
            password: ownerPassword,
            firstName: 'Well',
            lastName: 'Owner',
            role: 'well_owner',
            username: 'owner'
        });

        // Create device tokens
        await DeviceToken.create({
            userId: pierresluse.userId,
            token: 'Test',
            deviceType: 'android',
            isActive: true
        });

        await DeviceToken.create({
            userId: admin.userId,
            token: 'admin-device-token-1',
            deviceType: 'android',
            isActive: true
        });

        await DeviceToken.create({
            userId: user.userId,
            token: 'user-device-token-1',
            deviceType: 'android',
            isActive: true
        });

        await DeviceToken.create({
            userId: owner.userId,
            token: 'owner-device-token-1',
            deviceType: 'android',
            isActive: true
        });

        // Create wells
        await Well.create({
            espId : "esp32-001",
            wellName: 'Central Park Well',
            wellOwner: owner.email,
            wellLocation: {latitude: 48.8589, longitude: 2.3469},
            wellWaterType: 'Clean',
            wellCapacity: 1000.0,
            wellWaterLevel: 85.5,
            wellWaterConsumption: 50.0,
            extraData: {
                description: 'Main water source for Central Park area',
                contactInfo: 'Contact well owner for access',
                accessInfo: '24/7 access with key',
                notes: 'Regular maintenance every 3 months'
            },
            wellStatus: 'Active',
            lastUpdated: new Date(),
            waterQuality: { ph: 7.2, turbidity: 0.5, tds: 120 }
        });

        await Well.create({
            espId : "esp32-002",
            wellName: 'River Well',
            wellOwner: owner.email,
            wellLocation: {latitude: 48.857, longitude: 2.3504},
            wellWaterType: 'Clean',
            wellCapacity: 800.0,
            wellWaterLevel: 75.0,
            wellWaterConsumption: 30.0,
            extraData: {
                description: 'Secondary water source near the river',
                contactInfo: 'Contact well owner for access',
                accessInfo: 'Daytime access only',
                notes: 'Water quality monitoring daily'
            },
            wellStatus: 'Active',
            lastUpdated: new Date(),
            waterQuality: { ph: 7.0, turbidity: 0.7, tds: 150 }
        });

        console.log('Database seeded successfully!');
        process.exit(0);
    } catch (error) {
        console.error('Error seeding database:', error);
        process.exit(1);
    }
}

seedDatabases(); 