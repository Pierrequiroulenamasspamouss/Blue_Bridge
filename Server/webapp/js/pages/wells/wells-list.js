async function fetchWells() {
    try {
        const data = await apiFetch('/wells');
        setState({ wells: data.wells || [], pagination: data.pagination || state.pagination });
    } catch (error) {
        setState({ error: error.message || 'Failed to load wells' });
    }
}

function renderWells() {
    if (state.wells.length === 0 && !state.loading) {
        fetchWells();
    }

    const div = document.createElement('div');
    div.className = 'card';

    div.innerHTML = `
        <div style="display: flex; justify-content: space-between; align-items: center;">
            <h2><i class="fas fa-water"></i> Water Wells</h2>
            <button class="btn btn-success" onclick="nav('create-well')">
                <i class="fas fa-plus"></i> Add Well
            </button>
        </div>

        <form id="wellFilterForm" style="margin: 20px 0;">
            <div style="display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 10px;">
                <div class="form-group">
                    <input type="text" id="filterName" class="form-control" placeholder="Well Name">
                </div>
                <div class="form-group">
                    <select id="filterStatus" class="form-control">
                        <option value="">All Statuses</option>
                        <option value="active">Active</option>
                        <option value="inactive">Inactive</option>
                        <option value="maintenance">Maintenance</option>
                    </select>
                </div>
                <div class="form-group">
                    <select id="filterType" class="form-control">
                        <option value="">All Types</option>
                        <option value="fresh">Fresh</option>
                        <option value="mineral">Mineral</option>
                        <option value="salt">Salt</option>
                    </select>
                </div>
            </div>
            <button type="submit" class="btn" style="width: 100%; margin-top: 10px;">
                <i class="fas fa-filter"></i> Apply Filters
            </button>
        </form>
    `;

    if (state.loading && state.wells.length === 0) {
        div.innerHTML += '<p>Loading wells...</p>';
    } else if (state.wells.length === 0) {
        div.innerHTML += '<p>No wells found. Try adjusting your filters.</p>';
    } else {
        const wellsGrid = document.createElement('div');
        wellsGrid.className = 'grid';

        state.wells.forEach(well => {
            const wellCard = document.createElement('div');
            wellCard.className = 'card well-card';
            wellCard.onclick = () => nav('well-details', { well });

            let statusClass = '';
            if (well.wellStatus === 'active') statusClass = 'success';
            if (well.wellStatus === 'inactive') statusClass = 'danger';
            if (well.wellStatus === 'maintenance') statusClass = 'warning';

            wellCard.innerHTML = `
                <h3>${well.wellName}</h3>
                <p><strong>Status:</strong>
                    <span class="${statusClass}">${well.wellStatus}</span>
                </p>
                <p><strong>Type:</strong> ${well.wellWaterType || 'Unknown'}</p>
                <p><strong>Capacity:</strong> ${well.wellCapacity || 'N/A'}</p>
                <p><strong>Water Level:</strong> ${well.wellWaterLevel || 'N/A'}</p>
            `;

            wellsGrid.appendChild(wellCard);
        });

        div.appendChild(wellsGrid);
    }

    div.querySelector('#wellFilterForm').onsubmit = (e) => {
        e.preventDefault();
        setState({
            wellFilters: {
                wellName: document.getElementById('filterName').value,
                wellStatus: document.getElementById('filterStatus').value,
                wellWaterType: document.getElementById('filterType').value
            },
            wells: []
        });
    };

    return div;
}

window.pages = window.pages || {};
window.pages.wells = renderWells;