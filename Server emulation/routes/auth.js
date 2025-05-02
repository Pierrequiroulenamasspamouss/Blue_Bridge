const express = require('express');
const router = express.Router();
const db = require('../models');
const { User } = db;

// --- User Registration ---
router.post('/register', async (req, res) => {
    try {
        const { username, email, password, firstName, lastName, role, themePreference, latitude, longitude, location, waterNeeds, isWellOwner, lastActive } = req.body;
        if (!username || !email || !password) {
            return res.status(400).json({ error: 'Missing required fields' });
        }
        const existing = await User.findOne({ where: { email } });
        if (existing) {
            return res.status(409).json({ error: 'User already exists' });
        }
        const user = await User.create({
            username,
            email,
            password,
            firstName,
            lastName,
            role,
            themePreference,
            latitude,
            longitude,
            location,
            waterNeeds,
            isWellOwner,
            lastActive
        });
        res.status(201).json({
            status: 'success',
            message: 'Registration successful',
            data: {
                username: user.username,
                email: user.email,
                firstName: user.firstName,
                lastName: user.lastName,
                role: user.role
            }
        });
    } catch (error) {
        console.error('Error registering user:', error);
        res.status(500).json({ error: error.message });
    }
});

// --- User Login ---
router.post('/login', async (req, res) => {
    try {
        const { email, password } = req.body;
        const user = await User.findOne({ where: { email } });
        if (!user || user.password !== password) {
            return res.status(401).json({ error: 'Invalid credentials' });
        }
        res.json({ status: 'success', user: { username: user.username, email: user.email } });
    } catch (error) {
        console.error('Error logging in:', error);
        res.status(500).json({ error: error.message });
    }
});

module.exports = router; 