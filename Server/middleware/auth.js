const { User } = require('../models');

const validateToken = async (req, res, next) => {
    const { userId, loginToken } = req.body;
    if (!userId || !loginToken) {
        return res.status(401).json({ 
            status: 'error', 
            message: 'User ID and token required' 
        });
    }

    try {
        const user = await User.findOne({ where: { userId } });
        if (!user || user.loginToken !== loginToken) {
            return res.status(401).json({ 
                status: 'error', 
                message: 'Invalid token' 
            });
        }
        req.user = user;
        next();
    } catch (error) {
        console.error('Token validation error:', error);
        res.status(500).json({ 
            status: 'error', 
            message: 'Authentication error' 
        });
    }
};

module.exports = { validateToken }; 