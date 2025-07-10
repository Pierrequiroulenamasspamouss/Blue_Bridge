function renderLoadingOverlay() {
    const overlay = document.createElement('div');
    overlay.id = 'loadingOverlay';
    overlay.className = 'loading-overlay';
    overlay.style.display = 'none';

    overlay.innerHTML = `
        <div class="spinner"></div>
    `;

    document.body.appendChild(overlay);

    // Update visibility based on state
    function updateLoading() {
        overlay.style.display = state.loading ? 'flex' : 'none';
    }

    // Initial render
    updateLoading();

    // Return function to update loading state
    return updateLoading;
}

// Initialize loading overlay
const updateLoadingOverlay = renderLoadingOverlay();

// Update loading overlay when state changes
window.addEventListener('stateChange', updateLoadingOverlay);