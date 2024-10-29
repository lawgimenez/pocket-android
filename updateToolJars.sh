#!/bin/bash

# run this script after making changes to the project-tools module
# CI will use these updated jars

PROJECT_TOOLS_DIR=./project-tools

./gradlew project-tools:legacyToolJar
./gradlew project-tools:toolJar

rm "$PROJECT_TOOLS_DIR/legacyTools.jar"
rm "$PROJECT_TOOLS_DIR/tools.jar"

cp "$PROJECT_TOOLS_DIR/build/libs/legacyTools.jar" "$PROJECT_TOOLS_DIR/legacyTools.jar"
cp "$PROJECT_TOOLS_DIR/build/libs/tools.jar" "$PROJECT_TOOLS_DIR/tools.jar"
