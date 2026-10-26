#!/usr/bin/env bash
# A persistent, hot-reloading dev server for iterating on scenarios, so an edit costs a recompile and a redefine
# instead of a full server boot.
#
#   tools/scenarios/dev-server.sh start [--no-galacticraft]
#   tools/scenarios/dev-server.sh reload [--auto-restart]
#   tools/scenarios/dev-server.sh test <prefix>
#   tools/scenarios/dev-server.sh status
#   tools/scenarios/dev-server.sh stop
#
# FOR ITERATION ONLY. A server that stays up drifts: world state, static fields and registries keep what earlier runs
# did, and redefined classes do not re-run their static initializers. A green result here is a hint. Gates and final
# verification still use a fresh boot: tools/scenarios/run-headless.sh.
#
# start     Boots `./gradlew runServer` once, with the module flags, server setup and console pipe of run-headless.sh
#           (headless-lib.sh), a JDWP debug port on localhost, and waits for "Done". It then writes a ready marker.
#           The server stays up, detached, after this script and your ssh session end. One server per checkout.
# reload    Runs `./gradlew devServerSync` (compile, then copy into the server's class directory), then redefines
#           the server's loaded classes that changed (HotReload.java, over JDWP). Classes the server has not loaded
#           yet need nothing: they come from the class directory on first use. If the JVM refuses the redefinition,
#           or a resource file changed (those are read when the server boots), it prints "RESTART NEEDED" and exits
#           5; with --auto-restart it restarts the server instead.
# test      Sends `gregguns test <prefix>` to the console, waits up to GG_TEST_TIMEOUT seconds for the scenario
#           summary, prints the report (FAIL and SKIP lines, and the summary), and exits 0 on pass or 1 on fail,
#           timeout or a dead server. GG_VERBOSE=1 prints the PASS lines too.
# status    Prints whether the server runs, its ports, memory and CPU. Exit 0 if running and ready.
# stop      Stops this checkout's server: `stop` on the console, then TERM and KILL on its own pids only.
#
# What reloads (see the README in this folder for the table): method bodies, always. A plain JVM also takes a new
# private, static or final method; any other change of a class's shape (a field, a public or overridable method, an
# interface or superclass added or removed) is refused and needs a restart. With DCEVM (GG_DEVSRV_JAVA_HOME, below)
# those reload too. Never reloaded: anything a mod
# registers at boot (blocks, items, recipes, configs, lang and other resources), which is why a changed resource
# asks for a restart. A new class loads from the class directory when first used.
#
# Why JDWP with a small JDI client, not HotswapAgent: RFG's runServer runs the mod from the dev jar, which cannot be
# refreshed under a live JVM, so dev-server-init.gradle swaps it for a directory that a recompile keeps current. Only
# the redefinition of classes that are already loaded is then missing, and the JDK's own debug interface does that with
# no agent jar and no new Gradle dependency. The port is bound to 127.0.0.1. `./gradlew runServer --mcJvmArgs=...` passes the agent to the server JVM
# only; JAVA_TOOL_OPTIONS would also hit the Gradle JVMs and clash on the port.
#
# DCEVM: set GG_DEVSRV_JAVA_HOME to a DCEVM JDK 8 (the TravaOpenJDK 8 DCEVM build; its JVM is DCEVM by default).
# start and reload then give Gradle that JDK as the only Java 8 toolchain, so runServer runs on it, and the JVM accepts
# schema changes over the same JDWP redefinition.
#
# Environment:
#   GG_PORT              game port (default 25599)
#   GG_JDWP_PORT         debug port (default: the first free one of 5005..5009)
#   GG_MODULES           as in run-headless.sh (default: every module including icbm, since this is for ICBM work too)
#   GG_BOOT_TIMEOUT      seconds to wait for the boot (default 1200)
#   GG_TEST_TIMEOUT      seconds to wait for a scenario summary (default 1800)
#   GG_STOP_TIMEOUT      seconds to wait for a clean stop (default 120)
#   GG_GRADLE_ARGS       Gradle arguments for start and reload (default --no-daemon; e.g. "--offline")
#   GG_RELOAD_DAEMON     1: reload uses a warm Gradle daemon (about 4s instead of 11s, but ~0.8 GB more memory);
#                        stop ends the daemon whose working directory is this checkout
#   GG_DEVSRV_JAVA_HOME  DCEVM JDK 8 for the server (optional)
#   JAVA_HOME            JDK 11 or newer, to run HotReload.java and Gradle (see .java-version)
set -u
trap "" PIPE

here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
root="$(cd "$here/../.." && pwd)"
run_dir="$root/run/server"
state="$run_dir/dev"
fifo="$state/console"
log="$state/server.log"
manifest="$state/classes.manifest"
res_manifest="$state/resources.manifest"
settings="$state/settings"   # the flags start was given, so a restart repeats them
class_dirs=("$root/build/dev-server/main")   # dev-server-init.gradle: classes and resources in one directory
res_dir="$root/build/dev-server/main"
# shellcheck source=headless-lib.sh
. "$here/headless-lib.sh"

boot_timeout="${GG_BOOT_TIMEOUT:-1200}"
test_timeout="${GG_TEST_TIMEOUT:-1800}"
stop_timeout="${GG_STOP_TIMEOUT:-120}"
# shellcheck disable=SC2206
gradle_args=(${GG_GRADLE_ARGS---no-daemon})

die() { echo "dev-server: $*" >&2; exit 1; }
pid_alive() { [ -n "${1:-}" ] && kill -0 "$1" 2>/dev/null; }
read_state() { [ -f "$state/$1" ] && cat "$state/$1"; }
running() { [ -n "$(server_jvms)" ]; }
ready() { running && [ -f "$state/ready" ]; }
crashed() { grep -q -e '---- Minecraft Crash Report ----' -e 'FAILURE: Build failed' "$log" 2>/dev/null; }

# Gradle daemons started from this checkout (cwd = the project root). Only these are ever killed.
kill_root_daemons() {
  local d pid
  for d in /proc/[0-9]*; do
    pid="${d#/proc/}"
    [ "$(readlink "$d/cwd" 2>/dev/null)" = "$root" ] || continue
    tr '\0' ' ' < "$d/cmdline" 2>/dev/null | grep -q 'GradleDaemon' && kill "$pid" 2>/dev/null
  done
  return 0
}

# build_flags: sets `flags`, the Gradle arguments start and reload share (so Gradle's configuration cache is reused).
build_flags() {
  flags=(-I "$here/dev-server-init.gradle")
  [ -n "${GG_DEVSRV_JAVA_HOME:-}" ] || return 0
  [ -x "$GG_DEVSRV_JAVA_HOME/bin/java" ] || die "GG_DEVSRV_JAVA_HOME has no bin/java"
  # Gradle picks the Java 8 toolchain from the installation list. Put the DCEVM JDK in place of the plain Java 8 entry,
  # so it is the only Java 8 and runServer uses it.
  local list e keep=("$GG_DEVSRV_JAVA_HOME")
  list="$(sed -n 's/^org.gradle.java.installations.paths=//p' "${GRADLE_USER_HOME:-$HOME/.gradle}/gradle.properties" 2>/dev/null)"
  local IFS=,
  for e in $list; do
    grep -q 'JAVA_VERSION="1.8' "$e/release" 2>/dev/null || keep+=("$e")
  done
  flags+=("-Porg.gradle.java.installations.paths=${keep[*]}")
}

port_free() { ! (exec 3<>"/dev/tcp/127.0.0.1/$1") 2>/dev/null; }

# Hashes of the resource files, to notice a change the JVM cannot pick up. mcmod.info is left out: it carries the git
# state (a "-dirty" suffix) and nothing reads it after the boot.
resource_hashes() {
  if [ -d "$res_dir" ]; then (cd "$res_dir" && find . -type f ! -name '*.class' ! -name mcmod.info -print0 | sort -z | xargs -0 -r sha1sum); fi
}
snapshot_resources() { resource_hashes > "$res_manifest"; }

console_send() { echo "$1" > "$fifo"; }

# ---------------------------------------------------------------------------------------------------------------------
cmd_start() {
  local nogc=0
  while [ $# -gt 0 ]; do
    case "$1" in
      --no-galacticraft) nogc=1 ;;
      *) echo "unknown argument: $1" >&2; exit 2 ;;
    esac
    shift
  done
  running && die "a server is already running for this checkout (stop it first)"
  mkdir -p "$state"
  rm -f "$state/ready" "$state/pid" "$state/holder.pid"

  port="${GG_PORT:-25599}"
  modules="$(gg_modules_expand "${GG_MODULES:-all}")"
  gc_args=()
  [ "$nogc" -eq 1 ] && gc_args=(-PnoGalacticraft)
  gg_prepare_server || exit 2
  port_free "$port" || die "game port $port is in use"

  local jdwp="${GG_JDWP_PORT:-}" p
  if [ -z "$jdwp" ]; then
    for p in 5005 5006 5007 5008 5009; do
      if port_free "$p"; then jdwp="$p"; break; fi
    done
  fi
  [ -n "$jdwp" ] || die "no free debug port in 5005..5009"
  port_free "$jdwp" || die "debug port $jdwp is in use"
  echo "$jdwp" > "$state/jdwp.port"
  { echo "nogc=$nogc"; echo "port=$port"; echo "modules=$modules"; } > "$settings"

  build_flags
  local extra=("${flags[@]}" "--mcJvmArgs=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=127.0.0.1:$jdwp")
  rm -f "$fifo"; mkfifo "$fifo"
  : > "$log"
  # The holder keeps the pipe's write end open so the server never reads end-of-file; it blocks until the reader opens.
  setsid nohup bash -c 'exec 3>"$1"; exec sleep 2147483647' _ "$fifo" > /dev/null 2>&1 &
  echo $! > "$state/holder.pid"
  (cd "$root" && exec setsid nohup sh ./gradlew runServer --console=plain "${gc_args[@]}" "${gradle_args[@]}" \
    "${extra[@]}" \
    < "$fifo" > "$log" 2>&1) &
  echo $! > "$state/pid"

  echo "starting: modules $modules${gc_args[*]:+ (no Galacticraft)}, game port $port, debug port 127.0.0.1:$jdwp"
  local waited=0 answered=0 t0=$SECONDS
  until grep -Eq 'Done \([0-9.]+s\)!' "$log" 2>/dev/null; do
    if [ "$answered" -eq 0 ] && grep -q 'Do you want to start the server in online-mode' "$log" 2>/dev/null; then
      console_send n; answered=1
    fi
    if crashed || { ! pid_alive "$(read_state pid)" && ! running; }; then
      tail -n 25 "$log" >&2; cmd_stop > /dev/null 2>&1; die "the server crashed or exited during boot (log: $log)"
    fi
    if [ "$waited" -ge "$boot_timeout" ]; then
      tail -n 25 "$log" >&2; cmd_stop > /dev/null 2>&1; die "timed out after ${boot_timeout}s waiting for the boot"
    fi
    sleep 2; waited=$((waited + 2))
  done
  (cd "$root" && java "$here/HotReload.java" snapshot "$manifest" "${class_dirs[@]}") || die "could not snapshot the classes"
  snapshot_resources
  date +%s > "$state/ready"
  echo "ready in $((SECONDS - t0))s (log: $log)"
}

# ---------------------------------------------------------------------------------------------------------------------
cmd_reload() {
  local auto=0
  while [ $# -gt 0 ]; do
    case "$1" in
      --auto-restart) auto=1 ;;
      *) echo "unknown argument: $1" >&2; exit 2 ;;
    esac
    shift
  done
  ready || die "no ready server for this checkout (run start first)"
  local gc=()
  grep -q '^nogc=1' "$settings" 2>/dev/null && gc=(-PnoGalacticraft)
  local t0=$SECONDS
  # shellcheck disable=SC2086
  local rargs=("${gradle_args[@]}")
  [ "${GG_RELOAD_DAEMON:-0}" = 1 ] && rargs=()   # a warm Gradle daemon: about 4s instead of 11s per reload
  build_flags
  (cd "$root" && sh ./gradlew devServerSync --console=plain -q "${gc[@]}" "${flags[@]}" "${rargs[@]}") || die "compile failed"
  echo "compiled in $((SECONDS - t0))s"

  local why=""
  if [ -d "$res_dir" ] && ! resource_hashes | cmp -s - "$res_manifest"; then
    why="resource files changed (read only at boot)"
  else
    local rc=0
    (cd "$root" && java "$here/HotReload.java" redefine "$(read_state jdwp.port)" "$manifest" "${class_dirs[@]}") || rc=$?
    case "$rc" in
      0) echo "reload ok in $((SECONDS - t0))s"; return 0 ;;
      3) why="the JVM refused the class change" ;;
      *) die "hot reload failed (exit $rc)" ;;
    esac
  fi
  echo "RESTART NEEDED: $why"
  if [ "$auto" -eq 1 ]; then
    local flags=()
    grep -q '^nogc=1' "$settings" && flags=(--no-galacticraft)
    cmd_stop || true
    cmd_start "${flags[@]}"
    return $?
  fi
  return 5
}

# ---------------------------------------------------------------------------------------------------------------------
cmd_test() {
  [ $# -ge 1 ] || { echo "usage: dev-server.sh test <prefix>" >&2; exit 2; }
  ready || die "no ready server for this checkout (run start first)"
  local before waited=0 t0=$SECONDS
  before="$(wc -l < "$log")"
  console_send "gregguns test $1"
  until tail -n +"$((before + 1))" "$log" | grep -Eq 'Scenarios: [0-9]+ passed, [0-9]+ failed'; do
    running || { tail -n 25 "$log" >&2; die "the server died during the test"; }
    # A prefix needs a trailing "." (a package) or "*"; anything else must be a full scenario name.
    tail -n +"$((before + 1))" "$log" | grep -E 'Unknown scenario|gregguns.test.unknown' >&2 && exit 1
    [ "$waited" -ge "$test_timeout" ] && die "timed out after ${test_timeout}s waiting for the scenario summary"
    sleep 1; waited=$((waited + 1))
  done
  local out summary
  out="$(tail -n +"$((before + 1))" "$log")"
  # Each report line is in the log twice (the mod's logger and the console reply); show it once, without the prefix.
  if [ "${GG_VERBOSE:-0}" = 1 ]; then
    echo "$out" | sed -n 's/^.*\(\[GregGuns\].*\)$/\1/p' | awk '!seen[$0]++'
  else
    echo "$out" | sed -n 's/^.*\(\[GregGuns\].*\)$/\1/p' | grep -v -E '^\[GregGuns\] PASS ' | awk '!seen[$0]++'
  fi
  summary="$(echo "$out" | grep -E 'Scenarios: [0-9]+ passed, [0-9]+ failed' | tail -n 1)"
  echo "(test took $((SECONDS - t0))s)"
  [ "$(echo "$summary" | sed -E 's/.*passed, ([0-9]+) failed.*/\1/')" -eq 0 ]
}

# ---------------------------------------------------------------------------------------------------------------------
cmd_status() {
  local pids pid rss cpu
  pids="$(server_jvms | tr '\n' ' ')"
  if [ -z "$pids" ]; then echo "not running"; return 1; fi
  pid="${pids%% *}"
  rss="$(awk '/VmRSS/{printf "%.0f", $2/1024}' "/proc/$pid/status" 2>/dev/null)"
  cpu="$(ps -o %cpu= -p "$pid" 2>/dev/null | tr -d ' ')"
  echo "running: server pid $pid, game port $(sed -n 's/^port=//p' "$settings"), debug port 127.0.0.1:$(read_state jdwp.port)"
  echo "ready: $([ -f "$state/ready" ] && echo yes || echo "no (still booting)"); memory ${rss:-?} MB resident; cpu ${cpu:-?}% (lifetime average)"
  echo "log: $log"
  [ -f "$state/ready" ]
}

# ---------------------------------------------------------------------------------------------------------------------
cmd_stop() {
  local waited=0 hp
  if running && [ -p "$fifo" ]; then
    timeout 5 bash -c 'echo stop > "$1"' _ "$fifo" 2>/dev/null
    while running && [ "$waited" -lt "$stop_timeout" ]; do sleep 1; waited=$((waited + 1)); done
  fi
  kill_root_daemons         # a Gradle daemon left by GG_RELOAD_DAEMON=1 (its working directory is this checkout)
  kill_server_jvms          # only processes whose working directory is this checkout's run/server
  pid_alive "$(read_state pid)" && kill "$(read_state pid)" 2>/dev/null
  hp="$(read_state holder.pid)"
  pid_alive "$hp" && kill "$hp" 2>/dev/null
  rm -f "$state/ready" "$state/pid" "$state/holder.pid" "$fifo"
  echo "stopped"
}

sub="${1:-}"
[ $# -gt 0 ] && shift
case "$sub" in
  start) cmd_start "$@" ;;
  reload) cmd_reload "$@" ;;
  test) cmd_test "$@" ;;
  status) cmd_status ;;
  stop) cmd_stop ;;
  -h|--help|"") sed -n '2,56p' "$0"; [ -n "$sub" ] || exit 2 ;;
  *) echo "unknown subcommand: $sub" >&2; exit 2 ;;
esac
