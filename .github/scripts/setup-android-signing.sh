#!/bin/bash
set -e

# Create the .ci-secrets directory
mkdir -p "$GITHUB_WORKSPACE/.ci-secrets"

# Decode the keystore from base64 and save it locally
if [ -n "$ANDROID_KEYSTORE_BASE64" ]; then
  echo "Decoding Android keystore..."
  echo "$ANDROID_KEYSTORE_BASE64" | base64 -d > "$GITHUB_WORKSPACE/.ci-secrets/release.keystore"
  echo "✓ Keystore decoded successfully"
else
  echo "Warning: ANDROID_KEYSTORE_BASE64 not set"
fi