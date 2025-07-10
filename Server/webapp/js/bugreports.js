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

    div.querySelector('#bugForm').onsubmit = async (e) => {
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

    return div;
}

window.pages = window.pages || {};
window.pages.bugreports = renderBugReports;