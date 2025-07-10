function renderLocation() {
    if (!state.user) return nav('login');

    const div = document.createElement('div');
    div.className = 'card';
    div.style.maxWidth = '500px';
    div.style.margin = '20px auto';

    div.innerHTML = `
        <h2><i class="fas fa-map-marker-alt"></i> Update Location</h2>
        <form id="locForm">
            <div class="form-group">
                <label for="locLat">Latitude</label>
                <input type="number" id="locLat" class="form-control"
                    value="${state.user.location?.latitude || ''}" step="any" required>
            </div>
            <div class="form-group">
                <label for="locLon">Longitude</label>
                <input type="number" id="locLon" class="form-control"
                    value="${state.user.location?.longitude || ''}" step="any" required>
            </div>
            <button type="submit" class="btn" style="width: 100%;">
                <i class="fas fa-save"></i> Update Location
            </button>
        </form>
    `;

    div.querySelector('#locForm').onsubmit = async (e) => {
        e.preventDefault();

        const req = {
            latitude: parseFloat(document.getElementById('locLat').value),
            longitude: parseFloat(document.getElementById('locLon').value)
        };

        try {
            const data = await apiFetch('/user/location', 'PUT', req);

            if (data.user) {
                setState({
                    user: data.user,
                    page: 'profile',
                    success: 'Location updated successfully!'
                });
            }
        } catch (error) {
            setState({ error: error.message || 'Failed to update location' });
        }
    };

    return div;
}

window.pages = window.pages || {};
window.pages['update-location'] = renderLocation;