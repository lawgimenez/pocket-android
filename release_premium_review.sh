#!/bin/bash

## Builds a team build variant that purchases can be tested on (if they are whitelisted in Google Play)
##
## Usage:
## Using Terminal, from this directory, invoke:
## bash release_premium_review.sh
##
## Then find the apk in Pocket/build/outputs/apk/premiumReview/teamRelease/Pocket-premiumReview-teamRelease.apk
##

echo "BUILDING..."
./gradlew clean
./gradlew assemblePremiumReviewTeamRelease
echo "BUILD COMPLETE!"