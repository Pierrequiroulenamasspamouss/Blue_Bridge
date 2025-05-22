const path = require('path');
const models = require('../models');
const { User, DeviceToken, Well } = models;

// Initialize databases
async function initDatabases() {
    try {
        // Use alter: true instead of force: true after first run
        await models.User.sync({ force: true });
        await models.Well.sync({ force: true });
        await models.DeviceToken.sync({ force: true });
        
        console.log('All databases initialized successfully');
    } catch (error) {
        if (error.original && error.original.code === 'SQLITE_ERROR') {
            console.log('First-time database setup completed');
        } else {
            console.error('Error initializing databases:', error);
            process.exit(1);
        }
    }
}
// Run initialization
initDatabases();
