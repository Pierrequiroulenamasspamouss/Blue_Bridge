const express = require('express');
const router = express.Router();
const db = require('../models');
const { Well } = db;

// Get well stats
router.get('/', async (req, res) => {
    try {
        // Get count of wells
        const count = await Well.count();
        
        // Get average capacity, water level, and consumption
        const avgCapacity = await Well.findAll({
            attributes: [
                [db.sequelize.fn('AVG', db.sequelize.col('wellCapacity')), 'avgCapacity']
            ],
            raw: true
        });
        
        const avgWaterLevel = await Well.findAll({
            attributes: [
                [db.sequelize.fn('AVG', db.sequelize.col('wellWaterLevel')), 'avgWaterLevel']
            ],
            raw: true
        });
        
        const avgConsumption = await Well.findAll({
            attributes: [
                [db.sequelize.fn('AVG', db.sequelize.col('wellWaterConsumption')), 'avgConsumption']
            ],
            raw: true
        });
        
        // Get count of wells by status
        const statusCounts = await Well.findAll({
            attributes: [
                'wellStatus',
                [db.sequelize.fn('COUNT', db.sequelize.col('wellStatus')), 'count']
            ],
            group: ['wellStatus'],
            raw: true
        });
        
        // Get count of wells by water type
        const waterTypeCounts = await Well.findAll({
            attributes: [
                'wellWaterType',
                [db.sequelize.fn('COUNT', db.sequelize.col('wellWaterType')), 'count']
            ],
            group: ['wellWaterType'],
            raw: true
        });
        
        // Calculate total capacity and water level
        const totalCapacity = await Well.sum('wellCapacity');
        const totalWaterLevel = await Well.sum('wellWaterLevel');
        
        // Calculate percentage of total water available
        const percentageAvailable = totalCapacity > 0 ? (totalWaterLevel / totalCapacity) * 100 : 0;
        
        // Map status counts to object
        const statusCountsObj = {};
        statusCounts.forEach(item => {
            if (item.wellStatus) {
                statusCountsObj[item.wellStatus] = parseInt(item.count);
            }
        });
        
        // Map water type counts to object
        const waterTypeCountsObj = {};
        waterTypeCounts.forEach(item => {
            if (item.wellWaterType) {
                waterTypeCountsObj[item.wellWaterType] = parseInt(item.count);
            }
        });
        
        // Get recently updated wells (last 24 hours)
        const oneDayAgo = new Date();
        oneDayAgo.setDate(oneDayAgo.getDate() - 1);
        
        const recentlyUpdated = await Well.count({
            where: {
                lastUpdated: {
                    [db.Sequelize.Op.gte]: oneDayAgo
                }
            }
        });
        
        res.json({
            status: 'success',
            stats: {
                totalWells: count,
                avgCapacity: parseFloat((avgCapacity[0]?.avgCapacity || 0).toFixed(2)),
                avgWaterLevel: parseFloat((avgWaterLevel[0]?.avgWaterLevel || 0).toFixed(2)),
                avgConsumption: parseFloat((avgConsumption[0]?.avgConsumption || 0).toFixed(2)),
                totalCapacity: parseFloat((totalCapacity || 0).toFixed(2)),
                totalWaterLevel: parseFloat((totalWaterLevel || 0).toFixed(2)),
                percentageAvailable: parseFloat(percentageAvailable.toFixed(2)),
                statusCounts: statusCountsObj,
                waterTypeCounts: waterTypeCountsObj,
                recentlyUpdated
            }
        });
    } catch (error) {
        console.error('Error fetching stats:', error);
        res.status(500).json({ 
            status: 'error', 
            message: error.message 
        });
    }
});

module.exports = router; 