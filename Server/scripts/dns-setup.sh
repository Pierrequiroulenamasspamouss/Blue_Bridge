#!/bin/bash

# Configuration
FREEDNS_USERNAME="your_username"
FREEDNS_PASSWORD="your_password"
FREEDNS_HOSTNAME="your_hostname"
FREEDNS_DOMAIN="your_domain"
UPDATE_INTERVAL=300  # 5 minutes

# Function to get current IP
get_current_ip() {
    curl -s https://api.ipify.org
}

# Function to update DNS
update_dns() {
    local ip=$1
    curl -s "https://freedns.afraid.org/dynamic/update.php?${FREEDNS_USERNAME}:${FREEDNS_PASSWORD}@freedns.afraid.org&hostname=${FREEDNS_HOSTNAME}.${FREEDNS_DOMAIN}&myip=${ip}"
}

# Main loop
echo "Starting Dynamic DNS updater for ${FREEDNS_HOSTNAME}.${FREEDNS_DOMAIN}"
last_ip=""

while true; do
    current_ip=$(get_current_ip)
    
    if [ "$current_ip" != "$last_ip" ]; then
        echo "IP changed from $last_ip to $current_ip"
        response=$(update_dns "$current_ip")
        echo "DNS update response: $response"
        last_ip=$current_ip
    else
        echo "IP unchanged: $current_ip"
    fi
    
    sleep $UPDATE_INTERVAL
done 