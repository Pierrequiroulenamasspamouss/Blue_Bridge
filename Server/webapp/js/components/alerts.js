function renderAlerts() {
    const container = document.createElement('div');
    container.id = 'alerts-container';

    function updateAlerts() {
        container.innerHTML = '';

        if (state.error) {
            const errorDiv = document.createElement('div');
            errorDiv.className = 'alert alert-error';
            errorDiv.textContent = state.error;
            container.appendChild(errorDiv);
        }

        if (state.success) {
            const successDiv = document.createElement('div');
            successDiv.className = 'alert alert-success';
            successDiv.textContent = state.success;
            container.appendChild(successDiv);
        }
    }

    // Initial render
    updateAlerts();

    // Return function to update alerts
    return updateAlerts;
}

// Initialize alerts
const updateAlerts = renderAlerts();

// Update alerts when state changes
window.addEventListener('stateChange', updateAlerts);

// Make available globally if needed
window.renderAlerts = renderAlerts;
window.updateAlerts = updateAlerts;