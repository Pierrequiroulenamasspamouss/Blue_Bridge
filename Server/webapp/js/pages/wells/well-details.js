function deleteWell(espId) {
    if (!confirm('Are you sure you want to delete this well?')) return;

    apiFetch(`/wells/${espId}`, 'DELETE')
        .then(() => {
            setState({
                page: 'wells',
                wells: [],
                success: 'Well deleted successfully!'
            });
        })
        .catch(error => {
            setState({ error: error.message || 'Failed to delete well' });
        });
}

function renderWellDetails() {
    if (!state.well) return nav('wells');

    const well = state.well;
    const div = document.createElement('div');
    div.className = 'card';

    let statusClass = '';
    if (well.wellStatus === 'active') statusClass = 'success';
    if (well.wellStatus === 'inactive') statusClass = 'danger';
    if (well.wellStatus === 'maintenance') statusClass = 'warning';

    div.innerHTML = `
        <h2><i class="fas fa-water"></i> ${well.wellName}</h2>

        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin-top: 20px;">
            <div>
                <h3>Details</h3>
                <p><strong>ESP ID:</strong> ${well.espId}</p>
                <p><strong>Status:</strong> <span class="${statusClass}">${well.wellStatus}</span></p>
                <p><strong>Type:</strong> ${well.wellWaterType}</p>
                <p><strong>Owner:</strong> ${well.wellOwner || 'Unknown'}</p>
            </div>
            <div>
                <h3>Measurements</h3>
                <p><strong>Capacity:</strong> ${well.wellCapacity || 'N/A'}</p>
                <p><strong>Water Level:</strong> ${well.wellWaterLevel || 'N/A'}</p>
                <p><strong>Consumption:</strong> ${well.wellWaterConsumption || 'N/A'}</p>
                <p><strong>Location:</strong> ${well.wellLocation?.latitude}, ${well.wellLocation?.longitude}</p>
            </div>
        </div>

        <div style="margin-top: 30px; display: flex; gap: 10px;">
            <button class="btn" onclick="nav('edit-well', { well: ${JSON.stringify(well)} })">
                <i class="fas fa-edit"></i> Edit
            </button>
            <button class="btn btn-danger" onclick="deleteWell('${well.espId}')">
                <i class="fas fa-trash"></i> Delete
            </button>
            <button class="btn btn-secondary" onclick="nav('wells')">
                <i class="fas fa-arrow-left"></i> Back
            </button>
        </div>
    `;

    return div;
}

window.pages = window.pages || {};
window.pages['well-details'] = renderWellDetails;
window.deleteWell = deleteWell;