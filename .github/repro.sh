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
declare -A K=([q]="16 467" [w]="48 467" [e]="80 467" [r]="112 467" [t]="144 467" [y]="176 467" [u]="208 467" [i]="240 467" [o]="272 467" [p]="304 467" [a]="32 517" [s]="64 517" [d]="96 517" [f]="128 517" [g]="160 517" [h]="192 517" [j]="224 517" [k]="256 517" [l]="288 517" [z]="64 566" [x]="96 566" [c]="128 566" [v]="160 566" [b]="192 566" [n]="224 566" [m]="256 566" [_]="175 612" [BS]="293 566")
typ() { for c in "$@"; do adb shell input tap ${K[$c]}; sleep 0.35; done; }
typ h e l l o _ w o r l d; sleep 1; shot kb-hello-world
typ BS BS BS; sleep 1; shot kb-bs3
typ a b c; sleep 1; shot kb-abc
typ BS BS BS BS BS BS BS BS; sleep 1; shot kb-bs8
typ x y z; sleep 1; shot kb-xyz
adb shell input swipe 293 566 293 566 3000; sleep 1; shot kb-hold-bs
typ m o r e; sleep 1; shot kb-more
typ _ t e s t _; typ BS BS BS BS BS BS BS; sleep 1; shot kb-word-del
typ o k; sleep 1; shot kb-ok
adb logcat -d > "$OUT/logcat.txt"
