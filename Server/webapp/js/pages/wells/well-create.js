function renderWellCreate() {
    const div = document.createElement('div');
    div.className = 'card';
    div.style.maxWidth = '600px';
    div.style.margin = '20px auto';

    div.innerHTML = `
        <h2><i class="fas fa-plus"></i> Create New Well</h2>
        <form id="createWellForm">
            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 15px;">
                <div class="form-group">
                    <label for="createEspId">ESP ID</label>
                    <input type="text" id="createEspId" class="form-control" required>
                </div>
                <div class="form-group">
                    <label for="createWellName">Name</label>
                    <input type="text" id="createWellName" class="form-control" required>
                </div>
            </div>

            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 15px; margin-top: 10px;">
                <div class="form-group">
                    <label for="createLat">Latitude</label>
                    <input type="number" id="createLat" class="form-control" step="any" required>
                </div>
                <div class="form-group">
                    <label for="createLon">Longitude</label>
                    <input type="number" id="createLon" class="form-control" step="any" required>
                </div>
            </div>

            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 15px; margin-top: 10px;">
                <div class="form-group">
                    <label for="createType">Water Type</label>
                    <select id="createType" class="form-control">
                        <option value="fresh">Fresh</option>
                        <option value="mineral">Mineral</option>
                        <option value="salt">Salt</option>
                    </select>
                </div>
                <div class="form-group">
                    <label for="createStatus">Status</label>
                    <select id="createStatus" class="form-control">
                        <option value="active">Active</option>
                        <option value="inactive">Inactive</option>
                        <option value="maintenance">Maintenance</option>
                    </select>
                </div>
            </div>

            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 15px; margin-top: 10px;">
                <div class="form-group">
                    <label for="createCapacity">Capacity (L)</label>
                    <input type="number" id="createCapacity" class="form-control">
                </div>
                <div class="form-group">
                    <label for="createLevel">Water Level (%)</label>
                    <input type="number" id="createLevel" class="form-control" min="0" max="100">
                </div>
            </div>

            <div class="form-group" style="margin-top: 10px;">
                <label for="createOwner">Owner</label>
                <input type="text" id="createOwner" class="form-control">
            </div>

            <button type="submit" class="btn btn-success" style="width: 100%; margin-top: 20px;">
                <i class="fas fa-save"></i> Create Well
            </button>
        </form>
    `;

    div.querySelector('#createWellForm').onsubmit = async (e) => {
        e.preventDefault();

        const req = {
            espId: document.getElementById('createEspId').value,
            wellName: document.getElementById('createWellName').value,
            wellLocation: {
                latitude: parseFloat(document.getElementById('createLat').value),
                longitude: parseFloat(document.getElementById('createLon').value)
            },
            wellWaterType: document.getElementById('createType').value,
            wellStatus: document.getElementById('createStatus').value,
            wellCapacity: document.getElementById('createCapacity').value,
            wellWaterLevel: document.getElementById('createLevel').value,
            wellOwner: document.getElementById('createOwner').value
        };

        try {
            const data = await apiFetch('/wells', 'POST', req);

            if (data.well) {
                setState({
                    page: 'well-details',
                    well: data.well,
                    success: 'Well created successfully!'
                });
            }
        } catch (error) {
            setState({ error: error.message || 'Failed to create well' });
        }
    };

    return div;
}

window.pages = window.pages || {};
window.pages['well-create'] = renderWellCreate;