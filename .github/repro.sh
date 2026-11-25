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
tap_text "Add entry" "236 596"; sleep 2; shot sheet
tap_text "Note"; shot note-focused
adb shell input text hello; sleep 1; shot typed-hello
for i in 1 2 3 4 5 6; do adb shell input keyevent 67; done; sleep 1; shot cleared
adb shell input swipe 160 180 160 330 400; sleep 1.5; shot scrolled-up
tap_text "Cash in"; shot cash-in
tap_text "Cash out"; shot cash-out
adb shell input text world; sleep 1; shot typed-world
adb logcat -d > "$OUT/logcat.txt"
