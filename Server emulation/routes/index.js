const express = require('express');
const router = express.Router();

// Import sub-routers
const wellsRouter = require('./wells');
const authRouter = require('./auth');
const certificatesRouter = require('./certificates');
const usersRouter = require('./users');

// Mount routers WITHOUT /api prefix
router.use('/wells', wellsRouter);
router.use('/', authRouter);
router.use('/', certificatesRouter);
router.use('/', usersRouter);

module.exports = router; 