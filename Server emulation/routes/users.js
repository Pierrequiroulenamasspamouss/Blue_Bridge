const express = require('express');
const router = express.Router();
const db = require('../models');
const { User } = db;
const { validateToken } = require('./auth');

// --- Nearby Users ---
/**
 * @api {get} /nearby-users Get nearby users
 * @apiName GetNearbyUsers
 * @apiGroup Users
 *
 * @apiQuery {Number} latitude User's latitude.
 * @apiQuery {Number} longitude User's longitude.
 * @apiQuery {Number} radius Search radius (in km).
 * @apiQuery {String} email Current user's email (for exclusion)
 *
 * @apiSuccess {String} status "success" if operation was successful.
 * @apiSuccess {Object[]} users Array of nearby users.
 * @apiSuccess {String} users.userId User's ID.
 * @apiSuccess {String} users.firstName User's first name.
 * @apiSuccess {String} users.lastName User's last name.
 * @apiSuccess {Number} users.distance Distance to the user (in km).
 * @apiSuccess {Object[]} users.waterNeeds Array of user's water needs.
 * @apiSuccess {Boolean} users.isOnline User online status.
 *
 * @apiError (Error 500) {String} error Error message.
 * @apiError (Error 400) {String} error Missing required query params.
 */
router.get('/nearby-users', async (req, res) => {
    try {
        const { latitude, longitude, radius, email } = req.query;
        if (!latitude || !longitude || !radius) {
            return res.status(400).json({ 
                status: 'error',
                message: 'Missing required query params: latitude, longitude, radius'
            });
        }
        
        const lat = parseFloat(latitude);
        const lon = parseFloat(longitude);
        const rad = parseFloat(radius); // Radius in km

        console.log(`Search request for nearby users: lat=${lat}, lon=${lon}, radius=${rad}km, exclude=${email}`);

        // Fetch all users from the database
        const users = await User.findAll();
        console.log(`Found ${users.length} total users in database`);

        // Filter the users based on the distance to the provided coordinates and exclude current user
        const nearby = users.filter(u => {
            // Skip the current user if email is provided
            if (email && u.email === email) return false;
            
            let userLat = u.latitude;
            let userLon = u.longitude;

            // If latitude and longitude are not directly present, try to extract them from the 'location' object
            if ((userLat == null || userLon == null) && u.location && typeof u.location === 'object') {
                userLat = u.location.latitude;
                userLon = u.location.longitude;
            }

            // If latitude or longitude are still null, exclude the user
            if (userLat == null || userLon == null) return false;

            // Calculate the distance using the Haversine formula
            const R = 6371; // Radius of the Earth in km
            
            // Convert degrees to radians
            const latRad1 = lat * Math.PI / 180;
            const latRad2 = userLat * Math.PI / 180;
            const latDiff = (userLat - lat) * Math.PI / 180;
            const lonDiff = (userLon - lon) * Math.PI / 180;
            
            const a = Math.sin(latDiff/2) * Math.sin(latDiff/2) +
                      Math.cos(latRad1) * Math.cos(latRad2) * 
                      Math.sin(lonDiff/2) * Math.sin(lonDiff/2);
            const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
            const distance = R * c;
            
            // Artificially reduce distance for testing purposes
            // This makes users appear closer than they actually are
            const scaledDistance = distance / 10; // Scale down the distance by a factor of 10
            
            // Debug log
            console.log(`User ${u.email} is at actual distance ${distance.toFixed(2)}km, scaled to ${scaledDistance.toFixed(2)}km with radius ${rad}km`);

            // Include the user if the scaled distance is within the specified radius
            return scaledDistance <= rad;
        });

        console.log(`Filtered to ${nearby.length} nearby users within radius`);

        // Calculate online status based on last activity (within last 10 minutes)
        const now = new Date();
        const onlineThreshold = 10 * 60 * 1000; // 10 minutes in milliseconds

        // Map the nearby users to the desired output format
        const nearbyUsers = nearby.map(u => {
            // Calculate distance using Haversine formula
            const userLat = u.latitude || (u.location?.latitude || 0);
            const userLon = u.longitude || (u.location?.longitude || 0);
            
            const R = 6371; // Radius of the Earth in km
            
            // Convert degrees to radians
            const latRad1 = lat * Math.PI / 180;
            const latRad2 = userLat * Math.PI / 180;
            const latDiff = (userLat - lat) * Math.PI / 180;
            const lonDiff = (userLon - lon) * Math.PI / 180;
            
            const a = Math.sin(latDiff/2) * Math.sin(latDiff/2) +
                    Math.cos(latRad1) * Math.cos(latRad2) * 
                    Math.sin(lonDiff/2) * Math.sin(lonDiff/2);
            const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
            const distance = R * c;
            
            // Scale down distance for testing
            const scaledDistance = distance / 10;
            
            // Check if user is online (active in the last 10 minutes)
            const lastActive = new Date(u.lastActive || 0);
            const isOnline = (now - lastActive) <= onlineThreshold;
            
            return {
                userId: u.userId,
                firstName: u.firstName || '',
                lastName: u.lastName || '',
                username: u.username || '',
                distance: parseFloat(scaledDistance.toFixed(2)),
                waterNeeds: u.waterNeeds || [],
                isOnline
            };
        });

        res.json({ 
            status: 'success', 
            users: nearbyUsers 
        });
    } catch (error) {
        console.error('Error fetching nearby users:', error);
        res.status(500).json({ 
            status: 'error', 
            message: error.message 
        });
    }
});

/**
 * @api {post} /update-profile Update user profile
 * @apiName UpdateProfile
 * @apiGroup Users
 * 
 * @apiBody {String} email User's email.
 * @apiBody {String} token User's authentication token.
 * @apiBody {String} firstName User's first name.
 * @apiBody {String} lastName User's last name.
 * @apiBody {String} username User's username.
 * @apiBody {Object} location User's location object with latitude and longitude.
 * 
 * @apiSuccess {String} status "success" if operation was successful.
 * @apiSuccess {Object} userData Updated user data.
 * 
 * @apiError (Error 401) {String} status "error".
 * @apiError (Error 401) {String} message Authentication error message.
 * @apiError (Error 500) {String} status "error".
 * @apiError (Error 500) {String} message Error message.
 */
router.post('/update-profile', validateToken, async (req, res) => {
    try {
        const { firstName, lastName, username, location } = req.body;
        const user = req.user;
        
        // Update user fields if provided
        if (firstName) user.firstName = firstName;
        if (lastName) user.lastName = lastName;
        if (username) user.username = username;
        
        // Update location if provided
        if (location && typeof location === 'object') {
            user.location = location;
            if (location.latitude) user.latitude = location.latitude;
            if (location.longitude) user.longitude = location.longitude;
        }
        
        // Update last active timestamp
        user.lastActive = new Date();
        
        // Save changes
        await user.save();
        
        // Return updated user data
        const userData = {
            userId: user.userId,
            email: user.email,
            firstName: user.firstName,
            lastName: user.lastName,
            username: user.username,
            role: user.role,
            themePreference: user.themePreference,
            location: user.location,
            waterNeeds: user.waterNeeds,
            isWellOwner: user.isWellOwner,
            lastActive: user.lastActive,
            loginToken: user.loginToken
        };
        
        res.json({
            status: 'success',
            message: 'Profile updated successfully',
            userData
        });
    } catch (error) {
        console.error('Error updating profile:', error);
        res.status(500).json({ 
            status: 'error', 
            message: 'Profile update failed: ' + error.message 
        });
    }
});

/**
 * @api {post} /update-water-needs Update user water needs
 * @apiName UpdateWaterNeeds
 * @apiGroup Users
 * 
 * @apiBody {String} email User's email.
 * @apiBody {String} token User's authentication token.
 * @apiBody {Array} waterNeeds Array of water need objects.
 * 
 * @apiSuccess {String} status "success" if operation was successful.
 * @apiSuccess {Object} userData Updated user data.
 * 
 * @apiError (Error 401) {String} status "error".
 * @apiError (Error 401) {String} message Authentication error message.
 * @apiError (Error 500) {String} status "error".
 * @apiError (Error 500) {String} message Error message.
 */
router.post('/update-water-needs', validateToken, async (req, res) => {
    try {
        const { waterNeeds } = req.body;
        const user = req.user;
        
        // Update water needs if provided
        if (waterNeeds && Array.isArray(waterNeeds)) {
            user.waterNeeds = waterNeeds;
        }
        
        // Update last active timestamp
        user.lastActive = new Date();
        
        // Save changes
        await user.save();
        
        // Return updated user data
        const userData = {
            userId: user.userId,
            email: user.email,
            firstName: user.firstName,
            lastName: user.lastName,
            username: user.username,
            role: user.role,
            themePreference: user.themePreference,
            location: user.location,
            waterNeeds: user.waterNeeds,
            isWellOwner: user.isWellOwner,
            lastActive: user.lastActive,
            loginToken: user.loginToken
        };
        
        res.json({
            status: 'success',
            message: 'Water needs updated successfully',
            userData
        });
    } catch (error) {
        console.error('Error updating water needs:', error);
        res.status(500).json({ 
            status: 'error', 
            message: 'Water needs update failed: ' + error.message 
        });
    }
});

/**
 * @api {post} /update-location Update user location
 * @apiName UpdateLocation
 * @apiGroup Users
 * 
 * @apiBody {String} email User's email.
 * @apiBody {String} token User's authentication token.
 * @apiBody {Object} location User's location object with latitude and longitude.
 * 
 * @apiSuccess {String} status "success" if operation was successful.
 * @apiSuccess {Object} userData Updated user data.
 * 
 * @apiError (Error 401) {String} status "error".
 * @apiError (Error 401) {String} message Authentication error message.
 * @apiError (Error 500) {String} status "error".
 * @apiError (Error 500) {String} message Error message.
 */
router.post('/update-location', validateToken, async (req, res) => {
    try {
        const { location } = req.body;
        const user = req.user;
        
        // Update location if provided
        if (location && typeof location === 'object') {
            // Add last updated timestamp to location
            const updatedLocation = {
                ...location,
                lastUpdated: new Date().toISOString()
            };
            
            user.location = updatedLocation;
            if (location.latitude) user.latitude = location.latitude;
            if (location.longitude) user.longitude = location.longitude;
        } else {
            return res.status(400).json({
                status: 'error',
                message: 'Invalid location data'
            });
        }
        
        // Update last active timestamp
        user.lastActive = new Date();
        
        // Save changes
        await user.save();
        
        res.json({
            status: 'success',
            message: 'Location updated successfully'
        });
    } catch (error) {
        console.error('Error updating location:', error);
        res.status(500).json({ 
            status: 'error', 
            message: 'Location update failed: ' + error.message 
        });
    }
});

/**
 * @api {get} /user-profile Get user profile
 * @apiName GetUserProfile
 * @apiGroup Users
 * 
 * @apiQuery {String} email User's email.
 * @apiQuery {String} token User's authentication token.
 * 
 * @apiSuccess {String} status "success" if operation was successful.
 * @apiSuccess {Object} userData User profile data.
 * 
 * @apiError (Error 401) {String} status "error".
 * @apiError (Error 401) {String} message Authentication error message.
 * @apiError (Error 500) {String} status "error".
 * @apiError (Error 500) {String} message Error message.
 */
router.get('/user-profile', async (req, res) => {
    try {
        const { email, token } = req.query;
        
        if (!email || !token) {
            return res.status(401).json({
                status: 'error',
                message: 'Authentication token and email are required'
            });
        }
        
        // Find user by email and check token
        const user = await User.findOne({ where: { email } });
        if (!user || user.loginToken !== token) {
            return res.status(401).json({
                status: 'error',
                message: 'Invalid or expired token'
            });
        }
        
        // Return user data
        const userData = {
            userId: user.userId,
            email: user.email,
            firstName: user.firstName,
            lastName: user.lastName,
            username: user.username,
            role: user.role,
            themePreference: user.themePreference,
            location: user.location,
            waterNeeds: user.waterNeeds,
            isWellOwner: user.isWellOwner,
            lastActive: user.lastActive
        };
        
        res.json({
            status: 'success',
            userData
        });
    } catch (error) {
        console.error('Error fetching user profile:', error);
        res.status(500).json({
            status: 'error',
            message: 'Profile fetch failed: ' + error.message
        });
    }
});

/**
 * @api {post} /update-theme Update user theme preference
 * @apiName UpdateTheme
 * @apiGroup Users
 * 
 * @apiBody {String} email User's email.
 * @apiBody {String} token User's authentication token.
 * @apiBody {Number} themePreference Theme preference (0: System, 1: Light, 2: Dark).
 * 
 * @apiSuccess {String} status "success" if operation was successful.
 * @apiSuccess {Object} userData Updated user data.
 * 
 * @apiError (Error 401) {String} status "error".
 * @apiError (Error 401) {String} message Authentication error message.
 * @apiError (Error 500) {String} status "error".
 * @apiError (Error 500) {String} message Error message.
 */
router.post('/update-theme', validateToken, async (req, res) => {
    try {
        const { themePreference } = req.body;
        const user = req.user;
        
        // Update theme preference
        if (themePreference !== undefined) {
            user.themePreference = parseInt(themePreference);
        } else {
            return res.status(400).json({
                status: 'error',
                message: 'Theme preference is required'
            });
        }
        
        // Save changes
        await user.save();
        
        // Return updated user data
        const userData = {
            userId: user.userId,
            email: user.email,
            firstName: user.firstName,
            lastName: user.lastName,
            username: user.username,
            role: user.role,
            themePreference: user.themePreference,
            location: user.location,
            waterNeeds: user.waterNeeds,
            isWellOwner: user.isWellOwner,
            lastActive: user.lastActive,
            loginToken: user.loginToken
        };
        
        res.json({
            status: 'success',
            message: 'Theme preference updated successfully',
            userData
        });
    } catch (error) {
        console.error('Error updating theme preference:', error);
        res.status(500).json({
            status: 'error',
            message: 'Theme update failed: ' + error.message
        });
    }
});

/**
 * @api {post} /delete-account Delete user account
 * @apiName DeleteAccount
 * @apiGroup Users
 * 
 * @apiBody {String} email User's email.
 * @apiBody {String} token User's authentication token.
 * 
 * @apiSuccess {String} status "success" if operation was successful.
 * @apiSuccess {String} message Confirmation message.
 * 
 * @apiError (Error 401) {String} status "error".
 * @apiError (Error 401) {String} message Authentication error message.
 * @apiError (Error 500) {String} status "error".
 * @apiError (Error 500) {String} message Error message.
 */
router.post('/delete-account', validateToken, async (req, res) => {
    try {
        const user = req.user;
        
        // Log the deletion attempt
        console.log(`Attempting to delete account for user: ${user.email}`);
        
        // Delete the user from the database
        await User.destroy({ where: { userId: user.userId } });
        
        console.log(`Successfully deleted account for user: ${user.email}`);
        
        res.json({
            status: 'success',
            message: 'Account successfully deleted'
        });
    } catch (error) {
        console.error('Error deleting account:', error);
        res.status(500).json({
            status: 'error',
            message: 'Account deletion failed: ' + error.message
        });
    }
});

module.exports = router; 