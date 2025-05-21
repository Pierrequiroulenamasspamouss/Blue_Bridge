const express = require('express');
const router = express.Router();
const db = require('../models');
const { User } = db;
const { v4: uuidv4 } = require('uuid');
const { sendWelcomeEmail } = require('../services/emailService');

// Middleware to validate token
const validateToken = async (req, res, next) => {
    try {
        const token = req.body.token || req.query.token || req.headers['x-auth-token'];
        const email = req.body.email || req.query.email;
        
        if (!token || !email) {
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
        
        // Add user to request for use in route handlers
        req.user = user;
        next();
    } catch (error) {
        console.error('Token validation error:', error);
        res.status(500).json({ status: 'error', message: 'Authentication error' });
    }
};

// --- User Registration ---
router.post('/register', async (req, res) => {
    try {
        const { username, email, password, firstName, lastName, role, themePreference, location, waterNeeds, isWellOwner } = req.body;
        
        // Validate required fields
        if (!username || !email || !password || !firstName || !lastName) {
            return res.status(400).json({ 
                status: 'error', 
                message: 'Missing required fields' 
            });
        }
        
        // Check for existing user
        const existing = await User.findOne({ where: { email } });
        if (existing) {
            return res.status(409).json({ 
                status: 'error', 
                message: 'User already exists with this email' 
            });
        }
        
        // Generate login token
        const loginToken = uuidv4();
        
        // Extract latitude and longitude from location object if present
        let latitude = null;
        let longitude = null;
        
        if (location && typeof location === 'object') {
            latitude = location.latitude;
            longitude = location.longitude;
        }
        
        // Create user
        const user = await User.create({
            username,
            email,
            password,
            firstName,
            lastName,
            role: role || 'user',
            themePreference: themePreference || 0,
            latitude,
            longitude,
            location,
            waterNeeds: waterNeeds || [],
            isWellOwner: isWellOwner || false,
            lastActive: new Date(),
            loginToken
        });
        
        // Send welcome email
        try {
            await sendWelcomeEmail(email, `${firstName} ${lastName}`);
        } catch (emailError) {
            console.error('Error sending welcome email:', emailError);
            // Don't fail the registration if email fails
        }
        
        // Prepare userData object for response
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
            loginToken: user.loginToken
        };
        
        res.status(201).json({
            status: 'success',
            message: 'Registration successful',
            userData
        });
    } catch (error) {
        console.error('Error registering user:', error);
        res.status(500).json({ 
            status: 'error', 
            message: 'Registration failed: ' + error.message 
        });
    }
});

// --- User Login ---
router.post('/login', async (req, res) => {
    try {
        const { email, password } = req.body;
        
        // Validate required fields
        if (!email || !password) {
            return res.status(400).json({ 
                status: 'error', 
                message: 'Email and password are required' 
            });
        }
        
        // Find user by email
        const user = await User.findOne({ where: { email } });
        if (!user) {
            return res.status(401).json({ 
                status: 'error', 
                message: 'User not found with this email' 
            });
        }
        
        // Check password
        if (user.password !== password) {
            return res.status(401).json({ 
                status: 'error', 
                message: 'Invalid password' 
            });
        }
        
        // Generate a new login token
        const loginToken = uuidv4();
        user.loginToken = loginToken;
        user.lastActive = new Date();
        await user.save();
        
        // Prepare userData object (all fields)
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
            message: 'Login successful',
            userData
        });
    } catch (error) {
        console.error('Error logging in:', error);
        res.status(500).json({ 
            status: 'error', 
            message: 'Login failed: ' + error.message 
        });
    }
});

// --- Logout ---
router.post('/logout', validateToken, async (req, res) => {
    try {
        // Clear login token
        req.user.loginToken = null;
        await req.user.save();
        
        res.json({
            status: 'success',
            message: 'Logout successful'
        });
    } catch (error) {
        console.error('Error logging out:', error);
        res.status(500).json({ 
            status: 'error', 
            message: 'Logout failed: ' + error.message 
        });
    }
});

// --- Delete Account ---
router.post('/delete-account', async (req, res) => {
    try {
        const { email, password, token } = req.body;
        
        // Validate required fields
        if (!email || !password || !token) {
            return res.status(400).json({
                status: 'error',
                message: 'Email, password, and token are required'
            });
        }
        
        // Find user by email
        const user = await User.findOne({ where: { email } });
        if (!user) {
            return res.status(404).json({
                status: 'error',
                message: 'User not found with this email'
            });
        }
        
        // Verify password
        if (user.password !== password) {
            return res.status(401).json({
                status: 'error',
                message: 'Invalid password'
            });
        }
        
        // Verify token
        if (user.loginToken !== token) {
            return res.status(401).json({
                status: 'error',
                message: 'Invalid or expired token'
            });
        }
        
        // Log the deletion
        console.log(`Deleting account for user: ${email}`);
        
        // Delete the user
        await user.destroy();
        
        res.json({
            status: 'success',
            message: 'Account deleted successfully'
        });
    } catch (error) {
        console.error('Error deleting account:', error);
        res.status(500).json({
            status: 'error',
            message: 'Account deletion failed: ' + error.message
        });
    }
});

// --- Check token validity ---
router.post('/validate-token', async (req, res) => {
    try {
        const { email, token } = req.body;
        
        if (!email || !token) {
            return res.status(400).json({
                status: 'error',
                message: 'Email and token are required'
            });
        }
        
        const user = await User.findOne({ where: { email } });
        if (!user || user.loginToken !== token) {
            return res.status(401).json({
                status: 'error',
                message: 'Invalid or expired token'
            });
        }
        
        res.json({
            status: 'success',
            message: 'Token is valid',
            isValid: true
        });
    } catch (error) {
        console.error('Error validating token:', error);
        res.status(500).json({
            status: 'error',
            message: 'Token validation failed: ' + error.message
        });
    }
});

// Export router and validateToken middleware
module.exports = {
    router,
    validateToken
}; 