#!/bin/bash
set -e

# Create the .ci-secrets directory
mkdir -p "$GITHUB_WORKSPACE/.ci-secrets"

# Decode the keystore from base64 and save it
if [ -n "$ANDROID_KEYSTORE_BASE64" ]; then
  echo "Decoding Android keystore..."
  echo "$ANDROID_KEYSTORE_BASE64" | base64 -d > "$GITHUB_WORKSPACE/.ci-secrets/release.keystore"
  echo "✓ Keystore decoded successfully"
else
  echo "Warning: ANDROID_KEYSTORE_BASE64 not set"
fi

# Decode google-services.json if provided (optional)
if [ -n "$PLAY_SERVICE_JSON_BASE64" ]; then
  echo "Decoding google-services.json..."
  echo "$PLAY_SERVICE_JSON_BASE64" | base64 -d > "$GITHUB_WORKSPACE/app/google-services.json"
  echo "✓ google-services.json decoded successfully"
else
  echo "Note: PLAY_SERVICE_JSON_BASE64 not set (optional)"
fi
