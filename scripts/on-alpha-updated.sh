# fail if any commands fails
set -e
# debug log
set -x

git config --global user.name "Bitrise Bot"
git config --global user.email "max+bitrisebot@getpocket.com"

git checkout alpha
java -jar project-tools/legacyTools.jar -incrementAppVersionPart patch 1 Pocket/build.gradle.kts
java -jar project-tools/legacyTools.jar -setAppVersionPart build 0 Pocket/build.gradle.kts

git add Pocket/build.gradle.kts
git commit -m "chore(ci): incremented patch version [skip ci]"
git push origin alpha
# Update commit hash, so that the next workflow picks up the new commit.
envman add --key BITRISE_GIT_COMMIT --value "$(git rev-parse HEAD)"
