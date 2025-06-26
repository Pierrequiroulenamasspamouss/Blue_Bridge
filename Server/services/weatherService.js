const axios = require('axios');
const NodeCache = require('node-cache');

// Cache weather data for 1 hour (free API has frequent updates)
const weatherCache = new NodeCache({ stdTTL: 3600 });

/**
 * Get 5-day weather forecast for a location
 * @param {Object} location - { latitude: number, longitude: number }
 * @returns {Promise<Object>} - Formatted weather data
 */
const getWeatherData = async (location) => {
    // Input validation
    if (!location || typeof location !== 'object') {
        throw new Error('Location must be an object with latitude and longitude');
    }

    const { latitude: lat, longitude: lon } = location;

    if (typeof lat !== 'number' || typeof lon !== 'number' || isNaN(lat) || isNaN(lon)) {
        throw new Error('Invalid coordinates - must be numbers');
    }

    // Create cache key (rounded to 2 decimal places)
    const cacheKey = `${lat.toFixed(2)},${lon.toFixed(2)}`;

    // Try cache first
    const cachedData = weatherCache.get(cacheKey);
    if (cachedData) {
        console.log('Using cached weather data for', cacheKey);
        return formatResponse(cachedData);
    }

    try {
        // Free API endpoint (5-day forecast)
        const response = await axios.get('https://api.openweathermap.org/data/2.5/forecast', {
            params: {
                lat,
                lon,
                appid: process.env.OPENWEATHER_API_KEY,
                units: 'metric',
                cnt: 40 // Max items for free tier (5 days * 8 readings/day)
            },
            timeout: 5000
        });

        // Process the 3-hour interval data into daily forecasts
        const dailyData = processForecastData(response.data.list);

        // Cache the processed data
        weatherCache.set(cacheKey, dailyData);

        return formatResponse(dailyData);
    } catch (error) {
        console.error('Weather API error:', error.message);

        // Fallback to expired cache if available
        const expiredData = weatherCache.get(cacheKey, true); // allowExpired: true
        if (expiredData) {
            console.log('Using expired cache as fallback');
            return formatResponse(expiredData, 'Using slightly outdated data');
        }

        throw handleApiError(error);
    }
};

/**
 * Process 3-hour interval data into daily summaries
 */
const processForecastData = (forecastList) => {
    const days = {};

    forecastList.forEach(item => {
        const date = new Date(item.dt * 1000).toISOString().split('T')[0];

        if (!days[date]) {
            days[date] = {
                date,
                temps: [],
                feelsLike: [],
                humidity: [],
                descriptions: new Set(),
                icons: new Set(),
                windSpeeds: [],
                rainAmounts: [],
                pressures: []
            };
        }

        const day = days[date];
        day.temps.push(item.main.temp);
        day.feelsLike.push(item.main.feels_like);
        day.humidity.push(item.main.humidity);
        day.descriptions.add(item.weather[0].description);
        day.icons.add(item.weather[0].icon);
        day.windSpeeds.push(item.wind.speed);
        day.rainAmounts.push(item.rain?.['3h'] || 0);
        day.pressures.push(item.main.pressure);
    });

    // Convert to array and calculate daily averages
    return Object.values(days).map(day => ({
        date: day.date,
        time: '12:00:00', // Representative time
        temperature: average(day.temps),
        feelsLike: average(day.feelsLike),
        minTemperature: Math.min(...day.temps),
        maxTemperature: Math.max(...day.temps),
        humidity: average(day.humidity),
        description: Array.from(day.descriptions).join(', '),
        icon: `https://openweathermap.org/img/wn/${Array.from(day.icons)[0]}@2x.png`,
        windSpeed: average(day.windSpeeds),
        rainAmount: sum(day.rainAmounts),
        pressure: average(day.pressures),
        windDirection: 0 // Not available in free API
    }));
};

// Helper functions
const average = arr => arr.reduce((a, b) => a + b, 0) / arr.length;
const sum = arr => arr.reduce((a, b) => a + b, 0);

const formatResponse = (data, message = 'Weather data retrieved successfully') => ({
    status: 'success',
    message,
    data
});

const handleApiError = (error) => {
    if (error.response) {
        // API responded with error status
        const { status, data } = error.response;
        return new Error(`Weather API error: ${status} - ${data.message || 'Unknown error'}`);
    } else if (error.request) {
        return new Error('Weather service unavailable - please try again later');
    } else {
        return new Error(`Weather request failed: ${error.message}`);
    }
};

// Cache maintenance
setInterval(() => {
    const stats = weatherCache.getStats();
    console.log(`Cache stats: ${stats.keys} entries, ${stats.hits} hits`);
    weatherCache.flushStats();
}, 3600000); // Log stats hourly

module.exports = { getWeatherData };