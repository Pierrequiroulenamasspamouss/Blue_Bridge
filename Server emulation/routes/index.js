const express = require('express');
const router = express.Router();

// Import sub-routers
const wellsRouter = require('./wells');
const { router: authRouter } = require('./auth');
const certificatesRouter = require('./certificates');
const usersRouter = require('./users');
const notificationsRouter = require('./notifications');

// Mount routers WITHOUT /api prefix
router.use('/wells', wellsRouter);
router.use('/', authRouter);
router.use('/', certificatesRouter);
router.use('/', usersRouter);
router.use('/notifications', notificationsRouter);

module.exports = router;

const auth = require('./auth');
const wells = require('./wells');
const nearbyUsers = require('./nearbyUsers');
const notifications = require('./notifications');
const certificates = require('./certificates');
const weather = require('./weather');
const wellStatistics = require('./wellStatistics');

module.exports = {
    auth,
    wells,
    nearbyUsers,
    notifications,
    certificates,
    weather,
    wellStatistics
}; 