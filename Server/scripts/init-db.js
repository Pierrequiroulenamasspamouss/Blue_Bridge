const path = require('path');
const { Sequelize } = require('sequelize');
const { User, DeviceToken, Well } = require('../models');
const logger = require('../utils/logger');

// Database configuration
const databases = {
  users: {
    path: path.join(__dirname, '../data/users.sqlite'),
    models: [User]
  },
  wells: {
    path: path.join(__dirname, '../data/wells.sqlite'),
    models: [Well]
  },
  deviceTokens: {
    path: path.join(__dirname, '../data/deviceTokens.sqlite'),
    models: [DeviceToken]
  }
};

// Initialize databases
async function initDatabases() {
  try {
    for (const [dbName, config] of Object.entries(databases)) {
      const sequelize = new Sequelize({
        dialect: 'sqlite',
        storage: config.path,
        logging: msg => logger.debug(msg)
      });

      // Test connection
      await sequelize.authenticate();
      logger.info(`Connected to ${dbName} database successfully`);

      // Sync models
      for (const model of config.models) {
        await model.sync({ alter: true }); // Use alter for production, force for development
        logger.info(`Synced model ${model.name} for ${dbName} database`);
      }
    }

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