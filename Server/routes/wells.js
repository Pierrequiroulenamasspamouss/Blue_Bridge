const express = require('express');
const router = express.Router();
const db = require('../models');
const { Well } = db;
const multer = require('multer');
const sharp = require('sharp'); // Make sure to install sharp: npm install sharp
const { Op } = require('sequelize'); // Import Sequelize operators
const { validateToken } = require('../middleware/auth');
const { body, param, query, validationResult } = require('express-validator');
const validator = require('validator');

// Use memory storage for multer
const upload = multer({ storage: multer.memoryStorage() });


function mapToShortenedWellData(well) {
    // Handle wellLocation - ensure it's always an object
    let wellLocation = { latitude: 0.0, longitude: 0.0 };
    if (well.wellLocation) {
        if (typeof well.wellLocation === 'string') {
            try {
                wellLocation = JSON.parse(well.wellLocation);
            } catch (e) {
                console.error('Error parsing wellLocation:', e);
            }
        } else if (typeof well.wellLocation === 'object') {
            wellLocation = well.wellLocation;
        }
    }

    // Add last refresh time
    const lastRefreshTime = well.lastUpdated ? new Date(well.lastUpdated).getTime() : 0;
    
    return {
        status: "success",
        data: {
            wellName: well.wellName || '',
            wellLocation: wellLocation, // Use parsed object
            wellWaterType: well.wellWaterType || well.waterType || '',
            wellId: well.wellId || '',
            wellStatus: well.wellStatus || well.status || 'Unknown',
            wellOwner: well.wellOwner || '',
            wellCapacity: well.wellCapacity != null ? String(well.wellCapacity) : (well.capacity != null ? String(well.capacity) : ''),
            wellWaterLevel: well.wellWaterLevel != null ? String(well.wellWaterLevel) : (well.waterLevel != null ? String(well.waterLevel) : ''),
            wellWaterConsumption: well.wellWaterConsumption != null ? String(well.wellWaterConsumption) : (well.waterConsumption != null ? String(well.waterConsumption) : ''),
            waterQuality: typeof well.waterQuality === 'string' ? JSON.parse(well.waterQuality) : (well.waterQuality || { ph: 7.0, turbidity: 0.0, tds: 0 }),
            lastRefreshTime
        }
    };
}

// Get wells with optional filters
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
            wellId,
            minWaterLevel,
            maxWaterLevel,
            latitude,
            longitude,
            radius = 50
        } = req.query;

        // Build where clause
        const where = {};

        // Use the imported Op operators
        if (email) where.wellOwner = email;
        if (wellName) where.wellName = { [Op.like]: `%${wellName}%` };
        if (wellStatus) where.wellStatus = wellStatus;
        if (wellWaterType) where.wellWaterType = wellWaterType;
        if (wellOwner) where.wellOwner = wellOwner;
        if (wellId) where.wellId = wellId;
        if (minWaterLevel) where.wellWaterLevel = { [Op.gte]: minWaterLevel };
        if (maxWaterLevel) where.wellWaterLevel = { [Op.lte]: maxWaterLevel };

        // If coordinates are provided, find wells within radius
        if (latitude && longitude) {
            const lat = parseFloat(latitude);
            const lon = parseFloat(longitude);
            const rad = parseFloat(radius);

            where[Op.and] = [
                db.sequelize.literal(`
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

        // Get wells with pagination
        const wells = await Well.findAll({
            where,
            limit: parseInt(limit),
            offset: (page - 1) * limit,
            order: [['wellName', 'ASC']]
        });

        res.json({
            status: 'success',
            data: wells.map(well => mapToShortenedWellData(well).data),
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
router.post('/', [
    body('wellName').isString().trim().escape(),
    body('wellId').isString().trim().escape(),
    body('wellLocation').optional().custom(value => {
        if (typeof value === 'string') {
            const [lat, lon] = value.split(',').map(Number);
            if (isNaN(lat) || isNaN(lon)) throw new Error('Invalid coordinates');
        } else if (typeof value === 'object') {
            if (typeof value.latitude !== 'number' || typeof value.longitude !== 'number') throw new Error('Invalid coordinates');
        }
        return true;
    }),
    body('wellWaterType').optional().isString().trim().escape(),
    body('wellStatus').optional().isString().trim().escape(),
    body('wellOwner').optional().isString().trim().escape(),
    body('wellCapacity').optional().isNumeric(),
    body('wellWaterLevel').optional().isNumeric(),
    body('wellWaterConsumption').optional().isNumeric(),
    body('waterQuality').optional().custom(value => {
        if (typeof value === 'string') {
            try { JSON.parse(value); } catch { throw new Error('Invalid JSON'); }
        } else if (typeof value !== 'object') {
            throw new Error('Invalid waterQuality');
        }
        return true;
    }),
    body('extraData').optional().custom(value => {
        if (typeof value === 'string') {
            try { JSON.parse(value); } catch { throw new Error('Invalid JSON'); }
        }
        return true;
    })
], async (req, res) => {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
        return res.status(400).json({ status: 'error', errors: errors.array() });
    }
    try {
        const {
            wellName, wellLocation, wellWaterType, wellId, wellStatus, wellOwner, wellCapacity, wellWaterLevel, wellWaterConsumption, waterQuality, extraData
        } = req.body;
        
        // Check for required fields
        if (!wellName || !wellId) {
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
            where: { wellId: wellId }
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
            wellId,
            wellStatus: wellStatus || 'Active',
            wellOwner: wellOwner || req.user.email,
            ownerId: req.user.userId,
            wellCapacity: wellCapacity || 0,
            wellWaterLevel: wellWaterLevel || 0,
            wellWaterConsumption: wellWaterConsumption || 0,
            waterQuality: qualityObj || { ph: 7.0, turbidity: 0.0, tds: 0 },
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
router.get('/:wellId/details', async (req, res) => {
    try {
        // Find well without including the User association
        const well = await Well.findOne({
            where: { wellId: req.params.wellId }
        });
        
        if (!well) {
            const normalResponse = {
                status : "error",
                response : "no well found with this name"
            }
            return res.json(normalResponse);
        }

        // Parse wellImages and return only the count
        let images = [];
        if (well.wellImages) {
            try {
                images = typeof well.wellImages === 'string' ? JSON.parse(well.wellImages) : well.wellImages;
            } catch (e) {
                images = [];
            }
        }
        // Remove base64 data from images for details endpoint
        const imageCount = Array.isArray(images) ? images.filter(img => !!img).length : 0;

        // Prepare well data without base64 image data
        const wellData = {
            wellId: well.wellId,
            wellName: well.wellName,
            wellOwner: well.wellOwner,
            wellLocation: typeof well.wellLocation === 'string' ? well.wellLocation : JSON.stringify(well.wellLocation),
            wellWaterType: well.wellWaterType,
            wellCapacity: well.wellCapacity != null ? parseInt(well.wellCapacity) : 0, //TODO() TEMPORARY FIX, UPDATE ON THE APP LATER TO HANDLE FLOATS
            wellWaterLevel: well.wellWaterLevel != null ? parseInt(well.wellWaterLevel) : 0, //TODO() TEMPORARY FIX, UPDATE ON THE APP LATER TO HANDLE FLOATS
            wellWaterConsumption: well.wellWaterConsumption != null ? parseInt(well.wellWaterConsumption) : 0, //TODO() TEMPORARY FIX, UPDATE ON THE APP LATER TO HANDLE FLOATS
            wellStatus: well.wellStatus,
            waterQuality: typeof well.waterQuality === 'string' ? well.waterQuality : JSON.stringify(well.waterQuality),
            createdAt:well.createdAt,
            updatedAt: well.updatedAt,
            imageCount: imageCount
        };
        res.json({ status: 'success', data: wellData });
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
router.put('/:wellId/update', validateToken, async (req, res) => {
    try {
        const {
            wellName, wellLocation, wellWaterType, wellStatus, wellOwner, wellCapacity, wellWaterLevel, wellWaterConsumption, waterQuality, extraData
        } = req.body;
        
        // Check if well exists
        const well = await Well.findOne({
            where: { wellId: req.params.wellId }
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
            where: { wellId: req.params.wellId }
        });
        
        if (!updated) {
            return res.status(404).json({
                status: 'error',
                message: 'Well not found or no changes made'
            });
        }
        
        const updatedWell = await Well.findOne({ 
            where: { wellId: req.params.wellId }
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
router.put('/:wellId/edit', validateToken, async (req, res) => {
    try {
        const {
            wellName, wellLocation, wellWaterType, wellStatus, wellOwner, wellCapacity, wellWaterLevel, wellWaterConsumption, waterQuality, extraData
        } = req.body;
        
        // Check if well exists
        const well = await Well.findOne({
            where: { wellId: req.params.wellId }
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
            where: { wellId: req.params.wellId }
        });
        
        if (!updated) {
            return res.status(404).json({
                status: 'error',
                message: 'Well not found or no changes made'
            });
        }
        
        const updatedWell = await Well.findOne({ 
            where: { wellId: req.params.wellId }
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
router.delete('/:wellId', validateToken, async (req, res) => {
    try {
        const well = await Well.findOne({
            where: { wellId: req.params.wellId }
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
            where: { wellId: req.params.wellId }
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

// Get the stats of all wells,how many are active, how many inactive, etc...
router.get('/stats', async (req, res) => {
    try {
        const totalWells = await Well.count();
        const activeWells = await Well.count({ where: { wellStatus: 'Active' } });
        const inactiveWells = await Well.count({ where: { wellStatus: 'Inactive' } });
        const unknownStatusWells = await Well.count({ where: { wellStatus: { [Op.notIn]: ['Active', 'Inactive'] } } });

        // You can add more stats as needed, e.g., by water type
        const statsByWaterType = await Well.findAll({
            attributes: ['wellWaterType', [db.sequelize.fn('COUNT', db.sequelize.col('wellWaterType')), 'count']],
            group: ['wellWaterType']
        });

        res.json({
            status: 'success',
            data: {
                totalWells,
                activeWells,
                inactiveWells,
                unknownStatusWells,
                statsByWaterType
            }
        });
    } catch (error) {
        console.error('Error fetching well stats:', error);
        res.status(500).json({ status: 'error', message: 'Error fetching well stats: ' + error.message });
    }
});

// Update well water level - No authentication required
router.patch('/:wellId/water-level', async (req, res) => {
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
            where: { wellId: req.params.wellId }
        });
        
        if (!updated) {
            return res.status(404).json({
                status: 'error',
                message: 'Well not found'
            });
        }
        
        const well = await Well.findOne({ 
            where: { wellId: req.params.wellId }
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

// Get well by  well ID - No authentication required
router.get('/:wellId', async (req, res) => {
    try {
        const well = await Well.findOne({
            where: { wellId: req.params.wellId  }
        });
        
        if (!well) {
            const normalResponse = {
                status : "error",
                response : "no well found"
            }
            return res.json(normalResponse);
            
        }
        res.json(mapToShortenedWellData(well));
    } catch (error) {
        console.error('Error fetching well:', error);
        res.status(500).json({
            status: 'error',
            data: {
                message: error.message
            }
        });
    }
});

// Upload image for a well
router.post('/:wellId/images/:imageNumber/upload', [
    param('wellId').isString().trim().escape(),
    param('imageNumber').isInt({ min: 0, max: 9 }),
    body('description').optional().isString().trim().escape()
], upload.single('image'), async (req, res) => {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
        return res.status(400).json({ status: 'error', errors: errors.array() });
    }

    try {
        const { wellId, imageNumber } = req.params;
        const { description } = req.body;

        if (!req.file) {
            return res.status(400).json({
                status: 'error',
                message: 'No image file provided'
            });
        }

        // Validate image size (e.g., max 5MB)
        if (req.file.size > 5 * 1024 * 1024) {
            return res.status(400).json({
                status: 'error',
                message: 'Image too large (max 5MB)'
            });
        }

        // Compress and resize image
        let compressedBuffer;
        try {
            compressedBuffer = await sharp(req.file.buffer)
                .resize(256, 256, { fit: 'inside' })
                .jpeg({ quality: 80 })
                .toBuffer();
        } catch (e) {
            return res.status(400).json({
                status: 'error',
                message: 'Invalid image file'
            });
        }

        const base64Data = compressedBuffer.toString('base64');

        // Get well from database
        const well = await db.Well.findOne({ where: { wellId: wellId } });
        if (!well) {
            return res.status(404).json({
                status: 'error',
                message: 'Well not found'
            });
        }

        // Initialize images array if null
        let images = well.wellImages || [];
        if (typeof images === 'string') {
            try {
                images = JSON.parse(images);
            } catch (e) {
                images = [];
            }
        }

        const imageIndex = parseInt(imageNumber);

        // Create image data
        const imageData = {
            imageNumber: imageIndex,
            description: description || `Image ${imageIndex}`,
            data: base64Data,
            uploadDate: new Date().toISOString(),
            fileSize: compressedBuffer.length
        };

        // Ensure array is large enough
        while (images.length <= imageIndex) {
            images.push(null);
        }
        images[imageIndex] = imageData;

        // Update well
        await well.update({ wellImages: images });

        res.json({
            status: 'success',
            message: 'Image uploaded successfully',
            data: { imageNumber: imageIndex, description: imageData.description }
        });
    } catch (error) {
        console.error('Error uploading image:', error);
        res.status(500).json({
            status: 'error',
            message: 'Error uploading image: ' + error.message
        });
    }
});

// Get image for a well (return base64 data)
router.get('/:wellId/images/:imageNumber', async (req, res) => {
    try {
        const { wellId, imageNumber } = req.params;
        console.log(`Fetching image ${imageNumber} for well ${wellId}`);
        
        // Find the well
        const well = await db.Well.findOne({ where: { wellId } });
        if (!well) {
            console.log('Well not found');
            return res.status(404).json({ 
                status: 'error', 
                data: {},
                message: 'Well not found'
            });
        }

        console.log('Found well:', well.wellId);

        // Parse images JSON robustly
        let images = [];
        if (well.wellImages) {
            try {
                // First parse the outer JSON string
                let parsed = typeof well.wellImages === 'string' ? 
                    JSON.parse(well.wellImages) : 
                    well.wellImages;
                
                // If the result is still a string (double-encoded), parse again
                if (typeof parsed === 'string') {
                    parsed = JSON.parse(parsed);
                }
                
                // Ensure we have an array
                images = Array.isArray(parsed) ? parsed : [];

            } catch (e) {
                console.error('Error parsing wellImages:', e);
                return res.status(500).json({ 
                    status: 'error', 
                    data: {},
                    message: 'Error parsing image data'
                });
            }
        }

        // Convert imageNumber to integer
        const imgIndex = parseInt(imageNumber);
        console.log(`Looking for image number: ${imgIndex}`);
        
        // Find the image by imageNumber property
        const foundImage = images.find(img => img && img.imageNumber === imgIndex);
        
        if (!foundImage) {
            console.log('Image not found. Available images:', images.map(img => img.imageNumber));
            return res.status(404).json({ 
                status: 'error', 
                data: {},
                message: 'Image not found'
            });
        }

        console.log('Found image:', foundImage.imageNumber);
        
        // Return the image data in the expected format
        res.json({
            status: 'success',
            data: {
                description: foundImage.description,
                uploadDate: foundImage.uploadDate,
                fileSize: foundImage.fileSize,
                base64encodedImage: foundImage.data // This matches your frontend model
            }
        });
    } catch (error) {
        console.error('Error fetching image:', error);
        res.status(500).json({ 
            status: 'error', 
            data: {},
            message: 'Internal server error'
        });
    }
});

// Delete image from a well
router.delete('/:wellId/images/:imageNumber', [
    //validateToken,
    param('wellId').isString().trim().escape(),
    param('imageNumber').isInt({ min: 0, max: 9 })
], async (req, res) => {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
        return res.status(400).json({ status: 'error', errors: errors.array() });
    }

    try {
        const { wellId, imageNumber } = req.params;

        // Get well from database
        const well = await db.Well.findOne({ where: { wellId: wellId } });
        if (!well) {
            return res.status(404).json({
                status: 'error',
                message: 'Well not found'
            });
        }

        // Parse existing images
        let images = [];
        if (well.wellImages) {
            try {
                images = JSON.parse(well.wellImages);
            } catch (e) {
                console.error('Error parsing well images:', e);
            }
        }

        const imageIndex = parseInt(imageNumber);
        if (imageIndex < 0 || imageIndex >= images.length || !images[imageIndex]) {
            return res.status(404).json({
                status: 'error',
                message: 'Image not found'
            });
        }

        // Remove image from array
        images.splice(imageIndex, 1);

        // Renumber remaining images
        images = images.map((img, index) => {
            if (img) {
                img.imageNumber = index;
            }
            return img;
        });

        // Update well with new images data
        await well.update({ wellImages: JSON.stringify(images) });

        res.json({
            status: 'success',
            message: 'Image deleted successfully'
        });
    } catch (error) {
        console.error('Error deleting image:', error);
        res.status(500).json({
            status: 'error',
            message: 'Error deleting image: ' + error.message
        });
    }
});



module.exports = router; 