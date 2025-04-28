# WellConnect Server Emulation

This directory contains a server emulation for the WellConnect app, which provides a RESTful API for well data, user authentication, and nearby user discovery.

## Components

- `server_wrapper.py` - Main server implementation with request/response logging
- `new_auth_server.py` - Authentication service for login/registration
- `new_well_server.py` - Well data service for retrieving well information
- `new_nearby_server.py` - Nearby users service for location-based discovery
- `db_init.py` - Database initialization script with minimal sample data
- `generate_sample_data.py` - Script to generate a larger sample database
- `query_database.py` - Utility to examine database contents
- `run_server.py` - Simple script to start the server

## Database

The system uses SQLite with the following schema:

- `users` - User accounts with authentication and profile information
- `water_needs` - Water requirements for each user
- `wells` - Well information including location, capacity, and status
- `water_quality` - Historical water quality measurements for wells

## Getting Started

### Generate Sample Database

To create a sample database with random users and wells:

```bash
python generate_sample_data.py --users 30 --wells 50
```

This will create `wellconnect.db` with the specified number of users and wells.

### Query the Database

To examine the database contents:

```bash
# Check database status
python query_database.py --check

# List users (default shows 10)
python query_database.py --users

# List wells with a limit of 20
python query_database.py --wells --limit 20

# Show water needs
python query_database.py --water-needs

# Show water quality measurements
python query_database.py --water-quality

# Show detailed information for a specific user
python query_database.py --user 1

# Show detailed information for a specific well
python query_database.py --well 1
```

### Run the Server

To start the server on port 8090:

```bash
python run_server.py
```

The server provides an interactive console where you can:
- Check service status with `status`
- Disable services with `shutdown <service>` (e.g., `shutdown login`)
- Enable services with `boot <service>` (e.g., `boot login`)
- Clear logs with `clearlog`
- Exit the server with `exit`

## API Endpoints

The server supports both `/api/endpoint` and `/endpoint` formats for all routes.

### Authentication
- `POST /api/login` - Authenticate a user
- `POST /api/register` - Register a new user
- `POST /api/update-location` - Update user location
- `POST /api/update-water-needs` - Update user water needs

### Wells
- `GET /api/wells` or `GET /api/data/wells` - Get all wells
- `GET /api/wells/index` - Get filtered wells with query parameters
- `GET /api/wells/{esp_id}` - Get specific well details
- `GET /api/data/wells/{esp_id}` - Alternative endpoint for well details
- `GET /api/wells/stats` - Get well statistics

### Nearby Users
- `GET /api/nearby-users` - Find users within a specified radius

## Test Accounts

The sample database includes these accounts:

- Admin: `admin@wellconnect.com` / `admin123`
- Demo User: `demo@wellconnect.com` / `demo123`
- Plus randomly generated users with pattern: `firstname{id}@example.com` / `password{id}`

## Requirements

- Python 3.6 or higher
- SQLite3
- Required Python packages:
  - tabulate (for query_database.py)

# WellConnect Server - Apache Deployment Guide

This guide provides instructions for deploying the WellConnect API server on Apache with mod_wsgi.

## Prerequisites

- Apache 2.4 or newer
- Python 3.8 or newer
- mod_wsgi installed and configured for Apache
- Python dependencies: Flask, sqlite3

## Installation Steps

### 1. Install Required Packages

```bash
# Install Apache and mod_wsgi
sudo apt-get update
sudo apt-get install apache2 libapache2-mod-wsgi-py3

# Install Python dependencies
pip install flask
```

### 2. Set Up the Project Files

Clone or copy the project files to your server:

```bash
# Example location
mkdir -p /var/www/wellconnect
cp -r /path/to/your/Server\ emulation/* /var/www/wellconnect/
```

Make sure the files have the correct permissions:

```bash
# Set ownership to Apache user (often www-data)
sudo chown -R www-data:www-data /var/www/wellconnect
sudo chmod -R 755 /var/www/wellconnect
```

### 3. Configure Apache

Edit the Apache configuration file (`apache_config.conf`) and update the paths to match your deployment:

```
# Update these paths in apache_config.conf
<Directory /var/www/wellconnect>
    ...
</Directory>

WSGIDaemonProcess wellconnect python-home=/path/to/virtualenv threads=5
WSGIScriptAlias / /var/www/wellconnect/wsgi.py
```

Copy the configuration file to Apache's conf directory:

```bash
sudo cp /var/www/wellconnect/apache_config.conf /etc/apache2/sites-available/wellconnect.conf
sudo a2ensite wellconnect.conf
```

### 4. Enable Required Apache Modules

```bash
sudo a2enmod wsgi
sudo a2enmod headers  # For CORS support
sudo systemctl restart apache2
```

### 5. Test the Deployment

Access your API at the configured URL:

```
http://your-server-ip/api/wells
```

## Troubleshooting

Check Apache error logs for issues:

```bash
sudo tail -f /var/log/apache2/error.log
```

Check the WellConnect logs:

```bash
cat /var/www/wellconnect/server_logs.txt
```

## File Structure

- `apache_server.py` - The Flask WSGI application
- `wsgi.py` - WSGI entry point for Apache
- `apache_config.conf` - Apache configuration file
- `new_auth_server.py` - Authentication service
- `new_well_server.py` - Well data service
- `new_nearby_server.py` - Nearby users service
- `db_init.py` - Database initialization
- `wellconnect.db` - SQLite database file

## API Endpoints

- `/api/wells` - Get all wells
- `/api/wells/index` - Get filtered wells
- `/api/wells/stats` - Get well statistics
- `/api/wells/{esp_id}` - Get specific well details
- `/api/login` - User login
- `/api/register` - User registration
- `/api/update-location` - Update user location
- `/api/update-water-needs` - Update user water needs
- `/api/nearby-users` - Get nearby users

## Security Considerations

- Make sure your database file has appropriate permissions
- Consider using HTTPS for your API (configure SSL in Apache)
- Review the CORS headers to restrict access if needed
- Consider implementing API rate limiting 