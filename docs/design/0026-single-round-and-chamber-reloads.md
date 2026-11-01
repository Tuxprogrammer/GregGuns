# 0026 · Single loose rounds, chamber-by-chamber reloads and the idle reload

- Status: Accepted (by default)
- Gate: none
- Date: 2026-11-01

## Context

Techguns reloads a gun in one step: the whole magazine (or, for the shell-by-shell guns, every shell it can take) goes in at reload start, and the reload time is only a lockout ([gun-lifecycle.md § 7](../techguns-spec/02-guns/gun-lifecycle.md#7-reloading)). GregGuns already loads at the end of the reload so it can be interrupted (`ReloadStateMachine`). Three owner requests from the beta (2026-11-01) change how reloading feels:

1. **Loose rounds into magazine guns.** NPCs now drop 25–75% of a magazine in loose rounds (D349). The owner: "reload loose rounds (single shot mode) where you can reload one round into a gun by paying the full reload timer / animation so this is two ways we are nudging players to magazines rather than hard requiring them".
2. **Per-chamber reloads.** "For the revolver and shotgun I would prefer if we take the current reload time and split it into a per chamber reload, so that players can interrupt a partial reload and fire immediately."
3. **Idle reload.** "If the player doesn't fire for a little while the gun starts a partial reload of the revolver/shotgun."

## Decision

### 1. Single loose rounds (magazine-fed guns)

- **Which guns.** `GunStats.loadsSingleRounds()`: `ReloadMode.MAGAZINE`, a `looseAmmoType` set to an item other than the ammo type, and not an energy or charge gun. Today that is every `mag_*` gun: 9mm Pistol, Thompson, Mac10, M4, AK47, AUG, SCAR, Vector, Infiltrator, AS50, LMG, Minigun, PDW, Pulse Rifle, Akimbo Pistols and Akimbo Mac10s. Guns that already load from loose rounds (Revolvers, Bolt Action, Hand Cannon, launchers, Golden Gun, Nether Blaster), tank guns, energy guns, charge guns and NPCs (whose infinite source always has the magazine) are unchanged.
- **Rule.** A reload (the dry trigger or the reload key) takes a full magazine swap when a reload's magazines are at hand (pouch first, then inventory, as before). Otherwise, with the gun's loose round at hand, it loads **one shot's worth**: one round, two on the double-shot Akimbo Mac10s. It consumes one loose item per round, takes the **full reload time** with the full animation and sound, ejects nothing and refunds nothing, and never goes past the magazine size.
- **Akimbo (my call).** The pair's reload needs two magazines. With fewer, it loads single rounds into the shared clip: one round per reload in total, not one per gun, because the clip is one pool and a pair should not reload twice as fast as a single gun.
- **Costs.** Unchanged elsewhere: one loose item is one round here, the magazine route stays cheaper (one 9mm bullet is 10 Thompson rounds through a magazine), and the forced-reload refund stays at `GunItems.roundsPerLooseItem`, floored.

### 2. Chamber by chamber (Revolvers and shotguns)

- **Which guns.** A new `ReloadMode.CHAMBER`: Revolver, Golden Revolver, Akimbo Revolvers, Akimbo Golden Revolvers, Combat Shotgun and Sawed Off. The launchers (Grim Reaper, Grenade Launcher, Rocket Launcher, Nuke Launcher) and the Hand Cannon stay as they were: the owner named only the revolvers and shotguns, and a rocket-by-rocket launcher is a balance change nobody asked for. The single-shot ones would split into one chamber anyway.
- **Timing.** Chamber `i` takes `floor((i + 1) x T / N) - floor(i x T / N)` ticks (T the gun's reload time, N its magazine), so a reload from empty takes exactly T as before (Revolver 7, 8, 7, 8, 7, 8 ticks; Combat Shotgun 6 or 7 ticks a shell; Sawed Off 14). A chamber goes in at the end of its own time.
- **The run.** One reload is one run: at start it plans the chambers that the chamber credit and the ammo at hand cover, up to the empty chambers, and loads them one after another. It ends when the gun is full or the plan is done. The reload key on a part-loaded chamber gun starts a run for the empty chambers; nothing is ejected.
- **Interrupt.** A trigger pull made during the run stops it, and the gun fires on that tick with the chambers loaded so far. A pull with nothing loaded yet fires as soon as the first chamber is in. An automatic trigger (the Combat Shotgun) held since the dry click does not stop the run, so holding fire on an empty shotgun reloads it fully instead of alternating one shell and one shot. Switching the held item, dying or running out of ammo stops it, and every chamber already loaded stays.
- **Costs.** A shell is one round, so a shotgun pays one shell per chamber. A revolver's 9mm bullet fills a whole cylinder (6 rounds, 12 for a pair's two bullets). The bullet is paid when its first round goes in, and the bullet's other rounds wait on the gun as **chamber credit** (`GunState.credit()`, NBT `chamber_credit`), used by the next chambers before another bullet. Credit is at most one item's worth less one, is never handed back as items, and is clamped on read so a forged tag cannot hold more. The akimbo combine keeps the first gun's credit for the pair and the second gun's in its stored tag. A split gives the pair's credit to the first gun and the stored credit back to the second. A forced revolver reload therefore no longer throws away the bullet's spare rounds (before, a revolver with 5 rounds paid a bullet for 1 round).
- **Animation and sound.** The reload message carries the run's own length (the planned chambers' ticks, `ReloadStateMachine.totalTicks()`), and the existing reload animation plays once over the run. Each chamber is a segment of it, and the HUD's round count steps up as each one goes in. A run from empty looks and sounds exactly as before. The reload sound plays once per run (the sounds are final, design record 0017). An interrupt ends the animation and the shooter's own reload sound, as any canceled reload does.

### 3. Idle reload

- A chamber gun held as the main item, not full, with no shot, trigger pull or reload for `guns.idleReloadDelayTicks` (default 40, 2 s; 0 turns it off), starts a chamber run by itself, the same as the reload key: the same costs, animation, sound and interrupt.
- It runs on the server only. `GunController` counts idle ticks while the gun is held (any shot, held trigger, trigger press or reload resets the count; a gun not held is never idle). The ammo is counted only when the delay runs out, and then the count starts over, so with no ammo a player costs one inventory count per delay. It never starts for a dead player or a gun that is not the held item. (Minecraft 1.7.10 has no spectator mode.)

### Server authority

The server decides every load and every interrupt. The client mirror runs the same `ReloadStateMachine` over a counting-only view of the inventory for prediction, starts its own run when the server's `RELOAD_STARTED` arrives (the idle reload), and takes rounds and credit from the synced stack NBT. No new packet kind and no client-sent count. A reload-key press during a running reload no longer sends the shooter a `RELOAD_ENDED` rollback, which used to cut the running animation short.

### No ammo is created

Proved in `ReloadEconomyTest`:

- **Magazine guns.** Measure what a player could get back as loose rounds: `L = loose + roundsPerMagazine x loaded magazines + floor(rounds in the gun / R)`, with R the forced-reload refund rate. Refilling or unloading a magazine leaves L unchanged. A swap changes it by `floor(clip / R) - itemsPerReload x roundsPerMagazine`, which is at most 0 for every gun (checked per gun). A single-round load changes it by at most 0 because R is at least 1. Firing lowers it. Random sequences of these against the real state machine and refund never raise L.
- **Chamber guns.** Nothing is ever refunded, and an item becomes exactly its rounds (one loaded, the rest credit), so `loose x roundsPerItem + loaded + credit + fired` stays constant under random pulls, key presses, item switches and idle reloads.

## Consequences

- Code: `ReloadMode.CHAMBER`, `GunStats.loadsSingleRounds/chamberTicks`, `ReloadStateMachine` (`Load`, `choose`, `CHAMBERED`, `totalTicks`), `GunState.credit` and `GunNbt.KEY_CREDIT`, `GunController.idleReload`, `GunsConfig`, `ServerGunHandler`, `ClientGunMirror`, `AkimboNbt`, the six gun definitions. Tooltips: one Shift line per affected gun (`GunTooltipFormat.reloadRuleKey`). The ammo HUD adds a "Loose rounds" line under "Magazines" for a magazine gun when the player carries its loose rounds.
- Scenarios: `gun.thompson_smg.reload.single`, `gun.m4.reload.single`, `gun.lmg.reload.single`, `gun.revolver.reload.chamber`, `gun.combat_shotgun.reload.interrupt`, `gun.revolver.reload.idle`, `gun.revolver.reload.idle_off`. The revolver, golden revolver, shotgun, sawed-off and akimbo revolver reload scenarios check the chamber timing.
- Turrets and the Rampart mount keep their own reload (`TurretAmmo`), which tops up a chamber gun the way it topped up the shell guns.
- Spec deviation: [gregguns-spec 02-guns § Reloading](../gregguns-spec/02-guns/index.md#reloading-beta-deviations). DECISIONS row D350.

## Alternatives considered

- **One item per revolver chamber.** Simplest, but a revolver would cost six times as much ammo as before: an unfair loss. Refunding the unloaded rest on an interrupt needs a fractional item, so the credit on the gun is the only lossless form.
- **Consume the bullet at the start of the run.** The credit would exist before any round is loaded, and a canceled run would leave a paid, empty cylinder. Paying at the first chamber keeps the old "nothing moves until a round goes in" rule.
- **Replaying the whole reload motion for each chamber.** Each chamber would get a 7-tick wobble of the full tip-out (the Revolver) or a full dip-and-pump per shell. That reads as jitter, and the round count already steps per chamber. The one motion over the run keeps a full reload looking as it did.
- **Single rounds for every magazine gun's akimbo hand separately.** It would double the pair's loose-round loading speed for no reason.
- **Any held trigger interrupts a chamber run.** A held automatic Combat Shotgun would alternate one shell and one shot forever after running dry.
