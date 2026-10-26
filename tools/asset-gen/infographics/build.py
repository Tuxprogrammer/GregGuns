"""Build the player guide infographics into docs/assets/infographics/.

Usage (from the repo root):
    GREGGUNS_EXPORT_MODELS=build/infographics/export ./gradlew test --tests com.gregguns.tools.ModelExportTest
    python tools/asset-gen/infographics/build.py [page ...]

Needs Pillow, numpy and Playwright (see README.md). With no arguments every page is built.
"""

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

import kit  # noqa: E402
import pages_icbm  # noqa: E402
import pages_icbm_missile  # noqa: E402
import pages_icbm_silo  # noqa: E402
import pages_more as _more  # noqa: E402

PAGES = {**_more.PAGES, **pages_icbm.PAGES, **pages_icbm_missile.PAGES, **pages_icbm_silo.PAGES}


def main(names):
    names = names or list(PAGES)
    unknown = [n for n in names if n not in PAGES]
    if unknown:
        raise SystemExit(f"unknown page(s): {unknown}; known: {list(PAGES)}")
    kit.rasterize([(n, PAGES[n](), 1600) for n in names])


if __name__ == "__main__":
    main(sys.argv[1:])
