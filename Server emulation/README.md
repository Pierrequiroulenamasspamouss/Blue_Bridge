# WellConnect Reforge Server

A Node.js based API server for the WellConnect application.

## Setup

1. Install dependencies:
```bash
npm install
```

2. Create a `.env` file in the root directory with the following contents:
```
PORT=3000
NODE_ENV=development
JWT_SECRET=your-secret-key-here
DB_PATH=database.sqlite
```

3. Start the server:
```bash
npm start
```

For development with auto-reload:
```bash
npm run dev
```

## API Endpoints

### Wells

- `GET /api/wells` - Get all wells
- `GET /api/wells/:espId` - Get well by ESP ID
- `POST /api/wells` - Create new well
- `PUT /api/wells/:espId` - Update well
- `DELETE /api/wells/:espId` - Delete well
- `GET /api/wells/status/:status` - Get wells by status
- `PATCH /api/wells/:espId/water-level` - Update well water level

### Database Schema

The Well model includes:
- espId (unique identifier)
- wellName
- wellOwner
- latitude/longitude
- waterType
- capacity
- waterLevel
- waterConsumption
- status
- lastUpdated
- ownerId (foreign key to Users table)

## Development

The server uses:
- Express.js for the API
- Sequelize with SQLite for the database
- JWT for authentication
- CORS enabled for cross-origin requests 