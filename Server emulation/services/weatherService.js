const axios = require('axios');
const NodeCache = require('node-cache');

// Cache weather data for 24 hours (1 day)
const weatherCache = new NodeCache({ stdTTL: 86400 });

/**
 * Get weather data for a specific location
 * @param {number} lat - Latitude
 * @param {number} lon - Longitude
 * @returns {Promise<Object>} - Weather data
 */
const getWeatherData = async (lat, lon) => {
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
            // Add timestamp to show when it was cached
            return {
                ...cachedData,
                cachedAt: cachedData.cachedAt,
                fromCache: true
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
        
        // Add timestamp to the data
        const dataWithTimestamp = {
            ...response.data,
            cachedAt: new Date().toISOString(),
            fromCache: false
        };
        
        // Cache the data
        weatherCache.set(cacheKey, dataWithTimestamp);
        
        return dataWithTimestamp;
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
                ...cachedData,
                fromCache: true,
                isExpiredCache: true,
                error: error.message
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