# Beta client pass runbook (headless, Xvfb)

How to run the [beta client pass checklist](beta-client-pass.md) on the Linux test box with no display. The box is written as `<test-box>` and every path is relative to a checkout; put real hosts and users in your own notes, never in this repo.

## What exists today, and what does not

| Piece | State | Where |
|---|---|---|
| Server scenario runner (`gregguns test ...`) | Exists, headless | [README.md](README.md); `tools/run_scenarios.py`, `tools/smoke_server.py`, `tools/jvm_slot.py` |
| Client launch that joins a server by itself | Exists, documented | [README § Launching a client that joins a server by itself](README.md#launching-a-client-that-joins-a-server-by-itself): `./gradlew runClient --username=Alice --mcJvmArgs=-Dgregguns.autotest=a --mcJvmArgs=-Dgregguns.autotest.connect=<host>:<port> ...` |
| Autotest signals (`view`, `shot`, `hud`, `gui`, `hold`, `fire`, `creativegive`, `close`, `quit`) | Exists, dev environment only | `com.gregguns.client.autotest`; protocol in README |
| Screenshot scenarios | `mp.visual`, `mp.look.<gun>`, `mp.look.all`, `mp.look.charge`, `mp.look.chest`, `mp.grenade`, `mp.flashbang`, `mp.give`, `armor.showcase`, `armor.showcase.accessories`, `utility.charge_gui_shots` | README; see the Capture column **S** in the checklist |
| Client orchestrator `mp_check.py` | **Not in the repo.** The README says "not committed; the orchestrator keeps a copy". It is not in `tools/` | Recover the copy, or write the small driver in "Steps" below, and commit it |
| Xvfb, `xvfb-run`, Mesa or llvmpipe settings | **Nothing in the repo or the harness folder mentions them.** Whatever the box already uses is undocumented; this runbook is the first write-up | This file |
| Shader pack support in the dev client | **Missing.** `dependencies.gradle` has no Angelica entry, so `runClient` has no shader loader | "Shaders" below |
| Scenarios for machines, multiblocks, NPCs, structures, shader rows | Missing (Capture **N**) | Checklist groups F to K |

## Shaders

**Loader.** GTNH 2.9.0-RC-2 ships Angelica 2.2.28 (`angelica-2.2.28.jar`, mod IDs `angelica`, `embeddium`, `mcpatcherforge`, `notfine`), which carries the Iris shader loader. There is no OptiFine in the pack. Source: `reference/gtnh/2.9.0-RC-2/MOD-INDEX.md`, Angelica row. Pack config: `.minecraft/config/angelica-modules.cfg`, key `B:enableIris=true` (default).

**What Angelica says about itself** (README at tag 2.2.28, github.com/GTNewHorizons/Angelica): shaders are "Beta"; "Complementary + Euphoria Patches is the primary compatibility target"; "Sildurs, Seus, and any other shaders might work"; it "targets GL 3.3+ core profile".

**Config keys** (Angelica 2.2.28, `net/coderbot/iris/Iris.java` and `IrisConfig.java`). The Iris config file is `<game dir>/config/shaders.properties`; packs go in `<game dir>/shaderpacks/` (zip or folder). In the dev client the game dir is `run/client/`.

```properties
shaderPack=<file name inside shaderpacks/, for example ComplementaryReimagined_r5.x.zip>
enableShaders=true
```

`enableShaders=false` (or an empty `shaderPack`) is the no-shader pass. Both keys are read at startup; the pack can also be switched at runtime from the in-game shader screen, which is what row SH-11 tests. Other keys in the same file: `maxShadowRenderDistance` (default 32; lower it for software rendering), `enableDebugOptions`, `allowUnknownShaders`.

**Dev dependency (not in the repo today).** The pack runs Angelica beside lwjgl3ify and Hodgepodge (`lwjgl3ify-3.0.37.jar`, `hodgepodge-2.7.215.jar` in the pack). To get Angelica into `runClient`, a throwaway local edit to `dependencies.gradle` is needed, in the test worktree only and never committed:

```groovy
runtimeOnlyNonPublishable("com.github.GTNewHorizons:Angelica:2.2.28:dev")
```

Angelica's own build also pulls UniMixins >= 0.1.23 and GTNHLib >= 0.11.19 (README, "Required Dependencies"); GTNHLib 0.11.52 is already a dev dependency. Whether the Angelica dev jar boots in this project's Java 8 `runClient` (Angelica expects the lwjgl3ify path on Java 17+) is **not verified**. If it fails, use the fallback below.

**Fallback that matches what players run:** install the real GTNH 2.9.0-RC-2 client on `<test-box>` (`python -I tools/gtnh-reference/gtnh_ref.py fetch` extracts the same pack the reference folder uses), drop the built GregGuns jar in `mods/`, set the two properties above, and launch it under Xvfb with the same autotest properties. The autotest hooks run only when `DevEnvironment.isActive()`, so in a released-pack client use the console or a second player to send commands, and screenshot with F2 through the signals of a dev build or with `xdotool` and `import`. This route tests the shipped jar, which is closer to the beta.

**Shader pack.** Primary target: Complementary Reimagined (with the Euphoria Patches), per the Angelica README. Licensing: the GitHub API reports no SPDX license for `ComplementaryDevelopment/ComplementaryReimagined`, `sixthsurge/photon` or the BSL repo ("NOASSERTION" or not found), so **none of them can be called free and redistributable from what was checked here**. Do not commit a pack and do not put one in a release. Download it on `<test-box>` from the author's official page into `run/client/shaderpacks/`, and record the file name and version in the run's `summary.txt`. A pack with a clear open license (checked on its repo's LICENSE file) can replace it, but only after the Angelica README's compatibility list or a test run shows it works.

**Can it run under Xvfb with Mesa llvmpipe, or does it need the GPU?** Plainly:

- Without shaders: yes. Vanilla 1.7.10 with LWJGL 2 needs only OpenGL 2.1; Mesa's software renderer serves that. `LIBGL_ALWAYS_SOFTWARE=true` forces software rendering, and `GALLIUM_DRIVER=llvmpipe` picks llvmpipe (Mesa docs, docs.mesa3d.org/envvars.html).
- With shaders: **unproven and probably slow.** Angelica needs a GL 3.3+ core context. The Mesa pages fetched while writing this (llvmpipe and environment-variable docs) do not state which GL version llvmpipe offers, so the version on `<test-box>` must be measured: run `glxinfo -B` under Xvfb and read "OpenGL core profile version string". If it is 3.3 or higher, Angelica can start; if not, it will not, and `MESA_GL_VERSION_OVERRIDE=3.3` only changes the string (the Mesa docs warn that Mesa "may not really implement all the features"), so do not rely on it. Xvfb itself exposes GLX through Mesa's software path; whether the LWJGL context request (2 or 3) gets a core 3.3 context under Xvfb must also be tested on the box.
- Even when it starts, a shader pack such as Complementary at 1280x720 on a CPU renderer runs in single-digit frames per second at best, and the shadow pass is the expensive part. The autotest waits on rendered frames (a `shot` waits four), so it still works, but every timeout in the steps below must grow, and expect pass 2 to take several times as long as pass 1.
- GPU route if the probe fails or the speed is unusable: a GPU on `<test-box>` with a headless X server (Xorg with a dummy or `Option "AllowEmptyInitialConfiguration"` display) or VirtualGL with Xvfb as the 2D server. This is the only route that needs the GPU.

Decide with the probe in Step 1. Record the result in the run folder.

## Steps

All commands run from a checkout on `<test-box>`, one git worktree per process (two Gradle builds cannot share a project directory, README). Same commit for server and client. Follow the client lock and the Java process cap that the box uses (`tools/jvm_slot.py` waits for a free slot).

1. **Probe the display stack.**
   ```sh
   xvfb-run -a -s "-screen 0 1280x720x24 +extension GLX" glxinfo -B > out/glxinfo.txt
   grep -E "core profile version|OpenGL renderer|direct rendering" out/glxinfo.txt
   ```
   Pass 1 needs any OpenGL 2.1. Pass 2 needs core 3.3 or higher, or the GPU route.
2. **Build the server worktree and one client worktree** (`./gradlew build` once, then `runServer` / `runClient` reuse it). For pass 2 add the local Angelica line to the **client** worktree's `dependencies.gradle` and put the pack zip in its `run/client/shaderpacks/`.
3. **Prepare the server.** Offline mode, `GG_MODULES=guns,armor,machines,utility,npcs,worldgen`, flat peaceful world for groups A to I; the user is op. For group J use a second server world of type `default` with `spacingSmall`, `spacingMedium` and `spacingBig` at 2 to 4 in the GregGuns worldgen config. Reuse `tools/run_scenarios.py` for the boot and console plumbing (it writes `server.properties` and the module config).
4. **Prepare the client options.** In `run/client/options.txt`: `pauseOnLostFocus:false`, `fov:0.0`. In `run/client/config/gregguns.cfg`: modules on. Set `enableShaders=false` for pass 1 in `config/shaders.properties`.
5. **Pass 1, no shaders.** Start the client under Xvfb with the autotest properties:
   ```sh
   xvfb-run -a -s "-screen 0 1280x720x24 +extension GLX" \
     env LIBGL_ALWAYS_SOFTWARE=true GALLIUM_DRIVER=llvmpipe \
     ./gradlew runClient --username=Alice \
       --mcJvmArgs=-Dgregguns.autotest=a \
       --mcJvmArgs=-Dgregguns.autotest.connect=127.0.0.1:<port> \
       --mcArgs=--width --mcArgs=1280 --mcArgs=--height --mcArgs=720
   ```
   Wait for `AUTOTEST joined Alice` in the client log (bounded wait, 5 minutes; fail the run on timeout).
6. **Drive the groups in checklist order** from the server console. Between steps wait for the scenario's `PASS`/`FAIL` line, with a timeout per step (the Time column in the checklist, doubled):
   - A: `gregguns test mp.visual`, then `gregguns test mp.give`.
   - B: `gregguns test mp.look.all`.
   - C: `gregguns test mp.grenade`, `gregguns test mp.flashbang`.
   - D: `gregguns test armor.showcase`.
   - E: `gregguns test armor.showcase.accessories`.
   - F to J: the new scripted steps (Capture **N**): `/give`, `/setblock`, `/tp`, `/summon`, then `say [gregguns-autotest] view 2`, `say [gregguns-autotest] shot <id>`. Until a scenario exists, a console script that sends these lines with a pause of 2 seconds after each `shot` is enough. H also has `utility.charge_gui_shots`, `mp.look.charge`, `mp.look.chest`.
   - I: `/difficulty 2`, give eggs, spawn, wait, shot. Run `gregguns test npc.soak` once.
   - J: second world; `/tp` along the grid; shots.
7. **Collect, then quit.** `say [gregguns-autotest] quit`, then `stop` on the server. Copy `run/client/screenshots/*.png` into the output layout below, renaming per the checklist ids.
8. **Pass 2, shaders.** Set `shaderPack=<file>` and `enableShaders=true` in `config/shaders.properties`, start the client the same way (the log should contain Iris/Angelica shader-pack-loaded lines; if it shows the pack failed to compile, record the log and stop: that is a finding), then repeat step 6 with `sh` as the pass id and add group K. For K, create a water pool beside the test arena first (`/fill`), and photograph with the pack's defaults, then with depth of field and motion blur on.
9. **Scan the logs** with the failure signatures from the README (`Connection Lost`, `Mod rejections`, `fatal error`, `Crash Report`, `Exception`, `Caused by:`, `at com.gregguns`). Any hit is a crash-severity finding.
10. **Review the PNGs.** A person (or a reviewer who opens each file) goes through the checklist rows and marks pass or fail. The scripts only prove files exist.

## Output layout

```
out/beta-client-pass/<date>-<commit>/
  summary.txt              commit, box GL string, shader pack name and version, per-step result lines
  glxinfo.txt
  server.log
  client-np.log   client-sh.log
  autotest-np.txt autotest-sh.txt      only the "[gregguns] AUTOTEST" lines
  shots/
    np/<row-id>/<shot-id>.png
    sh/<row-id>/<shot-id>.png
  checklist-results.md     the checklist with a Pass/Fail/Note column filled in
```

`out/` is not tracked. Do not commit screenshots; publish only the filled `checklist-results.md` with the findings.

## Expected time

| Phase | Estimate |
|---|---|
| Setup (worktrees, build, pack download, probe) | 30 to 45 min, once |
| Pass 1 scripted (groups A to J, includes two boots) | about 75 min |
| Pass 2 scripted (all of pass 1 plus group K) | 2 to 4 hours on llvmpipe (frame-rate bound), about 1.5 hours on a GPU |
| Review of about 330 to 700 PNGs plus live-eyes rows | 1.5 to 2 hours |
| Total | about 6 to 8 hours wall clock on software rendering, 4 to 5 on a GPU; automation is hands-off after setup |

These are estimates from the per-group times in the checklist, not measurements. Most of the rows marked **N** need new scenarios or a console script first; budget half a day to write them before the first full run.
