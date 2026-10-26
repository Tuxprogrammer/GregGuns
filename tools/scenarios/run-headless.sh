#!/usr/bin/env bash
# Runs GregGuns' in-game scenarios headlessly on a dev dedicated server and reports the result.
#
#   tools/scenarios/run-headless.sh [--boot-only] [--no-galacticraft] [--command "<console command>"]
#                                   [--expect-log <text>]... [--expect-log-once <text>]... [--forbid-log <text>]...
#
# It writes the module flags and a minimal server setup into run/server/ (git-ignored), starts `./gradlew runServer`, waits
# for the server to finish booting, sends `gregguns test all` (or --command) to the console, waits for the
# `Scenarios: N passed, M failed` summary line, prints it, sends `stop`, and exits.
#
# A prefix runs only the scenarios whose name starts with it (a trailing "." or "*"), e.g. just the ICBM ones:
#   GG_MODULES=all tools/scenarios/run-headless.sh --command "gregguns test icbm."
#
# After the server has stopped, the log is checked against the assertions (fixed-string match): --expect-log needs the
# text at least once, --expect-log-once exactly once, --forbid-log never. With icbm on, the runner adds its own: without
# Galacticraft the self-disable line exactly once and no "ICBM module active", with Galacticraft "ICBM module active".
#
# Exit code: 0 when every scenario passed and every log assertion held (or, with --boot-only, when the server booted and
# stopped cleanly); 1 when a scenario or a log assertion failed, the server crashed or exited early, or a timeout
# expired; 2 on bad usage. On every exit the server JVM is stopped (`stop`, then killed after GG_STOP_TIMEOUT).
#
# Environment:
#   GG_MODULES        comma list of modules to switch on, the rest are switched off. Default: every module except
#                     icbm, i.e. guns,armor,machines,energyWeapons,utility,npcs,worldgen. Add icbm to test it, or
#                     use "all" for every module including icbm.
#   GG_WORK_DIR       where the server log and console pipe go (default run/server/headless/<stamp>-<pid>; the last 5
#                     default folders are kept, older ones are deleted)
#   GG_BOOT_TIMEOUT   seconds to wait for the server to finish booting (default 1200; the first run downloads and
#                     deobfuscates dependencies)
#   GG_TEST_TIMEOUT   seconds to wait for the summary line after the command is sent (default 1800)
#   GG_STOP_TIMEOUT   seconds to wait for a clean stop before the server is killed (default 120)
#   GG_PORT           server port (default 25599)
#   GG_GRADLE_ARGS    extra arguments for Gradle, e.g. "--offline"
#   JAVA_HOME         JDK for Gradle (see .java-version)
#
# --no-galacticraft passes -PnoGalacticraft to Gradle, which leaves Galacticraft and OpenComputers out of the runtime
# classpath (dependencies.gradle), to test that the ICBM module turns itself off.
set -u
trap "" PIPE  # a write to the console pipe after the server died must not kill this script

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
boot_only=0
gc_args=()
expect_any=()
expect_once=()
forbid=()
command_to_send="gregguns test all"
while [ $# -gt 0 ]; do
  case "$1" in
    --boot-only) boot_only=1 ;;
    --no-galacticraft) gc_args=(-PnoGalacticraft) ;;
    --command) shift; command_to_send="${1:?--command needs a value}" ;;
    --expect-log) shift; expect_any+=("${1:?--expect-log needs a value}") ;;
    --expect-log-once) shift; expect_once+=("${1:?--expect-log-once needs a value}") ;;
    --forbid-log) shift; forbid+=("${1:?--forbid-log needs a value}") ;;
    -h|--help) sed -n '2,36p' "$0"; exit 0 ;;
    *) echo "unknown argument: $1" >&2; exit 2 ;;
  esac
  shift
done

modules="${GG_MODULES:-guns,armor,machines,energyWeapons,utility,npcs,worldgen}"
[ "$modules" = "all" ] && modules="guns,armor,machines,energyWeapons,utility,npcs,worldgen,icbm"
boot_timeout="${GG_BOOT_TIMEOUT:-1200}"
test_timeout="${GG_TEST_TIMEOUT:-1800}"
stop_timeout="${GG_STOP_TIMEOUT:-120}"
port="${GG_PORT:-25599}"
# Word-split on purpose: GG_GRADLE_ARGS is a list of arguments.
# shellcheck disable=SC2206
extra_args=(${GG_GRADLE_ARGS:-})

run_dir="$root/run/server"  # the dev server's working directory
mkdir -p "$run_dir/config"

# --- server setup -------------------------------------------------------------------------------------------------
# Dev-only: the EULA is accepted for the throwaway dev server, and online mode is off because nobody connects.
echo "eula=true" > "$run_dir/eula.txt"
props="$run_dir/server.properties"
touch "$props"
set_prop() { # key value: replace the line or append it
  if grep -q "^$1=" "$props"; then sed -i "s|^$1=.*|$1=$2|" "$props"; else echo "$1=$2" >> "$props"; fi
}
set_prop online-mode false
set_prop server-port "$port"
# A world remembers its block and item ids, and FML refuses to open one that lost a mod's ids (it waits for
# /fml confirm). So the world without Galacticraft is a different world.
if [ "${#gc_args[@]}" -gt 0 ]; then set_prop level-name gg-scenarios-nogc; else set_prop level-name gg-scenarios; fi
set_prop level-seed 1337
set_prop spawn-monsters true
set_prop spawn-npcs true
set_prop spawn-animals true

# --- module flags -------------------------------------------------------------------------------------------------
# Rewrite only the `modules` category of config/gregguns.cfg; other categories are kept.
cfg="$run_dir/config/gregguns.cfg"
all_modules="guns armor machines energyWeapons utility npcs worldgen icbm"
for m in ${modules//,/ }; do
  case " $all_modules " in *" $m "*) ;; *) echo "unknown module in GG_MODULES: $m" >&2; exit 2 ;; esac
done
{
  if [ -f "$cfg" ]; then
    # Drop any existing `modules { ... }` block (it has no nested braces).
    awk 'BEGIN{skip=0} /^[[:space:]]*modules[[:space:]]*\{/{skip=1} skip==0{print} skip==1 && /\}/{skip=0}' "$cfg"
  fi
  echo "modules {"
  for m in $all_modules; do
    case ",$modules," in *",$m,"*) v=true ;; *) v=false ;; esac
    echo "    B:$m=$v"
  done
  echo "}"
} > "$cfg.new" && mv "$cfg.new" "$cfg"

# --- start the server ---------------------------------------------------------------------------------------------
if [ -n "${GG_WORK_DIR:-}" ]; then
  work="$GG_WORK_DIR"
else
  work="$run_dir/headless/$(date +%Y%m%d-%H%M%S)-$$"
fi
mkdir -p "$work"
if [ -z "${GG_WORK_DIR:-}" ]; then
  # Keep the newest 5 run folders (names start with the timestamp, so a name sort is a time sort).
  ls -1d "$run_dir"/headless/*/ 2>/dev/null | sort | head -n -5 | while read -r old; do rm -rf "$old"; done
fi
log="$work/server.log"
fifo="$work/console"
# runServer reads its console from stdin. Gradle forwards our stdin to the server, so a named pipe is the robust way
# to feed it commands later: the server holds the read end, and file descriptor 3 below is the write end, kept open
# for the whole run so the server never sees end-of-file before we say `stop`.
mkfifo "$fifo"
server_pid=""
fd3_open=0
forced=0  # set when the server had to be killed because it did not stop in time
alive() { [ -n "$server_pid" ] && kill -0 "$server_pid" 2>/dev/null; }

# The dev server is a child of the Gradle daemon, not of the Gradle client we started, so killing the client leaves it
# running with the port and the world lock. Find it by its working directory instead.
server_jvms() {
  local d pid
  for d in /proc/[0-9]*; do
    pid="${d#/proc/}"
    [ "$(readlink "$d/cwd" 2>/dev/null)" = "$run_dir" ] || continue
    tr '\0' ' ' < "$d/cmdline" 2>/dev/null | grep -q 'java' && echo "$pid"
  done
}
# kill_server_jvms: TERM, a bounded wait, then KILL.
kill_server_jvms() {
  local pids waited=0
  pids="$(server_jvms)"
  [ -n "$pids" ] || return 0
  # shellcheck disable=SC2086
  kill $pids 2>/dev/null
  while [ -n "$(server_jvms)" ] && [ "$waited" -lt 20 ]; do sleep 1; waited=$((waited + 1)); done
  pids="$(server_jvms)"
  # shellcheck disable=SC2086
  [ -z "$pids" ] || kill -9 $pids 2>/dev/null
}
# shutdown_server <seconds>: ask the server to stop, give it that long, then kill whatever is left.
shutdown_server() {
  local waited=0
  if [ "$fd3_open" -eq 1 ]; then
    echo "stop" >&3 2>/dev/null
  fi
  while alive && [ "$waited" -lt "$1" ]; do sleep 1; waited=$((waited + 1)); done
  if alive; then kill "$server_pid" 2>/dev/null; forced=1; fi
  if [ -n "$(server_jvms)" ]; then forced=1; fi
  kill_server_jvms
  if [ "$fd3_open" -eq 1 ]; then
    exec 3>&-
    fd3_open=0
  fi
}
if [ -n "$(server_jvms)" ]; then
  echo "a dev server is already running in $run_dir (pid $(server_jvms | tr '\n' ' ')); stop it first" >&2
  exit 1
fi

cleanup() {
  shutdown_server 20
  rm -f "$fifo"
}
trap cleanup EXIT

(cd "$root" && exec sh ./gradlew runServer --console=plain "${gc_args[@]}" "${extra_args[@]}" < "$fifo" > "$log" 2>&1) &
server_pid=$!
exec 3> "$fifo"
fd3_open=1

answered=0
crashed() { grep -q -e '---- Minecraft Crash Report ----' -e 'FAILURE: Build failed' "$log" 2>/dev/null; }
fail() {
  echo "FAIL: $1" >&2
  echo "server log: $log" >&2
  tail -n 25 "$log" >&2
  trap - EXIT
  shutdown_server 20
  rm -f "$fifo"
  exit 1
}

# wait_for <regex> <timeout seconds> <what>: poll the log until it matches; fail if the server dies first.
wait_for() {
  local waited=0
  until grep -Eq "$1" "$log" 2>/dev/null; do
    # The dev server (RFG's runServer) asks once, on stdin, whether to start in online mode. Answer "n": offline.
    if [ "$answered" -eq 0 ] && grep -q 'Do you want to start the server in online-mode' "$log" 2>/dev/null; then
      echo "n" >&3
      answered=1
    fi
    alive || fail "server exited before $3"
    crashed && fail "server crashed before $3"
    [ "$waited" -ge "$2" ] && fail "timed out after ${2}s waiting for $3"
    sleep 2; waited=$((waited + 2))
  done
}

echo "modules on: $modules${gc_args[*]:+ (no Galacticraft)}"
wait_for 'Done \([0-9.]+s\)!' "$boot_timeout" "the server to finish booting"
echo "server booted"

result=0
if [ "$boot_only" -eq 0 ]; then
  echo "$command_to_send" >&3
  wait_for 'Scenarios: [0-9]+ passed, [0-9]+ failed' "$test_timeout" "the scenario summary"
  summary="$(grep -E 'Scenarios: [0-9]+ passed, [0-9]+ failed' "$log" | tail -n 1)"
  grep -E '\] FAIL ' "$log" | sed 's/^/  /'
  echo "$summary"
  failed="$(echo "$summary" | sed -E 's/.*passed, ([0-9]+) failed.*/\1/')"
  [ "$failed" -eq 0 ] || result=1
fi

# --- stop ---------------------------------------------------------------------------------------------------------
shutdown_server "$stop_timeout"
[ "$forced" -eq 0 ] || fail "server did not stop within ${stop_timeout}s and was killed"
crashed && fail "server crashed while stopping"

# --- log assertions -----------------------------------------------------------------------------------------------
icbm_on=0
case ",$modules," in *",icbm,"*) icbm_on=1 ;; esac
if [ "$icbm_on" -eq 1 ]; then
  if [ "${#gc_args[@]}" -gt 0 ]; then
    expect_once+=("turns itself off")
    forbid+=("ICBM module active")
  else
    expect_any+=("ICBM module active")
    forbid+=("turns itself off")
  fi
fi
check_failed=0
for t in ${expect_any[@]+"${expect_any[@]}"}; do
  grep -qF -- "$t" "$log" || { echo "ASSERT FAIL: log lacks \"$t\"" >&2; check_failed=1; }
done
for t in ${expect_once[@]+"${expect_once[@]}"}; do
  n="$(grep -cF -- "$t" "$log")"
  [ "$n" -eq 1 ] || { echo "ASSERT FAIL: log has \"$t\" $n times, expected exactly 1" >&2; check_failed=1; }
done
for t in ${forbid[@]+"${forbid[@]}"}; do
  if grep -qF -- "$t" "$log"; then echo "ASSERT FAIL: log contains forbidden \"$t\"" >&2; check_failed=1; fi
done
[ "$check_failed" -eq 0 ] || result=1
# Surface the module lines a reader of --boot-only wants to see.
grep -E 'Module icbm|ICBM module' "$log" | head -n 5
echo "log: $log"
exit "$result"
