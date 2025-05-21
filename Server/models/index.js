const { Sequelize } = require('sequelize');
const path = require('path');

// Create separate database connections
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

// Import models
const User = require('./user')(usersDb);
const Well = require('./well')(wellsDb);
const DeviceToken = require('./deviceToken')(deviceTokensDb);

// Define associations
User.hasMany(DeviceToken, { foreignKey: 'userId' });
DeviceToken.belongsTo(User, { foreignKey: 'userId' });

Well.belongsTo(User, { foreignKey: 'ownerId' });
User.hasMany(Well, { foreignKey: 'ownerId' });

module.exports = {
    usersDb,
    wellsDb,
    deviceTokensDb,
    User,
    Well,
    DeviceToken
}; 