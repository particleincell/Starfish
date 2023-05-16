#!/bin/bash

# This will build a JAR file that starts at starfish.MainHeadless
# Starfish is dependent on the VTK library for the GUI only, which
# many users may not need if they are exclusively using the CLI.
# This script builds a JAR file without the GUI components such that
# users don't need to have the VTK library installed on their computer.

JAR_FILE="starfish.jar"
SOURCE_DIR="src"
MAIN_CLASS="starfish.MainHeadless"
TMP_DIR="tmp"

mkdir -p "$TMP_DIR"
# Copy Java files excluding those in the /src/starfish/gui folder
find "$SOURCE_DIR" -name "*.java" ! -path "*/starfish/gui/*" ! -path "src/starfish/Main.java" -exec cp --parents \{\} "$TMP_DIR" \;

javac -d "$TMP_DIR" -sourcepath "$TMP_DIR" $(find "$TMP_DIR" -name "*.java")
jar cfe "$JAR_FILE" "$MAIN_CLASS" -C "$TMP_DIR" .

rm -rf "$TMP_DIR"
