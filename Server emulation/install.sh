#!/bin/bash

# Exit on error
set -e

echo "Installing WellConnect Reforge Server..."

# Update system packages
echo "Updating system packages..."
sudo apt-get update
sudo apt-get upgrade -y

# Install Node.js and npm
echo "Installing Node.js and npm..."
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt-get install -y nodejs

# Install build essentials (needed for some npm packages)
echo "Installing build essentials..."
sudo apt-get install -y build-essential

# Verify installations
echo "Verifying installations..."
node --version
npm --version

# Create service user and setup directories
echo "Creating service user and directories..."
if ! id "wellconnect" &>/dev/null; then
    sudo useradd -m -s /bin/bash wellconnect
else
    # Ensure home directory exists with proper permissions
    sudo mkdir -p /home/wellconnect
    sudo chown wellconnect:wellconnect /home/wellconnect
    sudo chmod 755 /home/wellconnect
fi

# Ensure .npm directory exists with proper permissions
sudo mkdir -p /home/wellconnect/.npm
sudo chown -R wellconnect:wellconnect /home/wellconnect/.npm

# Create application directory
echo "Setting up application directory..."
sudo mkdir -p /opt/wellconnect
sudo chown wellconnect:wellconnect /opt/wellconnect

# Copy application files
echo "Copying application files..."
sudo cp -r ./* /opt/wellconnect/

# Set proper permissions
echo "Setting permissions..."
sudo chown -R wellconnect:wellconnect /opt/wellconnect
sudo chmod -R 755 /opt/wellconnect

# Install dependencies
echo "Installing Node.js dependencies..."
cd /opt/wellconnect
# Use sudo -H to ensure home directory is used correctly
sudo -H -u wellconnect bash -c 'npm install'

# Create environment file
echo "Creating environment file..."
if [ ! -f .env ]; then
    cat > .env << EOL
PORT=3000
NODE_ENV=production
JWT_SECRET=$(openssl rand -base64 32)
DB_PATH=/opt/wellconnect/database.sqlite
EOL
    sudo chown wellconnect:wellconnect .env
    sudo chmod 600 .env
fi

# Create systemd service
echo "Creating systemd service..."
cat > /etc/systemd/system/wellconnect.service << EOL
[Unit]
Description=WellConnect Reforge Server
After=network.target

[Service]
Type=simple
User=wellconnect
WorkingDirectory=/opt/wellconnect
ExecStart=/usr/bin/npm start
Restart=always
Environment=NODE_ENV=production

[Install]
WantedBy=multi-user.target
EOL

# Reload systemd and start service
echo "Starting service..."
sudo systemctl daemon-reload
sudo systemctl enable wellconnect
sudo systemctl start wellconnect

# Install and configure Nginx as reverse proxy
echo "Installing and configuring Nginx..."
sudo apt-get install -y nginx

# Create SSL directory and generate self-signed certificate
echo "Generating self-signed SSL certificate..."
sudo mkdir -p /etc/nginx/ssl
sudo openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
-keyout /etc/nginx/ssl/nginx-selfsigned.key \
-out /etc/nginx/ssl/nginx-selfsigned.crt \
-subj "/C=US/ST=State/L=City/O=Organization/CN=localhost"

# Create Nginx configuration
cat > /etc/nginx/sites-available/wellconnect << EOL
# HTTP - Redirect all traffic to HTTPS
server {
    listen 80;
    listen [::]:80;
    server_name _;

    return 301 https://\$host\$request_uri;
}

# HTTPS - Serve application
server {
    listen 443 ssl;
    listen [::]:443 ssl;
    server_name _;

    # SSL Configuration
    ssl_certificate /etc/nginx/ssl/nginx-selfsigned.crt;
    ssl_certificate_key /etc/nginx/ssl/nginx-selfsigned.key;
    
    # SSL Security Settings
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_prefer_server_ciphers on;
    ssl_ciphers ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384:ECDHE-ECDSA-CHACHA20-POLY1305:ECDHE-RSA-CHACHA20-POLY1305:DHE-RSA-AES128-GCM-SHA256:DHE-RSA-AES256-GCM-SHA384;
    
    # Security headers
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header Referrer-Policy "no-referrer-when-downgrade" always;
    add_header Content-Security-Policy "default-src 'self' http: https: data: blob: 'unsafe-inline'" always;

    # Rate limiting
    limit_req_zone \$binary_remote_addr zone=wellconnect:10m rate=10r/s;
    limit_req zone=wellconnect burst=20 nodelay;

    location / {
        proxy_pass http://localhost:3000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host \$host;
        proxy_cache_bypass \$http_upgrade;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;

        # Timeouts
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # Gzip compression
    gzip on;
    gzip_vary on;
    gzip_min_length 10240;
    gzip_proxied expired no-cache no-store private auth;
    gzip_types text/plain text/css text/xml text/javascript application/x-javascript application/xml application/json;
    gzip_disable "MSIE [1-6]\.";
}
EOL

# Enable the site
sudo ln -sf /etc/nginx/sites-available/wellconnect /etc/nginx/sites-enabled/
sudo rm -f /etc/nginx/sites-enabled/default

# Move rate limiting to http context
echo "http {
    limit_req_zone \$binary_remote_addr zone=wellconnect:10m rate=10r/s;
    include /etc/nginx/mime.types;
    include /etc/nginx/conf.d/*.conf;
    include /etc/nginx/sites-enabled/*;
}" | sudo tee /etc/nginx/nginx.conf

# Test Nginx configuration
sudo nginx -t

# Restart Nginx
sudo systemctl restart nginx

# Configure firewall
echo "Configuring firewall..."
sudo apt-get install -y ufw
sudo ufw allow OpenSSH
sudo ufw allow 80
sudo ufw allow 443
sudo ufw --force enable

echo "Installation complete!"
echo "The server should now be accessible at:"
echo "HTTP:  http://$(hostname -I | awk '{print $1}')"
echo "HTTPS: https://$(hostname -I | awk '{print $1}')"
echo ""
echo "Note: When accessing via HTTPS, you will see a security warning because"
echo "we're using a self-signed certificate. This is normal for local development."
echo ""
echo "Check status with: sudo systemctl status wellconnect"
echo "View logs with: sudo journalctl -u wellconnect -f" 