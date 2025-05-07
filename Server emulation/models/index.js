const fs = require('fs');
const path = require('path');
const { Sequelize } = require('sequelize');

// Configure database path (can be overridden by .env)
const dbPath = process.env.DB_PATH || path.join(__dirname, '..', 'data', 'database.sqlite');

// Ensure data directory exists
const dataDir = path.dirname(dbPath);
if (!fs.existsSync(dataDir)) {
    fs.mkdirSync(dataDir, { recursive: true });
}

// Create Sequelize instance
const sequelize = new Sequelize({
    dialect: 'sqlite',
    storage: dbPath,
    logging: process.env.NODE_ENV === 'development' ? console.log : false
});

// Initialize models
const db = {
    sequelize,
    Sequelize
};

// Import models
db.User = require('./user')(sequelize);
db.Well = require('./well')(sequelize);

// Define associations between models if needed
// Example: db.User.hasMany(db.Well);

// Sync database (don't force in production)
// This is now done in server.js to be more controlled

module.exports = db; 