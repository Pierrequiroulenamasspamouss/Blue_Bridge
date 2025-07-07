# BlueBridge API Documentation

## Overview
This document provides comprehensive documentation for the BlueBridge server API, including all endpoints, request/response models, and data structures used by the Android mobile application.


## HOW TO UPDATE THE SERVER EASILY:
- Run sudo ./update_latest.sh

- Try chmod +x update_latest.sh if there is a permissions issue. 


## Base URL
- **Development**: `http://localhost:80`
- **Production**: `http://bluebridge.homeonthewater.com`

## Authentication
Most endpoints require authentication using a `loginToken` and `userId` pair. The token is generated during login/registration and must be included in requests.

## Data Models

### Core Data Structures

#### Location
```json
{
  "latitude": 0.0,
  "longitude": 0.0,
  "lastUpdated": "never"
}
```

#### WaterNeed
```json
{
  "amount": 0.0,
  "usageType": "string",
  "description": "string",
  "priority": 0
}
```

#### WaterQuality
```json
{
  "ph": 7.0,
  "turbidity": 0.0,
  "tds": 0
}
```

#### MovementSpeeds
```json
{
  "walkingSpeed": 6.0,
  "drivingSpeed": 70.0
}
```

### User Models

#### UserData
```json
{
  "email": "string",
  "firstName": "string",
  "lastName": "string",
  "username": "string",
  "role": "string",
  "location": {
    "latitude": 0.0,
    "longitude": 0.0,
    "lastUpdated": "string"
  },
  "phoneNumber": "string|null",
  "themePreference": 0,
  "lastLogin": "string|null",
  "waterNeeds": [
    {
      "amount": 0.0,
      "usageType": "string",
      "description": "string",
      "priority": 0
    }
  ],
  "movementSpeeds": {
    "walkingSpeed": 6.0,
    "drivingSpeed": 70.0
  },
  "loginToken": "string|null",
  "userId": "string"
}
```

#### NearbyUser
```json
{
  "userId": "string",
  "username": "string",
  "firstName": "string",
  "lastName": "string",
  "email": "string",
  "role": "string",
  "waterNeeds": [
    {
      "amount": 0.0,
      "usageType": "string",
      "description": "string",
      "priority": 0
    }
  ],
  "lastActive": "string",
  "distance": 0.0
}
```

### Well Models

#### WellData
```json
{
  "id": 0,
  "wellName": "string",
  "wellLocation": {
    "latitude": 0.0,
    "longitude": 0.0
  },
  "wellWaterType": "string",
  "wellCapacity": "string",
  "wellWaterLevel": "string",
  "lastRefreshTime": 0,
  "wellStatus": "string",
  "waterQuality": {
    "ph": 7.0,
    "turbidity": 0.0,
    "tds": 0
  },
  "extraData": {},
  "description": "string",
  "lastUpdated": "string|null",
  "espId": "string",
  "wellWaterConsumption": "string",
  "wellOwner": "string"
}
```

#### ShortenedWellData
```json
{
  "wellName": "string",
  "wellLocation": {
    "latitude": 0.0,
    "longitude": 0.0
  },
  "wellWaterType": "string",
  "wellStatus": "string",
  "wellCapacity": "string",
  "wellWaterLevel": "string",
  "espId": "string"
}
```

### Weather Models

#### WeatherData
```json
{
  "date": "string",
  "time": "string",
  "temperature": 0.0,
  "feelsLike": 0.0,
  "minTemperature": 0.0,
  "maxTemperature": 0.0,
  "humidity": 0.0,
  "description": "string",
  "icon": "string",
  "windSpeed": 0.0,
  "rainAmount": 0.0,
  "pressure": 0.0,
  "windDirection": 0,
  "sunset": "string"
}
```

## API Endpoints

### Authentication Endpoints

#### 1. User Registration
- **URL**: `POST /api/auth/register`
- **Description**: Register a new user account
- **Request Body**:
```json
{
  "email": "string",
  "password": "string",
  "firstName": "string",
  "lastName": "string",
  "username": "string",
  "location": {
    "latitude": 0.0,
    "longitude": 0.0
  },
  "waterNeeds": [
    {
      "amount": 0.0,
      "usageType": "string",
      "description": "string",
      "priority": 0
    }
  ],
  "phoneNumber": "string|null",
  "role": "user",
  "themePreference": 0
}
```
- **Response**:
```json
{
  "status": "success|error",
  "message": "string",
  "userData": {
    // UserData object
  },
  "loginToken": "string"
}
```

#### 2. User Login
- **URL**: `POST /api/auth/login`
- **Description**: Authenticate user and get login token
- **Request Body**:
```json
{
  "email": "string",
  "password": "string"
}
```
- **Response**:
```json
{
  "status": "success|error",
  "message": "string",
  "data": {
    // UserData object
  }
}
```

#### 3. Token Validation
- **URL**: `POST /api/auth/validate`
- **Description**: Validate authentication token
- **Request Body**:
```json
{
  "token": "string",
  "userId": "string"
}
```
- **Response**:
```json
{
  "status": "success|error",
  "message": "string"
}
```

#### 4. Delete Account
- **URL**: `POST /api/auth/delete-account`
- **Description**: Delete user account
- **Request Body**:
```json
{
  "email": "string",
  "password": "string",
  "token": "string"
}
```
- **Response**:
```json
{
  "status": "success|error",
  "message": "string"
}
```

### User Management Endpoints

#### 5. Update Location
- **URL**: `POST /api/update-location`
- **Description**: Update user's current location
- **Request Body**:
```json
{
  "email": "string",
  "latitude": 0.0,
  "longitude": 0.0,
  "token": "string"
}
```
- **Response**:
```json
{
  "status": "success|error",
  "message": "string",
  "timestamp": "string"
}
```

#### 6. Update Water Needs
- **URL**: `POST /api/update-water-needs`
- **Description**: Update user's water needs
- **Request Body**:
```json
{
  "email": "string",
  "waterNeeds": [
    {
      "amount": 0.0,
      "usageType": "string",
      "description": "string",
      "priority": 0
    }
  ],
  "token": "string"
}
```
- **Response**:
```json
{
  "status": "success|error",
  "message": "string",
  "timestamp": "string"
}
```

#### 7. Update Profile
- **URL**: `POST /api/users/update-profile`
- **Description**: Update user profile information
- **Request Body**:
```json
{
  "email": "string",
  "firstName": "string",
  "lastName": "string",
  "username": "string",
  "location": {
    "latitude": 0.0,
    "longitude": 0.0
  },
  "token": "string"
}
```
- **Response**:
```json
{
  "status": "success|error",
  "message": "string",
  "timestamp": "string"
}
```

### Well Management Endpoints

#### 8. Get Wells (with filters)
- **URL**: `GET /api/wells`
- **Description**: Get paginated list of wells with optional filters
- **Query Parameters**:
  - `page` (int, default: 1): Page number
  - `limit` (int, default: 20): Items per page
  - `wellName` (string, optional): Filter by well name
  - `wellStatus` (string, optional): Filter by well status
  - `wellWaterType` (string, optional): Filter by water type
  - `wellOwner` (string, optional): Filter by well owner
  - `espId` (string, optional): Filter by ESP ID
  - `minWaterLevel` (int, optional): Minimum water level
  - `maxWaterLevel` (int, optional): Maximum water level
- **Response**:
```json
{
  "status": "success|error",
  "data": [
    // Array of WellData objects
  ],
  "pagination": {
    "total": 0,
    "page": 1,
    "limit": 20,
    "pages": 0
  }
}
```

#### 9. Get Well by ID
- **URL**: `GET /api/wells/{espId}/details`
- **Description**: Get detailed information about a specific well
- **Path Parameters**:
  - `espId` (string): ESP ID of the well
- **Response**: WellData object

#### 10. Create Well
- **URL**: `POST /api/wells`
- **Description**: Create a new well
- **Query Parameters**:
  - `email` (string): User email
  - `loginToken` (string): Authentication token
- **Request Body**: WellData object
- **Response**:
```json
{
  "status": "success|error",
  "message": "string",
  "timestamp": "string"
}
```

#### 11. Edit Well
- **URL**: `POST /api/wells/edit`
- **Description**: Update an existing well
- **Query Parameters**:
  - `email` (string): User email
  - `loginToken` (string): Authentication token
- **Request Body**: WellData object
- **Response**:
```json
{
  "status": "success|error",
  "message": "string",
  "timestamp": "string"
}
```

#### 12. Delete Well
- **URL**: `DELETE /api/wells/{espId}`
- **Description**: Delete a well
- **Path Parameters**:
  - `espId` (string): ESP ID of the well
- **Query Parameters**:
  - `email` (string): User email
  - `loginToken` (string): Authentication token
- **Response**:
```json
{
  "status": "success|error",
  "message": "string",
  "timestamp": "string"
}
```

#### 13. Get Well Statistics
- **URL**: `GET /api/wells/{espId}/stats`
- **Description**: Get statistics for a specific well
- **Path Parameters**:
  - `espId` (string): ESP ID of the well
- **Response**:
```json
{
  "status": "success|error",
  "data": {
    "totalWells": 0,
    "avgCapacity": 0.0,
    "avgWaterLevel": 0.0,
    "avgConsumption": 0.0,
    "totalCapacity": 0,
    "totalWaterLevel": 0,
    "percentageAvailable": 0.0,
    "statusCounts": {},
    "waterTypeCounts": {},
    "recentlyUpdated": 0
  }
}
```

### Nearby Users Endpoints

#### 14. Get Nearby Users
- **URL**: `POST /api/nearby-users`
- **Description**: Find users within a specified radius
- **Request Body**:
```json
{
  "latitude": 0.0,
  "longitude": 0.0,
  "radius": 0.0,
  "userId": "string",
  "loginToken": "string"
}
```
- **Response**:
```json
{
  "status": "success|error",
  "message": "string",
  "data": [
    // Array of NearbyUser objects
  ]
}
```

### Weather Endpoints

#### 15. Get Weather Data
- **URL**: `POST /api/weather`
- **Description**: Get weather forecast for a location
- **Request Body**:
```json
{
  "location": {
    "latitude": 0.0,
    "longitude": 0.0
  },
  "userId": "string",
  "loginToken": "string"
}
```
- **Response**:
```json
{
  "status": "success|error",
  "message": "string",
  "data": [
    // Array of WeatherData objects
  ]
}
```

### Notification Endpoints

#### 16. Register Notification Token
- **URL**: `POST /api/notifications/register`
- **Description**: Register device token for push notifications
- **Request Body**:
```json
{
  "userId": "string",
  "loginToken": "string",
  "deviceToken": "string"
}
```
- **Response**:
```json
{
  "status": "success|error",
  "message": "string",
  "timestamp": "string"
}
```

#### 17. Unregister Notification Token
- **URL**: `POST /api/notifications/unregister`
- **Description**: Unregister device token for push notifications
- **Request Body**:
```json
{
  "userId": "string",
  "loginToken": "string",
  "deviceToken": "string"
}
```
- **Response**:
```json
{
  "status": "success|error",
  "message": "string",
  "timestamp": "string"
}
```

### System Endpoints

#### 18. Server Status
- **URL**: `GET /status`
- **Description**: Get server status and version information
- **Response**:
```json
{
  "status": "success|error",
  "data": {
    "message": "string",
    "mode": "string",
    "status": "string",
    "timestamp": "string",
    "versions": {
      "server": "string",
      "mobile": "string"
    }
  }
}
```

#### 19. Get Server Certificate
- **URL**: `GET /api/certificates`
- **Description**: Get server SSL certificate
- **Response**:
```json
{
  "status": "success|error",
  "data": "string" // Base64 encoded certificate
}
```

#### 20. Submit Bug Report
- **URL**: `POST /api/bugreports`
- **Description**: Submit a bug report
- **Request Body**:
```json
{
  "name": "string",
  "description": "string",
  "category": "string",
  "extra": {}
}
```
- **Response**:
```json
{
  "status": "success|error",
  "message": "string",
  "timestamp": "string"
}
```

## Error Responses

All endpoints return consistent error responses:

```json
{
  "status": "error",
  "message": "Error description",
  "timestamp": "2024-01-01 12:00:00"
}
```

## Common HTTP Status Codes

- **200**: Success
- **201**: Created (for registration)
- **400**: Bad Request (missing required fields)
- **401**: Unauthorized (invalid credentials/token)
- **404**: Not Found
- **409**: Conflict (user already exists)
- **500**: Internal Server Error

## SMS API

The application also supports SMS-based communication for basic commands:

### SMS Commands

#### Get Nearby Wells (GNW)
- **Command**: `GNW [latitude,longitude]`
- **Description**: Get nearby wells via SMS
- **Example**: `GNW 40.7128,-74.0060`

#### Show Help (SH)
- **Command**: `SH [latitude,longitude]`
- **Description**: Show help information via SMS
- **Example**: `SH 40.7128,-74.0060`

## Notes
SMS server API requests and response: 
adb shell am start -a android.intent.action.SENDTO -d sms:+32491142936 --es sms_body "Automatic_message2" --ez exit_on_sent true &&adb shell input tap 960 2170
(to use with a phone connected to the server) 


1. **Authentication**: Most endpoints require a valid `loginToken` and `userId` pair
2. **Location Data**: All location data uses decimal degrees format
3. **Timestamps**: All timestamps are in ISO 8601 format
4. **Pagination**: List endpoints support pagination with `page` and `limit` parameters
5. **Water Needs**: Water needs are stored as JSON arrays with amount, usage type, description, and priority
6. **Well Status**: Common well statuses include "Active", "Inactive", "Maintenance", "Unknown"
7. **Water Types**: Common water types include "Fresh", "Brackish", "Salt", "Potable", "Non-potable" 
