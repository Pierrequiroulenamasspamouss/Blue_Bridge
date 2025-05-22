//TO CHANGE IF UPDATES NEEDED
const appLatestVersion = '0.1.2';
const serverVersion = '1.0.1';
/**
 * @fileoverview Main server file for the BlueBridge API.
 * This file sets up the Express application, configures middleware,
 * defines routes, and starts the server.
 */
const express = require('express');
const cors = require('cors');
const dotenv = require('dotenv');
const routes = require('./routes');
const http = require('http');
const https = require('https');
const fs = require('fs');
const path = require('path');
const { initializeFirebase } = require('./services/firebaseService');
const sequelize = require('./config/database');
const models = require('./models');


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

// Handle favicon request
app.get('/favicon.ico', (req, res) => {
    res.sendFile(path.join(__dirname, 'assets', 'favicon.ico'));
});

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

// Add support endpoint
app.get('/support', (req, res) => {
    res.sendFile(path.join(__dirname, 'html', 'support.html'));
});

/**
 * Dynamic API tree generator middleware.
 * Generates an HTML page displaying all available API routes.
 * @returns {function} An Express middleware function.
 */
const generateApiTree = () => {
    const extractRoutes = (stack, parentPath = '') => {
        const routes = new Set();

        stack.forEach(layer => {
            if (layer.route) {
                const routePath = layer.route.path;
                const fullPath = path.posix.join(parentPath, routePath);
                const methods = Object.keys(layer.route.methods).map(m => m.toUpperCase());
                methods.forEach(method => {
                    routes.add(JSON.stringify({ method, path: fullPath }));
                });
            } else if (layer.name === 'router' && layer.handle.stack) {
                const match = layer.regexp.toString().match(/\\\/([^\\\/\(\)\?\:]+)(?=\\\/|\(\?:|\?|\\\/\)|$)/);
                const subPath = match ? `/${match[1]}` : '';
                const newParentPath = path.posix.join(parentPath, subPath);
                const subRoutes = extractRoutes(layer.handle.stack, newParentPath);
                subRoutes.forEach(route => routes.add(route));
            }
        });

        return routes;
    };

    return (req, res) => {
        const routes = Array.from(extractRoutes(req.app._router.stack))
            .map(route => JSON.parse(route))
            .sort((a, b) => a.path.localeCompare(b.path));

        // Group routes by prefix
        const grouped = {};
        for (const { method, path } of routes) {
            const section = path.split('/')[1] || '/';
            grouped[section] = grouped[section] || [];
            grouped[section].push({ method, path });
        }

        let html = `<!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>BlueBridge API Documentation</title>
            <style>
                :root {
                    --primary-color: #3498db;
                    --success-color: #27ae60;
                    --warning-color: #f39c12;
                    --danger-color: #c0392b;
                    --info-color: #2980b9;
                    --text-color: #2c3e50;
                    --bg-color: #f8f9fa;
                    --card-bg: #ffffff;
                }
                
                body {
                    font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                    line-height: 1.6;
                    margin: 0;
                    padding: 0;
                    background: var(--bg-color);
                    color: var(--text-color);
                }
                
                .container {
                    max-width: 1200px;
                    margin: 0 auto;
                    padding: 2rem;
                }
                
                .header {
                    text-align: center;
                    margin-bottom: 3rem;
                    padding: 2rem;
                    background: var(--card-bg);
                    border-radius: 8px;
                    box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                }
                
                .header h1 {
                    color: var(--primary-color);
                    margin: 0;
                    font-size: 2.5rem;
                }
                
                .header p {
                    color: #666;
                    margin-top: 0.5rem;
                }
                
                .route-group {
                    background: var(--card-bg);
                    border-radius: 8px;
                    padding: 1.5rem;
                    margin-bottom: 2rem;
                    box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                }
                
                .route-group h2 {
                    color: var(--primary-color);
                    margin-top: 0;
                    padding-bottom: 0.5rem;
                    border-bottom: 2px solid var(--bg-color);
                }
                
                .route-table {
                    width: 100%;
                    border-collapse: collapse;
                    margin-top: 1rem;
                }
                
                .route-table th {
                    text-align: left;
                    padding: 1rem;
                    background: var(--bg-color);
                    font-weight: 600;
                }
                
                .route-table td {
                    padding: 1rem;
                    border-bottom: 1px solid var(--bg-color);
                }
                
                .method {
                    display: inline-block;
                    padding: 0.25rem 0.75rem;
                    border-radius: 4px;
                    color: white;
                    font-weight: 600;
                    font-size: 0.875rem;
                }
                
                .GET { background-color: var(--success-color); }
                .POST { background-color: var(--info-color); }
                .PUT { background-color: var(--warning-color); }
                .DELETE { background-color: var(--danger-color); }
                .PATCH { background-color: #8e44ad; }
                
                .route-path {
                    font-family: monospace;
                    color: var(--primary-color);
                }
                
                .route-path a {
                    color: inherit;
                    text-decoration: none;
                }
                
                .route-path a:hover {
                    text-decoration: underline;
                }
                
                .footer {
                    text-align: center;
                    margin-top: 3rem;
                    padding: 1rem;
                    color: #666;
                    font-size: 0.875rem;
                }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <h1>BlueBridge API Documentation</h1>
                    <p>Complete API reference and documentation</p>
                </div>`;

        for (const section of Object.keys(grouped).sort()) {
            html += `
                <div class="route-group">
                    <h2>/${section}</h2>
                    <table class="route-table">
                        <tr>
                            <th>Method</th>
                            <th>Path</th>
                        </tr>`;
            
            for (const { method, path } of grouped[section]) {
                const link = method === 'GET' ? `<a href="${path}" target="_blank">${path}</a>` : path;
                html += `
                    <tr>
                        <td><span class="method ${method}">${method}</span></td>
                        <td class="route-path">${link}</td>
                    </tr>`;
            }
            
            html += `
                    </table>
                </div>`;
        }

        html += `
                <div class="footer">
                    <p>Generated at ${new Date().toLocaleString()}</p>
                </div>
            </div>
        </body>
        </html>`;

        res.setHeader('Content-Type', 'text/html');
        res.send(html);
    };
};

/** API routes */
app.use('/api', routes);

// Serve static files
app.use(express.static(path.join(__dirname, 'html')));
app.use(express.static(path.join(__dirname, 'assets')));

/**
 * API Tree route.
 * @name get/tree
 * @function
 */
app.get('/tree', generateApiTree());

/**
 * Add response formatter middleware
 */
app.use((req, res, next) => {
    const originalJson = res.json;
    res.json = function(data) {
        return originalJson.call(this, {
            status: res.statusCode < 400 ? 'success' : 'error',
            data: data
        });
    };
    next();
});

/**
 * Update root route to redirect to /home
 */
app.get('/', (req, res) => {
    res.redirect('/home');
});

/**
 * Add status endpoint
 */
app.get('/status', (req, res) => {
    res.json({
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

/**
 * Download route for the APK file
 */
app.get('/download', (req, res) => {
    const filePath = path.join(__dirname, 'APK', 'BlueBridge.apk');
    res.download(filePath, 'BlueBridge_v' + appLatestVersion + '.apk'); // <-- use res.download
});

/**
 * 404 Not Found middleware.
 * @function
 */
app.use((req, res, next) => {
    res.status(404).json({
        message: `Path not found: ${req.originalUrl}`
    });
});

/** Error handling middleware. */
app.use((err, req, res, next) => {
    console.error(err.stack);
    res.status(500).json({
        error: 'Internal Server Error',
        message: isDev ? err.message : 'Something went wrong'
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
        /** Initialize Firebase */
        console.log('✅ Firebase Admin SDK initialized successfully using service account file');
        await initializeFirebase();

        /** Initialize database */
        console.log('✅ Database connection established successfully');
        await sequelize.authenticate();

        /** Sync database */
        console.log('✅ Database models synchronized');
        await sequelize.sync();

        /**
         * Start HTTP server
         * Create and start the HTTP server.
         */
        const server = http.createServer(app);
        server.listen(PORT, () => {
            console.log(`🚀 Server is running on port ${PORT}`);
            console.log(`📝 API Documentation available at http://localhost:${PORT}/api-docs`);
        });
        server.on('error', (err) => {
            console.error('Failed to start HTTP server:', err);
            process.exit(1);
        });

        // HTTPS Server (only if SSL files exist)
        const sslKeyPath = path.join(__dirname, 'ssl', 'private.key');
        const sslCertPath = path.join(__dirname, 'ssl', 'certificate.crt');

        if (fs.existsSync(sslKeyPath) && fs.existsSync(sslCertPath)) {
            const httpsOptions = {
                key: fs.readFileSync(sslKeyPath),
                cert: fs.readFileSync(sslCertPath)
            };

            const httpsPort = process.env.HTTPS_PORT || 3443;
            https.createServer(httpsOptions, app).listen(httpsPort, () => {
                console.log(`HTTPS server running on https://localhost:${httpsPort}`);
            });
        } else {
            console.log('SSL certificates not found. HTTPS server not started.');
        }
    } catch (error) {
        console.error('❌ Server startup failed:', error);
        process.exit(1);
    }
}

/**
 * Handles uncaught exceptions.
 */
process.on('uncaughtException', (err) => {
    console.error('Uncaught Exception thrown:', err);
    process.exit(1);
});

/**
 * Handles unhandled promise rejections.
 */
process.on('unhandledRejection', (reason, promise) => {
    console.error('Unhandled Rejection at:', promise, 'reason:', reason);
});

/** Starts the server. */
startServer();