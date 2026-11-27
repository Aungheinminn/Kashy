#!/usr/bin/env bash
set -u
OUT=repro-out
mkdir -p "$OUT"
step=0
shot() { step=$((step+1)); adb exec-out screencap -p > "$OUT/$(printf %02d $step)-$1.png"; adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; adb pull /sdcard/ui.xml "$OUT/$(printf %02d $step)-$1.xml" >/dev/null 2>&1; }
tap_text() {
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  adb pull /sdcard/ui.xml ui.xml >/dev/null 2>&1
  local xy
  xy=$(python3 - "$1" <<'PY'
import re, sys, xml.etree.ElementTree as ET
want = sys.argv[1]
for n in ET.parse("ui.xml").iter("node"):
    if n.get("text") == want or n.get("content-desc") == want:
        x1, y1, x2, y2 = map(int, re.findall(r"\d+", n.get("bounds")))
        print((x1 + x2) // 2, (y1 + y2) // 2)
        break
PY
)
  [ -z "$xy" ] && xy="${2:-}"
  echo "tap '$1' -> $xy"
  [ -n "$xy" ] && adb shell input tap $xy
  sleep 1.5
}
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell settings put global window_animation_scale 0
adb shell am start -n com.aungheinmin.kashy/.MainActivity
sleep 6
shot home
tap_text "Add entry"; sleep 2; shot sheet
key() { case $1 in 1) adb shell input tap 40 473;; 2) adb shell input tap 120 473;; 3) adb shell input tap 199 473;; 4) adb shell input tap 40 520;; 5) adb shell input tap 120 520;; BS) adb shell input tap 278 566;; esac; sleep 0.4; }
tap_text "Amount"; sleep 1; shot amount-focused
for k in 1 2 3 4 5; do key $k; done; sleep 1; shot keypad-12345
for k in BS BS BS; do key $k; done; sleep 1; shot keypad-bs3
for k in 3 4; do key $k; done; sleep 1; shot keypad-34
adb shell input swipe 278 566 278 566 2500; sleep 1; shot keypad-hold-bs
for k in 1 2; do key $k; done; sleep 1; shot keypad-12-after-hold
adb shell input swipe 160 400 160 120 500; sleep 1.5; shot scrolled
tap_text "Note"; sleep 1.5; shot note-focused
adb shell input text hello; adb shell input keyevent 62; adb shell input text world; sleep 1; shot typed-hello-world
adb shell uiautomator dump /sdcard/ui.xml >/dev/null; adb pull /sdcard/ui.xml "$OUT/kbd.xml" >/dev/null
adb shell dumpsys input_method | grep -E "mCurMethodId|mCurrentInputMethodInfo|mInputShown" > "$OUT/ime.txt"
adb shell wm size >> "$OUT/ime.txt"
for i in 1 2 3 4 5 6; do adb shell input tap 290 600; sleep 0.3; done; sleep 1; shot note-bs-guess
adb shell input text again; sleep 1; shot typed-again
adb logcat -d > "$OUT/logcat.txt"
