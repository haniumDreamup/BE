#!/bin/bash

set -e

echo "Starting Android Testing Environment..."

# Set environment variables
export ANDROID_HOME=/opt/android-sdk
export PATH=${PATH}:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools:${ANDROID_HOME}/emulator
export FLUTTER_HOME=/opt/flutter
export PATH=${PATH}:${FLUTTER_HOME}/bin
export DISPLAY=:1

# Wait for display to be ready
echo "Waiting for display..."
until xset q &>/dev/null; do
    echo "Waiting for X server..."
    sleep 1
done

echo "Display is ready"

# Check if Flutter is working
echo "Checking Flutter installation..."
flutter doctor --verbose || true

# Start ADB server
echo "Starting ADB server..."
adb start-server

# Start Android emulator in background with GPU disabled for headless operation
echo "Starting Android emulator..."
emulator -avd test_android \
    -no-window \
    -no-audio \
    -gpu swiftshader_indirect \
    -no-boot-anim \
    -memory 2048 \
    -partition-size 4096 \
    -verbose &

EMULATOR_PID=$!

# Wait for emulator to be ready
echo "Waiting for emulator to boot..."
adb wait-for-device

# Give emulator time to fully boot
sleep 30

# Check emulator status
echo "Checking emulator status..."
adb devices

# Test Flutter on Android
if [ -d "/app" ] && [ -f "/app/pubspec.yaml" ]; then
    echo "Flutter app found in /app, running tests..."
    cd /app

    # Set production server URL
    export API_BASE_URL=http://43.200.49.171:8080
    echo "Using production server: $API_BASE_URL"

    # Get dependencies
    flutter pub get

    # Build for Android with production config
    flutter build apk --debug --dart-define=API_BASE_URL=http://43.200.49.171:8080 || echo "Build failed, but continuing..."

    # Install and run on emulator
    flutter run -d android --verbose --dart-define=API_BASE_URL=http://43.200.49.171:8080 &

    FLUTTER_PID=$!

    echo "Flutter app started with PID: $FLUTTER_PID"
    echo "Emulator PID: $EMULATOR_PID"

    # Monitor logs
    echo "Starting log monitoring..."
    while true; do
        echo "=== Flutter Logs $(date) ==="
        flutter logs 2>/dev/null | head -20 || true

        echo "=== ADB Logs $(date) ==="
        adb logcat -d | tail -20 || true

        sleep 10
    done
else
    echo "No Flutter app found in /app. Keeping emulator running for manual testing..."

    # Keep container alive and monitor emulator
    while kill -0 $EMULATOR_PID 2>/dev/null; do
        echo "Emulator is running. VNC available on port 5901"
        echo "ADB devices:"
        adb devices
        sleep 60
    done
fi