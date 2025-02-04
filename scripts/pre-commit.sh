#!/usr/bin/env bash

# Fail if any commands fails.
set -e

if ! command -v kotlinc > /dev/null 2>&1; then
    echo "Please install kotlin with: brew install kotlin"
    exit 1
fi

VERSION_CATALOG_PATH=gradle/libs.versions.toml
# If changes to version catalog are staged..
if git diff --cached --name-only | grep -q "^$VERSION_CATALOG_PATH$"; then
  # .. sort it..
  scripts/sort-version-catalog.main.kts "$VERSION_CATALOG_PATH"
  # .. and stage it.
  git add -- "$VERSION_CATALOG_PATH"
fi
