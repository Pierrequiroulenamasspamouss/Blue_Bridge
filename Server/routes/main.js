const express = require('express');
const router = express.Router();
const path = require('path');
const fs = require('fs');
require('dotenv').config();

const isDev = process.env.NODE_ENV ;
const appLatestVersion = process.env.APP_LATEST_VERSION ;
const serverVersion = process.env.SERVER_VERSION;

// Helper function to read HTML files
const readHtmlFile = (filename) => {
    return fs.readFileSync(path.join(__dirname, '../html', `${filename}.html`), 'utf8');
};

// Static file routes
router.get('/favicon.ico', (req, res) => res.sendFile(path.join(__dirname, '../assets', 'favicon.ico')));
router.get('/', (req, res) => res.redirect('/home'));
router.get('/home', (req, res) => res.send(readHtmlFile('welcomePage')));
router.get('/about', (req, res) => res.send(readHtmlFile('about')));
router.get('/services', (req, res) => res.send(readHtmlFile('services')));
router.get('/send-notifications', (req, res) => res.send(readHtmlFile('send-notifications')));
router.get('/support', (req, res) => res.send(readHtmlFile('support')));
router.get('/contact', (req, res) => res.send(readHtmlFile('website_under_construction')));


// APK download route
router.get('/download', (req, res) => {
    const apkDir = path.join(__dirname, '../APK');
    const files = fs.readdirSync(apkDir);
    const apkFile = files.find(file => file.endsWith('.apk'));

    res.download(
        path.join(apkDir, apkFile),
        `BlueBridge_v${appLatestVersion}.apk`
    );
});

// API documentation route
router.get('/tree', (req, res) => {
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
    const routes = Array.from(extractRoutes(router.stack))
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

module.exports = router;