const express = require('express');
const cors = require('cors');
const dotenv = require('dotenv');
const { sequelize } = require('./models');
const routes = require('./routes');
const http = require('http');
const fs = require('fs');
const path = require('path');

// Load environment variables
dotenv.config();

const app = express();
const isDev = process.env.NODE_ENV === 'development' || true; // Default to dev mode
const PORT = process.env.PORT || 3000;

// Middleware
app.use(cors());
app.use(express.json());

// Request logging middleware
app.use((req, res, next) => {
    const startTime = Date.now();
    console.log(`[${new Date().toISOString()}] ${req.method} ${req.url}`);
    
    // Log request body if it exists and isn't a file upload
    if (req.body && Object.keys(req.body).length > 0 && req.headers['content-type'] === 'application/json') {
        console.log('Request Body:', JSON.stringify(req.body, null, 2));
    }
    
    // Capture and log the response
    const originalSend = res.send;
    res.send = function(body) {
        const responseTime = Date.now() - startTime;
        console.log(`[${new Date().toISOString()}] Response ${res.statusCode} in ${responseTime}ms`);
        
        // Only log response body for JSON responses that aren't too large
        if (res.get('Content-Type')?.includes('application/json') && body && body.length < 1000) {
            try {
                const bodyObj = JSON.parse(body);
                console.log('Response Body:', JSON.stringify(bodyObj, null, 2));
            } catch (e) {
                // Not valid JSON or too large to parse
            }
        }
        
        originalSend.call(this, body);
    };
    
    next();
});

// Homescreen
app.get('/home', (req, res) => {
    res.sendFile(path.join(__dirname, 'html', 'welcomePage.html'));
});

// Routes
app.use('/api', routes);

// Test route
app.get('/', (req, res) => {
    res.json({
        message: 'Welcome to the WellConnect API',
        mode: isDev ? 'Development' : 'Production',
        status: 'Online',
        timestamp: new Date().toISOString()
    });
});

// Not found middleware
app.use((req, res, next) => {
    res.status(404).json({ 
        status: 'error',
        message: `Path not found: ${req.originalUrl}` 
    });
});

// Error handling middleware
app.use((err, req, res, next) => {
    console.error('Server Error:', err.stack);
    res.status(500).json({ 
        status: 'error',
        message: 'Something went wrong!',
        error: isDev ? err.message : undefined
    });
});

// Start server
async function startServer() {
    try {
        // Sync database
        await sequelize.sync();
        console.log('Database synced successfully');

        // Start HTTP server
        http.createServer(app).listen(PORT, () => {
            console.log(`HTTP server running on http://localhost:${PORT}`);
        });
    } catch (error) {
        console.error('Unable to start server:', error);
        process.exit(1);
    }
}

// Handle uncaught exceptions
process.on('uncaughtException', (err) => {
    console.error('Uncaught Exception:', err);
    // Don't exit in development mode
    if (!isDev) {
        process.exit(1);
    }
});

// Handle unhandled promise rejections
process.on('unhandledRejection', (reason, promise) => {
    console.error('Unhandled Rejection at:', promise, 'reason:', reason);
    // Don't exit in development mode
    if (!isDev) {
        process.exit(1);
    }
});

// Start the server
startServer();