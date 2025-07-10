const API_BASE = '/api';

async function apiFetch(endpoint, method = 'GET', body = null, requiresAuth = true) {
    setState({ loading: true });

    const headers = {
        'Content-Type': 'application/json',
    };

    if (requiresAuth && state.user?.token) {
        headers['Authorization'] = `Bearer ${state.user.token}`;
    }

    try {
        const response = await fetch(`${API_BASE}${endpoint}`, {
            method,
            headers,
            body: body ? JSON.stringify(body) : null,
            credentials: 'include'
        });

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.message || 'Request failed');
        }

        return await response.json();
    } catch (error) {
        console.error('API Error:', error);
        setState({ error: error.message });
        throw error;
    } finally {
        setState({ loading: false });
    }
}

// Make API functions available globally
window.apiFetch = apiFetch;