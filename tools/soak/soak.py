#!/usr/bin/env python3
"""Soak orchestrator for the GregGuns pack server (runs on the test box). stdlib only.
usage: SOAK_ROOT=<folder> [SOAK_PORT=25670] python3 soak.py [seconds]; see docs/testing/m9-soak.md.
Starts 3 bots, writes their waypoint schedules, drives console commands, samples metrics. Writes soak/metrics.csv."""
import os, subprocess, sys, time, random, re
ROOT = os.environ['SOAK_ROOT']          # the test folder: srv/ (the pack server), bot/ (this script), console.fifo
PORT = os.environ.get('SOAK_PORT', '25670')
FIFO = ROOT + '/console.fifo'
LOG = ROOT + '/srv/logs/latest.log'
OUT = ROOT + '/soak'; os.makedirs(OUT, exist_ok=True)
TOTAL = float(sys.argv[1]) if len(sys.argv) > 1 else 3300   # seconds the bots stay connected
env = dict(os.environ)                   # jstat must be on PATH (a JDK)
def con(c):
    with open(FIFO, 'w') as f: f.write(c + '\n')
pid = open(ROOT + '/server.pid').read().strip()
def jpid():
    return subprocess.run(['pgrep', '-f', 'lwjgl3ify-forgePatches'], capture_output=True, text=True).stdout.split()[0]
JP = jpid()
rnd = random.Random(128)
# ---- schedules
sched = {'Alice': [], 'Bob': [], 'Carol': []}
names = list(sched)
pts = [(x, z) for x in range(-1056, 1057, 192) for z in range(-1056, 1057, 192)]  # 12x12
STEP = 12
t = 12
rows = {n: [] for n in names}
for i, p in enumerate(pts): rows[names[i % 3]].append(p)
for n in names:
    tt = 12
    for (x, z) in rows[n]:
        sched[n].append((tt, '/tp %d 110 %d' % (x, z))); tt += STEP
phaseA_end = 12 + STEP * len(rows['Alice'])
# Ross: Bob -> 64, Carol -> 63, 24 points each, 12 s apart; Alice keeps sweeping a wider ring in overworld
for n, dim in (('Bob', 64), ('Carol', 63)):
    tt = phaseA_end + 5
    for k in range(24):
        x = (k % 5) * 192 - 384; z = (k // 5) * 192 - 384
        sched[n].append((tt, '/tpl %d 150 %d %d' % (x, z, dim))); tt += STEP
rossEnd = phaseA_end + 5 + 24 * STEP
# loiter: every 90 s hop between revisited sites in the overworld (generated area), return from Ross first
sites = [(0, 0), (-500, 300), (700, -400), (-900, -900), (300, 900), (900, 900), (-300, -300), (500, 500)]
for n in names:
    tt = rossEnd + (names.index(n) * 20)
    k = names.index(n)
    first = True
    while tt < TOTAL - 30:
        x, z = sites[k % len(sites)]
        cmd = '/tpl %d 110 %d 0' % (x, z) if first else '/tp %d 110 %d' % (x, z)
        sched[n].append((tt, cmd)); first = False
        k += 1; tt += 90
for n in names:
    with open(OUT + '/%s.cmd' % n, 'w') as f:
        for d, c in sorted(sched[n]): f.write('%d\t%s\n' % (d, c))
# ---- start
t0 = time.time()
open(OUT + '/start_epoch', 'w').write(str(t0))
con('gregguns dumprecipes ' + ROOT + '/soak/recipes.json')
time.sleep(60)
procs = []
for n in names:
    procs.append(subprocess.Popen(['python3', os.path.join(os.path.dirname(os.path.abspath(__file__)), 'bot.py'), n, '127.0.0.1', PORT, str(TOTAL), OUT + '/%s.cmd' % n],
                                  stdout=open(OUT + '/%s.out' % n, 'w'), stderr=subprocess.STDOUT))
    time.sleep(3)
time.sleep(5)
for n in names:
    con('op ' + n); con('gamemode 1 ' + n)
bt0 = time.time()
hdr = 'elapsed_s,rss_mb,S0,S1,E,O,YGC,YGCT,FGC,FGCT,GCT'
m = open(OUT + '/metrics.csv', 'w'); m.write(hdr + '\n'); m.flush()
npc_runs = [1500, 2100, 2700]; done_npc = set()
last_tps = 0
while time.time() - bt0 < TOTAL + 20:
    el = time.time() - bt0
    if time.time() - last_tps >= 30:
        last_tps = time.time()
        con('forge tps')
        try:
            rss = int(subprocess.run(['ps', '-o', 'rss=', '-p', pid], capture_output=True, text=True).stdout.strip()) // 1024
            js = subprocess.run(['jstat', '-gcutil', JP], capture_output=True, text=True, env=env).stdout.strip().split('\n')[-1].split()
            # S0 S1 E O M CCS YGC YGCT FGC FGCT CGC CGCT GCT  (jdk21)
            m.write('%d,%d,%s,%s,%s,%s,%s,%s,%s,%s,%s\n' % (el, rss, js[0], js[1], js[2], js[3], js[6], js[7], js[8], js[9], js[-1])); m.flush()
        except Exception as e:
            m.write('%d,err,%s\n' % (el, e)); m.flush()
    for r in npc_runs:
        if el >= r and r not in done_npc:
            done_npc.add(r); con('gregguns test npc.soak')
    if all(p.poll() is not None for p in procs): break
    time.sleep(1)
con('forge tps')
open(OUT + '/finished', 'w').write(str(time.time()))
