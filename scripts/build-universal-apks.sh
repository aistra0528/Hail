#!/bin/bash
# Build universal APKs (all ABIs) for both debug and release

set -e

echo "🔨 Building universal APKs (all ABIs)..."

# Build universal debug APK
echo "📦 Building universal debug APK..."
./gradlew :app:assembleDebug --no-daemon -q

# Build universal release APK
echo "📦 Building universal release APK..."
./gradlew :app:assembleRelease --no-daemon -q

echo ""
echo "✅ Universal APKs built successfully!"
echo ""
echo "📁 Output locations:"
echo "   Debug:   app/build/outputs/apk/debug/"
echo "   Release: app/build/outputs/apk/release/"
echo ""
ls -la app/build/outputs/apk/debug/*.apk 2>/dev/null || echo "   (no debug APKs found)"
ls -la app/build/outputs/apk/release/*.apk 2>/dev/null || echo "   (no release APKs found)"
