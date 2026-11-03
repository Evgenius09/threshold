#!/usr/bin/env bash
# Deploy the CI-built mod jar for HEAD to the test phone, restart the game and check the log.
# Contract: tech.md, section 11.
set -euo pipefail

ADB_SERIAL="${THRESHOLD_ADB:-100.98.166.61:5555}"
PKG="${THRESHOLD_PKG:-com.movtery.zalithlauncher.v2}"
INSTANCE="${THRESHOLD_INSTANCE:-/sdcard/Android/data/com.movtery.zalithlauncher.v2/files/.minecraft/versions/1.21.1 Fabric 0.20.1}"
LOAD_TIMEOUT=180
SETTLE_SECONDS=30

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$ROOT/build/deploy"
LOG_REMOTE="$INSTANCE/logs/latest.log"

die() { echo "FAIL: $*" >&2; exit 1; }
adb_() { adb -s "$ADB_SERIAL" "$@"; }
# Paths contain spaces, so remote commands are passed as one quoted string.
rsh() { adb_ shell "$1"; }

cd "$ROOT"
VERSION="$(sed -n 's/^version=//p' gradle.properties)"
SHA="$(git rev-parse HEAD)"

# 1. CI run for HEAD
git fetch -q origin
git branch -r --contains "$SHA" | grep -q 'origin/' || die "HEAD not on origin, push first"

RUN_ID=""
for _ in $(seq 1 12); do
	RUN_ID="$(gh run list --commit "$SHA" --workflow build -L1 --json databaseId -q '.[0].databaseId')"
	[ -n "$RUN_ID" ] && break
	sleep 5
done
[ -n "$RUN_ID" ] || die "no CI run for ${SHA:0:7} after 60 s"
echo "CI run $RUN_ID for ${SHA:0:7}, waiting..."
gh run watch --exit-status "$RUN_ID" >/dev/null || die "CI run $RUN_ID is red: gh run view $RUN_ID --log-failed"

# 2. Artifact
rm -rf "$OUT"
mkdir -p "$OUT"
gh run download "$RUN_ID" -n threshold-jar -D "$OUT/jar"
JAR="$(ls "$OUT"/jar/threshold_gates-*.jar)"
[ "$(echo "$JAR" | wc -l)" -eq 1 ] || die "expected one jar in artifact, got: $JAR"

# 3. Phone
if ! adb_ get-state >/dev/null 2>&1; then
	adb connect "$ADB_SERIAL" >/dev/null 2>&1 || true
	adb_ get-state >/dev/null 2>&1 || die "phone not reachable at $ADB_SERIAL (Tailscale on? adb wireless on?)"
fi
rsh "mkdir -p '$INSTANCE/mods' && rm -f '$INSTANCE/mods/'threshold_gates-*.jar"
adb_ push "$JAR" "$INSTANCE/mods/" >/dev/null
echo "pushed $(basename "$JAR")"

# 4. Restart game and press «Запустить»
adb_ shell am force-stop "$PKG"
LAUNCH_TS="$(rsh 'date +%s' | tr -d '\r')"
adb_ shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1

tap_text() {
	rsh 'uiautomator dump /sdcard/threshold-ui.xml' >/dev/null 2>&1 || return 1
	local xml
	xml="$(rsh 'cat /sdcard/threshold-ui.xml; rm -f /sdcard/threshold-ui.xml')"
	local b
	b="$(echo "$xml" | grep -oE "text=\"$1\"[^>]*bounds=\"\[[0-9]+,[0-9]+\]\[[0-9]+,[0-9]+\]\"" | head -1 | grep -oE '[0-9]+' | tail -4 | tr '\n' ' ')" || true
	[ -n "$b" ] || return 1
	read -r x1 y1 x2 y2 <<<"$b"
	adb_ shell input tap $(((x1 + x2) / 2)) $(((y1 + y2) / 2))
}

launched=0
for _ in $(seq 1 10); do
	sleep 2
	if tap_text "Запустить"; then launched=1; break; fi
	tap_text "Главное меню" || true
done
[ "$launched" -eq 1 ] || die "launcher button «Запустить» not found"
echo "game starting..."

# 5. Wait for the mod list in a log written after launch
log_fresh() {
	local m
	m="$(rsh "stat -c %Y '$LOG_REMOTE' 2>/dev/null" | tr -d '\r')"
	[ -n "$m" ] && [ "$m" -ge "$LAUNCH_TS" ]
}
pull_log() { rsh "cat '$LOG_REMOTE'" >"$OUT/latest.log" 2>/dev/null || true; }
check_crash() {
	if grep -qE 'Crash report|---- Minecraft Crash Report' "$OUT/latest.log"; then return 0; fi
	grep -iE '(ERROR|Exception)' "$OUT/latest.log" | grep -qi 'threshold'
}
report_fail() {
	echo "---- last 30 lines of build/deploy/latest.log ----" >&2
	tail -30 "$OUT/latest.log" >&2
	die "$1"
}

deadline=$((SECONDS + LOAD_TIMEOUT))
loaded=0
while [ $SECONDS -lt $deadline ]; do
	sleep 5
	log_fresh || continue
	pull_log
	check_crash && report_fail "game crashed or logged a Threshold error during load"
	if grep -qE 'Loading [0-9]+ mods' "$OUT/latest.log"; then loaded=1; break; fi
done
[ "$loaded" -eq 1 ] || { pull_log; report_fail "no mod list in log after ${LOAD_TIMEOUT} s"; }

grep -qE "threshold_gates ${VERSION//./\\.}\b" "$OUT/latest.log" ||
	report_fail "threshold_gates $VERSION missing from the loaded mod list"

# A crash right after the mod list still counts as a failed deploy.
sleep "$SETTLE_SECONDS"
pull_log
check_crash && report_fail "game crashed or logged a Threshold error after load"
rsh "ps -A -o NAME" | grep -q "$PKG:game" || report_fail "game process is gone"

echo "OK: threshold_gates $VERSION loaded (log: build/deploy/latest.log)"
