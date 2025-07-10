// BlueBridge WebApp - Improved Version
const API_BASE = '/api';
const app = document.getElementById('app');
const loadingOverlay = document.getElementById('loadingOverlay');

// Application state
let state = {
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

// Helper function to set state and re-render
function setState(newState) {
    state = { ...state, ...newState };
    render();
}

// Show loading overlay
function setLoading(loading) {
    loadingOverlay.style.display = loading ? 'flex' : 'none';
    setState({ loading });
}

// Centralized API request handler
async function apiFetch(endpoint, method = 'GET', body = null, requiresAuth = true) {
    setLoading(true);
    
    const headers = {
        'Content-Type': 'application/json',
    };
    
    if (requiresAuth && state.token) {
        headers['Authorization'] = `Bearer ${state.token}`;
    }
    
    try {
        const response = await fetch(`${API_BASE}${endpoint}`, {
            method,
            headers,
            body: body ? JSON.stringify(body) : null,
            credentials: 'include' // For cookies
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
        setLoading(false);
    }
}

// Check if user is authenticated
async function checkAuth() {
    try {
        const data = await apiFetch('/auth/check', 'GET', null, false);
        if (data.authenticated && data.user) {
            setState({ user: data.user });
            return true;
        }
        return false;
    } catch {
        return false;
    }
}

// Navigation function
function nav(page, params = {}) {
    setState({ page, ...params, error: null, success: null });
}

// Main render function
function render() {
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
    switch (state.page) {
        case 'login': renderLogin(); break;
        case 'register': renderRegister(); break;
        case 'profile': renderProfile(); break;
        case 'update-location': renderUpdateLocation(); break;
        case 'update-water-needs': renderUpdateWaterNeeds(); break;
        case 'wells': renderWells(); break;
        case 'well-details': renderWellDetails(); break;
        case 'create-well': renderCreateWell(); break;
        case 'edit-well': renderEditWell(); break;
        case 'weather': renderWeather(); break;
        case 'nearby': renderNearby(); break;
        case 'notifications': renderNotifications(); break;
        case 'bugreports': renderBugReports(); break;
        default: renderHome(); break;
    }
}

// Render header component
function renderHeader() {
    const header = document.createElement('header');
    header.className = 'header';
    
    const titleDiv = document.createElement('div');
    titleDiv.innerHTML = `
        <h1>BlueBridge</h1>
        <p>Smart Water Management</p>
    `;
    
    const navDiv = document.createElement('div');
    navDiv.className = 'nav';
    
    if (state.user) {
        navDiv.innerHTML = `
            <button class="nav-btn ${state.page === 'home' ? 'active' : ''}" onclick="nav('home')">
                <i class="fas fa-home"></i> Home
            </button>
            <button class="nav-btn ${state.page === 'wells' ? 'active' : ''}" onclick="nav('wells')">
                <i class="fas fa-water"></i> Wells
            </button>
            <button class="nav-btn ${state.page === 'weather' ? 'active' : ''}" onclick="nav('weather')">
                <i class="fas fa-cloud-sun"></i> Weather
            </button>
            <button class="nav-btn ${state.page === 'nearby' ? 'active' : ''}" onclick="nav('nearby')">
                <i class="fas fa-users"></i> Nearby
            </button>
            <button class="nav-btn ${state.page === 'profile' ? 'active' : ''}" onclick="nav('profile')">
                <i class="fas fa-user"></i> Profile
            </button>
            <button class="nav-btn" onclick="logout()">
                <i class="fas fa-sign-out-alt"></i> Logout
            </button>
        `;
    } else {
        navDiv.innerHTML = `
            <button class="nav-btn ${state.page === 'home' ? 'active' : ''}" onclick="nav('home')">
                <i class="fas fa-home"></i> Home
            </button>
            <button class="nav-btn ${state.page === 'login' ? 'active' : ''}" onclick="nav('login')">
                <i class="fas fa-sign-in-alt"></i> Login
            </button>
            <button class="nav-btn ${state.page === 'register' ? 'active' : ''}" onclick="nav('register')">
                <i class="fas fa-user-plus"></i> Register
            </button>
        `;
    }
    
    header.appendChild(titleDiv);
    header.appendChild(navDiv);
    return header;
}

// Render home page
function renderHome() {
    const div = document.createElement('div');
    div.className = 'card';
    
    if (state.user) {
        div.innerHTML = `
            <h2>Welcome back, ${state.user.firstName}!</h2>
            <p>What would you like to do today?</p>
            <div class="grid" style="margin-top: 20px;">
                <div class="card well-card" onclick="nav('wells')">
                    <h3><i class="fas fa-water"></i> Browse Wells</h3>
                    <p>View and manage water wells in your area</p>
                </div>
                <div class="card well-card" onclick="nav('weather')">
                    <h3><i class="fas fa-cloud-sun"></i> Check Weather</h3>
                    <p>View current weather conditions</p>
                </div>
                <div class="card well-card" onclick="nav('nearby')">
                    <h3><i class="fas fa-users"></i> Nearby Users</h3>
                    <p>See other users in your area</p>
                </div>
                <div class="card well-card" onclick="nav('profile')">
                    <h3><i class="fas fa-user-cog"></i> Your Profile</h3>
                    <p>Update your account settings</p>
                </div>
            </div>
        `;
    } else {
        div.innerHTML = `
            <h2>Welcome to BlueBridge</h2>
            <p>Please login or register to access all features</p>
            <div style="display: flex; gap: 10px; margin-top: 20px;">
                <button class="btn" onclick="nav('login')">
                    <i class="fas fa-sign-in-alt"></i> Login
                </button>
                <button class="btn btn-success" onclick="nav('register')">
                    <i class="fas fa-user-plus"></i> Register
                </button>
            </div>
        `;
    }
    
    app.appendChild(div);
}

// Render login page
function renderLogin() {
    const div = document.createElement('div');
    div.className = 'card';
    div.style.maxWidth = '500px';
    div.style.margin = '50px auto';
    
    div.innerHTML = `
        <h2><i class="fas fa-sign-in-alt"></i> Login</h2>
        <form id="loginForm">
            <div class="form-group">
                <label for="loginEmail">Email</label>
                <input type="email" id="loginEmail" class="form-control" required autocomplete="username">
            </div>
            <div class="form-group">
                <label for="loginPassword">Password</label>
                <input type="password" id="loginPassword" class="form-control" required autocomplete="current-password">
            </div>
            <button type="submit" class="btn" style="width: 100%;">
                <i class="fas fa-sign-in-alt"></i> Login
            </button>
        </form>
        <p style="text-align: center; margin-top: 15px;">
            Don't have an account? <a href="#" onclick="nav('register')">Register here</a>
        </p>
    `;
    
    app.appendChild(div);
    
    document.getElementById('loginForm').onsubmit = async (e) => {
        e.preventDefault();
        const email = document.getElementById('loginEmail').value;
        const password = await hashPassword(document.getElementById('loginPassword').value);
        
        try {
            const data = await apiFetch('/auth/login', 'POST', { email, password }, false);
            
            if (data.user) {
                setState({ 
                    user: data.user, 
                    page: 'home',
                    success: 'Login successful!'
                });
            }
        } catch (error) {
            setState({ error: error.message || 'Login failed' });
        }
    };
}

// Render register page
function renderRegister() {
    const div = document.createElement('div');
    div.className = 'card';
    div.style.maxWidth = '500px';
    div.style.margin = '50px auto';
    
    div.innerHTML = `
        <h2><i class="fas fa-user-plus"></i> Register</h2>
        <form id="registerForm">
            <div class="form-group">
                <label for="regEmail">Email</label>
                <input type="email" id="regEmail" class="form-control" required>
            </div>
            <div class="form-group">
                <label for="regPassword">Password</label>
                <input type="password" id="regPassword" class="form-control" required>
            </div>
            <div class="form-group">
                <label for="regFirstName">First Name</label>
                <input type="text" id="regFirstName" class="form-control" required>
            </div>
            <div class="form-group">
                <label for="regLastName">Last Name</label>
                <input type="text" id="regLastName" class="form-control" required>
            </div>
            <div class="form-group">
                <label for="regUsername">Username</label>
                <input type="text" id="regUsername" class="form-control" required>
            </div>
            <div class="form-group">
                <label for="regLat">Latitude</label>
                <input type="number" id="regLat" class="form-control" step="any" required>
            </div>
            <div class="form-group">
                <label for="regLon">Longitude</label>
                <input type="number" id="regLon" class="form-control" step="any" required>
            </div>
            <div class="form-group">
                <label for="regPhone">Phone Number</label>
                <input type="tel" id="regPhone" class="form-control">
            </div>
            <button type="submit" class="btn btn-success" style="width: 100%;">
                <i class="fas fa-user-plus"></i> Register
            </button>
        </form>
        <p style="text-align: center; margin-top: 15px;">
            Already have an account? <a href="#" onclick="nav('login')">Login here</a>
        </p>
    `;
    
    app.appendChild(div);
    
    document.getElementById('registerForm').onsubmit = async (e) => {
        e.preventDefault();
        
        const req = {
            email: document.getElementById('regEmail').value,
            password: await hashPassword(document.getElementById('regPassword').value),
            firstName: document.getElementById('regFirstName').value,
            lastName: document.getElementById('regLastName').value,
            username: document.getElementById('regUsername').value,
            location: { 
                latitude: parseFloat(document.getElementById('regLat').value),
                longitude: parseFloat(document.getElementById('regLon').value)
            },
            phoneNumber: document.getElementById('regPhone').value,
            role: 'user'
        };
        
        try {
            const data = await apiFetch('/auth/register', 'POST', req, false);
            
            if (data.user) {
                setState({ 
                    user: data.user, 
                    page: 'home',
                    success: 'Registration successful! Please login.'
                });
                nav('login');
            }
        } catch (error) {
            setState({ error: error.message || 'Registration failed' });
        }
    };
}

// Render profile page
function renderProfile() {
    if (!state.user) return nav('login');
    
    const div = document.createElement('div');
    div.className = 'card';
    
    div.innerHTML = `
        <h2><i class="fas fa-user"></i> Your Profile</h2>
        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin-top: 20px;">
            <div>
                <h3>Personal Information</h3>
                <p><strong>Name:</strong> ${state.user.firstName} ${state.user.lastName}</p>
                <p><strong>Email:</strong> ${state.user.email}</p>
                <p><strong>Username:</strong> ${state.user.username}</p>
                <p><strong>Phone:</strong> ${state.user.phoneNumber || 'Not provided'}</p>
            </div>
            <div>
                <h3>Location</h3>
                <p><strong>Coordinates:</strong> 
                    ${state.user.location?.latitude || 'N/A'}, 
                    ${state.user.location?.longitude || 'N/A'}
                </p>
                <p><strong>Water Needs:</strong> 
                    ${state.user.waterNeeds?.length ? state.user.waterNeeds.map(n => `${n.usageType}: ${n.amount}L`).join(', ') : 'Not specified'}
                </p>
            </div>
        </div>
        <div style="margin-top: 30px; display: flex; gap: 10px;">
            <button class="btn" onclick="nav('update-location')">
                <i class="fas fa-map-marker-alt"></i> Update Location
            </button>
            <button class="btn" onclick="nav('update-water-needs')">
                <i class="fas fa-tint"></i> Update Water Needs
            </button>
        </div>
    `;
    
    app.appendChild(div);
}

// Render update location page
function renderUpdateLocation() {
    if (!state.user) return nav('login');
    
    const div = document.createElement('div');
    div.className = 'card';
    div.style.maxWidth = '500px';
    div.style.margin = '20px auto';
    
    div.innerHTML = `
        <h2><i class="fas fa-map-marker-alt"></i> Update Location</h2>
        <form id="locForm">
            <div class="form-group">
                <label for="locLat">Latitude</label>
                <input type="number" id="locLat" class="form-control" 
                    value="${state.user.location?.latitude || ''}" step="any" required>
            </div>
            <div class="form-group">
                <label for="locLon">Longitude</label>
                <input type="number" id="locLon" class="form-control" 
                    value="${state.user.location?.longitude || ''}" step="any" required>
            </div>
            <button type="submit" class="btn" style="width: 100%;">
                <i class="fas fa-save"></i> Update Location
            </button>
        </form>
    `;
    
    app.appendChild(div);
    
    document.getElementById('locForm').onsubmit = async (e) => {
        e.preventDefault();
        
        const req = {
            latitude: parseFloat(document.getElementById('locLat').value),
            longitude: parseFloat(document.getElementById('locLon').value)
        };
        
        try {
            const data = await apiFetch('/user/location', 'PUT', req);
            
            if (data.user) {
                setState({ 
                    user: data.user,
                    page: 'profile',
                    success: 'Location updated successfully!'
                });
            }
        } catch (error) {
            setState({ error: error.message || 'Failed to update location' });
        }
    };
}

// Render update water needs page
function renderUpdateWaterNeeds() {
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
    
    app.appendChild(div);
    
    document.getElementById('waterNeedsForm').onsubmit = async (e) => {
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
}

// Render wells list page
async function renderWells() {
    if (state.wells.length === 0 && !state.loading) {
        try {
            const data = await apiFetch('/wells');
            setState({ wells: data.wells || [], pagination: data.pagination || state.pagination });
        } catch (error) {
            setState({ error: error.message || 'Failed to load wells' });
        }
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
    
    app.appendChild(div);
    
    document.getElementById('wellFilterForm').onsubmit = (e) => {
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
}

// Render well details page
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
    
    app.appendChild(div);
}

// Render create well page
function renderCreateWell() {
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

    app.appendChild(div);

    document.getElementById('createWellForm').onsubmit = async (e) => {
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
}

// Render edit well page
function renderEditWell() {
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

    app.appendChild(div);

    document.getElementById('editWellForm').onsubmit = async (e) => {
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
}

// Delete well function
async function deleteWell(espId) {
    if (!confirm('Are you sure you want to delete this well?')) return;

    try {
        await apiFetch(`/wells/${espId}`, 'DELETE');
        setState({
            page: 'wells',
            wells: [],
            success: 'Well deleted successfully!'
        });
    } catch (error) {
        setState({ error: error.message || 'Failed to delete well' });
    }
}

// Render weather page
async function renderWeather() {
    if (!state.weather && !state.loading) {
        try {
            const data = await apiFetch('/weather');
            setState({ weather: data.weather });
        } catch (error) {
            setState({ error: error.message || 'Failed to load weather data' });
        }
    }

    const div = document.createElement('div');
    div.className = 'card';

    div.innerHTML = `
        <h2><i class="fas fa-cloud-sun"></i> Weather Forecast</h2>
        ${state.loading ? '<p>Loading weather data...</p>' : ''}
    `;

    if (state.weather) {
        const weather = state.weather;
        div.innerHTML += `
            <div class="weather-card">
                <div class="weather-main">
                    <span class="weather-icon">${getWeatherIcon(weather.icon)}</span>
                    <span class="weather-temp">${weather.temperature}°C</span>
                    <span class="weather-desc">${weather.description}</span>
                </div>
                <div class="weather-details">
                    <div class="weather-detail">
                        <i class="fas fa-tint"></i>
                        <p>Humidity</p>
                        <p>${weather.humidity}%</p>
                    </div>
                    <div class="weather-detail">
                        <i class="fas fa-wind"></i>
                        <p>Wind</p>
                        <p>${weather.windSpeed} km/h</p>
                    </div>
                    <div class="weather-detail">
                        <i class="fas fa-cloud-rain"></i>
                        <p>Rain</p>
                        <p>${weather.rainAmount} mm</p>
                    </div>
                </div>
            </div>

            <div style="margin-top: 20px;">
                <h3>3-Day Forecast</h3>
                <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 15px; margin-top: 10px;">
                    ${weather.forecast ? weather.forecast.slice(0, 3).map(day => `
                        <div class="card" style="text-align: center;">
                            <p><strong>${new Date(day.date).toLocaleDateString('en-US', { weekday: 'short' })}</strong></p>
                            <p style="font-size: 2rem;">${getWeatherIcon(day.icon)}</p>
                            <p>${day.temperature}°C</p>
                            <p>${day.description}</p>
                        </div>
                    `).join('') : '<p>No forecast data available.</p>'}
                </div>
            </div>
        `;
    }

    app.appendChild(div);
}

// Get weather icon
function getWeatherIcon(icon) {
    const icons = {
        '01d': '☀️', '01n': '🌙', '02d': '⛅', '02n': '☁️',
        '03d': '☁️', '03n': '☁️', '04d': '☁️', '04n': '☁️',
        '09d': '🌧️', '09n': '🌧️', '10d': '🌦️', '10n': '🌧️',
        '11d': '⛈️', '11n': '⛈️', '13d': '❄️', '13n': '❄️',
        '50d': '🌫️', '50n': '🌫️'
    };
    return icons[icon] || '🌡️';
}

// Render nearby users page
async function renderNearby() {
    if (state.nearbyUsers.length === 0 && !state.loading) {
        try {
            const data = await apiFetch('/users/nearby');
            setState({ nearbyUsers: data.users || [] });
        } catch (error) {
            setState({ error: error.message || 'Failed to load nearby users' });
        }
    }

    const div = document.createElement('div');
    div.className = 'card';

    div.innerHTML = `
        <h2><i class="fas fa-users"></i> Nearby Users</h2>
        ${state.loading ? '<p>Loading nearby users...</p>' : ''}
    `;

    if (state.nearbyUsers.length > 0) {
        const usersGrid = document.createElement('div');
        usersGrid.className = 'grid';

        state.nearbyUsers.forEach(user => {
            const userCard = document.createElement('div');
            userCard.className = 'card';

            userCard.innerHTML = `
                <h3>${user.firstName} ${user.lastName}</h3>
                <p><i class="fas fa-map-marker-alt"></i> ${user.distance} km away</p>
                <p><i class="fas fa-tint"></i> Water needs:
                    ${user.waterNeeds?.length ?
                        user.waterNeeds.map(n => `${n.usageType} (${n.amount}L)`).join(', ') :
                        'Not specified'}
                </p>
            `;

            usersGrid.appendChild(userCard);
        });

        div.appendChild(usersGrid);
    } else if (!state.loading) {
        div.innerHTML += '<p>No nearby users found.</p>';
    }

    app.appendChild(div);
}

// Render notifications page
function renderNotifications() {
    const div = document.createElement('div');
    div.className = 'card';

    div.innerHTML = `
        <h2><i class="fas fa-bell"></i> Notifications</h2>
        <p>Push notification registration and management is not available in the web version.</p>
        <p>Please use the mobile app for notification features.</p>
    `;

    app.appendChild(div);
}

// Render bug reports page
function renderBugReports() {
    const div = document.createElement('div');
    div.className = 'card';
    div.style.maxWidth = '600px';
    div.style.margin = '20px auto';

    div.innerHTML = `
        <h2><i class="fas fa-bug"></i> Submit Bug Report</h2>
        <form id="bugForm">
            <div class="form-group">
                <label for="bugName">Title</label>
                <input type="text" id="bugName" class="form-control" required>
            </div>
            <div class="form-group">
                <label for="bugCat">Category</label>
                <select id="bugCat" class="form-control" required>
                    <option value="">Select a category</option>
                    <option value="ui">User Interface</option>
                    <option value="functionality">Functionality</option>
                    <option value="performance">Performance</option>
                    <option value="security">Security</option>
                    <option value="other">Other</option>
                </select>
            </div>
            <div class="form-group">
                <label for="bugDesc">Description</label>
                <textarea id="bugDesc" class="form-control" rows="5" required></textarea>
            </div>
            <button type="submit" class="btn" style="width: 100%;">
                <i class="fas fa-paper-plane"></i> Submit Report
            </button>
        </form>
    `;

    app.appendChild(div);

    document.getElementById('bugForm').onsubmit = async (e) => {
        e.preventDefault();

        const req = {
            title: document.getElementById('bugName').value,
            category: document.getElementById('bugCat').value,
            description: document.getElementById('bugDesc').value,
            userId: state.user?.userId
        };

        try {
            await apiFetch('/bug-reports', 'POST', req);
            setState({
                success: 'Bug report submitted successfully!',
                error: null
            });
            document.getElementById('bugForm').reset();
        } catch (error) {
            setState({
                error: error.message || 'Failed to submit bug report',
                success: null
            });
        }
    };
}

// Logout function
async function logout() {
    try {
        await apiFetch('/auth/logout', 'POST');
        setState({
            user: null,
            page: 'home',
            success: 'Logged out successfully!'
        });
    } catch (error) {
        setState({ error: error.message || 'Failed to logout' });
    }
}
async function hashPassword(password) {
            const encoder = new TextEncoder();
            const data = encoder.encode(password);
            const hashBuffer = await crypto.subtle.digest('SHA-256', data);
            const hashArray = Array.from(new Uint8Array(hashBuffer));
            const base64Hash = btoa(String.fromCharCode(...hashArray));
            return base64Hash;
        }

// Initialize the app
async function init() {
    // Check if user is already authenticated
    //const isAuthenticated = await checkAuth(); NOT USED AND NOT WORKING.
    const isAuthenticated = true; // FOR TESTING ONLY

    if (isAuthenticated) {
        // Load initial data if needed
        if (state.page === 'wells') {
            try {
                const data = await apiFetch('/wells');
                setState({ wells: data.wells || [], pagination: data.pagination || state.pagination });
            } catch (error) {
                console.error('Failed to load wells:', error);
            }
        }
    } else if (state.page !== 'login' && state.page !== 'register') {
        nav('home');
    }

    render();
}

// Make functions available globally
window.nav = nav;
window.logout = logout;
window.deleteWell = deleteWell;

// Start the app
init();