#!/bin/bash

# Set the SSL directory path
SSL_DIR="/opt/wellconnect/ssl"

# Create directory for certificates if it doesn't exist
mkdir -p "$SSL_DIR"

# Copy the config file to the SSL directory
cat > "$SSL_DIR/openssl.cnf" << 'EOL'
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
O = WellConnect Development
OU = Development
CN = 192.168.0.207

[v3_req]
subjectAltName = @alt_names
basicConstraints = CA:FALSE
keyUsage = nonRepudiation, digitalSignature, keyEncipherment

[alt_names]
IP.1 = 192.168.0.207
DNS.1 = localhost
EOL

# Generate CA private key and certificate
openssl genrsa -out "$SSL_DIR/ca.key" 4096
openssl req -x509 -new -nodes -key "$SSL_DIR/ca.key" -sha256 -days 3650 -out "$SSL_DIR/ca.crt" -subj "/C=FR/ST=IDF/L=Paris/O=WellConnect CA/OU=Development/CN=WellConnect Root CA"

# Generate server_crt private key
openssl genrsa -out "$SSL_DIR/server.key" 2048

# Generate server_crt CSR using the configuration file
openssl req -new -key "$SSL_DIR/server.key" -out "$SSL_DIR/server.csr" -config "$SSL_DIR/openssl.cnf"

# Sign the server_crt certificate with our CA
openssl x509 -req -in "$SSL_DIR/server.csr" -CA "$SSL_DIR/ca.crt" -CAkey "$SSL_DIR/ca.key" -CAcreateserial -out "$SSL_DIR/server.crt" -days 365 -sha256 -extfile "$SSL_DIR/openssl.cnf" -extensions v3_req

# Set proper permissions
chmod 644 "$SSL_DIR/server.crt" "$SSL_DIR/ca.crt"
chmod 600 "$SSL_DIR/server.key" "$SSL_DIR/ca.key"

# Create PFX file for Windows import
openssl pkcs12 -export -out "$SSL_DIR/wellconnect.pfx" -inkey "$SSL_DIR/server.key" -in "$SSL_DIR/server.crt" -certfile "$SSL_DIR/ca.crt" -password pass:wellconnect

echo "Certificates generated successfully!"
echo "To trust these certificates:"
echo "1. Windows: Double-click ca.crt and install it in 'Trusted Root Certification Authorities'"
echo "2. Copy wellconnect.pfx to your Windows machine"
echo "3. Double-click wellconnect.pfx and follow the import wizard"
echo "4. Use password: wellconnect"

# Display the generated files
echo -e "\nGenerated files in $SSL_DIR:"
ls -l "$SSL_DIR" 