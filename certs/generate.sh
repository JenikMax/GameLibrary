#!/bin/bash
# Generate a self-signed certificate for development.
# For production, replace with Let's Encrypt certs.
set -euo pipefail

DOMAIN="${1:-game-library.local}"
DAYS=3650
KEY="key.pem"
CERT="cert.pem"

if [ -f "$KEY" ] && [ -f "$CERT" ]; then
    echo "Certificates already exist. Remove them to regenerate."
    exit 0
fi

openssl req -x509 -nodes -days "$DAYS" -newkey rsa:4096 \
    -keyout "$KEY" \
    -out "$CERT" \
    -subj "/CN=${DOMAIN}" \
    -addext "subjectAltName=DNS:${DOMAIN},DNS:localhost,IP:127.0.0.1"

chmod 600 "$KEY"
echo "Generated self-signed cert for ${DOMAIN} (${DAYS} days)"
echo "  key:  ${KEY}"
echo "  cert: ${CERT}"
