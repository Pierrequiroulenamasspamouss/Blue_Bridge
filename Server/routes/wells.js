const express = require('express');
const router = express.Router();
const db = require('../models');
const { Well, User } = db;
const { validateToken } = require('../middleware/auth');
const { getWeatherData } = require('../services/weatherService');

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
        status : "success",
        data : {
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
        }
    };
}

// Get wells with optional filters - No authentication required
router.get('/', async (req, res) => {
    try {
        const { 
            page = 1, 
            limit = 20, 
            email,
            wellName, 
            wellStatus, 
            wellWaterType, 
            wellOwner, 
            espId,
            minWaterLevel,
            maxWaterLevel,
            latitude,
            longitude,
            radius = 50 // Default radius in kilometers
        } = req.query;

        // Build where clause
        const where = {};
        if (email) where.wellOwner = email;
        if (wellName) where.wellName = { [db.Sequelize.Op.like]: `%${wellName}%` };
        if (wellStatus) where.wellStatus = wellStatus;
        if (wellWaterType) where.wellWaterType = wellWaterType;
        if (wellOwner) where.wellOwner = wellOwner;
        if (espId) where.espId = espId;
        if (minWaterLevel) where.wellWaterLevel = { [db.Sequelize.Op.gte]: minWaterLevel };
        if (maxWaterLevel) where.wellWaterLevel = { [db.Sequelize.Op.lte]: maxWaterLevel };

        // If coordinates are provided, find wells within radius
        if (latitude && longitude) {
            const lat = parseFloat(latitude);
            const lon = parseFloat(longitude);
            const rad = parseFloat(radius);

            // Calculate distance using Haversine formula
            where[db.Sequelize.Op.and] = [
                db.Sequelize.literal(`
                    (6371 * acos(
                        cos(radians(${lat})) * 
                        cos(radians(JSON_EXTRACT(wellLocation, '$.latitude'))) * 
                        cos(radians(JSON_EXTRACT(wellLocation, '$.longitude')) - radians(${lon})) + 
                        sin(radians(${lat})) * 
                        sin(radians(JSON_EXTRACT(wellLocation, '$.latitude')))
                    )) <= ${rad}
                `)
            ];
        }

        // Get total count for pagination
        const total = await Well.count({ where });

        // Get wells with pagination, sort by wellName alphabetically by default
        const wells = await Well.findAll({
            where,
            limit: parseInt(limit),
            offset: (page - 1) * limit,
            order: [['wellName', 'ASC']]  // Sort alphabetically by well name
        });

        // Map wells to shortened format
        const mappedWells = wells.map(well => mapToShortenedWellData(well).data);

        res.json({
            status: 'success',
            data: mappedWells,
            pagination: {
                total,
                page: parseInt(page),
                limit: parseInt(limit),
                pages: Math.ceil(total / limit)
            }
        });
    } catch (error) {
        console.error('Error fetching wells:', error);
        res.status(500).json({
            status: 'error',
            message: 'Error fetching wells: ' + error.message
        });
    }
});



// Create new well - Authentication required
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
            try {
                qualityObj = JSON.parse(waterQuality);
            } catch (e) {
                return res.status(400).json({ 
                    status: 'error',
                    message: 'Invalid waterQuality JSON' 
                });
            }
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

// Get well by ESP ID - No authentication required (without User association)
router.get('/:espId/details', async (req, res) => {
    try {
        // Find well without including the User association
        const well = await Well.findOne({
            where: { espId: req.params.espId }
        });
        
        if (!well) {

            const normalResponse = {
                status : "error",
                response : "no well found with this name"
            }
            return res.json(normalResponse);
            //return res.json(mockWell);
        }
        
        res.json(mapToShortenedWellData(well));
    } catch (error) {
        console.error('Error fetching well:', error);
        res.status(500).json({ 
            status: 'error', 
            data : {
            message: error.message 
            }
        });
    }
});

// Update well via /update path - Authentication required
router.put('/:espId/update', validateToken, async (req, res) => {
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
        console.error('Error updating well via update path:', error);
        res.status(400).json({ 
            status: 'error', 
            message: error.message 
        });
    }
});

// Update well via /edit path - Authentication required
router.put('/:espId/edit', validateToken, async (req, res) => {
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
        console.error('Error updating well via edit path:', error);
        res.status(400).json({ 
            status: 'error', 
            message: error.message 
        });
    }
});


// Delete well - Authentication required
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
        
        // Remove owner check to allow any authenticated user to delete wells
        // Any user with valid token can now delete wells
        
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

// Get wells by status - No authentication required
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

// Update well water level - No authentication required
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

// Update water quality - No authentication required
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

// Get wells in specified radius from coordinates - No authentication required
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

// Fallback route for the original /:espId path - redirects to /:espId/details
router.get('/:espId', async (req, res) => {
    try {
        // Just use the same implementation as /:espId/details but without the User association
        const well = await Well.findOne({
            where: { espId: req.params.espId }
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
            const normalResponse = {
                status : "error",
                response : "no well found"
            }
            return res.json(normalResponse);
            //return res.json(status: "success", data: mapToShortenedWellData(mockWell));
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

router.get('/:wellId', async (req, res) => {
    try {
        const well = await Well.findOne({
            where: { id: req.params.wellId }
        });

        if (!well) {
            return res.status(404).json({
                status: 'error',
                message: 'There is no well associated with this name'
            });
        }

        res.json(well);
    } catch (error) {
        console.error('Error fetching well:', error);
        res.status(500).json({
            status: 'error',
            message: 'Error fetching well data'
        });
    }
});

module.exports = router; 