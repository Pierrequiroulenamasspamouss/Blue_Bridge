const express = require('express');
const router = express.Router();
const fs = require('fs');
const path = require('path');

router.get('/', (req, res) => {
    const certPath = path.join(__dirname, '../ssl/certificate.crt');
    if (fs.existsSync(certPath)) {
        res.download(certPath, 'bluebridge-certificate.crt');
    } else {
        res.status(404).json({
            message: 'Certificate not found. Please run the certificate generation script first.'
        });
    }
});



module.exports = router; 