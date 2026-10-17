#!/usr/bin/env bash
set -euo pipefail
mkdir -p emulator-evidence
# emulator-runner executes each script line in its own shell; keep trap and build together.
collect() {
  adb pull /sdcard/Download/spotenhancer-evidence emulator-evidence/ || true
  adb logcat -d -v time > emulator-evidence/logcat.txt || true
}
trap collect EXIT
gradle :installer:assembleDebug --no-daemon
adb install -r installer/build/outputs/apk/debug/installer-debug.apk
gradle :testhost:connectedDebugAndroidTest --no-daemon
# connectedAndroidTest uninstalls the fixture. Reinstall only our original test APKs.
adb install -r testhost/build/outputs/apk/debug/testhost-debug.apk
adb install -r testhost/build/outputs/apk/androidTest/debug/testhost-debug-androidTest.apk
runner='org.example.spotifymixer.testhost.test/androidx.test.runner.AndroidJUnitRunner'
restart_test='org.example.spotifymixer.testhost.RestartPersistenceTest'
adb shell am instrument -w -e class "$restart_test" -e persistence write "$runner" > emulator-evidence/persistence-write.txt
adb shell am force-stop org.example.spotifymixer.testhost
adb shell am instrument -w -e class "$restart_test" -e persistence read "$runner" > emulator-evidence/persistence-read.txt
python3 - <<'PY'
from pathlib import Path
for phase in ('write','read'):
    result=Path(f'emulator-evidence/persistence-{phase}.txt').read_text()
    assert 'OK (1 test)' in result and 'FAILURES' not in result, result
print('Own fixture replacement preferences survived process stop; Spotify queue restoration is not tested.')
PY
