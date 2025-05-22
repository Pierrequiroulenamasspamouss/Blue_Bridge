#!/bin/bash

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${GREEN}Starting BlueBridge Server Installation...${NC}"

# Check if Node.js is installed
if ! command -v node &> /dev/null; then
    echo -e "${RED}Node.js is not installed. Please install Node.js first.${NC}"
    exit 1
fi

# Check if npm is installed
if ! command -v npm &> /dev/null; then
    echo -e "${RED}npm is not installed. Please install npm first.${NC}"
    exit 1
fi

# Create necessary directories
mkdir -p /opt/bluebridge
mkdir -p /opt/bluebridge/APK
mkdir -p /opt/bluebridge/ssl
mkdir -p /opt/bluebridge/html
mkdir -p /opt/bluebridge/data

# Clean previous installation
rm -rf /opt/bluebridge/*

# Copy files with force
cp -f server.js /opt/bluebridge/
cp -f package.json /opt/bluebridge/
cp -f .greenlockrc /opt/bluebridge/
cp -f routeExplorer.js /opt/bluebridge/

# Copy directories with force
cp -rf routes /opt/bluebridge/
cp -rf models /opt/bluebridge/
cp -rf services /opt/bluebridge/
cp -rf scripts /opt/bluebridge/
cp -rf APK /opt/bluebridge/
cp -rf ssl /opt/bluebridge/
cp -rf html /opt/bluebridge/
cp -rf data /opt/bluebridge/

# Install dependencies
cd /opt/bluebridge
npm install

# Set permissions
chmod +x /opt/bluebridge/server.js
chmod +x /opt/bluebridge/scripts/*.js

# Create SSL certificates if they don't exist
if [ ! -f "ssl/private.key" ] || [ ! -f "ssl/certificate.crt" ]; then
    echo -e "${YELLOW}Generating self-signed SSL certificates...${NC}"
    openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
        -keyout ssl/private.key \
        -out ssl/certificate.crt \
        -subj "/C=US/ST=State/L=City/O=Organization/CN=localhost"
fi

# Initialize databases
echo -e "${YELLOW}Initializing databases...${NC}"
node scripts/init-db.js

# Create .env file if it doesn't exist
if [ ! -f ".env" ]; then
    echo -e "${YELLOW}Creating .env file...${NC}"
    cat > .env << EOL
NODE_ENV=development
HTTP_PORT=3000
HTTPS_PORT=3443
JWT_SECRET=$(openssl rand -base64 32)
EOL
fi

# Set up DNS updater
echo -e "${YELLOW}Setting up DNS updater...${NC}"
chmod +x scripts/dns-setup.sh

# Create systemd service
echo -e "${YELLOW}Creating systemd service...${NC}"
sudo tee /etc/systemd/system/bluebridge.service << EOL
[Unit]
Description=BlueBridge Server
After=network.target

[Service]
Type=simple
User=$USER
WorkingDirectory=$(pwd)
ExecStart=/usr/bin/npm run start
Restart=on-failure
Environment=NODE_ENV=production

[Install]
WantedBy=multi-user.target
EOL

# Enable and start service
echo -e "${YELLOW}Enabling and starting service...${NC}"
sudo systemctl daemon-reload
sudo systemctl enable bluebridge
sudo systemctl start bluebridge

echo -e "${GREEN}Installation completed successfully!${NC}"
echo -e "${YELLOW}The server should now be running on:${NC}"
echo -e "HTTP: http://localhost:3000"
echo -e "HTTPS: https://localhost:3443"
echo -e "${YELLOW}To check the service status:${NC}"
echo -e "sudo systemctl status bluebridge"