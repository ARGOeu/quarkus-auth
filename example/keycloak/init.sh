#!/bin/sh

REALM_LIST="rciam ishare"

echo "Waiting 30 seconds..."
sleep 30
# Added 10 more seconds, if not started properly, does not get access token

echo "Short buffer for DB flush..."
sleep 50

echo "Getting Admin Token..."
TOKEN_RESPONSE=$(curl -s -X POST "http://keycloak:8080/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=tempadmin" \
  -d "password=tempadmin" \
  -d "grant_type=password" \
  -d "client_id=admin-cli")

if [ $? -ne 0 ]; then
  echo "ERROR: Failed to get token"
  exit 1
fi

# Extract token
ACCTOK=$(echo "$TOKEN_RESPONSE" | sed 's/.*"access_token":"\([^"]*\)".*/\1/')

if [ -z "$ACCTOK" ]; then
  echo "ERROR: Could not extract access token"
  exit 1
fi

echo "Token obtained successfully."

echo "Configuring Member User Attribute..."
for REALM_NAME in $REALM_LIST; do
  echo ""
  echo "[${REALM_NAME}] Configuring..."

  # --- can select different namespaces ---
  case "$REALM_NAME" in
    rciam)
      URN_NAMESPACE="urn:rciam.example.org"
      ;;
    ishare)
      URN_NAMESPACE="urn:ishare.test.example.org"
      ;;
    *)
      # Default fallback if realm is not listed above
      URN_NAMESPACE="urn:default.example.org"
      echo "Warning: No specific namespace found for '$REALM_NAME'. Using default."
      ;;
  esac

  echo "Using namespace: $URN_NAMESPACE"

  curl -s -X POST "http://keycloak:8080/realms/${REALM_NAME}/agm/admin/member-user-attribute/configuration" \
    -H "Authorization: Bearer $ACCTOK" \
    -H "Content-Type: application/json" \
    -H "Accept: application/json" \
    -d "{\"userAttribute\": \"localEntitlements\", \"urnNamespace\": \"$URN_NAMESPACE\", \"signatureMessage\": \"RCIAM Support team\"}"

  if [ $? -eq 0 ]; then
    echo "✓ [${REALM_NAME}] Success"
  else
    echo "✗ [${REALM_NAME}] Failed"
  fi

  # Small delay between requests
  sleep 1
done
echo ""
echo "All steps completed successfully."