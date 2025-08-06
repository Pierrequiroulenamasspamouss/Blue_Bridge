const express = require('express');
const router = express.Router();
const path = require('path');
require('dotenv').config();



// Import all API sub-routers
const wellsRouter = require('./wells');
const authRouter = require('./auth');
const certificatesRouter = require('./certificates');
const usersRouter = require('./users');
const notificationsRouter = require('./notifications');
const nearbyUsersRouter = require('./nearbyUsers');
const weatherRouter = require('./weather');
const wellStatisticsRouter = require('./wellStatistics');
const bugReportsRouter = require('./bugreports');


// Mount all API routers
router.use('/wells', wellsRouter);
router.use('/auth', authRouter);
router.use('/certificates', certificatesRouter);
router.use('/users', usersRouter);
router.use('/notifications', notificationsRouter);
router.use('/nearby-users', nearbyUsersRouter);
router.use('/weather', weatherRouter);
router.use('/well-statistics', wellStatisticsRouter);
router.use('/bugreports', bugReportsRouter);





router.get('/', (req, res) => {
    res.sendFile(path.join(__dirname, '../documentation', 'API_DOCUMENTATION.md'));
});

// Chat support endpoint
router.post('/chat/support', (req, res) => {
    try {
        const { senderId, receiverId, content, messageType } = req.body;
        
        // Log the received message
        console.log('Received chat message:', {
            senderId,
            receiverId,
            content,
            messageType
        });
        
        // For now, just acknowledge receipt
        res.json({
            status: "success",
            message: "Message received successfully",
            timestamp: new Date().toISOString().replace('T', ' ').substring(0, 19)
        });
    } catch (error) {
        console.error('Error processing chat message:', error);
        res.status(500).json({
            status: "error",
            message: "Failed to process message",
            timestamp: new Date().toISOString().replace('T', ' ').substring(0, 19)
        });
    }
});

module.exports = router;