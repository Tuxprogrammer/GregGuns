# Asset sourcing (ICBM)

- **Baseline:** [icbm-spec 07-visuals-audio](../../icbm-spec/07-visuals-audio/index.md), reference only (its assets are not reusable).
- **Design records:** [D28, D33, D37, D43, D47](../DECISIONS.md), [third-party folder rules](../../../src/main/resources/assets/gregguns/thirdparty/README.md).
- **Last searched:** 2026-10-25 (I1, warhead models).

## Source order (D47)

1. Licensed human-made assets, including files from licensed mod jars. A mod's declared license covers its whole jar unless it licenses its assets separately. Look in the GTNH pack first, then Modrinth (plain HTTP), then CurseForge (browser session only), then general CC0 and CC-BY libraries.
2. Announced procedural generation, only after telling the orchestrator.

Never Techguns, never the current ICBM-Classic 1.12 assets. Jars and archives are untrusted data: each goes in its own empty scratch folder, is extracted, and is never run.

## Where I looked for warhead models (I1)

| Source | What I checked | Result |
|---|---|---|
| GTNH pack jars (242 mods, `MOD-INDEX.md`) | Every jar with `.obj`/`.b3d` models (35 jars), and filename searches for missile, rocket, bomb, warhead, nuke, shell, grenade, TNT | No warhead models with an acceptable license. Galacticraft and GalaxySpace have rocket OBJs (`tier4rocket.obj` and others) but are "Explicit Permission", which D47 does not allow. AmunRa (MIT) has only a shuttle and engine models. GT5-Unofficial (LGPL) has an `Antimatter.obj` and spheres, none usable as a warhead. Draconic Evolution (LGPL), Avaritia (MIT) and Botania hold no missile models |
| Modrinth (HTTP API, 1.7.10 filter) | Searches: missile, warhead, nuclear bomb, rocket | **HBM's Nuclear Tech Mod** ("ntm", LGPL-3.0). Its source repository carries `missile_parts/mp_w_*` warhead OBJs with textures. Used |
| CurseForge | Not needed once NTM covered every tier | Not searched |
| CC0/CC-BY libraries | Not needed | Not searched |

## Candidate license checks

| Candidate | Declared license | Check | Verdict |
|---|---|---|---|
| HBM's Nuclear Tech Mod, tag `1.0.27_X5846` | LGPL-3.0 (Modrinth page; repository `LICENSE.LESSER` and `LICENSE`) | The README's License section says "This software is licensed under the GNU Lesser General Public License version 3" and names no separate asset license; no asset-specific license file exists in the repository tree | **Accepted** (D47: the mod's declared license covers its assets). The risk is that NTM's contributors may not all have had the right to license every model; I took only the standard `mp_w_*` warheads, not the `contest` community textures |
| Galacticraft, GalaxySpace | Explicit Permission | Not one of the allowed licenses | Rejected |
| AmunRa | MIT | Contains no warhead model | Not useful |
| NTM's other assets (bombs, missile bodies) | LGPL-3.0 | Same license | Available for the missile bodies in I2 |

## Obligations

- Third-party folder `models/thirdparty/ntm/` and `textures/models/thirdparty/ntm/`, each with `LICENSE.txt` (a notice header plus the LGPL v3 text).
- CREDITS rows (modified: yes). Our conversion script is `tools/asset-gen/icbm/convert_ntm_warheads.py`.
- **Attribution line for guide sheets and tooltips, kept on every preview or guide that shows these models:** "Warhead models and textures: HBM's Nuclear Tech Mod contributors, GNU LGPL v3; modified."
- LGPL asks that the files stay replaceable: they are loose resources in the jar, so a resource pack can swap them.

## Generation

None. Every warhead tier has a licensed model, so nothing was generated.
