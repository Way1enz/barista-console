#!/bin/bash
# Build & Run — Brew & Bean Coffee Shop
# Requires: Java 25

set -e

echo "Brew & Bean — Building..."

# Collect all source files
SOURCES=$(find src -name "*.java")
mkdir -p out

# Compile (--enable-preview for latest features)
javac --enable-preview --release 25 -d out $SOURCES

echo "Build successful. Starting app..."
echo ""

# Run
java --enable-preview -cp out coffee.Main
