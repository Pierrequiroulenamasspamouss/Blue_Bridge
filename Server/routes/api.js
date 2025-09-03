const express = require('express');
const router = express.Router();
const path = require('path');
require('dotenv').config();

// Import routers
const authRouter = require('./auth');
const usersRouter = require('./users');
const wellsRouter = require('./wells');
const weatherRouter = require('./weather');
const nearbyUsersRouter = require('./nearbyUsers');
const notificationsRouter = require('./notifications');
const certificatesRouter = require('./certificates');
const bugReportsRouter = require('./bugreports');
const wellStatisticsRouter = require('./wellStatistics');
const chatsRouter = require('./chats');

// Use routers
router.use('/auth', authRouter);
router.use('/users', usersRouter);
router.use('/wells', wellsRouter);
router.use('/weather', weatherRouter);
router.use('/nearbyUsers', nearbyUsersRouter);
router.use('/notifications', notificationsRouter);
router.use('/certificates', certificatesRouter);
router.use('/bugreports', bugReportsRouter);
router.use('/wellStatistics', wellStatisticsRouter);
router.use('/chats', chatsRouter);

router.get('/', (req, res) => {
    res.sendFile(path.join(__dirname, '../documentation', 'API_DOCUMENTATION.md'));
});

module.exports = router;