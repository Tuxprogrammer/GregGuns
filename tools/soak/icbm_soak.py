#!/usr/bin/env python3
"""ICBM soak driver (I7) for a GregGuns pack server started with -Dgregguns.testHooks=true. stdlib only.

Runs on the test box, next to the server. The server's stdin is a FIFO (console.fifo) and its stdout goes to a log file.

  icbm_soak.py --root R --log L --out O scenarios icbm.soak.missiles icbm.soak.blasts ...
      runs each soak scenario alone, sampling the server every 5 s (forge tps, gregguns soakstat, RSS, jstat heap)
  icbm_soak.py --root R --log L --out O --port P play SECONDS
      the long play soak: three bots (bot.py) visit silo complex sites and sweep fresh land, a missile salvo
      (icbm.soak.salvo) runs every 10 minutes, the server is sampled every 30 s; chunk tickets are counted with no
      player online before and after
  icbm_soak.py --root R --log L --out O --log2 L2 --start "CMD" restart SCENARIO DELAY SETTLE
      starts SCENARIO (icbm.soak.salvo), stops the server DELAY seconds later with missiles in the air, starts it
      again with CMD (which must log to L2), waits SETTLE seconds and checks that no GregGuns chunk ticket is left
  icbm_soak.py --log L --out O report
      writes O/summary.md from the log and O/metrics.csv

R holds console.fifo and server.pid. Every wait is bounded. See docs/testing/i7-soak.md."""
import argparse, csv, math, os, random, re, subprocess, sys, threading, time

STAT = re.compile(r'SOAKSTAT label=(\S+) (.*)$')
TPS = re.compile(r'Overall\s*: Mean tick time: ([\d.]+) ms\. Mean TPS: ([\d.]+)')
RESULT = re.compile(r'SOAKRESULT (.*)$')
SITE = re.compile(r'Silo complex, cell (-?\d+),(-?\d+), center x=(-?\d+) z=(-?\d+)')


class Server:
    def __init__(self, root, log):
        self.root, self.log = root, log
        self.pid = open(os.path.join(root, 'server.pid')).read().strip()

    def con(self, line):
        # A FIFO open blocks until the server reads; the server keeps it open (exec 3<>), so this is quick.
        with open(os.path.join(self.root, 'console.fifo'), 'w') as f:
            f.write(line + '\n')

    def lines(self):
        with open(self.log, 'rb') as f:
            return f.read().decode('utf-8', 'replace').splitlines()

    def count(self, rx):
        return sum(1 for l in self.lines() if re.search(rx, l))

    def wait(self, rx, after, timeout):
        """Waits until rx has matched more than `after` times; returns False on timeout or a dead server."""
        end = time.time() + timeout
        while time.time() < end:
            if self.count(rx) > after:
                return True
            if not self.alive():
                return False
            time.sleep(3)
        return False

    def alive(self):
        return os.path.exists('/proc/' + self.pid)

    def java_pid(self):
        # server.pid is the java process itself (run.sh execs it).
        return self.pid

    def rss_mb(self):
        try:
            return int(open('/proc/%s/status' % self.pid).read().split('VmRSS:')[1].split()[0]) // 1024
        except Exception:
            return -1

    def heap_mb(self):
        """Heap in use from jstat -gc (S0U S1U EU OU, KB); -1 when jstat is not on PATH."""
        try:
            out = subprocess.run(['jstat', '-gc', self.java_pid()], capture_output=True, text=True, timeout=20).stdout
            head, row = out.strip().split('\n')[-2:]
            v = dict(zip(head.split(), row.split()))
            return int(sum(float(v[k]) for k in ('S0U', 'S1U', 'EU', 'OU')) / 1024)
        except Exception:
            return -1


class Sampler(threading.Thread):
    """Every `period` seconds: forge tps, gregguns soakstat <label>, RSS and heap into metrics.csv."""

    def __init__(self, srv, out, period, label):
        super().__init__(daemon=True)
        self.srv, self.period, self.label = srv, period, label
        self.stop = threading.Event()
        new = not os.path.exists(os.path.join(out, 'metrics.csv'))
        self.f = open(os.path.join(out, 'metrics.csv'), 'a', newline='')
        self.w = csv.writer(self.f)
        if new:
            self.w.writerow(['epoch', 'label', 'rss_mb', 'heap_mb'])
        self.peak_rss = 0
        self.peak_heap = 0

    def run(self):
        while not self.stop.is_set():
            try:
                self.srv.con('forge tps')
                self.srv.con('gregguns soakstat ' + self.label[0])
            except OSError:
                pass
            rss, heap = self.srv.rss_mb(), self.srv.heap_mb()
            self.peak_rss, self.peak_heap = max(self.peak_rss, rss), max(self.peak_heap, heap)
            self.w.writerow([int(time.time()), self.label[0], rss, heap])
            self.f.flush()
            self.stop.wait(self.period)


def scenarios(srv, out, names, timeout):
    label = ['-']
    s = Sampler(srv, out, 5, label)
    s.start()
    rows = []
    for n in names:
        label[0] = n
        before = srv.count(r'\] Scenarios: ')
        t0 = time.time()
        srv.con('gregguns test ' + n)
        ok = srv.wait(r'\] Scenarios: ', before, timeout)
        rows.append((n, ok, int(time.time() - t0), s.peak_rss, s.peak_heap))
        print('%s finished=%s in %d s, peak RSS %d MB, peak heap %d MB' % rows[-1], flush=True)
        s.peak_rss = s.peak_heap = 0
        if not ok:
            break
        time.sleep(10)
    s.stop.set()
    with open(os.path.join(out, 'runs.csv'), 'a', newline='') as f:
        csv.writer(f).writerows(rows)
    return all(r[1] for r in rows)


def sites(srv):
    before = srv.count(SITE.pattern)
    srv.con('gregguns locate complex 12')
    time.sleep(8)
    found = []
    for l in srv.lines():
        m = SITE.search(l)
        if m:
            found.append((int(m.group(3)), int(m.group(4))))
    return found[before:] or [(0, 0)]


def play(srv, out, seconds, port, here):
    """The long soak. Bots: Alice and Bob tour complex sites and sweep outward; Carol visits the Moon in between."""
    label = ['play-idle']
    s = Sampler(srv, out, 30, label)
    s.start()
    time.sleep(65)  # an idle sample before anyone joins: tickets and loaded chunks with no player
    found = sites(srv)
    print('complex sites:', found, flush=True)
    rnd = random.Random(7)
    names = ['Alice', 'Bob', 'Carol']
    sched = {n: [] for n in names}
    # Each bot: a complex site (2 minutes at its middle, then a ring of four 30-second stops 160 blocks out), next
    # site, ... then a widening sweep of fresh land, 20 s a stop; repeat until the end.
    for k, n in enumerate(names):
        t, i, ring = 15.0 + 5 * k, k, 0
        while t < seconds - 60:
            x, z = found[i % len(found)]
            if n == 'Carol' and i % 3 == 2:
                sched[n].append((t, '/tpl %d 120 %d 28' % (x // 8, z // 8)))  # the Moon (Galacticraft dim 28)
                t += 300
                sched[n].append((t, '/tpl %d 120 %d 0' % (x, z)))
                t += 10
            sched[n].append((t, '/tp %d 140 %d' % (x, z)))
            t += 120
            for a in range(4):
                ang = math.pi / 2 * a + rnd.random()
                sched[n].append((t, '/tp %d 140 %d' % (x + 160 * math.cos(ang), z + 160 * math.sin(ang))))
                t += 30
            ring += 1
            for _ in range(6):
                ang = rnd.random() * 2 * math.pi
                r = 600 + 120 * ring + rnd.random() * 400
                sched[n].append((t, '/tp %d 140 %d' % (x + r * math.cos(ang), z + r * math.sin(ang))))
                t += 20
            i += 3
    procs = []
    for n in names:
        with open(os.path.join(out, n + '.cmd'), 'w') as f:
            for d, c in sorted(sched[n]):
                f.write('%d\t%s\n' % (d, c))
        procs.append(subprocess.Popen(
            [sys.executable, '-I', os.path.join(here, 'bot.py'), n, '127.0.0.1', str(port), str(seconds),
             os.path.join(out, n + '.cmd')], stdout=open(os.path.join(out, n + '.out'), 'w'), stderr=subprocess.STDOUT))
        time.sleep(3)
    time.sleep(8)
    for n in names:
        srv.con('op ' + n)
        srv.con('gamemode 1 ' + n)
    label[0] = 'play'
    t0 = time.time()
    next_salvo = t0 + 300
    salvos = 0
    while time.time() - t0 < seconds + 30 and srv.alive():
        if time.time() >= next_salvo:
            srv.con('gregguns test icbm.soak.salvo')
            salvos += 1
            next_salvo += 600
        if all(p.poll() is not None for p in procs):
            break
        time.sleep(5)
    for p in procs:
        try:
            p.wait(timeout=60)
        except subprocess.TimeoutExpired:
            p.kill()
    # Let the last salvo finish and the dimensions with nobody in them unload, then the closing idle samples.
    label[0] = 'play-settle'
    time.sleep(240)
    label[0] = 'play-idle-end'
    time.sleep(65)
    s.stop.set()
    print('play finished: %d salvos, server alive %s' % (salvos, srv.alive()), flush=True)
    return srv.alive()


def stat_line(srv, label):
    """Asks for one soakstat sample and returns it as a dict."""
    before = srv.count('SOAKSTAT label=' + label + ' ')
    srv.con('gregguns soakstat ' + label)
    if not srv.wait('SOAKSTAT label=' + label + ' ', before, 60):
        return {}
    for l in reversed(srv.lines()):
        m = STAT.search(l)
        if m and m.group(1) == label:
            return dict(kv.split('=', 1) for kv in m.group(2).split())
    return {}


def restart(srv, out, scenario, delay, settle, start_cmd, log2):
    """A server restart while missiles fly: their tickets come back with them and are gone once they land."""
    srv.con('gregguns test ' + scenario)
    # The salvo sets up, settles and takes its baseline (about 45 s) before it launches.
    time.sleep(delay)
    inflight = stat_line(srv, 'restart-inflight')
    print('in flight before the stop:', inflight.get('flying'), 'tickets', inflight.get('tickets'), flush=True)
    srv.con('save-all')
    time.sleep(5)
    srv.con('stop')
    end = time.time() + 180
    while srv.alive() and time.time() < end:
        time.sleep(2)
    if srv.alive():
        print('the server did not stop', flush=True)
        return False
    subprocess.run(start_cmd, shell=True, timeout=60)
    srv2 = Server(srv.root, log2)
    if not srv2.wait(r'Done \(', 0, 600):
        print('the server did not come back', flush=True)
        return False
    early = stat_line(srv2, 'restart-early')
    time.sleep(settle)
    after = stat_line(srv2, 'restart-after')
    print('after the restart: early', early.get('flying'), 'flying /', early.get('tickets'), 'tickets; after',
          settle, 's:', after.get('flying'), 'flying /', after.get('tickets'), 'tickets /', after.get('forced'),
          'forced', flush=True)
    ok = after.get('tickets') == '0' and after.get('forced') == '0' and after.get('flying') == '0'
    with open(os.path.join(out, 'restart.txt'), 'w') as f:
        f.write('inflight %s\nearly %s\nafter %s\nok %s\n' % (inflight, early, after, ok))
    return ok


def report(log, out):
    stats, tps, results = [], [], []
    with open(log, 'rb') as f:
        for l in f.read().decode('utf-8', 'replace').splitlines():
            m = STAT.search(l)
            if m:
                d = dict(kv.split('=', 1) for kv in m.group(2).split())
                d['label'] = m.group(1)
                d['time'] = l[1:9]
                stats.append(d)
                continue
            m = TPS.search(l)
            if m:
                tps.append((l[1:9], float(m.group(1)), float(m.group(2))))
                continue
            m = RESULT.search(l)
            if m:
                results.append(dict(kv.split('=', 1) for kv in m.group(1).split()))
    with open(os.path.join(out, 'stats.csv'), 'w', newline='') as f:
        if stats:
            keys = ['time', 'label'] + [k for k in stats[0] if k not in ('time', 'label')]
            w = csv.DictWriter(f, keys, extrasaction='ignore')
            w.writeheader()
            w.writerows(stats)
    with open(os.path.join(out, 'tps.csv'), 'w', newline='') as f:
        w = csv.writer(f)
        w.writerow(['time', 'overall_mean_tick_ms', 'overall_tps'])
        w.writerows(tps)
    lines = ['| Soak | Verdict | Baseline ms | Burst mean / p95 / max ms | Burst s (server s) | 20 TPS again after s'
             ' | First 30 s after ms | Back near baseline after s (ms) | Tickets before -> after | Peak tickets / forced'
             ' chunks | Peak heap MB | Peak entities |',
             '|---|---|---|---|---|---|---|---|---|---|---|---|']
    for r in results:
        lines.append('| %s | %s | %s | %s / %s / %s | %s (%s) | %s | %s | %s (%s) | %s | %s / %s | %s | %s |' % (
            r['name'], r['verdict'], r['baseline_ms'], r['burst_mean_ms'], r['burst_p95_ms'], r['burst_max_ms'],
            r['burst_s'], r['burst_server_s'], r['at20tps_after_s'], r['first30s_ms'], r['back_after_s'],
            r['recovery_ms'], r['tickets'], r['peak_tickets'], r['peak_forced'], r['peak_heap_mb'],
            r['peak_entities']))
    play = [s for s in stats if s['label'].startswith('play')]
    if play:
        idle0 = [s for s in play if s['label'] == 'play-idle']
        idle1 = [s for s in play if s['label'] == 'play-idle-end']
        live = [s for s in play if s['label'] == 'play']
        means = [float(s['tick_mean_ms']) for s in live]
        lines += ['', '| Play soak | Value |', '|---|---|',
                  '| samples (30 s) | %d |' % len(live),
                  '| mean tick, median / max of 100-tick means | %.2f / %.2f ms |' % (
                      sorted(means)[len(means) // 2] if means else 0, max(means) if means else 0),
                  '| tickets idle before -> idle after | %s -> %s |' % (
                      idle0[-1]['tickets'] if idle0 else '?', idle1[-1]['tickets'] if idle1 else '?'),
                  '| forced chunks idle before -> idle after | %s -> %s |' % (
                      idle0[-1]['forced'] if idle0 else '?', idle1[-1]['forced'] if idle1 else '?'),
                  '| loaded chunks idle before -> idle after | %s -> %s |' % (
                      idle0[-1]['loaded_chunks'] if idle0 else '?', idle1[-1]['loaded_chunks'] if idle1 else '?'),
                  '| peak tickets / forced while playing | %s / %s |' % (
                      max(int(s['tickets']) for s in live) if live else 0,
                      max(int(s['forced']) for s in live) if live else 0),
                  '| peak heap MB (in-game sample) | %s |' % (max(int(s['heap_mb']) for s in live) if live else 0)]
    with open(os.path.join(out, 'summary.md'), 'w') as f:
        f.write('\n'.join(lines) + '\n')
    print('\n'.join(lines))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--root')
    ap.add_argument('--log', required=True)
    ap.add_argument('--out', required=True)
    ap.add_argument('--port', default='25690')
    ap.add_argument('--timeout', type=int, default=3600, help='seconds one scenario may take')
    ap.add_argument('--log2', help='restart: the log of the restarted server')
    ap.add_argument('--start', help='restart: the shell command that starts the server again')
    ap.add_argument('mode', choices=['scenarios', 'play', 'restart', 'report'])
    ap.add_argument('args', nargs='*')
    a = ap.parse_args()
    os.makedirs(a.out, exist_ok=True)
    here = os.path.dirname(os.path.abspath(__file__))
    ok = True
    if a.mode == 'scenarios':
        ok = scenarios(Server(a.root, a.log), a.out, a.args, a.timeout)
    elif a.mode == 'play':
        ok = play(Server(a.root, a.log), a.out, float(a.args[0]), a.port, here)
    elif a.mode == 'restart':
        ok = restart(Server(a.root, a.log), a.out, a.args[0], float(a.args[1]), float(a.args[2]), a.start, a.log2)
        print('restart check', 'PASS' if ok else 'FAIL', flush=True)
        a.log = a.log2 or a.log
    report(a.log, a.out)
    open(os.path.join(a.out, 'finished'), 'w').write('%s %s\n' % (int(time.time()), ok))
    sys.exit(0 if ok else 1)


if __name__ == '__main__':
    main()
