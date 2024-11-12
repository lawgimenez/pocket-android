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
sed -e "s/{release-version}/$RELEASE_VERSION/g" "scripts/release-pr.md" | \
  gh pr create \
    --title "Release $RELEASE_VERSION" \
    --body-file - \
    --base "beta" \
    --head "$RELEASE_BRANCH" \
    --label "ignore-for-release" \

# sed is a "stream editor", used here to replace placeholders in the PR body template
# the result is piped to gh (GitHub CLI)
# --body-file - tells it to read body from standard input, in this case the piped result from sed
