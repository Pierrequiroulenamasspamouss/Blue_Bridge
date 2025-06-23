const { Sequelize } = require('sequelize');
const path = require('path');

// Import model factories
const UserFactory = require('./user');
const WellFactory = require('./well');
const DeviceTokenFactory = require('./deviceToken');

// Create Sequelize instances for each database
const sequelizeUsers = new Sequelize({
  dialect: 'sqlite',
  storage: path.join(__dirname, '../data/users.sqlite'),
  logging: false
});
const sequelizeWells = new Sequelize({
  dialect: 'sqlite',
  storage: path.join(__dirname, '../data/wells.sqlite'),
  logging: false
});
const sequelizeTokens = new Sequelize({
  dialect: 'sqlite',
  storage: path.join(__dirname, '../data/deviceTokens.sqlite'),
  logging: false
});

// Initialize models and export them
const db = {
  User: UserFactory(sequelizeUsers),
  Well: WellFactory(sequelizeWells),
  DeviceToken: DeviceTokenFactory(sequelizeTokens),
  sequelizeUsers,
  sequelizeWells,
  sequelizeTokens
};

module.exports = db; 