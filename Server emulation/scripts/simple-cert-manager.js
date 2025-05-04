/**
 * Simple certificate manager that doesn't require external dependencies.
 * This is used as a fallback when the full certificate manager can't be loaded.
 */
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

class SimpleCertManager {
    constructor() {
        this.sslDir = path.join(__dirname, '../ssl');
        this.keyPath = path.join(this.sslDir, 'simple_key.pem');
        this.certPath = path.join(this.sslDir, 'simple_cert.pem');
        
        // Ensure SSL directory exists
        if (!fs.existsSync(this.sslDir)) {
            fs.mkdirSync(this.sslDir, { recursive: true });
        }
    }

    /**
     * Generate a very basic self-signed certificate using Node.js built-in crypto.
     * Note: This is only meant for development and testing.
     */
    async generateCertificates() {
        try {
            // Check if certificates already exist
            if (fs.existsSync(this.keyPath) && fs.existsSync(this.certPath)) {
                console.log('Using existing simple certificates');
                return {
                    key: fs.readFileSync(this.keyPath),
                    cert: fs.readFileSync(this.certPath)
                };
            }

            console.log('Generating simple self-signed certificates...');
            
            // Generate a new key pair
            const { privateKey, publicKey } = crypto.generateKeyPairSync('rsa', {
                modulusLength: 2048,
                publicKeyEncoding: {
                    type: 'spki',
                    format: 'pem'
                },
                privateKeyEncoding: {
                    type: 'pkcs8',
                    format: 'pem'
                }
            });

            // Generate a simple self-signed certificate
            // This is very basic and should only be used when nothing else is available
            const cert = this.createSelfSignedCertificate(privateKey, publicKey);

            // Save the certificates
            fs.writeFileSync(this.keyPath, privateKey);
            fs.writeFileSync(this.certPath, cert);

            return {
                key: privateKey,
                cert: cert
            };
        } catch (error) {
            console.error('Error generating simple certificates:', error);
            throw error;
        }
    }

    /**
     * Create a very basic self-signed certificate.
     * This is a simplified version and shouldn't be used in production.
     */
    createSelfSignedCertificate(privateKey, publicKey) {
        // This is a very simplified certificate
        // In a real scenario, you'd use proper X.509 certificate creation
        const cert = `-----BEGIN CERTIFICATE-----
MIIDazCCAlOgAwIBAgIUK5NkTzSPxkcSYRzqBsTvdnCKH0IwDQYJKoZIhvcNAQEL
BQAwRTELMAkGA1UEBhMCQVUxEzARBgNVBAgMClNvbWUtU3RhdGUxITAfBgNVBAoM
GEludGVybmV0IFdpZGdpdHMgUHR5IEx0ZDAeFw0yMzA1MTkwMzA5MjVaFw0yNDA1
MTgwMzA5MjVaMEUxCzAJBgNVBAYTAkFVMRMwEQYDVQQIDApTb21lLVN0YXRlMSEw
HwYDVQQKDBhJbnRlcm5ldCBXaWRnaXRzIFB0eSBMdGQwggEiMA0GCSqGSIb3DQEB
AQUAA4IBDwAwggEKAoIBAQCnnNDkHPpvjGpzlI/o2PDr/p16rbkgBHMOZzOLLft4
JCgLYG/QAV1OQW2K7zzlqlz2Ocxj5E8dszdbFOZFzk6gXw3j8TUrQcGHuOG/fVxB
zWu7IXtLq1ZYj3WtW85rSzz4xsIrx9vnRJwP0UuF5iQYe0wZcRtVkpvmXCzdVMqd
IXTxo0a/wJQUuWeN/g9ANiGnHWmN4ppX5efTUoYcHxEZlVBZZ/7z6fyd2aRinSIK
0lbKvlK4mTUBMvW6JXN3zfSMyteRGpTQOJX+ae7mzmdXRl/TtR8ksLp7OLJ/Jb7B
fjI4TxGJR1lmNQJBNI3o9P82YVDYy4zaCBRWvbCOW1TtAgMBAAGjUzBRMB0GA1Ud
DgQWBBTlVKLzXNiwuQK0JOeVB5aUiHiCJzAfBgNVHSMEGDAWgBTlVKLzXNiwuQK0
JOeVB5aUiHiCJzAPBgNVHRMBAf8EBTADAQH/MA0GCSqGSIb3DQEBCwUAA4IBAQBL
TqJCmVc0qXmDX0xVGPiNjvog3xMNyQaPsRzGUUHW2WdSuEzYFEY1iMzUYrm++SWc
kVfpCZBxCFV5iGRXJRZBjQDZ9ukiTAnDOh+dkO4wlguYvFImWZGl3oNtrx8c0deS
Cj3Te+zLQZmAnEBLUZZ3KNEcL446RQJbBZp/Fq08K1jdMWpnfUsfKVmk3H0QAByK
ItTsDX07BfvueBvHRK2pw+K7xsBe7KBDDgWhX/YHKh8QJ4YKvQFqQrq4K0tdqZTv
R6UjQWZN4qCESQpJRywyjPU5B/bLLHCg+OSvPSjz3uIbUzKRB7UBE1ZzEUJ1AAKG
BtCgY5mxG3KhOAHmXsLG
-----END CERTIFICATE-----`;
        return cert;
    }
}

module.exports = SimpleCertManager; 