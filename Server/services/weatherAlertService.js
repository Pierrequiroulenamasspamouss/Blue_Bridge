const { User, DeviceToken } = require('../models');
const { sendMulticastPushNotification } = require('./firebaseService');
const { Op } = require('sequelize');

const WEATHER_ALERT_RADIUS = 30; // km

function calculateDistance(lat1, lon1, lat2, lon2) {
    const R = 6371; // Earth's radius in km
    const dLat = (lat2 - lat1) * Math.PI / 180;
    const dLon = (lon2 - lon1) * Math.PI / 180;
    const a = Math.sin(dLat/2) * Math.sin(dLat/2) +
              Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) * 
              Math.sin(dLon/2) * Math.sin(dLon/2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    return R * c;
}

function generateWeatherAlert(weatherData) {
    const alerts = [];
    
    // Check for rain in next 3 days
    if (weatherData.forecast.some(day => day.precipitation > 5)) {
        alerts.push({
            type: 'rain',
            message: 'Rain expected in the next few days. Plan accordingly.'
        });
    }
    
    // Check for high temperatures
    if (weatherData.forecast.some(day => day.temperature > 35)) {
        alerts.push({
            type: 'heat',
            message: 'High temperatures expected. Stay hydrated and take precautions.'
        });
    }
    
    // Check for extreme weather conditions
    if (weatherData.forecast.some(day => day.windSpeed > 30)) {
        alerts.push({
            type: 'wind',
            message: 'Strong winds expected. Secure outdoor items.'
        });
    }
    
    return alerts;
}

async function sendWeatherAlerts(weatherData) {
    try {
        const alerts = generateWeatherAlert(weatherData);
        if (alerts.length === 0) return;

        // Find users within radius
        const users = await User.findAll({
            where: {
                'location': {
                    [Op.ne]: null
                },
                'notificationPreferences.weatherAlerts': true
            },
            include: [{
                model: DeviceToken,
                where: { isActive: true },
                required: false
            }]
        });

        const notifications = [];
        for (const user of users) {
            const userLocation = user.location;
            const distance = calculateDistance(
                weatherData.latitude,
                weatherData.longitude,
                userLocation.latitude,
                userLocation.longitude
            );

            if (distance <= WEATHER_ALERT_RADIUS) {
                const deviceTokens = user.DeviceTokens.map(dt => dt.token);
                if (deviceTokens.length > 0) {
                    notifications.push({
                        tokens: deviceTokens,
                        alerts: alerts
                    });
                }
            }
        }

        // Send notifications
        for (const notification of notifications) {
            for (const alert of notification.alerts) {
                await sendMulticastPushNotification(
                    notification.tokens,
                    'Weather Alert',
                    alert.message,
                    { type: 'weather_alert', alertType: alert.type }
                );
            }
        }

        console.log(`Sent ${notifications.length} weather alerts`);
    } catch (error) {
        console.error('Error sending weather alerts:', error);
    }
}

module.exports = {
    sendWeatherAlerts
}; 