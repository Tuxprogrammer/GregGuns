"""Minecraft's own textures for the guide sheets, read from the vanilla 1.7.10 client jar at build time.

The rule for Minecraft content is to follow Mojang's usage guidelines, not to avoid it (CONTRIBUTING, ICBM decision
D55). The jar is never copied into the repo: each texture is extracted into the git-ignored build cache and shown
unmodified, credited on the sheet that uses it (docs/assets/CREDITS.md). Looked up in this order: $GREGGUNS_MC_JAR,
a Minecraft 1.7.10 client jar in the reference pack or in a Prism Launcher install, then the Gradle cache's
mc-vanilla/1.7.10/client.jar that the mod's own build downloads.
"""

from __future__ import annotations

import os
import zipfile
from functools import lru_cache
from pathlib import Path

import numpy as np
from PIL import Image

import gt
from render3d import REPO

OUT = REPO / "build/infographics/vanilla"

# Prism keeps the jar under libraries/com/mojang/minecraft/1.7.10/; a Gradle build keeps its own copy.
_JAR_GLOBS = ("libraries/**/minecraft-1.7.10-client.jar", "libraries/**/1.7.10.jar")
_PRISM = Path.home() / ".local/share/PrismLauncher"
_GRADLE = Path.home() / ".gradle/caches/retro_futura_gradle/mc-vanilla/1.7.10/client.jar"


def _candidate_jars():
    if os.environ.get("GREGGUNS_MC_JAR"):
        yield Path(os.environ["GREGGUNS_MC_JAR"])
    for d in gt._reference_dirs():
        for g in _JAR_GLOBS:
            yield from sorted(d.glob(f"*/{g}"))
    yield from sorted(_PRISM.glob("libraries/com/mojang/minecraft/1.7.10/*.jar"))
    yield _GRADLE


@lru_cache(maxsize=1)
def _jar() -> zipfile.ZipFile:
    for j in _candidate_jars():
        if j.is_file():
            return zipfile.ZipFile(j)
    raise SystemExit("Minecraft 1.7.10 client jar not found. Set GREGGUNS_MC_JAR=<path to the jar>, or run "
                     "./gradlew setupDecompWorkspace once so the Gradle cache holds mc-vanilla/1.7.10/client.jar.")


def texture(path: str) -> Path:
    """A vanilla texture such as "blocks/glowstone" or "entity/zombie_pigman" as a PNG in the build cache, byte for
    byte what the jar holds."""
    p = OUT / "jar" / f"{path}.png"
    if not p.exists():
        p.parent.mkdir(parents=True, exist_ok=True)
        p.write_bytes(_jar().read(f"assets/minecraft/textures/{path}.png"))
    return p


# Each icon is one jar texture. The log shows its oak bark and the wool its white; the piston, TNT and crafting table
# show a side, the way their item icons are read at a glance.
TEXTURES = {"tnt": "blocks/tnt_side", "stone": "blocks/stone", "cobblestone": "blocks/cobblestone",
            "log": "blocks/log_oak", "wool": "blocks/wool_colored_white", "leather": "items/leather",
            "string": "items/string", "slimeball": "items/slimeball", "flint_and_steel": "items/flint_and_steel",
            "piston": "blocks/piston_side", "crafting_table": "blocks/crafting_table_front"}


def icon(name: str) -> Path:
    """One of TEXTURES as its 16x16 vanilla PNG, unmodified."""
    return texture(TEXTURES[name])
