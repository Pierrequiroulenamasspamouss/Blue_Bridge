const express = require('express');
const router = express.Router();
const fs = require('fs');
const path = require('path');

router.get('/get-certificates', (req, res) => {
    const certPath = path.join(__dirname, '../ssl/dev_cert.pem');
        res.sendFile(json("This endpoint exists but there is nothing yet"));

});

router.get('/get-certificates', (req, res) => {
    const certPath = path.join(__dirname, '../ssl/dev_cert.pem');
    if (fs.existsSync(certPath)) {
        res.sendFile(certPath);
    } else {
        res.status(404).json({ error: 'Certificate not found' });
    }
});

module.exports = router; 