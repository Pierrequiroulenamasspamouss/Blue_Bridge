const { Sequelize } = require('sequelize');
const path = require('path');
const fs = require('fs');
require('dotenv').config({ path: path.join(__dirname, '../../.env') });

// Create database directory if it doesn't exist
const dbDir = path.join(__dirname, '../data');
if (!fs.existsSync(dbDir)) {
    fs.mkdirSync(dbDir, { recursive: true });
}

// Initialize Sequelize with SQLite - use local path
const sequelize = new Sequelize({
    dialect: 'sqlite',
    storage: path.join(dbDir, 'database.sqlite'),
    logging: console.log
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