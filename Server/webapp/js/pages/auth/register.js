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

    div.querySelector('#registerForm').onsubmit = async (e) => {
        e.preventDefault();

        const req = {
            email: document.getElementById('regEmail').value,
            password: document.getElementById('regPassword').value,
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

        await register(req);
    };

    return div;
}

window.pages = window.pages || {};
window.pages.register = renderRegister;