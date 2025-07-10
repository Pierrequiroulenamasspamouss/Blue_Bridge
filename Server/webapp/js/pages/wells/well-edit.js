function renderWellEdit() {
    if (!state.well) return nav('wells');

    const well = state.well;
    const div = document.createElement('div');
    div.className = 'card';
    div.style.maxWidth = '600px';
    div.style.margin = '20px auto';

    div.innerHTML = `
        <h2><i class="fas fa-edit"></i> Edit Well</h2>
        <form id="editWellForm">
            <div class="form-group">
                <label for="editWellName">Name</label>
                <input type="text" id="editWellName" class="form-control" value="${well.wellName}" required>
            </div>

            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 15px; margin-top: 10px;">
                <div class="form-group">
                    <label for="editType">Water Type</label>
                    <select id="editType" class="form-control">
                        <option value="fresh" ${well.wellWaterType === 'fresh' ? 'selected' : ''}>Fresh</option>
                        <option value="mineral" ${well.wellWaterType === 'mineral' ? 'selected' : ''}>Mineral</option>
                        <option value="salt" ${well.wellWaterType === 'salt' ? 'selected' : ''}>Salt</option>
                    </select>
                </div>
                <div class="form-group">
                    <label for="editStatus">Status</label>
                    <select id="editStatus" class="form-control">
                        <option value="active" ${well.wellStatus === 'active' ? 'selected' : ''}>Active</option>
                        <option value="inactive" ${well.wellStatus === 'inactive' ? 'selected' : ''}>Inactive</option>
                        <option value="maintenance" ${well.wellStatus === 'maintenance' ? 'selected' : ''}>Maintenance</option>
                    </select>
                </div>
            </div>

            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 15px; margin-top: 10px;">
                <div class="form-group">
                    <label for="editCapacity">Capacity (L)</label>
                    <input type="number" id="editCapacity" class="form-control" value="${well.wellCapacity || ''}">
                </div>
                <div class="form-group">
                    <label for="editLevel">Water Level (%)</label>
                    <input type="number" id="editLevel" class="form-control" value="${well.wellWaterLevel || ''}" min="0" max="100">
                </div>
            </div>

            <button type="submit" class="btn" style="width: 100%; margin-top: 20px;">
                <i class="fas fa-save"></i> Save Changes
            </button>
        </form>
    `;

    div.querySelector('#editWellForm').onsubmit = async (e) => {
        e.preventDefault();

        const req = {
            wellName: document.getElementById('editWellName').value,
            wellWaterType: document.getElementById('editType').value,
            wellStatus: document.getElementById('editStatus').value,
            wellCapacity: document.getElementById('editCapacity').value,
            wellWaterLevel: document.getElementById('editLevel').value
        };

        try {
            const data = await apiFetch(`/wells/${well.espId}`, 'PUT', req);

            if (data.well) {
                setState({
                    page: 'well-details',
                    well: data.well,
                    success: 'Well updated successfully!'
                });
            }
        } catch (error) {
            setState({ error: error.message || 'Failed to update well' });
        }
    };

    return div;
}

window.pages = window.pages || {};
window.pages['well-edit'] = renderWellEdit;