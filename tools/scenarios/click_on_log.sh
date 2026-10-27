#!/usr/bin/env bash
# Clicks the mouse once on the DISPLAY when a line shows up in a log, then parks the pointer.
#
# A scenario cannot press a client's GUI button itself, and mp_check.py runs a "gregguns test" step to its end before the
# next step. Start this in the background before the step ("sh nohup tools/scenarios/click_on_log.sh ... &"): the
# scenario takes a labeled shot when the panel is ready, the client logs the shot, and this script clicks. Dev only.
#
# Coordinates are in the game window's own pixels (the window is centered on the display).
#
# Usage: click_on_log.sh <log> <pattern> <click-x> <click-y> <park-x> <park-y> [timeout-seconds]
set -u
log=$1 pattern=$2 cx=$3 cy=$4 px=$5 py=$6 limit=${7:-3000}
waited=0
until [ -f "$log" ] && grep -q -- "$pattern" "$log"; do
    sleep 0.5
    waited=$((waited + 1))
    if [ "$waited" -gt $((limit * 2)) ]; then
        echo "click_on_log: no '$pattern' in $log after ${limit}s" >&2
        exit 1
    fi
done
sleep 1
# The game window is centered on the display, not at its corner: aim inside the window.
win=$(xdotool search --onlyvisible --name Minecraft 2>/dev/null | head -1)
move() { if [ -n "$win" ]; then xdotool mousemove --window "$win" "$1" "$2"; else xdotool mousemove "$1" "$2"; fi; }
move "$cx" "$cy"
sleep 0.6
echo "click_on_log: window ${win:-none}, pointer at $(xdotool getmouselocation) for '$pattern'"
xdotool mousedown 1
sleep 0.15
xdotool mouseup 1
sleep 1.5
move "$px" "$py"
