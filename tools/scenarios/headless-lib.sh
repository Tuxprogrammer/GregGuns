#!/usr/bin/env bash
# Shared pieces of run-headless.sh and dev-server.sh (sourced, not run): the dev server's setup files and finding its JVM.
#
# The caller sets these globals first:
#   run_dir   the server's working directory (<repo>/run/server)
#   port      the game port
#   modules   comma list of module names (already expanded from "all")
#   gc_args   array: (-PnoGalacticraft) or empty

# gg_modules_expand <list>: "all" becomes every module.
gg_modules_expand() {
  if [ "$1" = "all" ]; then echo "guns,armor,machines,energyWeapons,utility,npcs,worldgen,icbm"; else echo "$1"; fi
}

# gg_prepare_server: writes eula.txt, server.properties and the modules block of config/gregguns.cfg. Returns 2 on an
# unknown module name.
gg_prepare_server() {
  mkdir -p "$run_dir/config"
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
    case " $all_modules " in *" $m "*) ;; *) echo "unknown module in GG_MODULES: $m" >&2; return 2 ;; esac
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
}

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
