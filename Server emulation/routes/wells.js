const express = require('express');
const router = express.Router();
const db = require('../models');
const { Well, User } = db;
const { validateToken } = require('./auth');

function mapToShortenedWellData(well) {
    // Extract latitude/longitude from wellLocation JSON if present
    let latitude = '';
    let longitude = '';
    if (well.wellLocation && typeof well.wellLocation === 'object') {
        latitude = well.wellLocation.latitude != null ? String(well.wellLocation.latitude) : '';
        longitude = well.wellLocation.longitude != null ? String(well.wellLocation.longitude) : '';
    }
    
    // Add last refresh time
    const lastRefreshTime = well.lastUpdated ? new Date(well.lastUpdated).getTime() : 0;
    
    return {
        id: well.id || well.espId || '',
        wellName: well.wellName || '',
        wellLocation: well.wellLocation || { latitude: 0.0, longitude: 0.0 },
        wellWaterType: well.wellWaterType || well.waterType || '',
        espId: well.espId || '',
        wellStatus: well.wellStatus || well.status || 'Unknown',
        wellOwner: well.wellOwner || '',
        wellCapacity: well.wellCapacity != null ? String(well.wellCapacity) : (well.capacity != null ? String(well.capacity) : ''),
        wellWaterLevel: well.wellWaterLevel != null ? String(well.wellWaterLevel) : (well.waterLevel != null ? String(well.waterLevel) : ''),
        wellWaterConsumption: well.wellWaterConsumption != null ? String(well.wellWaterConsumption) : (well.waterConsumption != null ? String(well.waterConsumption) : ''),
        waterQuality: well.waterQuality || { ph: 7.0, turbidity: 0.0, tds: 0 },
        lastRefreshTime
    };
}

// Get all wells
router.get('/', async (req, res) => {
    try {
        const wells = await Well.findAll({
            include: [{
                model: User,
                as: 'owner',
                attributes: ['username', 'email']
            }]
        });
        res.json(wells.map(mapToShortenedWellData));
    } catch (error) {
        console.error('Error fetching wells:', error);
        res.status(500).json({ 
            status: 'error', 
            message: error.message 
        });
    }
});

// Get well by ESP ID
router.get('/:espId', async (req, res) => {
    try {
        const well = await Well.findOne({
            where: { espId: req.params.espId },
            include: [{
                model: User,
                as: 'owner',
                attributes: ['username', 'email']
            }]
        });
        
        if (!well) {
            // If well not found in database, create a mock well for demo purposes
            const mockWell = {
                espId: req.params.espId,
                wellName: `Well ${req.params.espId}`,
                wellLocation: { latitude: 40.7128, longitude: -74.0060 },
                wellWaterType: 'Clean',
                wellStatus: 'Active',
                wellOwner: 'Demo User',
                wellCapacity: '1000',
                wellWaterLevel: '750',
                wellWaterConsumption: '10',
                waterQuality: { ph: 7.2, turbidity: 0.5, tds: 150 },
                lastUpdated: new Date()
            };
            return res.json(mapToShortenedWellData(mockWell));
        }
        
        res.json(mapToShortenedWellData(well));
    } catch (error) {
        console.error('Error fetching well:', error);
        res.status(500).json({ 
            status: 'error', 
            message: error.message 
        });
    }
});

// Create new well
router.post('/', validateToken, async (req, res) => {
    try {
        const {
            wellName, wellLocation, wellWaterType, espId, wellStatus, wellOwner, wellCapacity, wellWaterLevel, wellWaterConsumption, waterQuality, extraData
        } = req.body;
        
        // Check for required fields
        if (!wellName || !espId) {
            return res.status(400).json({
                status: 'error',
                message: 'Well name and ESP ID are required'
            });
        }
        
        // Accept wellLocation as either a string or object
        let locationObj = null;
        if (typeof wellLocation === 'string') {
            const [lat, lon] = wellLocation.split(',').map(Number);
            if (!isNaN(lat) && !isNaN(lon)) {
                locationObj = { latitude: lat, longitude: lon };
            }
        } else if (typeof wellLocation === 'object') {
            locationObj = wellLocation;
        }
        
        let qualityObj = null;
        if (typeof waterQuality === 'string') {
            qualityObj = JSON.parse(waterQuality);
        } else if (typeof waterQuality === 'object') {
            qualityObj = waterQuality;
        }
        
        // Check if well with this ESP ID already exists
        const existingWell = await Well.findOne({
            where: { espId: espId }
        });
        
        if (existingWell) {
            return res.status(409).json({
                status: 'error',
                message: 'Well with this ESP ID already exists'
            });
        }
        
        const well = await Well.create({
            wellName,
            wellLocation: locationObj,
            wellWaterType: wellWaterType || 'Clean',
            espId,
            wellStatus: wellStatus || 'Active',
            wellOwner: wellOwner || req.user.email,
            ownerId: req.user.userId,
            wellCapacity: wellCapacity || 0,
            wellWaterLevel: wellWaterLevel || 0,
            wellWaterConsumption: wellWaterConsumption || 0,
            waterQuality: qualityObj || { ph: 7.0, turbidity: 0.0, tds: 0 },
            extraData: extraData || {},
            lastUpdated: new Date()
        });
        
        res.status(201).json({
            status: 'success',
            message: 'Well created successfully',
            well: mapToShortenedWellData(well)
        });
    } catch (error) {
        console.error('Error creating well:', error);
        res.status(400).json({ 
            status: 'error', 
            message: error.message 
        });
    }
});

// Update well
router.put('/:espId', validateToken, async (req, res) => {
    try {
        const {
            wellName, wellLocation, wellWaterType, wellStatus, wellOwner, wellCapacity, wellWaterLevel, wellWaterConsumption, waterQuality, extraData
        } = req.body;
        
        // Check if well exists
        const well = await Well.findOne({
            where: { espId: req.params.espId }
        });
        
        if (!well) {
            return res.status(404).json({
                status: 'error',
                message: 'Well not found'
            });
        }
        
        // Accept wellLocation as either a string or object
        let locationObj = well.wellLocation;
        if (wellLocation) {
            if (typeof wellLocation === 'string') {
                const [lat, lon] = wellLocation.split(',').map(Number);
                if (!isNaN(lat) && !isNaN(lon)) {
                    locationObj = { latitude: lat, longitude: lon };
                }
            } else if (typeof wellLocation === 'object') {
                locationObj = wellLocation;
            }
        }
        
        // Update well
        const [updated] = await Well.update({
            wellName: wellName || well.wellName,
            wellLocation: locationObj,
            wellWaterType: wellWaterType || well.wellWaterType,
            wellStatus: wellStatus || well.wellStatus,
            wellOwner: wellOwner || well.wellOwner,
            wellCapacity: wellCapacity !== undefined ? wellCapacity : well.wellCapacity,
            wellWaterLevel: wellWaterLevel !== undefined ? wellWaterLevel : well.wellWaterLevel,
            wellWaterConsumption: wellWaterConsumption !== undefined ? wellWaterConsumption : well.wellWaterConsumption,
            waterQuality: waterQuality || well.waterQuality,
            extraData: extraData || well.extraData,
            lastUpdated: new Date()
        }, {
            where: { espId: req.params.espId }
        });
        
        if (!updated) {
            return res.status(404).json({
                status: 'error',
                message: 'Well not found or no changes made'
            });
        }
        
        const updatedWell = await Well.findOne({ 
            where: { espId: req.params.espId } 
        });
        
        res.json({
            status: 'success',
            message: 'Well updated successfully',
            well: mapToShortenedWellData(updatedWell)
        });
    } catch (error) {
        console.error('Error updating well:', error);
        res.status(400).json({ 
            status: 'error', 
            message: error.message 
        });
    }
});

// Delete well
router.delete('/:espId', validateToken, async (req, res) => {
    try {
        const well = await Well.findOne({
            where: { espId: req.params.espId }
        });
        
        if (!well) {
            return res.status(404).json({
                status: 'error',
                message: 'Well not found'
            });
        }
        
        // Check if user is the owner of the well
        if (well.ownerId && well.ownerId !== req.user.userId) {
            return res.status(403).json({
                status: 'error',
                message: 'You are not authorized to delete this well'
            });
        }
        
        const deleted = await Well.destroy({
            where: { espId: req.params.espId }
        });
        
        if (!deleted) {
            return res.status(404).json({
                status: 'error',
                message: 'Well not found or could not be deleted'
            });
        }
        
        res.status(200).json({
            status: 'success',
            message: 'Well deleted successfully'
        });
    } catch (error) {
        console.error('Error deleting well:', error);
        res.status(500).json({ 
            status: 'error', 
            message: error.message 
        });
    }
});

// Get wells by status
router.get('/status/:status', async (req, res) => {
    try {
        const wells = await Well.findAll({
            where: { wellStatus: req.params.status }
        });
        
        res.json({
            status: 'success',
            wells: wells.map(mapToShortenedWellData)
        });
    } catch (error) {
        console.error('Error fetching wells by status:', error);
        res.status(500).json({ 
            status: 'error', 
            message: error.message 
        });
    }
});

// Update well water level
router.patch('/:espId/water-level', async (req, res) => {
    try {
        const { waterLevel } = req.body;
        
        if (waterLevel === undefined) {
            return res.status(400).json({
                status: 'error',
                message: 'Water level is required'
            });
        }
        
        const [updated] = await Well.update({
            wellWaterLevel: waterLevel,
            lastUpdated: new Date()
        }, {
            where: { espId: req.params.espId }
        });
        
        if (!updated) {
            return res.status(404).json({
                status: 'error',
                message: 'Well not found'
            });
        }
        
        const well = await Well.findOne({ 
            where: { espId: req.params.espId } 
        });
        
        res.json({
            status: 'success',
            message: 'Water level updated successfully',
            well: mapToShortenedWellData(well)
        });
    } catch (error) {
        console.error('Error updating water level:', error);
        res.status(400).json({ 
            status: 'error', 
            message: error.message 
        });
    }
});

// Get well stats
router.get('/stats/summary', async (req, res) => {
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

// Update water quality
router.patch('/:espId/water-quality', async (req, res) => {
    try {
        const { waterQuality } = req.body;
        
        if (!waterQuality || typeof waterQuality !== 'object') {
            return res.status(400).json({
                status: 'error',
                message: 'Water quality object is required'
            });
        }
        
        const [updated] = await Well.update({
            waterQuality,
            lastUpdated: new Date()
        }, {
            where: { espId: req.params.espId }
        });
        
        if (!updated) {
            return res.status(404).json({
                status: 'error',
                message: 'Well not found'
            });
        }
        
        const well = await Well.findOne({ 
            where: { espId: req.params.espId } 
        });
        
        res.json({
            status: 'success',
            message: 'Water quality updated successfully',
            well: mapToShortenedWellData(well)
        });
    } catch (error) {
        console.error('Error updating water quality:', error);
        res.status(400).json({ 
            status: 'error', 
            message: error.message 
        });
    }
});

// Get wells in specified radius from coordinates
router.get('/nearby/:latitude/:longitude/:radius', async (req, res) => {
    try {
        const { latitude, longitude, radius } = req.params;
        
        if (!latitude || !longitude || !radius) {
            return res.status(400).json({
                status: 'error',
                message: 'Latitude, longitude, and radius are required'
            });
        }
        
        const lat = parseFloat(latitude);
        const lon = parseFloat(longitude);
        const rad = parseFloat(radius); // Radius in km
        
        // Fetch all wells from the database
        const wells = await Well.findAll();
        
        // Filter wells based on distance using Haversine formula
        const nearbyWells = wells.filter(well => {
            let wellLat = null;
            let wellLon = null;
            
            if (well.wellLocation && typeof well.wellLocation === 'object') {
                wellLat = well.wellLocation.latitude;
                wellLon = well.wellLocation.longitude;
            }
            
            if (wellLat === null || wellLon === null) {
                return false;
            }
            
            // Calculate distance using Haversine formula
            const R = 6371; // Radius of the Earth in km
            const dLat = (wellLat - lat) * Math.PI/180;
            const dLon = (wellLon - lon) * Math.PI/180;
            const a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                    Math.cos(lat * Math.PI/180) * Math.cos(wellLat * Math.PI/180) * 
                    Math.sin(dLon/2) * Math.sin(dLon/2);
            const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
            const distance = R * c;
            
            // Add distance property to well object
            well.distance = parseFloat(distance.toFixed(2));
            
            // Include well if distance is within radius
            return distance <= rad;
        });
        
        // Return nearby wells with distance included
        res.json({
            status: 'success',
            wells: nearbyWells.map(well => ({
                ...mapToShortenedWellData(well),
                distance: well.distance
            }))
        });
    } catch (error) {
        console.error('Error fetching nearby wells:', error);
        res.status(500).json({
            status: 'error',
            message: error.message
        });
    }
});

module.exports = router; 