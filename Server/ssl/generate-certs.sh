#!/bin/bash

# Set the device IP address (edit this variable to match your device)
DEVICE_IP="${DEVICE_IP:-192.168.0.204}"

# Set the SSL directory path
SSL_DIR="/opt/bluebridge/ssl"

# Create directory for certificates if it doesn't exist
mkdir -p "$SSL_DIR"

# Copy the config file to the SSL directory
cat > "$SSL_DIR/openssl.cnf" << EOL
[req]
default_bits = 2048
prompt = no
default_md = sha256
x509_extensions = v3_req
distinguished_name = dn

[dn]
C = FR
ST = IDF
L = Paris
O = BlueBridge Development
OU = Development
CN = $DEVICE_IP

[v3_req]
subjectAltName = @alt_names
basicConstraints = CA:FALSE
keyUsage = nonRepudiation, digitalSignature, keyEncipherment

[alt_names]
IP.1 = $DEVICE_IP
DNS.1 = localhost
EOL

# Generate CA private key and certificate
openssl genrsa -out "$SSL_DIR/ca_key.key" 4096
openssl req -x509 -new -nodes -key "$SSL_DIR/ca_key.key" -sha256 -days 3650 -out "$SSL_DIR/ca_cert.crt" -subj "/C=FR/ST=IDF/L=Paris/O=BlueBridge CA/OU=Development/CN=BlueBridge Root CA"

# Generate server_crt private key
openssl genrsa -out "$SSL_DIR/server_key.key" 2048

# Generate server_crt CSR using the configuration file
openssl req -new -key "$SSL_DIR/server_key.key" -out "$SSL_DIR/server_csr.csr" -config "$SSL_DIR/openssl.cnf"

# Sign the server_crt certificate with our CA
openssl x509 -req -in "$SSL_DIR/server_csr.csr" -CA "$SSL_DIR/ca_cert.crt" -CAkey "$SSL_DIR/ca_key.key" -CAcreateserial -out "$SSL_DIR/server_crt.crt" -days 365 -sha256 -extfile "$SSL_DIR/openssl.cnf" -extensions v3_req

# Set proper permissions
chmod 644 "$SSL_DIR/server_crt.crt" "$SSL_DIR/ca_cert.crt"
chmod 600 "$SSL_DIR/server_key.key" "$SSL_DIR/ca_key.key"

# Create PFX file for Windows import
openssl pkcs12 -export -out "$SSL_DIR/bluebridge.pfx" -inkey "$SSL_DIR/server.key" -in "$SSL_DIR/server.crt" -certfile "$SSL_DIR/ca.crt" -password pass:bluebridge

echo "Certificates generated successfully!"
echo "To trust these certificates:"
echo "1. Windows: Double-click ca.crt and install it in 'Trusted Root Certification Authorities'"
echo "2. Copy bluebridge.pfx to your Windows machine"
echo "3. Double-click bluebridge.pfx and follow the import wizard"
echo "4. Use password: bluebridge"

# Display the generated files
echo -e "\nGenerated files in $SSL_DIR:"
ls -l "$SSL_DIR" 