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
const isDev = process.env.NODE_ENV ;
const appLatestVersion = process.env.APP_LATEST_VERSION ;
const serverVersion = process.env.SERVER_LATEST_VERSION;

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

// API status endpoint
router.get('/status', (req, res) => {
    res.apiSuccess({
        message: 'Welcome to the BlueBridge API',
        mode: isDev ? 'Development' : 'Production',
        status: 'Online',
        timestamp: new Date().toISOString(),
        versions: {
            server: serverVersion,
            mobile: appLatestVersion
        }
    });
});

router.get('/', (req, res) => {
    res.sendFile(path.join(__dirname, '../documentation', 'API_DOCUMENTATION.md'));
});

module.exports = router;