"""Where the texture generators write. One switch for icons.py, blocks.py, gui.py and models.py.

By default they write in place: textures under src/main/resources/assets/gregguns, contact sheets under
docs/assets/previews, credit rows under tools/asset-gen/credits and model_layouts.md beside the scripts.
Set GREGGUNS_ASSETS_OUT=<dir> to build into a scratch folder instead (same pattern as
GREGGUNS_INFOGRAPHICS_OUT):

    <dir>/assets/gregguns/...   textures and .mcmeta files
    <dir>/previews/...          contact sheets, GIFs
    <dir>/credits/*.csv         credit rows
    <dir>/model_layouts.md

Nothing in the working tree changes then, so a rebuild can be diffed against the committed output.
"""

import os
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
_OUT = os.environ.get("GREGGUNS_ASSETS_OUT", "").strip()

if _OUT:
    _base = Path(_OUT).expanduser().resolve()
    RES = _base / "assets" / "gregguns"
    PRE = _base / "previews"
    CREDITS_DIR = _base / "credits"
    LAYOUT_MD = _base / "model_layouts.md"
else:
    RES = ROOT / "src" / "main" / "resources" / "assets" / "gregguns"
    PRE = ROOT / "docs" / "assets" / "previews"
    CREDITS_DIR = ROOT / "tools" / "asset-gen" / "credits"
    LAYOUT_MD = ROOT / "tools" / "asset-gen" / "model_layouts.md"
