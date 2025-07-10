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
                    ${state.user.waterNeeds?.length ?
                        state.user.waterNeeds.map(n => `${n.usageType}: ${n.amount}L`).join(', ') :
                        'Not specified'}
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

    return div;
}

window.pages = window.pages || {};
window.pages.profile = renderProfile;