# Scenario tools

| Tool | What it does |
|---|---|
| `run-headless.sh` | Boots a fresh dev server, runs scenarios, stops. The gate. See its header. |
| `dev-server.sh` | A persistent server that hot-reloads code, for iterating on scenarios. This page. |
| `headless-lib.sh` | Server setup and process helpers that both of the above source. |
| `HotReload.java` | The class-redefinition client that `dev-server.sh reload` runs (a source file, no build step). |
| `dev-server-init.gradle` | Gradle init script `dev-server.sh` passes with `-I`: it puts a directory of classes and resources, not the dev jar, on the server classpath. |
| `mp_check.py` | Multiplayer checks and the one-client screenshot driver. `--angelica` turns on connected textures (below). |

## dev-server.sh: for iteration only

Run it on the test box, inside your own checkout (one server per checkout).

```sh
tools/scenarios/dev-server.sh start [--no-galacticraft]   # boot once, about 40 to 55 s warm
tools/scenarios/dev-server.sh test icbm.missile.           # run a prefix ("." for a package, "*", or a full name)
# edit code, then:
tools/scenarios/dev-server.sh reload [--auto-restart]      # recompile and redefine in the live JVM
tools/scenarios/dev-server.sh test icbm.missile.
tools/scenarios/dev-server.sh status                       # running? ports, memory
tools/scenarios/dev-server.sh stop                         # always stop it when you are done
```

`test` prints the FAIL and SKIP lines and the summary, and exits 0 on pass and 1 on fail, timeout or a dead server.
`GG_TEST_TIMEOUT` bounds the wait.

**A persistent server drifts.** World state, static fields, caches and registries keep whatever earlier runs did,
and a redefined class does not run its static initializers again. A green result here is a hint, not a result.
Gates and final verification still use a fresh boot with `run-headless.sh`.

### What reloads

| Change | Plain JVM (default) | DCEVM JDK (`GG_DEVSRV_JAVA_HOME`) |
|---|---|---|
| Method body, in a loaded class | reloads | reloads |
| A new class (including a new scenario class, if something that runs later references it) | loads on first use | loads on first use |
| Add or remove a private, static or final method | reloads | reloads |
| Add or remove a field or a public or overridable method, change a signature, a superclass or interfaces | RESTART NEEDED | reloads |
| Anything registered at boot: blocks, items, entities, recipes, the scenario list, config, lang and other resources | RESTART NEEDED | RESTART NEEDED |
| A changed `static final` constant that other classes inlined | stale until restart | stale until restart |

`reload` says `RESTART NEEDED: <why>` and exits 5 when a restart is the only way. `--auto-restart` restarts instead.
Registering a new scenario in a list that is built at boot is a boot-time change: restart.

### How it works

`runServer` normally runs the mod from the dev jar, which cannot be refreshed under a running JVM. The init script
swaps the jar for the directory `build/dev-server/main` (classes and resources side by side, as in the jar), which the
`devServerSync` task keeps current, so `./gradlew devServerSync` is all the server needs to see new and not-yet-loaded
classes. For classes the server has already loaded, `HotReload.java` attaches over JDWP
(`127.0.0.1` only, ports 5005 to 5009, `--mcJvmArgs` so only the server JVM gets the agent) and redefines the ones
whose bytes changed. It keeps a hash manifest of the classes the server has, so it only sends what changed.

### Environment

`GG_PORT`, `GG_MODULES` (default: every module), `GG_BOOT_TIMEOUT`, `GG_TEST_TIMEOUT`, `GG_STOP_TIMEOUT`,
`GG_GRADLE_ARGS` (default `--no-daemon`), `GG_JDWP_PORT`, `GG_VERBOSE=1` (print PASS lines), and:

- `GG_RELOAD_DAEMON=1`: reload uses a warm Gradle daemon (about 4 s instead of 11 s) at the cost of about 0.8 GB.
  `stop` ends the daemon whose working directory is your checkout and no other.
- `GG_DEVSRV_JAVA_HOME`: a DCEVM JDK 8 (the TravaOpenJDK 8 DCEVM build, whose JVM is DCEVM by default). The server
  then runs on it and accepts added and removed methods and fields.

State lives in `run/server/dev/` (git-ignored): the console pipe, the log, the ready marker, the manifests.
`stop` only ever signals processes whose working directory is your checkout.

## Connected textures in the dev client

The pack's CTM engine is MCPatcherForge, bundled in Angelica 2.2.28; the plain dev client has no Angelica, so casings show flat. Every multiblock screenshot should be taken with it on:

```sh
python -I tools/scenarios/mp_check.py <server-dir> <client-dir> --angelica ...   # or GG_DEV_ANGELICA=1
./gradlew runClient -Pgg.devAngelica=true ...                                    # by hand
```

- The switch is a runtime-only dependency in `dependencies.gradle` (`com.github.GTNewHorizons:Angelica:2.2.28:dev`, the pack's version) that exists only with `-Pgg.devAngelica=true` or `GG_DEV_ANGELICA=1`. Builds, tests, `runServer` and the headless suite never see it. Give it to the client only: Angelica is a client mod.
- `mp_check.py --angelica` passes the property to every client's Gradle call and writes `run/client/config/mcpatcherforge.cfg` with `connected_textures` enabled.
- It runs on the box's Java 8 client JDK under Xvfb with llvmpipe (Angelica makes a GL 3.3 core context; the existing `MESA_GL_VERSION_OVERRIDE=3.3` is enough). UniMixins comes in with it and Hodgepodge is already on the classpath. No lwjgl3ify or Java 17 needed.
- The first client run downloads Angelica's dependencies into the Gradle cache.
- `testing.ctm_casings` (dev only, needs one connected client, not part of `all`) places a Works Casing wall with a controller, four Bastion Plating metas side by side, a casing ceiling and a built Ammunition Works, and signals screenshots. `tools/asset-gen/infographics/ctm_expect.py <dir>` renders what the same layouts should look like through `ctmsel`, with the tile numbers.
