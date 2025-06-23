const { Sequelize } = require('sequelize');
const path = require('path');
const bcrypt = require('bcrypt');
const { v4: uuidv4 } = require('uuid');
const logger = require('../utils/logger');

// Initialize models
const sequelize = new Sequelize({
  dialect: 'sqlite',
  storage: path.join(__dirname, '../users.sqlite'),
  logging: msg => logger.debug(msg)
});

const models = require('../models');
models.sequelize = sequelize;

// Configuration
const SEED_PASSWORD_SALT_ROUNDS = 10;
const ADMIN_EMAIL = 'admin@bluebridge.com';
const USER_EMAIL = 'user@bluebridge.com';
const OWNER_EMAIL = 'owner@bluebridge.com';

// Enhanced logger for validation errors
function logValidationErrors(errors) {
  logger.error('Validation errors encountered:');
  errors.forEach((err, index) => {
    logger.error(`  ${index + 1}. Field: ${err.path}`);
    logger.error(`     Type: ${err.type}`);
    logger.error(`     Value: ${JSON.stringify(err.value)}`);
    logger.error(`     Message: ${err.message}`);
    if (err.validatorKey) {
      logger.error(`     Validator: ${err.validatorKey}`);
    }
  });
}

// Helper functions
const randomFloat = (min, max, decimals = 2) =>
  parseFloat((Math.random() * (max - min) + min).toFixed(decimals));

const randomInt = (min, max) =>
  Math.floor(Math.random() * (max - min + 1)) + min;

const randomDate = (start, end) =>
  new Date(start.getTime() + Math.random() * (end.getTime() - start.getTime()));

// Seed data with all required fields
const seedUsers = [
  {
    userId: uuidv4(),
    email: 'pierresluse@gmail.com',
    phoneNumber: '+33666666666',
    role: 'admin',
    themePreference: 0,
    password: 'Uy6qvZV0iA2/drm4zACDLCCm7BE9aCKZVQ16bg80XiU=',
    firstName: 'Pierre',
    lastName: 'Sluse',
    username: 'Pi2R',
    location: JSON.stringify({ latitude: 48.8589, longitude: 2.3469 }),
    waterNeeds: JSON.stringify([{ type: 'drinking', amount: 2.5 }]),
    notificationPreferences: JSON.stringify({
      email: true,
      sms: false,
      push: true
    }),
    lastActive: new Date(),
    createdAt: new Date(),
    updatedAt: new Date(),
    loginToken: uuidv4(),
    isWellOwner: true
  },
  {
    userId: uuidv4(),
    email: ADMIN_EMAIL,
    firstName: 'Admin',
    lastName: 'User',
    role: 'admin',
    username: 'admin',
    password: '', // Will be hashed
    phoneNumber: '+33123456789',
    location: JSON.stringify({ latitude: 48.857, longitude: 2.3522 }),
    waterNeeds: JSON.stringify([{ type: 'irrigation', amount: 5.0 }]),
    notificationPreferences: JSON.stringify({
      email: true,
      sms: true,
      push: true
    }),
    lastActive: new Date(),
    createdAt: new Date(),
    updatedAt: new Date()
  },
  {
    userId: uuidv4(),
    email: USER_EMAIL,
    firstName: 'Regular',
    lastName: 'User',
    role: 'user',
    username: 'user',
    password: '', // Will be hashed
    phoneNumber: '+33987654321',
    location: JSON.stringify({ latitude: 48.855, longitude: 2.350 }),
    waterNeeds: JSON.stringify([{ type: 'industrial', amount: 10.0 }]),
    notificationPreferences: JSON.stringify({
      email: false,
      sms: true,
      push: false
    }),
    lastActive: new Date(),
    createdAt: new Date(),
    updatedAt: new Date()
  },
  {
    userId: uuidv4(),
    email: OWNER_EMAIL,
    firstName: 'Well',
    lastName: 'Owner',
    role: 'well_owner',
    username: 'owner',
    password: '', // Will be hashed
    phoneNumber: '+33555555555',
    location: JSON.stringify({ latitude: 48.853, longitude: 2.348 }),
    waterNeeds: JSON.stringify([{ type: 'mixed', amount: 7.5 }]),
    notificationPreferences: JSON.stringify({
      email: true,
      sms: false,
      push: true
    }),
    lastActive: new Date(),
    createdAt: new Date(),
    updatedAt: new Date()
  }
];

// Generate wells with all required fields
const seedWells = Array.from({ length: 10 }, (_, i) => {
  const wellId = i + 1;
  return {
    espId: `esp32-${String(wellId).padStart(3, '0')}`,
    wellName: `Well ${wellId}`,
    description: `Description for well ${wellId}`,
    location: JSON.stringify({ latitude: randomFloat(48.85, 48.86), longitude: randomFloat(2.34, 2.36) }),
    latitude: randomFloat(48.85, 48.86),
    longitude: randomFloat(2.34, 2.36),
    water_level: randomFloat(30, 100).toString(),
    water_quality: 'Good',
    status: ['Active', 'Maintenance', 'Inactive'][randomInt(0, 2)],
    owner: OWNER_EMAIL,
    contact_info: 'contact@example.com',
    access_info: '24/7 access',
    notes: 'Regular maintenance',
    last_update: new Date(),
    wellWaterConsumption: randomFloat(10, 100).toString(),
    wellWaterType: ['Clean', 'Potable', 'Brackish'][randomInt(0, 2)],
    createdAt: new Date(),
    updatedAt: new Date()
  };
});

async function seedDatabases() {
  try {
    logger.info('Starting database seeding...');

    // Sync all models
    await models.sequelize.sync({ force: true });
    logger.info('Database tables created');

    // Hash passwords
    for (const user of seedUsers) {
      if (!user.password.startsWith('Uy6qvZV0iA2')) {
        user.password = await bcrypt.hash('defaultPassword123', SEED_PASSWORD_SALT_ROUNDS);
      }
    }

    // Create users
    const createdUsers = await models.User.bulkCreate(seedUsers, {
      validate: true,
      returning: true
    });
    logger.info(`Created ${createdUsers.length} users`);

    // Create device tokens
    const deviceTokens = createdUsers.map(user => ({
      tokenId: uuidv4(),
      userId: user.userId,
      token: `${user.username}-device-token-${uuidv4().slice(0, 8)}`,
      deviceType: ['android', 'ios'][randomInt(0, 1)],
      isActive: true,
      lastUsed: new Date(),
      createdAt: new Date(),
      updatedAt: new Date()
    }));
    await models.DeviceToken.bulkCreate(deviceTokens, { validate: true });
    logger.info(`Created ${deviceTokens.length} device tokens`);

    // Create wells
    const createdWells = await models.Well.bulkCreate(seedWells, { validate: true });
    logger.info(`Created ${createdWells.length} wells`);

    logger.info('Database seeded successfully!');
    process.exit(0);
  } catch (error) {
    logger.error('SEEDING FAILED:');
    logger.error(`Error name: ${error.name}`);
    logger.error(`Error message: ${error.message}`);
    logger.error(`Full error object: ${JSON.stringify(error, null, 2)}`);

    if (error.name === 'AggregateError' && error.errors) {
      logger.error('AggregateError details:');
      error.errors.forEach((err, idx) => {
        logger.error(`  [${idx + 1}] ${err.name}: ${err.message}`);
        if (err.errors) {
          err.errors.forEach((subErr, subIdx) => {
            logger.error(`    [${subIdx + 1}] ${subErr.path}: ${subErr.message}`);
          });
        }
      });
    }

    if (error.name === 'SequelizeValidationError') {
      logValidationErrors(error.errors);
    }

    process.exit(1);
  }
}

// Run seeding
seedDatabases();