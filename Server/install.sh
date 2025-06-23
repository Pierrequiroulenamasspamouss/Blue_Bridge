#!/bin/bash

# --- Configuration ---
# The directory where the server will be installed
DEST_DIR="/opt/bluebridge"
# The directory where this script is located (source of files)
SOURCE_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

# --- Colors for output ---
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${GREEN}Starting BlueBridge Server Installation...${NC}"
echo -e "Source: $SOURCE_DIR"
echo -e "Destination: $DEST_DIR"

# --- Pre-flight Checks ---
# Check if running as root
if [ "$EUID" -ne 0 ]; then
  echo -e "${RED}Please run as root or with sudo.${NC}"
  exit 1
fi

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

# Check if rsync is installed
if ! command -v rsync &> /dev/null; then
    echo -e "${RED}rsync is not installed. Please install it (e.g., 'sudo apt-get install rsync').${NC}"
    exit 1
fi

# --- Installation ---
# Create necessary directories
echo -e "${YELLOW}Creating required directories in $DEST_DIR...${NC}"
mkdir -p "$DEST_DIR/data"
mkdir -p "$DEST_DIR/ssl"
mkdir -p "$DEST_DIR/APK"
mkdir -p "$DEST_DIR/html"

# Copy application files (preserves data directory)
echo -e "${YELLOW}Copying server files...${NC}"
rsync -av --exclude 'data/' --exclude 'install.sh' --exclude '.git/' "$SOURCE_DIR/" "$DEST_DIR/"

# Install dependencies in the destination
echo -e "${YELLOW}Installing npm dependencies in $DEST_DIR...${NC}"
cd "$DEST_DIR" || { echo -e "${RED}Failed to change directory to $DEST_DIR${NC}"; exit 1; }
npm install

# Set permissions
echo -e "${YELLOW}Setting permissions...${NC}"
chmod +x "$DEST_DIR/server.js"
if [ -d "$DEST_DIR/scripts" ]; then
    chmod +x "$DEST_DIR/scripts"/*.js
fi

# Initialize databases only if they don't exist
if [ ! -f "$DEST_DIR/data/users.sqlite" ]; then
    echo -e "${YELLOW}Database not found. Initializing...${NC}"
    node "$DEST_DIR/scripts/init-db.js"
else
    echo -e "${GREEN}Existing database found. Skipping initialization.${NC}"
fi

# SSL Certificate generation is removed as requested.

# --- Service Setup ---
# Create systemd service
echo -e "${YELLOW}Creating systemd service...${NC}"
SERVICE_USER=$SUDO_USER
if [ -z "$SERVICE_USER" ]; then
    SERVICE_USER=$(whoami)
fi

sudo tee /etc/systemd/system/bluebridge.service > /dev/null << EOL
[Unit]
Description=BlueBridge Server
After=network.target

[Service]
Type=simple
User=$SERVICE_USER
WorkingDirectory=$DEST_DIR
ExecStart=/usr/bin/node $DEST_DIR/server.js
Restart=on-failure
Environment=NODE_ENV=production

[Install]
WantedBy=multi-user.target
EOL

# Create init.d script for broader compatibility
echo -e "${YELLOW}Creating init.d script...${NC}"
sudo tee /etc/init.d/bluebridge > /dev/null << EOL
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

# --- Final Steps ---
# Enable and start service
echo -e "${YELLOW}Reloading services and starting BlueBridge...${NC}"
sudo systemctl daemon-reload
sudo systemctl enable bluebridge
sudo systemctl restart bluebridge

echo -e "${GREEN}Installation completed successfully!${NC}"
echo -e "To check the service status, run: ${YELLOW}sudo systemctl status bluebridge${NC}"