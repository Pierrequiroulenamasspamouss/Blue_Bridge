# WellConnect Server Emulation

This server emulation package provides a development environment for testing and developing the WellConnect mobile application.

## Features

- Complete REST API emulation for WellConnect mobile app
- User authentication and account management
- Well data monitoring and management 
- Push notification token registration for Firebase Cloud Messaging
- Dynamic API tree visualization at `/tree` endpoint
- SSL support for secure connections

## Quick Start

### Installation

The easiest way to install is using the provided installation script:

```bash
chmod +x install.sh
sudo ./install.sh
```

For manual installation, follow these steps:

1. Install Node.js (v14 or higher) and npm
2. Install dependencies: `npm install`
3. Create a `.env` file based on the example below
4. Start the server: `npm start`

### Environment Configuration

Create a `.env` file in the root directory with the following content:

```
PORT=3000
NODE_ENV=development
JWT_SECRET=your_jwt_secret
DB_PATH=./database.sqlite

# Firebase configuration
FIREBASE_PROJECT_ID=wellconnect-458200
FIREBASE_CLIENT_EMAIL=firebase-adminsdk-xxxxx@wellconnect-458200.iam.gserviceaccount.com
FIREBASE_PRIVATE_KEY="-----BEGIN PRIVATE KEY-----\nYour private key here\n-----END PRIVATE KEY-----\n"
FIREBASE_WEB_PUSH_CERT=BJ9Mf0mMKZO8pnRYwL-
FIREBASE_SERVER_KEY=jdWcCM2iqQUmzsgXJnIT5yjrrfrgF_2rmjkM9gUv1QbWSlLcG2cyTA93qIHmSyvx2c6o
```

### Available Scripts

- `npm start`: Start the server in production mode
- `npm run dev`: Start the server with hot-reloading for development
- `npm run seed`: Seed the database with test data
- `npm run clean`: Reset the database and reseed with test data
- `npm run setup`: Install dependencies and seed the database

## API Documentation

Once the server is running, visit `/tree` in your browser for a complete and dynamically generated API tree with clickable endpoints.

The main API routes include:

### Authentication
- `POST /api/register`: Register a new user
- `POST /api/login`: Login and get auth token
- `POST /api/logout`: Logout and invalidate token
- `POST /api/delete-account`: Delete user account

### User Management
- `GET /api/user/profile`: Get user profile
- `PUT /api/user/profile`: Update user profile
- `PUT /api/user/location`: Update user location
- `PUT /api/user/water-needs`: Update user water needs

### Well Monitoring
- `GET /api/wells`: Get list of all wells
- `GET /api/wells/:espId`: Get well details
- `POST /api/wells`: Create new well
- `PUT /api/wells/:espId`: Update well data
- `DELETE /api/wells/:espId`: Delete well

### Push Notifications
- `POST /api/notifications/register`: Register device for push notifications
- `POST /api/notifications/unregister`: Unregister device for push notifications
- `GET /api/notifications/status`: Check notification registration status

## Firebase Cloud Messaging Integration

This server emulation can be used to register and manage notification tokens for Firebase Cloud Messaging (FCM). 

To properly test FCM, you need to:

1. Set up the proper Firebase credentials in the `.env` file
2. Register device tokens using the `/api/notifications/register` endpoint
3. Send test notifications using Firebase Console or the `/api/notifications/send` endpoint (if implemented)

## Development Notes

### Adding New Endpoints

When you add new endpoints to the server, they will automatically appear in the `/tree` endpoint visualization without any further configuration.

### Database

The server uses SQLite for data storage. The database file is created at the path specified in `DB_PATH`.

### Testing with the WellConnect App

To test with the WellConnect app:

1. Make sure the server is running on a network accessible to your device or emulator
2. Update the app's server URL to point to your server instance
3. For mobile devices on the same network, use your machine's local IP address

## Troubleshooting

### Common Issues

- **EADDRINUSE error**: The port is already in use. Change the PORT in the .env file or stop the other process.
- **Missing environment variables**: Ensure all required environment variables are set in the .env file.
- **Database permissions**: Make sure the user running the server has write permissions for the database file path.

## License

This software is proprietary and confidential. Unauthorized copying or distribution is prohibited. 