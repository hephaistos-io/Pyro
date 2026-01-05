#!/bin/bash
# Stripe CLI Webhook Forwarding for Sandbox Testing
#
# This script starts the Stripe CLI to forward webhook events to your local server.
# Use this when running the application in sandbox mode.
#
# Prerequisites:
# 1. Install Stripe CLI: brew install stripe/stripe-cli/stripe
# 2. Login to Stripe: stripe login
# 3. Have your sandbox.env configured with your test API keys
#
# Usage:
#   ./scripts/stripe-sandbox.sh                    # Default endpoint
#   ./scripts/stripe-sandbox.sh http://localhost:8080/api/webhooks/stripe  # Custom endpoint

set -e

WEBHOOK_ENDPOINT="${1:-http://localhost:8080/api/webhooks/stripe}"

echo ""
echo "============================================="
echo "  Stripe Sandbox Webhook Forwarding"
echo "============================================="
echo ""
echo "Endpoint: $WEBHOOK_ENDPOINT"
echo ""
echo "Starting Stripe CLI..."
echo "Copy the webhook signing secret (whsec_xxx) to your sandbox.env file"
echo ""
echo "Press Ctrl+C to stop"
echo ""
echo "============================================="
echo ""

stripe listen --forward-to "$WEBHOOK_ENDPOINT" \
    --events checkout.session.completed,customer.subscription.created,customer.subscription.updated,customer.subscription.deleted,invoice.paid,invoice.payment_failed,charge.dispute.created,charge.dispute.updated,charge.dispute.closed
