#!/bin/sh
# Stripe CLI Entrypoint for Docker Sandbox Testing
#
# This script:
# 1. Starts stripe listen with API key authentication
# 2. Parses the webhook signing secret from output
# 3. Writes secret to shared volume for webapp-api to read
# 4. Continues forwarding webhook events

set -e

SHARED_DIR="/shared"
SECRET_FILE="$SHARED_DIR/stripe-webhook-secret"
EVENTS="checkout.session.completed,customer.subscription.created,customer.subscription.updated,customer.subscription.deleted,invoice.paid,invoice.payment_failed,charge.dispute.created,charge.dispute.updated,charge.dispute.closed"

# Ensure shared directory exists
mkdir -p "$SHARED_DIR"

# Remove stale secret file from previous runs
rm -f "$SECRET_FILE"

echo "Starting Stripe CLI webhook forwarding..."
echo "API Key: ${STRIPE_API_KEY:0:12}..."
echo "Forward URL: $FORWARD_URL"
echo "Events: $EVENTS"
echo ""

# Start stripe listen and process output
# The secret appears in a line like: "Ready! Your webhook signing secret is whsec_xxx"
stripe listen \
    --api-key "$STRIPE_API_KEY" \
    --forward-to "$FORWARD_URL" \
    --events "$EVENTS" \
    2>&1 | while IFS= read -r line; do
        echo "$line"

        # Parse webhook secret from output
        if echo "$line" | grep -q "webhook signing secret is"; then
            secret=$(echo "$line" | grep -oE 'whsec_[a-zA-Z0-9]+')
            if [ -n "$secret" ]; then
                echo "$secret" > "$SECRET_FILE"
                echo ""
                echo "=== Webhook secret written to $SECRET_FILE ==="
                echo ""
            fi
        fi
    done
