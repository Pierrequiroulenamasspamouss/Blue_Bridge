// Authentication Service
async function hashPassword(password) {
    const encoder = new TextEncoder();
    const data = encoder.encode(password);
    const hashBuffer = await crypto.subtle.digest('SHA-256', data);
    const hashArray = Array.from(new Uint8Array(hashBuffer));
    const base64Hash = btoa(String.fromCharCode(...hashArray));
    return base64Hash;
}

async function checkAuth() {
    try {
        // Check if we have user data in state
        if (state.user && state.user.userId && state.user.loginToken) {
            // Validate the token with the server
            const data = await apiFetch('/auth/validate', 'POST', {
                token: state.user.loginToken,
                userId: state.user.userId
            }, false);

            return data.status === 'success';
        }
        return false;
    } catch {
        return false;
    }
}

async function login(email, password) {
    try {
        // Hash the password client-side as required
        const hashedPassword = await hashPassword(password);

        const response = await apiFetch('/auth/login', 'POST', {
            email,
            password: hashedPassword
        }, false);

        console.log('Login response:', response); // Debug logging

        if (response.status === 'success' && response.data) {
            setState({
                user: {
                    ...response.data,
                    loginToken: response.data.loginToken
                },
                page: 'home',
                success: 'Login successful!',
                error: null // Clear any previous errors
            });
            return true;
        } else {
            setState({
                error: response.message || 'Login failed',
                success: null
            });
            return false;
        }
    } catch (error) {
        console.error('Login error:', error);
        setState({
            error: error.message || 'Login failed',
            success: null
        });
        return false;
    }
}

async function register(userData) {
    try {
        // Hash the password client-side
        userData.password = await hashPassword(userData.password);

        const response = await apiFetch('/auth/register', 'POST', {
            email: userData.email,
            password: userData.password,
            firstName: userData.firstName,
            lastName: userData.lastName,
            username: userData.username,
            location: userData.location,
            phoneNumber: userData.phoneNumber,
            role: 'user',
            themePreference: 0
        }, false);

        if (response.status === 'success' && response.userData) {
            setState({
                user: {
                    ...response.userData,
                    loginToken: response.loginToken
                },
                page: 'home',
                success: 'Registration successful!',
                error: null
            });
            return true;
        } else {
            setState({
                error: response.message || 'Registration failed',
                success: null
            });
            return false;
        }
    } catch (error) {
        console.error('Registration error:', error);
        setState({
            error: error.message || 'Registration failed',
            success: null
        });
        return false;
    }
}

async function logout() {
    try {
        const response = await apiFetch('/auth/logout', 'POST', {
            token: state.user?.loginToken,
            userId: state.user?.userId
        });

        if (response.status === 'success') {
            setState({
                user: null,
                page: 'home',
                success: 'Logged out successfully!',
                error: null
            });
            return true;
        } else {
            setState({
                error: response.message || 'Failed to logout',
                success: null
            });
            return false;
        }
    } catch (error) {
        console.error('Logout error:', error);
        setState({
            error: error.message || 'Failed to logout',
            success: null
        });
        return false;
    }
}

// Make auth functions available globally
window.checkAuth = checkAuth;
window.login = login;
window.register = register;
window.logout = logout;
window.hashPassword = hashPassword;