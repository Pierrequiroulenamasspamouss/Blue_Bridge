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

# Set up DNS updater
echo -e "${YELLOW}Setting up DNS updater...${NC}"
chmod +x scripts/dns-setup.sh

# Create systemd service
echo -e "${YELLOW}Creating systemd service...${NC}"
sudo tee /etc/systemd/system/bluebridge.service << EOL
[Unit]
Description=BlueBridge Server
After=network.target

[Service] # LSB compliant header for init.d script
Type=simple
User=$USER
WorkingDirectory=/opt/bluebridge
ExecStart=/usr/bin/node /opt/bluebridge/server.js
Restart=on-failure
Environment=NODE_ENV=production

[Install]
WantedBy=multi-user.target
EOL

# Create init.d script for broader compatibility (e.g., sudo service)
echo -e "${YELLOW}Creating init.d script...${NC}"
sudo tee /etc/init.d/bluebridge << EOL
#!/bin/sh
### BEGIN INIT INFO
# Provides:          bluebridge
# Required-Start:    \$remote_fs \$syslog
# Required-Stop:     \$remote_fs \$syslog
# Default-Start:     2 3 4 5
# Default-Stop:      0 1 6
# Short-Description: Start BlueBridge server at boot time
# Description:       Enable service provided by BlueBridge.
### END INIT INFO

case "\$1" in
  start)   systemctl start bluebridge ;;
  stop)    systemctl stop bluebridge ;;
  restart) systemctl restart bluebridge ;;
  status)  systemctl status bluebridge ;;
  *) echo "Usage: \$0 {start|stop|restart|status}" ; exit 1 ;;
esac
EOL
sudo chmod +x /etc/init.d/bluebridge

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
echo -e "or"
echo -e "sudo service bluebridge status"