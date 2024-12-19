#!/usr/bin/env bash

# Fail if any commands fails.
set -e

if ! command -v kotlinc > /dev/null 2>&1; then
    echo "Please install kotlin with: brew install kotlin"
    exit 1
fi


scripts/sort-version-catalog.main.kts
