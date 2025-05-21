#!/bin/bash
#TODO  missing the new directory APK at installation. Fix that. Also clean the folder of the previous install.
#TODO : force the rewrite of all the files into the /opt directory if they are already there.
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
echo -e "${YELLOW}Creating necessary directories...${NC}"
mkdir -p data
mkdir -p ssl
mkdir -p html

# Install dependencies
echo -e "${YELLOW}Installing dependencies...${NC}"
npm install

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