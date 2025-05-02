const fs = require('fs');
const path = require('path');
const https = require('https');
const selfsigned = require('selfsigned');
const greenlock = require('greenlock-express');

class CertificateManager {
    constructor(config) {
        this.config = config;
        this.sslDir = path.join(__dirname, '../ssl');
        this.devCertsPath = {
            key: path.join(this.sslDir, 'dev_key.pem'),
            cert: path.join(this.sslDir, 'dev_cert.pem')
        };
        this.prodCertsPath = path.join(this.sslDir, 'production');
        
        // Ensure SSL directory exists
        if (!fs.existsSync(this.sslDir)) {
            fs.mkdirSync(this.sslDir, { recursive: true });
        }
    }

    async generateDevCertificates() {
        // Check if dev certificates already exist and are valid
        if (this.areDevCertificatesValid()) {
            console.log('Using existing development certificates');
            return this.loadDevCertificates();
        }

        console.log('Generating new development certificates...');
        const attrs = [{ name: 'commonName', value: 'localhost' }];
        const pems = selfsigned.generate(attrs, {
            algorithm: 'sha256',
            days: 365,
            keySize: 2048
        });

        // Save the certificates
        fs.writeFileSync(this.devCertsPath.key, pems.private);
        fs.writeFileSync(this.devCertsPath.cert, pems.cert);

        return {
            key: pems.private,
            cert: pems.cert
        };
    }

    areDevCertificatesValid() {
        try {
            if (!fs.existsSync(this.devCertsPath.key) || !fs.existsSync(this.devCertsPath.cert)) {
                return false;
            }

            const cert = fs.readFileSync(this.devCertsPath.cert);
            const certInfo = new (require('node-forge')).pki.certificateFromPem(cert);
            const expiryDate = new Date(certInfo.validity.notAfter);
            const now = new Date();

            // Return true if certificate exists and is not expired
            return expiryDate > now;
        } catch (error) {
            console.error('Error checking dev certificates:', error);
            return false;
        }
    }

    loadDevCertificates() {
        return {
            key: fs.readFileSync(this.devCertsPath.key),
            cert: fs.readFileSync(this.devCertsPath.cert)
        };
    }

    async setupProductionCertificates(app) {
        return new Promise((resolve, reject) => {
            greenlock.init({
                packageRoot: path.join(__dirname, '..'),
                configDir: this.prodCertsPath,
                maintainerEmail: this.config.email,
                cluster: false
            }).ready((glx) => {
                const gl = glx.manager;
                gl.defaults({
                    subscriberEmail: this.config.email,
                    agreeToTerms: true,
                    challenges: {
                        'http-01': {
                            module: 'acme-http-01-standalone'
                        }
                    }
                });

                // Check if we need to generate/renew certificates
                this.checkAndRenewCertificates(gl, this.config.domain)
                    .then(() => {
                        glx.serve(app);
                        resolve();
                    })
                    .catch(reject);
            });
        });
    }

    async checkAndRenewCertificates(gl, domain) {
        try {
            const sites = await gl.sites.get({ subject: domain });
            if (!sites || !sites.length || this.shouldRenewCertificate(sites[0])) {
                console.log('Generating/renewing production certificates...');
                await gl.certificates.add({
                    subject: domain,
                    altnames: [domain]
                });
                console.log('Production certificates updated successfully');
            } else {
                console.log('Using existing production certificates');
            }
        } catch (error) {
            console.error('Error checking/renewing certificates:', error);
            throw error;
        }
    }

    shouldRenewCertificate(site) {
        if (!site.renewAt) return true;
        const renewDate = new Date(site.renewAt);
        const now = new Date();
        // Renew if certificate expires in less than 30 days
        return (renewDate - now) < (30 * 24 * 60 * 60 * 1000);
    }
}

module.exports = CertificateManager; 