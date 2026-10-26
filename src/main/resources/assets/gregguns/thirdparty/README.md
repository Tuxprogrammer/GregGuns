# Third-party assets

This folder is the home of the rules for third-party assets (ICBM module, design record 0018 § 10-11, D28, D35). It holds no assets yet.

Every other asset in GregGuns is original and MIT. A third-party asset is a human-made file under **CC0, CC-BY, MIT or LGPL**. Nothing NC, ND, share-alike-only or All Rights Reserved, and never the current ICBM-Classic 1.12 assets. Check the license at the source before adding a file.

## Where the files go

Minecraft loads textures, models and sounds only from their standard folders, so a third-party file sits in the standard folder inside a `thirdparty/<source>/` subfolder, never mixed in with our own:

```
textures/items/thirdparty/<source>/<name>.png
textures/blocks/thirdparty/<source>/<name>.png
models/thirdparty/<source>/<name>.obj
sounds/thirdparty/<source>/<name>.ogg
```

`<source>` is a short lower-case name for where the files came from (an author, pack or mod), for example `kenney`, `opengameart-jdoe` or a mod's id. For files taken from a mod jar, the mod's license must cover its assets, and the CREDITS row names the mod, its version and the jar's download URL (D47).

## Rules for each file

1. **License text beside the files.** Put the source's license text in the same `thirdparty/<source>/` folder as `LICENSE.txt` (CC-BY: also the attribution line it asks for). One license file covers a folder, so a folder holds files from one source and one license.
2. **A `CREDITS.md` entry.** Add the file to the "Third-party assets" section of [docs/assets/CREDITS.md](../../../../../../docs/assets/CREDITS.md) with its author, source URL or commit, and license.
3. **Keep the file as it came.** If you modify it, say so in the CREDITS row (the license may require it).
4. **Code is different.** Third-party code does not go here: it follows [CONTRIBUTING § Ported code](../../../../../../CONTRIBUTING.md#ported-code-mit) and has an entry in [THIRD-PARTY-NOTICES.md](../../../../../../THIRD-PARTY-NOTICES.md).

The rules are in [CONTRIBUTING § Assets and localization](../../../../../../CONTRIBUTING.md#assets-and-localization).
