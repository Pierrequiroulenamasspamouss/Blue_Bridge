const express = require('express');
const router = express.Router();
const db = require('../models');
const { Well } = db;
const multer = require('multer');
const fs = require('fs');
const path = require('path');
const { Op } = require('sequelize'); // Import Sequelize operators
const { validateToken } = require('../middleware/auth');
const { body, param, query, validationResult } = require('express-validator');
const validator = require('validator');

// Configure multer for image upload
const storage = multer.diskStorage({
    destination: function (req, file, cb) {
        const uploadDir = path.join(__dirname, '../uploads/wells');
        if (!fs.existsSync(uploadDir)) {
            fs.mkdirSync(uploadDir, { recursive: true });
        }
        cb(null, uploadDir);
    },
    filename: function (req, file, cb) {
        const wellId = req.params.wellId;
        const imageNumber = req.params.imageNumber;
        const ext = path.extname(file.originalname);
        cb(null, `well_${wellId}_image_${imageNumber}${ext}`);
    }
});

const upload = multer({
  storage: multer.diskStorage({
    destination: (req, file, cb) => {
      const uploadPath = path.join(__dirname, '../uploads/wells');
      if (!fs.existsSync(uploadPath)) {
        fs.mkdirSync(uploadPath, { recursive: true });
      }
      cb(null, uploadPath);
    },
    filename: (req, file, cb) => {
      const { wellId, imageNumber } = req.params;
      cb(null, `well_${wellId}_image_${imageNumber}${path.extname(file.originalname)}`);
    }
  }),
  fileFilter: (req, file, cb) => {
    // More permissive check for testing
    const allowedTypes = ['image/jpeg', 'image/png', 'image/gif'];
    if (allowedTypes.includes(file.mimetype)) {
      cb(null, true);
    } else {
      console.log('Rejected file:', file); // Log rejected files
      cb(new Error(`Invalid file type. Only ${allowedTypes.join(', ')} are allowed.`), false);
    }
  }
});



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
        status: "success",
        data: {
            wellName: well.wellName || '',
            wellLocation: well.wellLocation || { latitude: 0.0, longitude: 0.0 },
            wellWaterType: well.wellWaterType || well.waterType || '',
            wellId: well.wellId || '',
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
            where: { [Op.or]: [{ wellId: req.params.wellId }, { id: req.params.wellId }] }
        });
        if (!well) {
            // If well not found in database, create a mock well for demo purposes
            const mockWell = {
                wellId: req.params.wellId,
                wellName: `Well ${req.params.wellId}`,
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
            data: {
                message: error.message
            }
        });
    }
});

// Upload image for a well
router.post('/:wellId/images/:imageNumber/upload', [
    //validateToken,
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

        // Create or update image data
        const imageData = {
            imageNumber: imageIndex,
            description: description || `Image ${imageIndex}`,
            filename: req.file.filename,
            uploadDate: new Date().toISOString(),
            fileSize: req.file.size
        };

        // Update or add image
        if (imageIndex < images.length) {
            images[imageIndex] = imageData;
        } else {
            // Add new image
            while (images.length <= imageIndex) {
                images.push(null);
            }
            images[imageIndex] = imageData;
        }

        // Update well with new images data
        await well.update({ wellImages: JSON.stringify(images) });

        res.json({
            status: 'success',
            message: 'Image uploaded successfully',
            data: imageData
        });
    } catch (error) {
        console.error('Error uploading image:', error);
        res.status(500).json({
            status: 'error',
            message: 'Error uploading image: ' + error.message
        });
    }
});

router.get('/:wellId/images/:imageNumber', async (req, res) => {
    try {
        const { wellId, imageNumber } = req.params;

        // Validate parameters
        if (!validator.isAlphanumeric(wellId) || !validator.isInt(imageNumber, { min: 0, max: 9 })) {
            return res.status(400).json({
                status: 'error',
                message: 'Invalid well ID or image number format.'
            });
        }

        // Find the well
        const well = await db.Well.findOne({ where: { wellId } });
        if (!well) {
            return res.status(404).json({ status: 'error', message: 'Well not found' });
        }

        // Parse images JSON
        let images = [];
        if (well.wellImages) {
            try {
                images = JSON.parse(well.wellImages);
            } catch (e) {
                return res.status(500).json({ status: 'error', message: 'Error parsing image data' });
            }
        }

        const imgIndex = parseInt(imageNumber);
        if (imgIndex >= images.length || !images[imgIndex]) {
            return res.status(404).json({ status: 'error', message: 'Image not found' });
        }

        const image = images[imgIndex];
        const imagePath = path.join(__dirname, '../uploads/wells', image.filename);

        res.sendFile(imagePath);
    } catch (error) {
        console.error('Error fetching image:', error);
        res.status(500).json({ status: 'error', message: 'Server error: ' + error.message });
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

        // Delete image file
        const imagePath = path.join(__dirname, '../uploads/wells', `well_${wellId}_image_${imageNumber}.jpg`);
        if (fs.existsSync(imagePath)) {
            fs.unlinkSync(imagePath);
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