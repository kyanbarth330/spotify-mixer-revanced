#!/usr/bin/env bash
set -euo pipefail
mkdir -p emulator-evidence
# emulator-runner executes each script line in its own shell; keep trap and build together.
collect() {
  adb pull /sdcard/Android/data/org.example.spotifymixer.testhost/files/screenshots emulator-evidence/ || true
  adb logcat -d -v time > emulator-evidence/logcat.txt || true
}
trap collect EXIT
gradle :testhost:connectedDebugAndroidTest --no-daemon
