const express = require('express');
const router = express.Router();
const db = require('../models');
const { User } = db;

// --- Nearby Users ---
router.get('/nearby-users', async (req, res) => {
    try {
        const { latitude, longitude, radius } = req.query;
        if (!latitude || !longitude || !radius) {
            return res.status(400).json({ error: 'Missing required query params' });
        }
        const lat = parseFloat(latitude);
        const lon = parseFloat(longitude);
        const rad = parseFloat(radius);
        // Simple filter: users within radius (in degrees, not km)
        const users = await User.findAll();
        const nearby = users.filter(u => {
            let userLat = u.latitude;
            let userLon = u.longitude;
            // If not present as flat fields, try to extract from location JSON
            if ((userLat == null || userLon == null) && u.location && typeof u.location === 'object') {
                userLat = u.location.latitude;
                userLon = u.location.longitude;
            }
            if (userLat == null || userLon == null) return false;
            const dLat = userLat - lat;
            const dLon = userLon - lon;
            return Math.sqrt(dLat * dLat + dLon * dLon) <= rad / 111; // rough conversion
        });
        // Always return latitude and longitude for each user if available
        const result = nearby.map(u => {
            let userLat = u.latitude;
            let userLon = u.longitude;
            if ((userLat == null || userLon == null) && u.location && typeof u.location === 'object') {
                userLat = u.location.latitude;
                userLon = u.location.longitude;
            }
            return {
                username: u.username,
                email: u.email,
                latitude: userLat,
                longitude: userLon
            };
        });
        res.json({ status: 'success', users: result });
    } catch (error) {
        console.error('Error fetching nearby users:', error);
        res.status(500).json({ error: error.message });
    }
});

module.exports = router; 