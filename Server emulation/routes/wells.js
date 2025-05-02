const express = require('express');
const router = express.Router();
const db = require('../models');
const { Well, User } = db;

function mapToShortenedWellData(well) {
    // Extract latitude/longitude from wellLocation JSON if present
    let latitude = '';
    let longitude = '';
    if (well.wellLocation && typeof well.wellLocation === 'object') {
        latitude = well.wellLocation.latitude != null ? String(well.wellLocation.latitude) : '';
        longitude = well.wellLocation.longitude != null ? String(well.wellLocation.longitude) : '';
    }
    return {
        wellName: well.wellName || '',
        wellLocation: well.wellLocation || { latitude: 0.0, longitude: 0.0 },
        wellWaterType: well.wellWaterType || well.waterType || '',
        espId: well.espId || '',
        wellStatus: well.wellStatus || well.status || 'Unknown',
        wellOwner: well.wellOwner || '',
        wellCapacity: well.wellCapacity != null ? String(well.wellCapacity) : (well.capacity != null ? String(well.capacity) : ''),
        wellWaterLevel: well.wellWaterLevel != null ? String(well.wellWaterLevel) : (well.waterLevel != null ? String(well.waterLevel) : ''),
        wellWaterConsumption: well.wellWaterConsumption != null ? String(well.wellWaterConsumption) : (well.waterConsumption != null ? String(well.waterConsumption) : '')
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
        res.status(500).json({ error: error.message });
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
            return res.status(404).json({ error: 'Well not found' });
        }
        res.json(mapToShortenedWellData(well));
    } catch (error) {
        console.error('Error fetching well:', error);
        res.status(500).json({ error: error.message });
    }
});

// Create new well
router.post('/', async (req, res) => {
    try {
        const {
            wellName, wellLocation, wellWaterType, espId, wellStatus, wellOwner, wellCapacity, wellWaterLevel, wellWaterConsumption, waterQuality, extraData
        } = req.body;
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
        const well = await Well.create({
            wellName,
            wellLocation: locationObj,
            wellWaterType,
            espId,
            wellStatus,
            wellOwner,
            wellCapacity,
            wellWaterLevel,
            wellWaterConsumption,
            waterQuality: qualityObj,
            extraData
        });
        res.status(201).json(mapToShortenedWellData(well));
    } catch (error) {
        console.error('Error creating well:', error);
        res.status(400).json({ error: error.message });
    }
});

// Update well
router.put('/:espId', async (req, res) => {
    try {
        const {
            wellName, wellLocation, wellWaterType, wellStatus, wellOwner, wellCapacity, wellWaterLevel, wellWaterConsumption, waterQuality, extraData
        } = req.body;
        let locationObj = null;
        if (typeof wellLocation === 'string') {
            const [lat, lon] = wellLocation.split(',').map(Number);
            if (!isNaN(lat) && !isNaN(lon)) {
                locationObj = { latitude: lat, longitude: lon };
            }
        } else if (typeof wellLocation === 'object') {
            locationObj = wellLocation;
        }
        const [updated] = await Well.update({
            wellName,
            wellLocation: locationObj,
            wellWaterType,
            wellStatus,
            wellOwner,
            wellCapacity,
            wellWaterLevel,
            wellWaterConsumption,
            waterQuality,
            extraData
        }, {
            where: { espId: req.params.espId }
        });
        if (!updated) {
            return res.status(404).json({ error: 'Well not found' });
        }
        const well = await Well.findOne({ where: { espId: req.params.espId } });
        res.json(mapToShortenedWellData(well));
    } catch (error) {
        console.error('Error updating well:', error);
        res.status(400).json({ error: error.message });
    }
});

// Delete well
router.delete('/:espId', async (req, res) => {
    try {
        const deleted = await Well.destroy({
            where: { espId: req.params.espId }
        });
        if (!deleted) {
            return res.status(404).json({ error: 'Well not found' });
        }
        res.status(204).send();
    } catch (error) {
        console.error('Error deleting well:', error);
        res.status(500).json({ error: error.message });
    }
});

// Get wells by status
router.get('/status/:status', async (req, res) => {
    try {
        const wells = await Well.findAll({
            where: { wellStatus: req.params.status }
        });
        res.json(wells.map(mapToShortenedWellData));
    } catch (error) {
        console.error('Error fetching wells by status:', error);
        res.status(500).json({ error: error.message });
    }
});

// Update well water level
router.patch('/:espId/water-level', async (req, res) => {
    try {
        const { waterLevel } = req.body;
        const [updated] = await Well.update({
            wellWaterLevel: waterLevel,
            lastUpdated: new Date()
        }, {
            where: { espId: req.params.espId }
        });
        if (!updated) {
            return res.status(404).json({ error: 'Well not found' });
        }
        const well = await Well.findOne({ where: { espId: req.params.espId } });
        res.json(mapToShortenedWellData(well));
    } catch (error) {
        console.error('Error updating water level:', error);
        res.status(400).json({ error: error.message });
    }
});

// Stats endpoint
router.get('/stats', async (req, res) => {
    try {
        const count = await Well.count();
        const avgCapacity = await Well.aggregate('wellCapacity', 'avg', { plain: false });
        res.json({ count, avgCapacity: avgCapacity[0] ? avgCapacity[0].avg : 0 });
    } catch (error) {
        console.error('Error fetching stats:', error);
        res.status(500).json({ error: error.message });
    }
});

module.exports = router; 