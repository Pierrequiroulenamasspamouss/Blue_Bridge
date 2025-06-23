const { Sequelize } = require('sequelize');
const path = require('path');
const bcrypt = require('bcrypt');
const { v4: uuidv4 } = require('uuid');
const logger = require('../utils/logger');
const { User, DeviceToken, Well } = require('../models');

// Configuration
const SEED_PASSWORD_SALT_ROUNDS = 10;
const ADMIN_EMAIL = 'admin@bluebridge.com';
const USER_EMAIL = 'user@bluebridge.com';
const OWNER_EMAIL = 'owner@bluebridge.com';

// Seed data
const seedUsers = [
  {
    userId: uuidv4(),
    email: 'pierresluse@gmail.com',
    phoneNumber: '+33666666666',
    role: 'admin',
    themePreference: 0,
    password: 'Uy6qvZV0iA2/drm4zACDLCCm7BE9aCKZVQ16bg80XiU=', // Note: In production, always hash passwords
    firstName: 'Pierre',
    lastName: 'Sluse',
    username: 'Pi2R'
  },
  {
    userId: uuidv4(),
    email: ADMIN_EMAIL,
    firstName: 'Admin',
    lastName: 'User',
    role: 'admin',
    username: 'admin'
  },
  {
    userId: uuidv4(),
    email: USER_EMAIL,
    firstName: 'Regular',
    lastName: 'User',
    role: 'user',
    username: 'user'
  },
  {
    userId: uuidv4(),
    email: OWNER_EMAIL,
    firstName: 'Well',
    lastName: 'Owner',
    role: 'well_owner',
    username: 'owner'
  }
];

const seedWells = [
  {
    espId: "esp32-001",
    name: 'Central Park Well',
    owner: OWNER_EMAIL,
    location: JSON.stringify({ latitude: 48.8589, longitude: 2.3469 }),
    waterType: 'Clean',
    capacity: 1000.0,
    waterLevel: 85.5,
    waterConsumption: 50.0,
    extraData: JSON.stringify({
      description: 'Main water source for Central Park area',
      contactInfo: 'Contact well owner for access',
      accessInfo: '24/7 access with key',
      notes: 'Regular maintenance every 3 months'
    }),
    status: 'Active',
    waterQuality: JSON.stringify({ ph: 7.2, turbidity: 0.5, tds: 120 })
  },
  {
    espId: "esp32-002",
    name: 'River Well',
    owner: OWNER_EMAIL,
    location: JSON.stringify({ latitude: 48.857, longitude: 2.3504 }),
    waterType: 'Clean',
    capacity: 800.0,
    waterLevel: 75.0,
    waterConsumption: 30.0,
    extraData: JSON.stringify({
      description: 'Secondary water source near the river',
      contactInfo: 'Contact well owner for access',
      accessInfo: 'Daytime access only',
      notes: 'Water quality monitoring daily'
    }),
    status: 'Active',
    waterQuality: JSON.stringify({ ph: 7.0, turbidity: 0.7, tds: 150 })
  }
];

async function seedDatabases() {
  try {
    logger.info('Starting database seeding...');

    // Hash passwords for seeded users
    for (const user of seedUsers) {
      if (user.email !== 'pierresluse@gmail.com') { // Skip hashing for the pre-hashed password
        user.password = await bcrypt.hash('defaultPassword123', SEED_PASSWORD_SALT_ROUNDS);
      }
    }

    // Create users
    const createdUsers = await User.bulkCreate(seedUsers, { returning: true });
    logger.info(`Created ${createdUsers.length} users`);

    // Create device tokens
    const deviceTokens = createdUsers.map(user => ({
      tokenId: uuidv4(),
      userId: user.userId,
      token: `${user.username}-device-token-1`,
      deviceType: 'android',
      isActive: true
    }));

    await DeviceToken.bulkCreate(deviceTokens);
    logger.info(`Created ${deviceTokens.length} device tokens`);

    // Create wells
    const createdWells = await Well.bulkCreate(seedWells);
    logger.info(`Created ${createdWells.length} wells`);

    logger.info('Database seeded successfully!');
    process.exit(0);
  } catch (error) {
    logger.error('Error seeding database:', error);
    process.exit(1);
  }
}

// Run seeding
seedDatabases();