/**
 * @fileoverview Main server file for the WellConnect API.
 * This file sets up the Express application, configures middleware,
 * defines routes, and starts the server.
 */


const express = require('express');
const cors = require('cors');
const dotenv = require('dotenv');
const { sequelize } = require('./models');
const routes = require('./routes');
const http = require('http');
const fs = require('fs');
const path = require('path');
const { initializeFirebase } = require('./services/firebaseService');

/**
 * Loads environment variables from a .env file.
 */
dotenv.config();

/**
 * Creates an instance of an Express application.
 * @type {express.Application}
 */
const app = express();

/**
 * Checks if the application is running in development mode.
 * @type {boolean}
 */
const isDev = process.env.NODE_ENV === 'development' || true; // Default to dev mode if not set

/**
 * The port on which the server will listen. Defaults to 3000.
 * @type {number}
 */
const PORT = process.env.PORT || 3000;

/** Middleware */
app.use(cors());
app.use(express.json());

/** Add specific CORS headers for notifications endpoint */
app.use('/api/notifications/*', (req, res, next) => {
    res.header('Access-Control-Allow-Origin', '*');
    res.header('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
    res.header('Access-Control-Allow-Headers', 'Origin, X-Requested-With, Content-Type, Accept, Authorization');
    if (req.method === 'OPTIONS') {
        return res.sendStatus(200);
    }
    next();
});

/**
 * Request logging middleware.
 * Logs request details and response times for debugging and monitoring purposes.
 * Logs request bodies and response bodies if conditions are met.
 */
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

/**
 * Route for serving the welcome page.
 * @name get/home
 * @function
 */
app.get('/home', (req, res) => {
    res.sendFile(path.join(__dirname, 'html', 'welcomePage.html'));
});

/**
 * Route for serving the Push Notifications UI.
 * @name get/send-notifications
 * @function
 */
app.get('/send-notifications', (req, res) => {
    res.sendFile(path.join(__dirname, 'html', 'send-notifications.html'));
});

/**
 * Dynamic API tree generator middleware.
 * Generates an HTML page displaying all available API routes.
 * @returns {function} An Express middleware function.
 */
const generateApiTree = () => {
    /** Helper function to get all routes from the Express app. */
    const getRoutes = (app) => {
        const stack = app._router.stack;
        
        const findRoutes = (stack, basePath = '') => {
            stack.forEach(layer => {
                if (layer.route) {
                    // Routes registered directly on the app
                    const path = basePath + layer.route.path;
                    const methods = Object.keys(layer.route.methods)
                        .filter(method => layer.route.methods[method])
                        .map(method => method.toUpperCase());
                    
                    if (!path.includes(':') && path !== '/') { // Skip parameterized routes and root
                        routes.push({ path, methods });
                    }
                } else if (layer.name === 'router' && layer.handle.stack) {
                    // Routes added via router
                    let routerPath = basePath;
                    if (layer.regexp && layer.regexp.source !== '^\\/?$') {
                        // Extract router base path
                        const match = layer.regexp.toString().match(/\(\?:([^)]+)\)/);
                        if (match) {
                            routerPath += '/' + match[1].replace(/\\\//g, '/').replace(/\\/g, '');
                        }
                    }
                    findRoutes(layer.handle.stack, routerPath);
                } else if (layer.name === 'bound dispatch' && layer.handle && layer.handle.stack) {
                    // Router middleware
                    findRoutes(layer.handle.stack, basePath);
                }
            });
        };
        const routes = [];
        findRoutes(stack);
        return routes;
    };
    

    return (req, res) => {
        const routes = getRoutes(app);
        
        // Group routes by base path
        const groupedRoutes = {};
        routes.forEach(route => {
            const parts = route.path.split('/').filter(Boolean);
            const basePath = parts.length > 0 ? parts[0] : '/';
            
            if (!groupedRoutes[basePath]) {
                groupedRoutes[basePath] = [];
            }
            groupedRoutes[basePath].push(route);
        });
        
        // Generate HTML
        let html = `
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>WellConnect API Tree</title>
            <style>
                body {
                    font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                    line-height: 1.6;
                    color: #333;
                    max-width: 1000px;
                    margin: 0 auto;
                    padding: 20px;
                    background-color: #f8f9fa;
                }
                h1 {
                    color: #0056b3;
                    border-bottom: 2px solid #0056b3;
                    padding-bottom: 10px;
                    margin-bottom: 30px;
                }
                .section {
                    background-color: white;
                    border-radius: 8px;
                    box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                    padding: 20px;
                    margin-bottom: 20px;
                }
                h2 {
                    color: #0056b3;
                    margin-top: 0;
                }
                .endpoint {
                    margin-bottom: 15px;
                    border-left: 5px solid #6c757d;
                    padding-left: 15px;
                }
                .endpoint:hover {
                    border-left-color: #0056b3;
                }
                .path {
                    font-family: monospace;
                    font-size: 16px;
                    font-weight: bold;
                    color: #333;
                }
                a {
                    color: #0056b3;
                    text-decoration: none;
                }
                a:hover {
                    text-decoration: underline;
                }
                .method {
                    display: inline-block;
                    padding: 2px 6px;
                    margin-right: 5px;
                    font-size: 12px;
                    font-weight: bold;
                    color: white;
                    background-color: #6c757d;
                    border-radius: 4px;
                }
                .GET { background-color: #28a745; }
                .POST { background-color: #007bff; }
                .PUT { background-color: #fd7e14; }
                .DELETE { background-color: #dc3545; }
                .PATCH { background-color: #6f42c1; }
                .description {
                    margin-top: 5px;
                    font-size: 14px;
                    color: #6c757d;
                }
                footer {
                    text-align: center;
                    margin-top: 40px;
                    color: #6c757d;
                    font-size: 14px;
                }
                .timestamp {
                    font-style: italic;
                    margin-top: 10px;
                    font-size: 12px;
                    color: #6c757d;
                }
            </style>
        </head>
        <body>
            <h1>WellConnect API Tree</h1>
            <p>This is an automatically generated map of available API endpoints. Click on the links to go to the endpoint documentation or testing.</p>
        `;
        
        // Sort sections alphabetically
        const sortedSections = Object.keys(groupedRoutes).sort();
        
        sortedSections.forEach(section => {
            html += `
            <div class="section">
                <h2>/${section}</h2>
            `;
            
            // Sort endpoints alphabetically within each section
            const sortedRoutes = groupedRoutes[section].sort((a, b) => a.path.localeCompare(b.path));
            
            sortedRoutes.forEach(route => {
                html += `
                <div class="endpoint">
                    <div class="path-row">
                        ${route.methods.map(method => `<span class="method ${method}">${method}</span>`).join(' ')}
                        <span class="path"><a href="${route.path}">${route.path}</a></span>
                    </div>
                </div>
                `;
            });
            
            html += `</div>`;
        });
        
        html += `
            <footer>
                <p>WellConnect API Server</p>
                <p class="timestamp">Generated on: ${new Date().toLocaleString()}</p>
            </footer>
        </body>
        </html>
        `;
        
        res.setHeader('Content-Type', 'text/html');
        res.send(html);
    };
};

/** API routes */
app.use('/api/auth', routes.auth.router);
app.use('/api/wells', routes.wells);
app.use('/api/nearby-users', routes.nearbyUsers);
app.use('/api/notifications', routes.notifications);
app.use('/api/certificates', routes.certificates);
app.use('/api/weather', routes.weather);
app.use('/api/wellstatistics', routes.wellStatistics);

/**
 * API Tree route.
 * @name get/tree
 * @function
 */
app.get('/tree', generateApiTree());

/**
 * Test route to check if the server is running.
 * @name get/
 * @function
 */
app.get('/', (req, res) => {
    res.json({
        message: 'Welcome to the WellConnect API',
        mode: isDev ? 'Development' : 'Production',
        status: 'Online',
        timestamp: new Date().toISOString(),
    });
});

/**
 * 404 Not Found middleware.
 * @function
 */
app.use((req, res, next) => {
    res.status(404).json({ 
        status: 'error',
        message: `Path not found: ${req.originalUrl}` 
    });
});
/** Error handling middleware. */
app.use((err, req, res, next) => {
    console.error('Server Error:', err.stack);
    res.status(500).json({ 
        status: 'error',
        message: 'Something went wrong!',
        error: isDev ? err.message : undefined
    });
});

/**
 * Starts the server after syncing the database.
 * @async
 * @function
 * @throws {Error} If there is an error starting the server or syncing the database.
 */
async function startServer() {
    try {
        /** Sync database */
        await sequelize.sync();
        console.log('Database synced successfully');

        /** Initialize Firebase */
        console.log('Initializing Firebase Admin SDK...');
        initializeFirebase();

        /**
         * Start HTTP server
         * Create and start the HTTP server.
         */
        http.createServer(app).listen(PORT, () => {
            console.log(`HTTP server running on http://localhost:${PORT}`);
        });
    } catch (error) {
        console.error('Unable to start server:', error);
        process.exit(1);
    }
}

/**
 * Handles uncaught exceptions.
 */
process.on('uncaughtException', (err) => {
    console.error('Uncaught Exception:', err);
    /** Exit the process if not in development mode. */
    if (!isDev) {
        process.exit(1);
    }
});

/**
 * Handles unhandled promise rejections.
 */
process.on('unhandledRejection', (reason, promise) => {
    console.error('Unhandled Rejection at:', promise, 'reason:', reason);
     /** Exit the process if not in development mode. */
    if (!isDev) {
        process.exit(1);
    }
});

/** Starts the server. */
startServer();
