function renderWaterNeeds() {
    if (!state.user) return nav('login');

    const div = document.createElement('div');
    div.className = 'card';
    div.style.maxWidth = '500px';
    div.style.margin = '20px auto';

    div.innerHTML = `
        <h2><i class="fas fa-tint"></i> Update Water Needs</h2>
        <form id="waterNeedsForm">
            <div class="form-group">
                <label for="waterNeedsInput">Water Needs (JSON format)</label>
                <textarea id="waterNeedsInput" class="form-control" rows="6">${
                    JSON.stringify(state.user.waterNeeds || [
                        { usageType: "drinking", amount: 2 },
                        { usageType: "cooking", amount: 1 }
                    ], null, 2)
                }</textarea>
                <small>Example: [{"usageType": "drinking", "amount": 2}, {"usageType": "cooking", "amount": 1}]</small>
            </div>
            <button type="submit" class="btn" style="width: 100%;">
                <i class="fas fa-save"></i> Update Water Needs
            </button>
        </form>
    `;

    div.querySelector('#waterNeedsForm').onsubmit = async (e) => {
        e.preventDefault();

        let waterNeeds;
        try {
            waterNeeds = JSON.parse(document.getElementById('waterNeedsInput').value);
        } catch (err) {
            setState({ error: 'Invalid JSON format' });
            return;
        }

        try {
            const data = await apiFetch('/user/water-needs', 'PUT', { waterNeeds });

            if (data.user) {
                setState({
                    user: data.user,
                    page: 'profile',
                    success: 'Water needs updated successfully!'
                });
            }
        } catch (error) {
            setState({ error: error.message || 'Failed to update water needs' });
        }
    };

    return div;
}

window.pages = window.pages || {};
window.pages['update-water-needs'] = renderWaterNeeds;