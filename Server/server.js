/**
 * @fileoverview Main server file for the BlueBridge API.
 * This file sets up the Express application, configures middleware,
 * defines routes, and starts the server.
 */

// ======================
// Configuration Constants
// ======================
const appLatestVersion = '0.1.4';
const serverVersion = '1.0.2';
const isDev = process.env.NODE_ENV === 'development' || true;
const PORT = process.env.PORT || 80;
const httpsPort = process.env.HTTPS_PORT || 443;

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

// =============
// Database Setup
// =============
const { initializeFirebase } = require('./services/firebaseService');
const sequelize = require('./config/database');
const models = require('./models');

// ===============
// Route Imports
// ===============
const routes = require('./routes');
const { listenerCount } = require('process');
const { log } = require('console');

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
app.use(express.static(path.join(__dirname, 'html')));
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
// Routes
// ===========
// Static file routes
app.get('/favicon.ico', (req, res) => res.sendFile(path.join(__dirname, 'assets', 'favicon.ico')));
app.get('/', (req, res) => res.redirect('/home'));
app.get('/home', (req, res) => res.send(readHtmlFile('welcomePage')));
app.get('/about', (req, res) => res.send(readHtmlFile('about')));
app.get('/services', (req, res) => res.send(readHtmlFile('services')));
app.get('/send-notifications', (req, res) => res.send(readHtmlFile('send-notifications')));
app.get('/support', (req, res) => res.send(readHtmlFile('support')));

// API status route
app.get('/status', (req, res) => {
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

// APK download route
app.get('/download', (req, res) => {
    res.download(
        path.join(__dirname, 'APK', 'BlueBridge.apk'),
        `BlueBridge_v${appLatestVersion}.apk`
    );
});

// API routes
app.use('/api', routes);

// API documentation route
app.get('/tree', (req, res) => {
    // Extract routes from the app
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

    // Process routes into HTML
    const routes = Array.from(extractRoutes(app._router.stack))
        .map(route => JSON.parse(route))
        .sort((a, b) => a.path.localeCompare(b.path));

    // Group routes by prefix
    const grouped = {};
    for (const { method, path } of routes) {
        const section = path.split('/')[1] || '/';
        grouped[section] = grouped[section] || [];
        grouped[section].push({ method, path });
    }

    // Generate routes HTML content
    let routesContent = '';
    for (const section of Object.keys(grouped).sort()) {
        routesContent += `
            <div class="route-group">
                <h2>/${section}</h2>
                <table class="route-table">
                    <tr>
                        <th>Method</th>
                        <th>Path</th>
                    </tr>`;
    
                
        for (const { method, path } of grouped[section]) {
            const link = method === 'GET' ? `<a href="${path}" target="_blank">${path}</a>` : path;
            routesContent += `
                <tr>
                    <td><span class="method ${method}">${method}</span></td>
                    <td class="route-path">${link}</td>
                </tr>`;
        }
        
        routesContent += `
                </table>
            </div>`;
    }

    // Read template and replace placeholders
    const apiTreeHtml = readHtmlFile('apiTree')
        .replace('{{routesContent}}', routesContent)
        .replace('{{generationTime}}', new Date().toLocaleString());
    
    res.send(apiTreeHtml);
});

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
        
        await sequelize.authenticate();
        console.log('✅ Database connected');
        
        await sequelize.sync();
        console.log('✅ Database synchronized');

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

