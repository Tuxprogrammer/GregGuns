"""Merge tools/asset-gen/credits/*.csv into docs/assets/CREDITS.md.

Each generator writes its own CSV so they can run in parallel. This rewrites the block between the
generated-credits markers in CREDITS.md and leaves the hand-written rows above it alone.
Run: python -I -X utf8 tools/asset-gen/merge_credits.py
"""

import csv
import glob
import os

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
CREDITS = os.path.join(ROOT, "docs", "assets", "CREDITS.md")
START, END = "<!-- generated-credits:start -->", "<!-- generated-credits:end -->"


def render():
    out = [START, "", "## Generated assets", ""]
    out.append("Every asset below was generated procedurally by Claude (Anthropic) with the named generator and seed. "
               "Re-running the generator reproduces it.")
    for path in sorted(glob.glob(os.path.join(ROOT, "tools", "asset-gen", "credits", "*.csv"))):
        with open(path, encoding="utf-8", newline="") as f:
            rows = list(csv.DictReader(f))
        kind = os.path.splitext(os.path.basename(path))[0]
        out += ["", f"### {kind} ({len(rows)})", "", "| asset | path | generator | seed | method | license | date |",
                "|---|---|---|---|---|---|---|"]
        for r in rows:
            # sounds.csv (written by gen_sounds.py) may still use the old "licence" header
            cells = [(r.get(k) or (r.get("licence") if k == "license" else "") or "").replace("|", "\\|")
                     for k in ("asset", "path", "generator", "seed", "method", "license", "date")]
            out.append("| " + " | ".join(cells) + " |")
    out += ["", END]
    return "\n".join(out)


def main():
    text = open(CREDITS, encoding="utf-8").read()
    block = render()
    if START in text:
        text = text[: text.index(START)] + block + text[text.index(END) + len(END):]
    else:
        text = text.rstrip("\n") + "\n\n" + block + "\n"
    with open(CREDITS, "w", encoding="utf-8", newline="\n") as f:
        f.write(text)
    print("updated", CREDITS)


if __name__ == "__main__":
    main()
