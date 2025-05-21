const selfsigned = require('selfsigned');
const fs = require('fs');
const path = require('path');

// Create ssl directory if it doesn't exist
const sslDir = path.join(__dirname, '../ssl');
if (!fs.existsSync(sslDir)) {
    fs.mkdirSync(sslDir, { recursive: true });
}

// Generate certificates
const attrs = [
    { name: 'commonName', value: 'localhost' },
    { name: 'countryName', value: 'US' },
    { name: 'organizationName', value: 'BlueBridge' },
    { name: 'organizationalUnitName', value: 'Development' }
];

const pems = selfsigned.generate(attrs, {
    algorithm: 'sha256',
    days: 365,
    keySize: 2048,
    extensions: [
        {
            name: 'basicConstraints',
            cA: true
        },
        {
            name: 'keyUsage',
            keyCertSign: true,
            digitalSignature: true,
            nonRepudiation: true,
            keyEncipherment: true,
            dataEncipherment: true
        },
        {
            name: 'subjectAltName',
            altNames: [
                {
                    type: 2,
                    value: 'localhost'
                },
                {
                    type: 7,
                    ip: '127.0.0.1'
                }
            ]
        }
    ]
});

// Write certificates to files
fs.writeFileSync(path.join(sslDir, 'private.key'), pems.private);
fs.writeFileSync(path.join(sslDir, 'certificate.crt'), pems.cert);

console.log('SSL certificates generated successfully!');
console.log('Private key saved to:', path.join(sslDir, 'private.key'));
console.log('Certificate saved to:', path.join(sslDir, 'certificate.crt')); 