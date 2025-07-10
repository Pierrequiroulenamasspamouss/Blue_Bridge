function getWeatherIcon(icon) {
    const icons = {
        '01d': '☀️', '01n': '🌙', '02d': '⛅', '02n': '☁️',
        '03d': '☁️', '03n': '☁️', '04d': '☁️', '04n': '☁️',
        '09d': '🌧️', '09n': '🌧️', '10d': '🌦️', '10n': '🌧️',
        '11d': '⛈️', '11n': '⛈️', '13d': '❄️', '13n': '❄️',
        '50d': '🌫️', '50n': '🌫️'
    };
    return icons[icon] || '🌡️';
}

async function fetchWeather() {
    try {
        const data = await apiFetch('/weather');
        setState({ weather: data.weather });
    } catch (error) {
        setState({ error: error.message || 'Failed to load weather data' });
    }
}

function renderWeather() {
    if (!state.weather && !state.loading) {
        fetchWeather();
    }

    const div = document.createElement('div');
    div.className = 'card';

    div.innerHTML = `
        <h2><i class="fas fa-cloud-sun"></i> Weather Forecast</h2>
        ${state.loading ? '<p>Loading weather data...</p>' : ''}
    `;

    if (state.weather) {
        const weather = state.weather;
        div.innerHTML += `
            <div class="weather-card">
                <div class="weather-main">
                    <span class="weather-icon">${getWeatherIcon(weather.icon)}</span>
                    <span class="weather-temp">${weather.temperature}°C</span>
                    <span class="weather-desc">${weather.description}</span>
                </div>
                <div class="weather-details">
                    <div class="weather-detail">
                        <i class="fas fa-tint"></i>
                        <p>Humidity</p>
                        <p>${weather.humidity}%</p>
                    </div>
                    <div class="weather-detail">
                        <i class="fas fa-wind"></i>
                        <p>Wind</p>
                        <p>${weather.windSpeed} km/h</p>
                    </div>
                    <div class="weather-detail">
                        <i class="fas fa-cloud-rain"></i>
                        <p>Rain</p>
                        <p>${weather.rainAmount} mm</p>
                    </div>
                </div>
            </div>

            <div style="margin-top: 20px;">
                <h3>3-Day Forecast</h3>
                <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 15px; margin-top: 10px;">
                    ${weather.forecast ? weather.forecast.slice(0, 3).map(day => `
                        <div class="card" style="text-align: center;">
                            <p><strong>${new Date(day.date).toLocaleDateString('en-US', { weekday: 'short' })}</strong></p>
                            <p style="font-size: 2rem;">${getWeatherIcon(day.icon)}</p>
                            <p>${day.temperature}°C</p>
                            <p>${day.description}</p>
                        </div>
                    `).join('') : '<p>No forecast data available.</p>'}
                </div>
            </div>
        `;
    }

    return div;
}

window.pages = window.pages || {};
window.pages.weather = renderWeather;