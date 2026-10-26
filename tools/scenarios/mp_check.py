"""Two-client multiplayer check (and one-client screenshot driver) for the GregGuns dev environment.

Boots a dedicated dev server and one or more autotest clients (users Alice, Bob, Carol, ...), runs console steps on the
server, collects the logs and the clients' screenshots, then shuts everything down. Dev environment only.

Usage (from any directory; needs a display, e.g. Xvfb, for the clients):

    DISPLAY=:52 LIBGL_ALWAYS_SOFTWARE=1 MESA_GL_VERSION_OVERRIDE=3.3 GG_MODULES=all \\
      python -I tools/scenarios/mp_check.py <server-dir> <client-a-dir> [<client-b-dir> ...] \\
        [--port 25613] [--out build/mp-check] [--step "<console command>"]... [--join-timeout 1500]

* Every directory is its own git worktree or clone of the same commit: two Gradle builds cannot share a project dir.
* The Java and Gradle environment must already be set up (JAVA_HOME and so on); the script only calls ./gradlew.
* GG_MODULES is a comma list of modules to switch on in the server and every client (default
  guns,armor,machines), or "all" for every module including icbm.
* Steps run in order on the server console. A step starting with "gregguns test " waits for the
  "Scenarios: N passed, M failed" summary line (up to 15 minutes by default); "wait <seconds>" sleeps; anything
  else is sent and followed by a short pause. "sh <command>" runs a local shell command (for example
  "sh xdotool mousemove 940 530" to park the pointer outside the GUI). A "say [gregguns-autotest] <signal>" step steers every client.
  With no --step the steps are "gregguns test mp.sync" and "gregguns test mp.visual".
* Exit code 0 when every client logged "AUTOTEST joined", every test step ended with 0 failed and nothing crashed.
  The result line is "RESULT ok=<bool> steps=[...]". Logs and screenshots land under --out (server.log,
  client-<name>.log, shots/<name>-*.png). Read the PNGs: the script only proves that files were written.

See docs/testing/README.md, "Running the whole check".
"""

import glob
import os
import re
import shutil
import subprocess
import sys
import threading
import time

NAMES = ["Alice", "Bob", "Carol", "Dave"]
SUMMARY = re.compile(r"Scenarios: (\d+) passed, (\d+) failed")


def parse(argv):
    dirs, steps, opts = [], [], {"port": "25613", "out": "build/mp-check", "join": 1500, "wait": 900}
    i = 0
    while i < len(argv):
        a = argv[i]
        if a in ("--port", "--out", "--join-timeout", "--step-timeout", "--step"):
            i += 1
            v = argv[i]
            if a == "--step":
                steps.append(v)
            elif a == "--join-timeout":
                opts["join"] = int(v)
            elif a == "--step-timeout":
                opts["wait"] = int(v)
            else:
                opts[a[2:]] = v
        else:
            dirs.append(os.path.abspath(a))
        i += 1
    if len(dirs) < 2 or len(dirs) - 1 > len(NAMES):
        sys.exit(__doc__)
    return dirs[0], dirs[1:], steps or ["gregguns test mp.sync", "gregguns test mp.visual"], opts


def enable_modules(run_dir, modules):
    os.makedirs(os.path.join(run_dir, "config"), exist_ok=True)
    cfg = os.path.join(run_dir, "config", "gregguns.cfg")
    text = open(cfg).read() if os.path.exists(cfg) else "modules {\n}\n"
    for m in modules:
        if re.search(r"B:%s=" % m, text):
            text = re.sub(r"B:%s=\w+" % m, "B:%s=true" % m, text)
        else:
            text = re.sub(r"modules \{\n", "modules {\n    B:%s=true\n" % m, text, count=1)
    open(cfg, "w").write(text)


def set_lines(path, kv, sep):
    keep = []
    if os.path.exists(path):
        keep = [l for l in open(path).read().splitlines() if not l.startswith(tuple(k + sep for k in kv))]
    open(path, "w").write("\n".join(keep + [k + sep + v for k, v in kv.items()]) + "\n")


class Proc:
    """A Gradle child with a log file, a line list and a stdin."""

    def __init__(self, name, root, args, log):
        self.name, self.lines = name, []
        self.p = subprocess.Popen(
            [os.path.join(root, "gradlew")] + args + ["--console=plain"], cwd=root, stdin=subprocess.PIPE,
            stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, errors="replace")
        self.f = open(log, "w")
        threading.Thread(target=self._pump, daemon=True).start()

    def _pump(self):
        for line in self.p.stdout:
            self.lines.append(line)
            self.f.write(line)
            self.f.flush()

    def wait_for(self, rx, seconds, start=0):
        end = time.time() + seconds
        while time.time() < end and self.p.poll() is None:
            for l in self.lines[start:]:
                m = re.search(rx, l)
                if m:
                    return m
            time.sleep(2)
        for l in self.lines[start:]:
            m = re.search(rx, l)
            if m:
                return m
        return None

    def send(self, text):
        try:
            self.p.stdin.write(text + "\n")
            self.p.stdin.flush()
        except OSError:
            pass


def main():
    server_dir, client_dirs, steps, opts = parse(sys.argv[1:])
    out = os.path.abspath(opts["out"])
    os.makedirs(out, exist_ok=True)
    mods = os.environ.get("GG_MODULES", "guns,armor,machines")
    if mods == "all":
        mods = "guns,armor,machines,energyWeapons,utility,npcs,worldgen,icbm"
    modules = [m for m in mods.split(",") if m]
    users = NAMES[:len(client_dirs)]

    sdir = os.path.join(server_dir, "run", "server")
    os.makedirs(sdir, exist_ok=True)
    open(os.path.join(sdir, "eula.txt"), "w").write("eula=true\n")
    set_lines(os.path.join(sdir, "server.properties"), {
        "server-port": opts["port"], "online-mode": "false", "level-name": "mpcheck-world", "level-type": "FLAT",
        "generate-structures": "false", "spawn-monsters": "false", "spawn-animals": "false", "difficulty": "0",
        "gamemode": "0", "spawn-protection": "0", "view-distance": "6", "allow-flight": "true"}, "=")
    enable_modules(sdir, modules)
    for d in client_dirs:
        cdir = os.path.join(d, "run", "client")
        os.makedirs(cdir, exist_ok=True)
        enable_modules(cdir, modules)
        set_lines(os.path.join(cdir, "options.txt"), {
            "pauseOnLostFocus": "false", "snooperEnabled": "false", "musicVolume": "0.0", "enableVsync": "false",
            "maxFps": "30", "guiScale": "2", "viewDistance": "6"}, ":")
        shutil.rmtree(os.path.join(cdir, "screenshots"), ignore_errors=True)

    ok, results = False, []
    server, clients = None, []
    try:
        server = Proc("server", server_dir, ["runServer"], os.path.join(out, "server.log"))
        assert server.wait_for(r"Done \(.*For help", 1500), "server did not boot"
        for user in users:
            server.send("op " + user)
        for user, d in zip(users, client_dirs):
            c = Proc(user, d, [
                "runClient", "--username=" + user, "--mcJvmArgs=-Dgregguns.autotest=" + user.lower()[0],
                "--mcJvmArgs=-Dgregguns.autotest.connect=127.0.0.1:" + opts["port"]]
                + ["--mcArgs=" + w for w in ("--width", "960", "--height", "540")], os.path.join(out, "client-%s.log" % user))
            clients.append(c)
            # One client at a time: each join must finish before the next client starts.
            assert c.wait_for(r"AUTOTEST joined", opts["join"]), user + " did not join"
        time.sleep(15)
        for step in steps:
            if step.startswith("sh "):
                subprocess.call(step[3:], shell=True)  # e.g. xdotool mousemove, on the same DISPLAY
                results.append((step, None))
                continue
            if step.startswith("wait "):
                time.sleep(float(step.split()[1]))
                results.append((step, None))
                continue
            mark = len(server.lines)
            server.send(step)
            if step.startswith("gregguns test "):
                m = server.wait_for(SUMMARY.pattern, opts["wait"], mark)
                results.append((step, (int(m.group(1)), int(m.group(2))) if m else None))
                time.sleep(20)  # let the clients take their last shots
            else:
                time.sleep(3)
                results.append((step, None))
        tests = [r for _, r in results if r is not None]
        crashed = any(l.startswith("---- Minecraft Crash Report") for p in [server] + clients for l in p.lines)
        wanted = [s for s, _ in results if s.startswith("gregguns test ")]
        ok = (not crashed and len(tests) == len(wanted)
              and all(r[1] == 0 and r[0] > 0 for r in tests))
    except AssertionError as e:
        print("FAILED:", e)
    finally:
        for c in clients:
            c.send("quit")
        if server:
            server.send("say [gregguns-autotest] quit")
            time.sleep(5)
            server.send("stop")
        for p in [server] + clients:
            if p:
                try:
                    p.p.wait(timeout=120)
                except subprocess.TimeoutExpired:
                    p.p.kill()
        os.makedirs(os.path.join(out, "shots"), exist_ok=True)
        for user, d in zip(users, client_dirs):
            for png in glob.glob(os.path.join(d, "run", "client", "screenshots", "*.png")):
                shutil.copy(png, os.path.join(out, "shots"))
        joined = [u for u, c in zip(users, clients) if any("AUTOTEST joined" in l for l in c.lines)]
        ok = ok and joined == users
        print("RESULT ok=%s joined=%s steps=%s" % (ok, joined, results))
    sys.exit(0 if ok else 1)


if __name__ == "__main__":
    main()
