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

    return div;
}

// Register page render function
window.pages = window.pages || {};
window.pages.home = renderHome;