const { sequelize } = require('../models');

async function updateSchema() {
    console.log('Starting database schema update...');
    
    try {
        // Check if deviceTokens column exists
        const [results] = await sequelize.query("PRAGMA table_info(Users)");
        const hasDeviceTokens = results.some(column => column.name === 'deviceTokens');
        
        if (!hasDeviceTokens) {
            console.log('Adding deviceTokens column to Users table...');
            await sequelize.query("ALTER TABLE Users ADD COLUMN deviceTokens TEXT");
            console.log('deviceTokens column added successfully');
        } else {
            console.log('deviceTokens column already exists');
        }
        
        console.log('Schema update completed successfully');
    } catch (error) {
        console.error('Error updating schema:', error);
    } finally {
        await sequelize.close();
    }
}

// Run the update function
updateSchema(); 