// Application state
const state = {
    user: null,
    page: 'home',
    wells: [],
    well: null,
    weather: null,
    nearbyUsers: [],
    notifications: [],
    bugReports: [],
    error: null,
    success: null,
    loading: false,
    pagination: { page: 1, limit: 10, total: 0, pages: 1 },
    wellFilters: {},
};

// State setter with optional callback
function setState(newState, callback) {
    Object.assign(state, newState);
    if (callback) callback();
}

// Make state available globally
window.state = state;
window.setState = setState;