const nodemailer = require('nodemailer');

// Create a transporter using Gmail
const transporter = nodemailer.createTransport({
    service: 'gmail',
    auth: {
        user: process.env.GMAIL_USER,
        pass: process.env.GMAIL_APP_PASSWORD // Use an App Password from Google Account
    }
});

/**
 * Sends a welcome email to a newly registered user
 * @param {string} email - The recipient's email address
 * @param {string} name - The user's name
 * @returns {Promise<boolean>} - Whether the email was sent successfully
 */
const sendWelcomeEmail = async (email, name) => {
    try {
        const mailOptions = {
            from: process.env.GMAIL_USER,
            to: email,
            subject: 'Welcome to Blue Bridge',
            text: `Hello ${name},\n\nThank you for registering with Blue Bridge. Your account has been successfully created.\n\nBest regards,\nThe Blue Bridge Team`,
            html: `
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                    <h2 style="color: #0056b3;">Welcome to Blue Bridge</h2>
                    <p>Hello ${name},</p>
                    <p>Thank you for registering with Blue Bridge. Your account has been successfully created.</p>
                    <p>If you did not create this account, please contact our support team immediately.</p>
                    <p>Best regards,<br>The Blue Bridge Team</p>
                </div>
            `
        };

        await transporter.sendMail(mailOptions);
        return true;
    } catch (error) {
        console.error('Error sending welcome email:', error);
        return false;
    }
};

module.exports = {
    sendWelcomeEmail
}; 