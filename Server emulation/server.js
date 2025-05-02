const express = require('express');
const cors = require('cors');
const dotenv = require('dotenv');
const { sequelize } = require('./models');
const routes = require('./routes');
const https = require('https');
const http = require('http');
const CertificateManager = require('./scripts/cert-manager');
const path = require('path');

// Load environment variables
dotenv.config();

const app = express();
const isDev = process.env.NODE_ENV === 'development';
const PORT = process.env.PORT || (isDev ? 3443 : 443);
const HTTP_PORT = process.env.HTTP_PORT || 80;

// Middleware
app.use(cors());
app.use(express.json());


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
        mode: isDev ? 'Development' : 'Production'
    });
});

// Error handling middleware
app.use((err, req, res, next) => {
    console.error(err.stack);
    res.status(500).json({ error: 'Something went wrong!' });
});

// Start server with appropriate certificates
async function startServer() {
    try {
        // Sync database
        await sequelize.sync();
        console.log('Database synced successfully');

        const certManager = new CertificateManager({
            email: process.env.EMAIL,
            domain: process.env.DOMAIN
        });

        if (isDev) {
            // Development mode with self-signed certificates
            const certs = await certManager.generateDevCertificates();
            const httpsServer = https.createServer(certs, app);

            httpsServer.listen(PORT, () => {
                console.log(`Development server running on https://localhost:${PORT}`);
                console.log('Using self-signed certificates');
            });

            // Also start HTTP server for development
            http.createServer(app).listen(HTTP_PORT, () => {
                console.log(`HTTP server running on http://localhost:${HTTP_PORT}`);
            });
        } else {
            // Production mode with Let's Encrypt certificates
            await certManager.setupProductionCertificates(app);
            console.log(`Production server running with Let's Encrypt certificates on port ${PORT}`);
        }
    } catch (error) {
        console.error('Unable to start server:', error);
        process.exit(1);
    }
}

startServer();