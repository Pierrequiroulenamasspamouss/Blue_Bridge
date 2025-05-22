const sequelize = require('../config/database');
const User = require('./user');
const DeviceToken = require('./deviceToken');
const Well = require('./well');

// Initialize models
const models = {
    User: User(sequelize),
    DeviceToken: DeviceToken(sequelize),
    Well: Well(sequelize)
};

// Set up associations
Object.keys(models).forEach(modelName => {
    if (models[modelName].associate) {
        models[modelName].associate(models);
    }
});

// Sync database
sequelize.sync() //TODO: check if { alter: true, force: false } is useful or not inside sequelize.sync({ alter: true, force: false })
    .then(() => {
        console.log('Database synced successfully');
    })
    .catch(err => {
        console.error('Error syncing database:', err);
    });

module.exports = models; 