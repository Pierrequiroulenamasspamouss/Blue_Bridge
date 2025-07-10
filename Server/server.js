/**
 * @fileoverview Main server file for the BlueBridge API.
 * This file sets up the Express application, configures middleware,
 * defines routes, and starts the server.
 */

// =============
// Dependencies
// =============
const express = require('express');
const cors = require('cors');
const dotenv = require('dotenv');
const http = require('http');
const https = require('https');
const fs = require('fs');
const path = require('path');

// ============
// Initial Setup
// ============
dotenv.config();
const app = express();


// ======================
// Configuration Constants
// ======================


const isDev = process.env.NODE_ENV === 'development' || true;
const PORT = process.env.HTTP_PORT ;
const httpsPort = process.env.HTTPS_PORT ;


// =============
// Database Setup
// =============
const { initializeFirebase } = require('./services/firebaseService');
const models = require('./models');

// ===============
// Route Imports
// ===============
const apiRouter = require('./routes/api');
const webappRouter = require('./routes/webapp');
const mainRouter = require('./routes/main');

// =================
// Helper Functions
// =================
/**
 * Reads HTML file from the html directory
 * @param {string} filename - Name of the HTML file (without extension)
 * @returns {string} HTML content
 */
const readHtmlFile = (filename) => {
    return fs.readFileSync(path.join(__dirname, 'html', `${filename}.html`), 'utf8');
};

// ==============
// Middleware
// ==============
app.use(cors());
app.use(express.json());
app.use(express.static(path.join(__dirname, 'public')));
app.use(express.static(path.join(__dirname, 'assets')));

// Request logging middleware
app.use((req, res, next) => {
    const startTime = Date.now();
    console.log(`[${new Date().toISOString()}] ${req.method} ${req.url}`);
    
    if (req.body && Object.keys(req.body).length > 0 && req.headers['content-type'] === 'application/json') {
        console.log('Request Body:', JSON.stringify(req.body, null, 2));
    }
    
    const originalSend = res.send;
    res.send = function(body) {
        const responseTime = Date.now() - startTime;
        console.log(`[${new Date().toISOString()}] Response ${res.statusCode} in ${responseTime}ms`);
        originalSend.call(this, body);
    };
    
    next();
});

// Response formatter middleware
app.use((req, res, next) => {
    res.apiSuccess = (data) => {
        return res.json({
            status: 'success',
            data: data
        });
    };
    
    res.apiError = (message, statusCode = 400) => {
        return res.status(statusCode).json({
            status: 'error',
            message: message
        });
    };
    
    next();
});

// ===========
// Route Mounting
// ===========
app.use('/api', apiRouter);          // All API endpoints
app.use('/webapp', webappRouter);    // Web application routes //NOT AVAILABLE RIGHT NOW, STILL A WIP
app.use('/', mainRouter);            // Main website routes (HTML pages, etc.)

// ===================
// Error Handling
// ===================
// 404 Not Found
app.use((req, res) => {
    res.apiError(`Path not found: ${req.originalUrl}`, 404);
});

// Global error handler
app.use((err, req, res, next) => {
    console.error(err.stack);
    res.apiError(isDev ? err.message : 'Something went wrong', 500);
});

// ===================
// Server Initialization
// ===================
async function startServer() {
    try {
        // Initialize services
        await initializeFirebase();
        console.log('✅ Firebase initialized');
        
        console.log('✅ Database connections are managed by the models.');

        // Create HTTP server
        const server = http.createServer(app);
        server.listen(PORT, () => {
            console.log(`🚀 Server running on port ${PORT}`);
            console.log(`📝 API docs at http://localhost:${PORT}/tree`);
        });

        // Optional HTTPS server
        const sslKeyPath = path.join(__dirname, 'ssl', 'private.key');
        const sslCertPath = path.join(__dirname, 'ssl', 'certificate.crt');
        
        if (fs.existsSync(sslKeyPath) && fs.existsSync(sslCertPath)) {
            https.createServer({
                key: fs.readFileSync(sslKeyPath),
                cert: fs.readFileSync(sslCertPath)
            }, app).listen(httpsPort, () => {
                console.log(`🔒 HTTPS server on port ${httpsPort}`);
            });
        } else {
            console.log('ℹ️ HTTPS not configured - missing SSL certificates');
        }
    } catch (error) {
        console.error('❌ Server startup failed:', error);
        process.exit(1);
    }
}

// ===================
// Process Handlers
// ===================
process.on('uncaughtException', (err) => {
    console.error('Uncaught Exception:', err);
    process.exit(1);
});

process.on('unhandledRejection', (reason, promise) => {
    console.error('Unhandled Rejection at:', promise, 'reason:', reason);
});

// Start the server
startServer();