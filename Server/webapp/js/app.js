// Navigation function
function nav(page, params = {}) {
    setState({
        page,
        ...params,
        error: null,
        success: null
    }, render);
}

// Main render function
function render() {
    const app = document.getElementById('app');
    app.innerHTML = '';

    // Add header if not on auth pages
    if (!['login', 'register'].includes(state.page)) {
        app.appendChild(renderHeader());
    }

    // Show error/success messages
    if (state.error) {
        const errorDiv = document.createElement('div');
        errorDiv.className = 'alert alert-error';
        errorDiv.textContent = state.error;
        app.appendChild(errorDiv);
    }

    if (state.success) {
        const successDiv = document.createElement('div');
        successDiv.className = 'alert alert-success';
        successDiv.textContent = state.success;
        app.appendChild(successDiv);
    }

    // Render the current page
    if (window.pages[state.page]) {
        app.appendChild(window.pages[state.page]());
    } else {
        app.appendChild(window.pages.home());
    }
}

// Initialize the app
async function init() {
    // Check if user is already authenticated
    await checkAuth();

    // Load initial data if needed
    if (state.user && state.page === 'wells') {
        try {
            const data = await apiFetch('/wells');
            setState({ wells: data.wells || [] });
        } catch (error) {
            console.error('Failed to load wells:', error);
        }
    }

    // Initial render
    render();
}

// Make navigation available globally
window.nav = nav;

// Start the app
document.addEventListener('DOMContentLoaded', init);