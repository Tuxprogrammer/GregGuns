# gtnh-reference

A helper for researching the APIs of GTNH and the other mods in the pack. It keeps an extracted GTNH pack in the git-ignored `reference/` folder, indexes every mod jar against its GitHub source and tag, and decompiles jars as a fallback when no source exists.

**When and how to use it, and the rules that come with it:** [docs/gregguns-spec/INTEGRATION.md](../../docs/gregguns-spec/INTEGRATION.md).

Requirements: Python 3.9+ (standard library only), `git`, a JDK on `PATH` for `decompile`, and `gh` for the GitHub commands it suggests.

```sh
T=tools/gtnh-reference/gtnh_ref.py
python -I $T fetch <pack-zip-url> [--keep-archive]  # download, extract, index; sets reference/gtnh/CURRENT
python -I $T index                                  # rebuild MOD-INDEX.md / mod-index.json
python -I $T mod <name|modid|jar>                   # jar, mod IDs, licence, source link, extras, gh commands
python -I $T where <class-or-fragment>              # which jar ships a class
python -I $T clone <name|modid|jar>                 # shallow clone at the pinned ref -> reference/src/
python -I $T jars <name|modid|jar>                  # download dev / API-source / sources jars -> reference/maven/
python -I $T decompile <name|modid|jar|path.jar> [--class fqcn ...] [--force]   # fallback -> reference/decompiled/
```

GTNH's release metadata only knows the source of GTNH-maintained mods. [`source-overrides.json`](source-overrides.json) adds hand-verified sources, API jars, dev jars and maven coordinates for the others. When you find a new one, add it there (the file's `_comment` explains the fields) and run `index`. Unlike `reference/`, it's committed.

Run it from the repo root, with `python -I`, and never from inside `reference/`: the pack is downloaded, untrusted content.

What it writes, all under `reference/` (git-ignored):

| Path | Contents |
|---|---|
| `gtnh/CURRENT` | The active pack version |
| `gtnh/<version>/` | The extracted MultiMC instance, plus `manifest.json`, `MOD-INDEX.md` and `mod-index.json` |
| `gtnh/gtnh-assets.json` | GTNH's index of mod repos, licences and jar names |
| `gtnh/_download/` | The pack zip, if kept |
| `src/<Mod>@<tag>/` | Shallow source clones |
| `maven/<Mod>/` | Downloaded dev, API-source and sources jars |
| `decompiled/<jar>/` | Vineflower output, with SRG names remapped to MCP names |
| `tools/` | The pinned Vineflower jar |
