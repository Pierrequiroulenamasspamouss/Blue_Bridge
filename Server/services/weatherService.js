const axios = require('axios');
const NodeCache = require('node-cache');

// Cache weather data for 24 hours (1 day)
const weatherCache = new NodeCache({ stdTTL: 86400 });

/**
 * Get weather data for a specific location
 * @param {Object} location - Location object with latitude and longitude
 * @param {string} userId - User ID for authentication
 * @param {string} loginToken - Login token for authentication
 * @returns {Promise<Object>} - Weather data in the format matching WeatherResponse
 */
const getWeatherData = async (location, userId, loginToken) => {
    if (!location || typeof location !== 'object') {
        throw new Error('Location must be an object with latitude and longitude');
    }

    const lat = location.latitude;
    const lon = location.longitude;

    if (typeof lat !== 'number' || typeof lon !== 'number' || isNaN(lat) || isNaN(lon)) {
        throw new Error('Location must have valid latitude and longitude numbers');
    }

    try {
        // Create a cache key from the coordinates
        // Round to 2 decimal places to avoid too many cache entries for nearby locations
        const roundedLat = Math.round(lat * 100) / 100;
        const roundedLon = Math.round(lon * 100) / 100;
        const cacheKey = `${roundedLat},${roundedLon}`;
        
        // Check if we have cached data and it's not expired
        const cachedData = weatherCache.get(cacheKey);
        if (cachedData) {
            console.log('Returning cached weather data for:', cacheKey);
            return {
                status: 'success',
                message: 'Weather data retrieved successfully',
                data: cachedData
            };
        }
        
        // If no cached data, fetch from API
        console.log('Fetching fresh weather data for:', cacheKey);
        
        // Set a timeout for the API request (5 seconds)
        const response = await axios.get('https://api.openweathermap.org/data/2.5/weather', {
            params: {
                lat,
                lon,
                appid: process.env.OPENWEATHER_API_KEY,
                units: 'metric'
            },
            timeout: 5000
        });

        // Get current date and time
        const now = new Date();
        const date = now.toISOString().split('T')[0]; // YYYY-MM-DD
        const time = now.toTimeString().split(' ')[0]; // HH:MM:SS
        
        // Format the data to match Android app's WeatherData class
        const weatherData = {
            date: date,
            time: time,
            temperature: response.data.main.temp,
            feelsLike: response.data.main.feels_like,
            minTemperature: response.data.main.temp_min,
            maxTemperature: response.data.main.temp_max,
            humidity: response.data.main.humidity,
            description: response.data.weather[0].description,
            icon: response.data.weather[0].icon,
            windSpeed: response.data.wind.speed,
            rainAmount: response.data.rain ? response.data.rain['1h'] || 0 : 0,
            pressure: response.data.main.pressure,
            windDirection: response.data.wind.deg,
            sunset: new Date(response.data.sys.sunset * 1000) // Convert Unix timestamp to Date
        };
        
        // Cache the data
        weatherCache.set(cacheKey, weatherData);
        
        return {
            status: 'success',
            message: 'Weather data retrieved successfully',
            data: weatherData
        };
    } catch (error) {
        console.error('Error fetching weather data:', error);
        
        // Check if there's expired cached data we can use as a fallback
        const roundedLat = Math.round(lat * 100) / 100;
        const roundedLon = Math.round(lon * 100) / 100;
        const cacheKey = `${roundedLat},${roundedLon}`;
        
        const cachedData = weatherCache.get(cacheKey);
        if (cachedData) {
            console.log('Using expired cache as fallback for:', cacheKey);
            return {
                status: 'success',
                message: 'Using cached weather data',
                data: cachedData
            };
        }
        
        // If we have no cached data at all, handle the error gracefully
        if (error.response) {
            // The request was made and the server responded with a status code
            // that falls out of the range of 2xx
            throw new Error(`Weather API error: ${error.response.status} ${error.response.data.message || error.message}`);
        } else if (error.request) {
            // The request was made but no response was received
            throw new Error('Weather service is not responding. Please try again later.');
        } else {
            // Something happened in setting up the request that triggered an Error
            throw new Error(`Weather request failed: ${error.message}`);
        }
    }
};

// Clear expired cache entries
const clearExpiredCache = () => {
    const stats = weatherCache.getStats();
    console.log(`Weather cache stats: ${stats.keys} keys, ${stats.hits} hits, ${stats.misses} misses`);
    weatherCache.flushStats();
};

// Run cleanup every hour
setInterval(clearExpiredCache, 3600000);

module.exports = {
    getWeatherData
}; 