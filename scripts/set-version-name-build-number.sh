#!/usr/bin/env bash

# Fail if any commands fails.
set -e

BUILD_NUMBER=$1

SOURCE_FILE=Pocket/build.gradle.kts
SOURCE_PATTERN=\$versionBuild
REPLACEMENT=$BUILD_NUMBER

sed -i'.bkp' -e "s/$SOURCE_PATTERN/$REPLACEMENT/" "$SOURCE_FILE"
# sed is a "stream editor", used here to edit a file
#  -i modifies the file in place (writes the edited version to the same file)
#  '.bkp' after -i specifies an extension to use for the backup file
#         we don't need a backup file, but on macos it is required, so add it for portability
#  -e specifies the command to run
#     our command finds the last part of the version name in the gradle script and updates it to a new value
#  last argument is the path to the file to edit
