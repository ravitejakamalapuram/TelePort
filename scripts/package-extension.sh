#!/usr/bin/env zsh
# scripts/package-extension.sh — Creates a clean ZIP for the TelePort Chrome extension

# Define paths
SCRIPT_DIR="${0:A:h}"
ROOT_DIR="${SCRIPT_DIR:h}"
EXTENSION_DIR="${ROOT_DIR}/chrome-extension"
OUTPUT_ZIP="${ROOT_DIR}/teleport-chrome-extension.zip"

echo "Packaging TelePort Chrome Extension..."
echo "Source: ${EXTENSION_DIR}"
echo "Output: ${OUTPUT_ZIP}"

# Navigate to the extension folder to avoid nested root directory in the zip file
cd "${EXTENSION_DIR}" || exit 1

# Remove existing zip if it exists
if [[ -f "${OUTPUT_ZIP}" ]]; then
  rm "${OUTPUT_ZIP}"
fi

# Package extension files
zip -r "${OUTPUT_ZIP}" . \
  -x "*.DS_Store" \
  -x "Thumbs.db" \
  -x "*.map" \
  -x "*__pycache__*"

echo "Successfully packaged TelePort Chrome Extension!"
echo "Zip file size: $(du -h "${OUTPUT_ZIP}" | cut -f1)"
