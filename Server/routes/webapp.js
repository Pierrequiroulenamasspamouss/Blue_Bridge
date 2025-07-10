const express = require('express');
const router = express.Router();
const path = require('path');
const under_construction = true

if (under_construction) {
router.use(express.static(path.join(__dirname, '../html')))
router.get('*', (req, res) => {
    res.sendFile(path.join(__dirname, '../html/website_under_construction.html'));
});
}
else {
    // Serve static files from the webapp directory
    router.use(express.static(path.join(__dirname, '../webapp')));

    // Webapp entry point
    router.get('*', (req, res) => {
        res.sendFile(path.join(__dirname, '../webapp/index.html'));
    });
}



module.exports = router;