const express = require('express');
const router = express.Router();

// Import sub-routers
const wellsRouter = require('./wells');
const authRouter = require('./auth');
const certificatesRouter = require('./certificates');
const usersRouter = require('./users');
const notificationsRouter = require('./notifications');
const nearbyUsersRouter = require('./nearbyUsers');
const weatherRouter = require('./weather');
const wellStatisticsRouter = require('./wellStatistics');
const bugReportsRouter = require('./bugreports');

// Mount routers
router.use('/wells', wellsRouter);
router.use('/auth', authRouter);
router.use('/certificates', certificatesRouter);
router.use('/users', usersRouter);
router.use('/notifications', notificationsRouter);
router.use('/nearby-users', nearbyUsersRouter);
router.use('/weather', weatherRouter);
router.use('/well-statistics', wellStatisticsRouter);
router.use('/bugreports', bugReportsRouter);

module.exports = router; 