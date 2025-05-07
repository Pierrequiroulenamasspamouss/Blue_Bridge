# WellConnect Server Emulation

This directory contains a Node.js server that emulates the WellConnect API for development and testing purposes.

## Features

- RESTful API for user management, well monitoring, and nearby user discovery
- Authentication with token validation
- SQLite database for data persistence
- User registration, login, profile management
- Well creation, monitoring, and statistics
- Mock data generation for testing

## Getting Started

### Prerequisites

- Node.js 14.x or higher
- npm or yarn

### Installation

1. Navigate to the Server emulation directory:
   ```bash
   cd "Server emulation"
   ```

2. Install dependencies and seed the database (one-step process):
   ```bash
   npm run setup
   ```
   
   This will install all dependencies and populate the database with sample data.

   Or you can install and seed separately:
   ```bash
   npm install
   npm run seed
   ```

### Running the Server

Start the development server:
```bash
npm start
```

If you encounter certificate-related errors, you can run the HTTP-only version:
```bash
npm run start:http
```

The server will be available at:
- HTTP: http://localhost:3000

## API Endpoints

### Authentication

- **POST /api/register** - Register a new user
- **POST /api/login** - Login and get authentication token
- **POST /api/logout** - Logout (invalidate token)
- **POST /api/validate-token** - Check if a token is valid

### User Management

- **GET /api/user-profile** - Get user profile data
- **POST /api/update-profile** - Update user profile
- **POST /api/update-water-needs** - Update user water needs
- **POST /api/update-location** - Update user location
- **POST /api/update-theme** - Update user theme preference
- **GET /api/nearby-users** - Find nearby users

### Well Management

- **GET /api/wells** - Get all wells
- **GET /api/wells/:espId** - Get well by ESP ID
- **POST /api/wells** - Create new well
- **PUT /api/wells/:espId** - Update well
- **DELETE /api/wells/:espId** - Delete well
- **GET /api/wells/status/:status** - Get wells by status
- **PATCH /api/wells/:espId/water-level** - Update well water level
- **PATCH /api/wells/:espId/water-quality** - Update water quality
- **GET /api/wells/nearby/:latitude/:longitude/:radius** - Get wells in specified radius
- **GET /api/wells/stats/summary** - Get well statistics

## Default User Credentials

You can use these credentials to test the API:

- **Email**: pierresluse@gmail.com
- **Password**: Test

## Example Requests

### Login

```bash
curl -X POST http://localhost:3000/api/login \
  -H "Content-Type: application/json" \
  -d '{"email":"pierresluse@gmail.com","password":"Uy6qvZV0iA2/drm4zACDLCCm7BE9aCKZVQ16bg80XiU="}'
```

### Get Nearby Users

```bash
curl "http://localhost:3000/api/nearby-users?latitude=48.8566&longitude=2.3522&radius=10&email=pierresluse@gmail.com"
```

### Get Well Statistics

```bash
curl http://localhost:3000/api/wells/stats/summary
```

## Database

The server uses SQLite for data persistence. The database file is located at:
```
Server emulation/data/database.sqlite
```

To reset the database, delete this file and run the seed script again:
```bash
rm Server\ emulation/data/database.sqlite
node scripts/seed-db.js
```

## Troubleshooting

- If Node.js or npm is not installed or not in your path:
  1. Download and install Node.js from https://nodejs.org/
  2. Ensure it's added to your system PATH
  3. Open a new terminal window after installation
- If the server fails to start, check if port 3000 is already in use
- If authentication fails, make sure you're using the correct credentials and token
- Check the console logs for detailed error messages

## License

This project is for educational purposes only. 
