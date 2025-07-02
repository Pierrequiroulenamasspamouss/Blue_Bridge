# BlueBridge Environment Variables Reference

## Overview
This document provides a quick reference for all environment variables used in the BlueBridge server application.

## Environment Variables

### Server Configuration
```env
NODE_ENV=development          # Environment mode (development/production)
PORT=80                      # HTTP port for the server
HTTPS_PORT=443               # HTTPS port for the server
```

### Domain and SSL Configuration
```env
DOMAIN=bluebridge.homeonthewater.com    # Domain name for SSL certificates
EMAIL=bluebridgeapp@gmail.com           # Email for SSL certificate notifications
```

### Firebase Configuration
```env
FIREBASE_TYPE=service_account
FIREBASE_PROJECT_ID=your-firebase-project-id
FIREBASE_PRIVATE_KEY_ID=your-private-key-id
FIREBASE_PRIVATE_KEY="-----BEGIN PRIVATE KEY-----\nYour private key here\n-----END PRIVATE KEY-----\n"
FIREBASE_CLIENT_EMAIL=your-service-account@your-project.iam.gserviceaccount.com
FIREBASE_CLIENT_ID=your-client-id
FIREBASE_AUTH_URI=https://accounts.google.com/o/oauth2/auth
FIREBASE_TOKEN_URI=https://oauth2.googleapis.com/token
FIREBASE_AUTH_PROVIDER_X509_CERT_URL=https://www.googleapis.com/oauth2/v1/certs
FIREBASE_CLIENT_X509_CERT_URL=https://www.googleapis.com/robot/v1/metadata/x509/your-service-account%40your-project.iam.gserviceaccount.com
FIREBASE_UNIVERSE_DOMAIN=googleapis.com
```

### Gmail Configuration
```env
GMAIL_USER=bluebridgeapp@gmail.com           # Gmail account for sending emails
GMAIL_APP_PASSWORD=tbix drij febn renr       # App-specific password (not regular password)
```

### OpenWeather API
```env
OPENWEATHER_API_KEY=your-openweather-api-key    # API key for weather service
```

## Where These Variables Are Used

### Server Configuration
- **`server.js`**: Main server configuration, port settings, environment detection

### Firebase Configuration
- **`services/firebaseService.js`**: Firebase Admin SDK initialization for push notifications

### Email Configuration
- **`services/emailService.js`**: Gmail SMTP configuration for sending emails

### Weather Configuration
- **`services/weatherService.js`**: OpenWeather API integration

### SSL Certificate Generation
- **`scripts/generate-public-cert.js`**: Domain and email for SSL certificate generation

## Important Notes

1. **Firebase Private Key**: The private key must include the `\n` characters which are converted to actual newlines by the application
2. **Gmail App Password**: This is NOT your regular Gmail password. You must generate an App Password from your Google Account settings
3. **OpenWeather API Key**: Register at [OpenWeatherMap](https://openweathermap.org/api) to get your API key
4. **Firebase Configuration**: All Firebase values come from your Firebase project's service account key file

## Security Considerations

- Never commit the `.env` file to version control
- Keep API keys and passwords secure
- Regularly rotate sensitive credentials
- Use environment-specific configurations for development vs production 