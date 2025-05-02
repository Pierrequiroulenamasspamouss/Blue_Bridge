const greenlock = require('greenlock-express');
const path = require('path');
require('dotenv').config();

// Ensure these environment variables are set in your .env file
const DOMAIN = process.env.DOMAIN;
const EMAIL = process.env.EMAIL;

if (!DOMAIN || !EMAIL) {
    console.error('Please set DOMAIN and EMAIL in your .env file');
    process.exit(1);
}

greenlock.init({
    packageRoot: path.join(__dirname, '..'),
    configDir: path.join(__dirname, '../ssl'),
    maintainerEmail: EMAIL,
    cluster: false
}).ready((glx) => {
    glx.serveApp(app);

    // Get SSL certificates
    glx.manager.defaults({
        subscriberEmail: EMAIL,
        agreeToTerms: true,
        challenges: {
            'http-01': {
                module: 'acme-http-01-standalone'
            }
        }
    });

    glx.manager.certificates.add({
        subject: DOMAIN,
        altnames: [DOMAIN]
    }).then(() => {
        console.log('Certificates generated successfully!');
        process.exit(0);
    }).catch((err) => {
        console.error('Error generating certificates:', err);
        process.exit(1);
    });
}); 