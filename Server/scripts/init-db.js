const path = require('path');
const { Sequelize } = require('sequelize');
const logger = require('../utils/logger');

// Import model factories
const UserFactory = require('../models/user');
const WellFactory = require('../models/well');
const DeviceTokenFactory = require('../models/deviceToken');

// Create Sequelize instances for each database
const sequelizeUsers = new Sequelize({
  dialect: 'sqlite',
  storage: path.join(__dirname, '../data/users.sqlite'),
  logging: msg => logger.debug(msg)
});
const sequelizeWells = new Sequelize({
  dialect: 'sqlite',
  storage: path.join(__dirname, '../data/wells.sqlite'),
  logging: msg => logger.debug(msg)
});
const sequelizeTokens = new Sequelize({
  dialect: 'sqlite',
  storage: path.join(__dirname, '../data/deviceTokens.sqlite'),
  logging: msg => logger.debug(msg)
});

// Initialize models for each DB
const User = UserFactory(sequelizeUsers);
const Well = WellFactory(sequelizeWells);
const DeviceToken = DeviceTokenFactory(sequelizeTokens);

// Initialize each database
async function initDatabases() {
  try {
    // Users DB
    await sequelizeUsers.authenticate();
    logger.info('Connected to users database successfully');
    await User.sync({ alter: true });
    logger.info('Synced User model for users database');

    // Wells DB
    await sequelizeWells.authenticate();
    logger.info('Connected to wells database successfully');
    await Well.sync({ alter: true });
    logger.info('Synced Well model for wells database');

    // DeviceTokens DB
    await sequelizeTokens.authenticate();
    logger.info('Connected to deviceTokens database successfully');
    await DeviceToken.sync({ alter: true });
    logger.info('Synced DeviceToken model for deviceTokens database');

    logger.info('All databases initialized successfully');
  } catch (error) {
    logger.error('Database initialization failed:', error);
    process.exit(1);
  }
}

// Run initialization
(async () => {
  try {
    await initDatabases();
    process.exit(0);
  } catch (error) {
    process.exit(1);
  }
})();