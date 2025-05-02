const { Sequelize } = require('sequelize');
const path = require('path');
require('dotenv').config({ path: path.join(__dirname, '../../.env') });

// Initialize Sequelize with SQLite
const sequelize = new Sequelize({
    dialect: 'sqlite',
    storage: '/opt/wellconnect/database.sqlite',
    logging: false
});

const db = {};

// Import models
db.User = require('./user')(sequelize);
db.Well = require('./well')(sequelize);

// Set up associations
Object.keys(db).forEach(modelName => {
    if (db[modelName].associate) {
        db[modelName].associate(db);
    }
});

// Add sequelize instance and Sequelize class
db.sequelize = sequelize;
db.Sequelize = Sequelize;

// Export models and sequelize instance
module.exports = db; 