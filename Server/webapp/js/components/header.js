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

// Make header function available globally
window.renderHeader = renderHeader;