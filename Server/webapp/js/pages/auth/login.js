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

    // Add form submission handler
    div.querySelector('#loginForm').onsubmit = async (e) => {
        e.preventDefault();
        const email = document.getElementById('loginEmail').value;
        const password = document.getElementById('loginPassword').value;
        await login(email, password);
    };

    return div;
}

// Register page render function
window.pages = window.pages || {};
window.pages.login = renderLogin;