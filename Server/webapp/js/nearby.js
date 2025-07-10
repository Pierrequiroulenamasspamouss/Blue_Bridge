async function fetchNearbyUsers() {
    try {
        const data = await apiFetch('/users/nearby');
        setState({ nearbyUsers: data.users || [] });
    } catch (error) {
        setState({ error: error.message || 'Failed to load nearby users' });
    }
}

function renderNearby() {
    if (state.nearbyUsers.length === 0 && !state.loading) {
        fetchNearbyUsers();
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

    return div;
}

window.pages = window.pages || {};
window.pages.nearby = renderNearby;