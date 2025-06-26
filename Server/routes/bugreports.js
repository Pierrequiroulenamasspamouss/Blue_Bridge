const express = require('express');
const router = express.Router();
const fs = require('fs');
const path = require('path');

const BUGREPORTS_PATH = path.join(__dirname, '../data/bugreports.json');

// Ensure the data directory exists
const dataDir = path.join(__dirname, '../data');
if (!fs.existsSync(dataDir)) {
    fs.mkdirSync(dataDir, { recursive: true });
}

// POST /api/bugreports
router.post('/', (req, res) => {
    const { name, description, category, extra } = req.body;
    if (!name || !description || !category) {
        return res.status(400).json({ status: 'error', message: 'Missing required fields: name, description, category' });
    }
    const report = {
        name,
        description,
        category,
        extra: extra || {},
        timestamp: new Date().toISOString()
    };
    let reports = [];
    if (fs.existsSync(BUGREPORTS_PATH)) {
        try {
            reports = JSON.parse(fs.readFileSync(BUGREPORTS_PATH, 'utf8'));
        } catch (e) {
            // If file is corrupted, start fresh
            reports = [];
        }
    }
    reports.push(report);
    fs.writeFileSync(BUGREPORTS_PATH, JSON.stringify(reports, null, 2));
    res.json({ status: 'success', message: 'Bug report submitted' });
});

module.exports = router; 