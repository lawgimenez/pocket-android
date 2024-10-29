#!/usr/bin/env bash

# Fail if any commands fails.
set -e

RELEASE_BRANCH=$1

if [ -z "$RELEASE_BRANCH" ]; then
  echo "RELEASE_BRANCH is missing!"
  exit 1
fi

# Assuming release branch pattern, extract version for PR title.
RELEASE_VERSION=${RELEASE_BRANCH#release-}

echo "Opening release PR…"
gh pr create \
  --title "Release $RELEASE_VERSION" \
  --body-file "scripts/release-pr.md" \
  --base "beta" \
  --head "$RELEASE_BRANCH" \
  --label "ignore-for-release" \
