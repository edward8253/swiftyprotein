#!/bin/bash

source .android-env

if [ "$#" -ne 1 ]; then
    echo "Usage: $0 <debug|release>"
    exit 1
fi

case "$1" in
    debug)
        APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
        ./gradlew assembleDebug
        ;;
    release)
        APK_PATH="app/build/outputs/apk/release/app-release.apk"
        ./gradlew assembleRelease
        ;;
    *)
        echo "Invalid build type: $1. Expected 'debug' or 'release'."
        exit 1
        ;;
esac

DEVICE=$(adb devices | awk '$2 == "device" && $1 !~ /^emulator-/ {print $1; exit}')

if [ -z "$DEVICE" ]; then
    echo "No physical Android device found."
    sleep 5
    exit 1
fi

echo "Installing APK on device $DEVICE..."
adb -s "$DEVICE" install -r "$APK_PATH"
echo "APK installed successfully on device $DEVICE."
sleep 2

exit 0