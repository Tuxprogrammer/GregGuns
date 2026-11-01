"""The icbm-computer-control guide sheet: the OpenComputers hookups of every ICBM machine and the Golden Dome floppy
with its four programs.

Every fact is read from the code at build time: the floppy's label, recipe and files from `GoldenDomeFloppy.java`, the
calls of each component from the `@Callback` methods of its environment class, the signals from the component logic,
and every line the sheet quotes from a program (usage, prompts, the sample `launch` session) is checked against the Lua
source shipped in the jar, so a changed program stops the build instead of leaving the sheet stale (D50).

The computer, Adapter and Disk Drive are drawn with OpenComputers' own textures (CC0, inputs/thirdparty/opencomputers),
the Launch Control and the defense machines with their real faces.

Run on its own:  python tools/asset-gen/infographics/pages_icbm_computers.py
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

import kit as K  # noqa: E402
import pages_icbm_defense as DEF  # noqa: E402
import pages_icbm_missile as MIS  # noqa: E402
import pages_icbm_silo as SILO  # noqa: E402
import scenes as S  # noqa: E402
from kit import chip, esc  # noqa: E402
from pages import img, page_doc, slot  # noqa: E402

JAVA = K.REPO / "src/main/java/com/gregguns"
LUA = K.ASSETS / "lua/golden_dome"
OC = Path(__file__).resolve().parent / "inputs/thirdparty/opencomputers"
OC_CREDIT = ("Computer Case, Adapter, Disk Drive and floppy textures: OpenComputers by Florian “Sangar” Nücke "
             "and contributors (GT New Horizons fork), public domain (CC0 1.0).")
OC_TIER1 = (0xAB, 0xAB, 0xAB)   # Color.byTier(0): a Tier 1 Computer Case is drawn tinted light gray


def _src(rel: str) -> str:
    return (JAVA / rel).read_text(encoding="utf-8")


def _lua(rel: str) -> str:
    return (LUA / rel).read_text(encoding="utf-8")


def _need(text: str, where: str, *facts: str) -> None:
    for f in facts:
        if f not in text:
            raise SystemExit(f"{where}: '{f}' not found; pages_icbm_computers.py no longer matches the code")


# ---------------------------------------------------------------- facts, read from the code

FLOPPY = _src("icbm/compat/oc/GoldenDomeFloppy.java")
LABEL = re.search(r'LABEL = "([^"]+)"', FLOPPY).group(1)
CIRCUIT = int(re.search(r"CIRCUIT = (\d+);", FLOPPY).group(1))
DURATION_S = int(re.search(r"\.duration\((\d+)\)", FLOPPY).group(1)) / 20
_need(FLOPPY, "GoldenDomeFloppy.java", 'Items.get("floppy")', 'Ing.gt("circuit", "Good", 1, Tier.MV', "VP[Tier.HV.voltageIndex()]",
      "assemblerRecipes", "FileSystem.asReadOnly", "COLOR_YELLOW = 11")
PROGRAMS = re.findall(r'"bin/(\w+)\.lua"', FLOPPY)
assert PROGRAMS == ["launch", "radarwatch", "autodefense", "emp"], PROGRAMS


def _callbacks(env: str) -> list[str]:
    src = _src(f"icbm/compat/oc/{env}.java")
    calls = re.findall(r"@Callback\b(?:\(.*?\))?\s*public Object\[\] (\w+)\(", src, re.S)
    if not calls or len(calls) != src.count("@Callback"):
        raise SystemExit(f"{env}: could not read every @Callback method; update pages_icbm_computers.py")
    return calls


# (component names, machine, the calls the sheet lists, the signals, env class)
COMPONENTS = [
    (("gg_launch_control", "gg_silo"), "Launch Control and Missile Silo",
     ["getMissile", "getFuel", "setTarget", "setTargetFromStick", "canLaunch", "launch"], ["launcher_ready", "launcher_fired"],
     "LaunchEnvironment"),
    (("gg_radar",), "Radars and Radar Tower", ["getContacts", "getIncoming", "setProtectedRadius"],
     ["radar_contact", "radar_incoming", "radar_lost"], "RadarEnvironment"),
    (("gg_air_defense",), "Air-Defense Battery", ["getSamCount", "fireAt", "setAuto"], ["sam_fired", "sam_hit"],
     "AirDefenseEnvironment"),
    (("gg_emp_tower",), "EMP Tower", ["isReady", "getCooldown", "setMode", "setRadius", "fire"], ["emp_ready", "emp_fired"],
     "EmpTowerEnvironment"),
]
_COMPONENT_SRC = "".join(_src(f"icbm/component/{n}") for n in ("LaunchComponentLogic.java", "RadarComponentLogic.java",
                         "AirDefenseComponentLogic.java", "EmpComponentLogic.java", "AirDefenseSignals.java", "SignalTracker.java",
                         "ContactTracker.java")) + _src("icbm/compat/oc/LaunchEnvironment.java")
_OC_SRC = "".join(_src(f"icbm/compat/oc/{n}.java") for n in ("OcCompat", "LaunchEnvironment", "RadarEnvironment",
                  "AirDefenseEnvironment", "EmpTowerEnvironment"))
for names, _m, calls, signals, env in COMPONENTS:
    have = _callbacks(env)
    assert "getInfo" in have, env
    for c in calls:
        assert c in have, f"{env} has no @Callback {c}"
    for nm in names:
        _need(_OC_SRC, "OpenComputers drivers", nm)
    for sg in signals:
        _need(_COMPONENT_SRC, "component signals", f'"{sg}"')

# The recipe as the real pack builds it (the whole-pack recipe export the icbm-recipes sheet reads): an Assembler row
# turning OC's blank floppy and an MV circuit into another floppy, at the code's circuit, time and HV voltage.
import pages_icbm_recipes as REC  # noqa: E402

_GD = REC.rows(lambda r: r["map"] == "gt.recipe.assembler" and REC.out_key(r) == "OpenComputers:item:4"
               and [REC.base(i["k"]) for i in r["inputs"]][:1] == ["OpenComputers:item:4"])
if len(_GD) != 1:
    raise SystemExit(f"the recipe dump has {len(_GD)} Golden Dome Assembler rows, expected 1; dump the pack again")
_GD = _GD[0]
_MV = [i for i in _GD["inputs"] if REC.dump()["itemdata"].get(REC.base(i["k"]), {}).get("m") == "Good"]
assert len(_GD["inputs"]) == 2 and len(_MV) == 1 and _MV[0]["n"] == 1, _GD
assert _GD["circuit"] == CIRCUIT and _GD["duration"] / 20 == DURATION_S and _GD["eut"] == 480, _GD   # GT VP[HV] = 480
MV_CIRCUIT = REC._gt_item_tex("gt.metaitem.03", 80)   # one GT circuit of the MV tier, as the recipes sheet draws them
if MV_CIRCUIT is None:
    raise SystemExit("GT jar has no gt.metaitem.03/80.png; update pages_icbm_computers.py")

# The EMP modes the tower accepts.
_need(_lua("bin/emp.lua"), "emp.lua", "mode <all|missiles|machines>")

# A sample `launch` session: a full Tier 1 missile on a Launch Control. Each line is the program's own format.
T1 = re.search(r'T1\("missile_body_t1", ExplosiveTier\.T1, (\d+), \d+, (\d+), [0-9.]+, [0-9.]+, \d+, [0-9.]+F, "([^"]+)"',
               _src("icbm/missile/MissileTier.java"))
T1_FUEL, T1_FLUID = int(T1.group(1)), T1.group(3)
T1_FILL = int(re.search(r'T1\("warhead_t1", ExplosiveTier\.T1, (\d+),', _src("icbm/warhead/WarheadCasing.java")).group(1))
_need(_src("icbm/warhead/WarheadBlast.java"), "WarheadBlast.java", 'CONDENSED("condensed", ExplosiveTier.T1')
TARGET = (412, -230)   # world X and Z of the column the missile aims at, within a Tier 1 missile's range of its pad
LAUNCH = _lua("bin/launch.lua")
_need(LAUNCH, "launch.lua", '"Launcher: " .. l.type .. " " .. l.address:sub(1, 8)', '"Target:   "', '"Missile:  tier %s, explosive %s, fill %s, fueled %s"',
      '"Fuel:     %s %d / %d mB"', 'gd.confirm("Launch now? This cannot be undone.")', 'print("Launched.")', 'print("Canceled.")', '"Cannot launch: "')
_need(_lua("lib/goldendome.lua"), "goldendome.lua", '" [y/N] "')
SESSION = [("cmd", f"launch {TARGET[0]} {TARGET[1]}"),
           ("out", "Launcher: gg_launch_control 3f9a1c07"),
           ("out", f"Target:   {TARGET[0]}, {TARGET[1]}"),
           ("out", f"Missile:  tier 1, explosive condensed, fill {T1_FILL}, fueled true"),
           ("out", f"Fuel:     {T1_FLUID} {T1_FUEL} / {T1_FUEL} mB"),
           ("ask", "Launch now? This cannot be undone. [y/N] ", "y"),
           ("out", "Launched.")]

# Usage lines quoted on the sheet, checked against each program.
USAGE = {
    "launch": ["launch <x> <z>", "launch --stick", "launch --list"],
    "radarwatch": ["radarwatch"],
    "autodefense": ["autodefense start [--emp] | stop | status"],
    "emp": ["emp status | mode <all|missiles|machines> | radius <r> | fire"],
}
for prog, lines in USAGE.items():
    src = _lua(f"bin/{prog}.lua")
    for ln in lines:
        _need(src, f"{prog}.lua", ln if prog != "launch" else ln.split("   ")[0])
_need(LAUNCH, "launch.lua", "take the target from a data stick in a Launch Control", "list the launchers that are connected")
_need(_lua("bin/radarwatch.lua"), "radarwatch.lua", 'event.listen("radar_incoming"', "computer.beep", "Ctrl+C to stop")
AUTO = _lua("bin/autodefense.lua")
_need(AUTO, "autodefense.lua", "battery.fireAt(contactId)", 'tower.setMode("missiles")', 'tower.getMode() ~= "missiles"',
      "/tmp/autodefense.log")
assert "setAuto" not in AUTO.replace("auto-engage", ""), "autodefense must never change a battery's own auto setting"
_need(_lua("bin/emp.lua"), "emp.lua", 'gd.confirm("Fire the EMP pulse? Machines in range may be damaged.")')
_need(_lua(".autorun.lua"), ".autorun.lua", "PATH")


# ---------------------------------------------------------------- pictures


def _oc(name: str) -> Path:
    return OC / f"{name}.png"


def _case_tex() -> tuple:
    """The Tier 1 Computer Case faces, tinted as OC draws the block (its render color by tier)."""
    return _tinted(_oc("case_top"), "case_top"), _tinted(_oc("case_side"), "case_side"), _tinted(_oc("case_front"), "case_front")


def _tinted(src: Path, name: str) -> Path:
    from PIL import Image
    import numpy as np
    out = K.TEXCACHE / f"oc-{name}-t1.png"
    if not out.exists():
        out.parent.mkdir(parents=True, exist_ok=True)
        a = np.array(Image.open(src).convert("RGBA")).astype(float)
        a[..., :3] = a[..., :3] * (np.array(OC_TIER1) / 255.0)
        Image.fromarray(a.astype(np.uint8)).save(out)
    return out


def setup_scene() -> Path:
    """A Disk Drive and a Computer Case, an Adapter touching both the case and a Launch Control, in one row."""
    # Seen from the front, +x runs to the left: the drive sits at the right end of the row in the world, the left on the sheet.
    blocks = {(3, 0, 0): ("drive", S.FRONT), (2, 0, 0): ("case", S.FRONT), (1, 0, 0): ("adapter", S.FRONT),
              (0, 0, 0): ("lc", S.FRONT)}
    tex = {"drive": (_oc("generic_top"), _oc("disk_drive_side"), _oc("disk_drive_front")),
           "case": _case_tex(),
           "adapter": (_oc("adapter_top"), _oc("adapter_side"), _oc("adapter_side")),
           "lc": SILO.launch_control_tex()}
    return K.voxels("oc-setup", blocks, tex, yaw=-24, pitch=24, ppu=120)


def machines_row() -> str:
    hv = DEF.radar_build("radar_hv", "Radar (HV)")
    radar = DEF.render_with_dish(hv, "radar-hv-dish", DEF.dish_spot(hv, "RadarShape"), 1.0, -60, yaw=-32, pitch=24, ppu=58)
    pics = [(MIS.launch_control_icon(), "Launch Control"), (radar, "Radar"),
            (DEF.machine_block("air_defense_battery", "HV", ppu=150), "Air-Defense Battery"),
            (DEF.machine_block("emp_tower", "EV", ppu=150), "EMP Tower")]
    return "".join(f'<div class="mc">{img(p, h=96)}<span>{esc(t)}</span></div>' for p, t in pics)


# ---------------------------------------------------------------- panels

REC_PART_CSS = """
.part { display: inline-flex; flex-direction: column; align-items: center; gap: 5px; text-align: center; width: 150px; }
.part .pl { font-size: 16px; line-height: 1.2; color: var(--muted); }
.part .pl b { color: var(--txt); font-weight: 600; }
"""

CSS = DEF.CSS + REC_PART_CSS + """
.mc { display: flex; flex-direction: column; align-items: center; gap: 6px; font-size: 16px; color: var(--muted); }
.cap { display: grid; grid-template-columns: repeat(4, 1fr); gap: 6px; margin-top: 6px; text-align: center; font-size: 16px; color: var(--muted); }
.cap b { color: var(--txt); display: block; font-size: 17px; }
.term { background: #000; border: 6px solid #3a3f46; border-radius: 6px; padding: 14px 18px; font-family: "DejaVu Sans Mono", "Fira Mono", Consolas, monospace;
  font-size: 19px; line-height: 1.45; color: #f2f2f2; white-space: pre; box-shadow: inset 0 0 0 2px #15181c; }
.term .p { color: #9fe870; } .term .u { color: #ffd84d; }
code, .cmd { font-family: "DejaVu Sans Mono", "Fira Mono", Consolas, monospace; font-size: 17px; color: var(--txt);
  background: rgba(255,255,255,.07); padding: 1px 6px; border-radius: 4px; }
.prog { display: grid; grid-template-columns: 1fr 1fr; gap: 18px; margin-top: 18px; }
.prog .card h2 { font-family: "DejaVu Sans Mono", "Fira Mono", Consolas, monospace; font-stretch: normal; letter-spacing: 0; }
.use { display: flex; flex-direction: column; gap: 6px; margin: 8px 0 10px; }
.rec { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; font-size: 18px; color: var(--muted); }
.rec .plus { font-size: 26px; color: var(--muted); }
"""


def _terminal() -> str:
    rows = []
    for kind, text, *rest in SESSION:
        if kind == "cmd":
            rows.append(f'<span class="p">/home # </span>{esc(text)}')
        elif kind == "ask":
            rows.append(f'{esc(text)}<span class="u">{esc(rest[0])}</span>')
        else:
            rows.append(esc(text))
    return '<div class="term">' + "\n".join(rows) + "</div>"


def _prog(name: str, uses: list[str], text: str) -> str:
    u = "".join(f'<span class="cmd">{esc(x)}</span>' for x in uses)
    return f'<div class="card"><h2>{esc(name)}</h2><div class="use">{u}</div><div class="pt">{text}</div></div>'


def sheet() -> str:
    floppy = slot(K.icon(_oc("floppy_yellow"), 64), 72, LABEL)
    rec = (f'<span class="part">{slot(K.icon(_oc("floppy_gray"), 64), 52, "Floppy Disk")}<span class="pl"><b>1 &times;</b> Floppy Disk</span></span>'
           f'<span class="plus">+</span>'
           f'<span class="part">{slot(K.icon(MV_CIRCUIT, 64), 52, "MV circuit")}<span class="pl"><b>1 &times;</b> MV circuit (any)</span></span>'
           f'<span class="plus">&rarr;</span>'
           f'<span class="part">{slot(K.icon(_oc("floppy_yellow"), 64), 52, LABEL)}<span class="pl">{esc(LABEL)}</span></span>')

    top = f"""<div class="card" style="display:grid;grid-template-columns:1.25fr 1fr;gap:26px;align-items:center">
<div><h2>Run your missile base from a computer</h2><div class="pt" style="margin-top:8px">Every ICBM machine talks to an
<b>OpenComputers</b> computer. Put an <b>Adapter</b> against the machine and connect the Adapter to the computer, touching it
or by cable: the machine shows up as a <b>component</b>. Launchers, radars, batteries and the EMP tower all do. The
<b>{esc(LABEL)}</b> floppy brings ready-made programs, so you can launch, watch the sky and defend without writing any Lua.</div>
<div style="display:flex;gap:26px;margin-top:14px;justify-content:center">{machines_row()}</div></div>
<div class="spot" style="padding:10px 6px 4px">{img(setup_scene(), w=560)}
<div class="cap"><span><b>Disk Drive</b>the floppy goes here</span><span><b>Computer Case</b>runs the programs</span><span><b>Adapter</b>touches the machine</span><span><b>Launch Control</b>or any ICBM machine</span></div></div></div>"""

    disk = f"""<div class="grid" style="grid-template-columns:1fr 1.1fr;margin-top:18px">
<div class="card"><div style="display:flex;align-items:center;gap:16px">{floppy}<h2>The {esc(LABEL)} floppy</h2>{chip("HV")}</div>
<div class="label" style="margin:14px 0 6px">Assembler &middot; {chip("HV")} &middot; {DURATION_S:g} seconds &middot; programmed circuit {CIRCUIT}</div>
<div class="rec">{rec}</div>
<div class="pt" style="margin-top:12px">Put it in a <b>Disk Drive</b> connected to the computer. Its programs work as commands
at once, with nothing to install: <code>launch</code>, <code>radarwatch</code>, <code>autodefense</code> and <code>emp</code>.
The disk is read-only. Its library, <code>goldendome</code>, is there for your own scripts too.</div>
<div class="label" style="margin:16px 0 6px">Safe by design</div>
<div class="pt"><code>launch</code> and <code>emp fire</code> always ask first. <code>autodefense</code> only adds targets: it
never switches a battery&#39;s own auto mode, so stopping it, a reboot or a power cut leaves the batteries exactly as they were.</div></div>
<div class="card"><h2>Launching from the keyboard</h2><div class="pt" style="margin:6px 0 12px">A full Tier 1 missile on a
Launch Control. <code>launch</code> shows the target, the missile and its fuel, then waits for <b>y</b>.</div>{_terminal()}
<div class="pt" style="margin-top:12px">Any other answer prints <b>Canceled.</b> and nothing flies. If the launcher is not ready,
<code>launch</code> says why instead of asking: no power, no fuel, the target out of range, and so on.</div></div></div>"""

    progs = '<div class="prog">' + "".join([
        _prog("launch", USAGE["launch"],
              "Aims the first connected launcher at a column and fires it after you confirm. <b>--stick</b> takes the "
              "target from the Data Stick in a Launch Control (a silo has no stick slot: give it coordinates). "
              "<b>--list</b> shows every launcher the computer can reach."),
        _prog("radarwatch", USAGE["radarwatch"],
              "A live table of every contact on every connected radar, soonest impact first, with its position, tier and "
              "time to impact. It <b>beeps</b> when a missile is coming for you. Ctrl+C stops it."),
        _prog("autodefense", USAGE["autodefense"],
              "Runs in the background. When a radar flags an <b>incoming</b> missile, every connected Air-Defense Battery "
              "is told to fire at it (a battery still cooling down refuses). With <b>--emp</b> the EMP Tower is set to <b>missiles</b> mode and fired too, but only while "
              "it stays in that mode, so it never drains your machines. Every action is logged to "
              "<code>/tmp/autodefense.log</code>."),
        _prog("emp", USAGE["emp"],
              "Shows the tower&#39;s mode, radius and cooldown, changes them, and fires it after you confirm. Modes: "
              "<b>all</b>, <b>missiles</b>, <b>machines</b>."),
    ]) + "</div>"

    rows = "".join(
        f"<tr><td>{' / '.join(f'<b>{esc(n)}</b>' for n in names)}</td><td>{esc(machine)}</td>"
        f"<td>{', '.join(esc(c) for c in calls)}</td><td>{', '.join(esc(s) for s in signals)}</td></tr>"
        for names, machine, calls, signals, _env in COMPONENTS)
    table = f"""<div class="card" style="margin-top:18px"><h2>Write your own</h2>
<div class="pt" style="margin:6px 0 10px">Each machine is a component with these calls (plus <b>getInfo</b>, which reports its
range, costs and limits from the server&#39;s config). A refused action answers <b>false</b> and the reason the machine&#39;s
screen shows, such as <code>no_power</code> or <code>cooldown</code>. Signals arrive only when something changes, with the
component&#39;s address first.</div>
<table class="t"><tr><th>Component</th><th>Machine</th><th>Main calls</th><th>Signals</th></tr>{rows}</table></div>"""

    body = f"""{top}{disk}
<div class="label" style="margin:26px 0 0">The four programs</div>{progs}
{table}"""
    return page_doc("Computer Control", f"The {LABEL} floppy and the OpenComputers hookups of every ICBM machine.", body,
                    extra=CSS,
                    credit=[OC_CREDIT,
                            "Machine overlays © HBM's Nuclear Tech Mod contributors, GNU LGPL v3; modified.",
                            "Hulls: GregTech 5 Unofficial textures, LGPL-3.0."])


PAGES = {"icbm-computer-control": sheet}

if __name__ == "__main__":
    import build
    build.main(["icbm-computer-control"])
