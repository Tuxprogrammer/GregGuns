import re, sys, collections, subprocess
f = sys.argv[1]
p = subprocess.Popen(['jfr', 'print', '--events', 'jdk.ExecutionSample', '--stack-depth', '60', f], stdout=subprocess.PIPE, text=True, errors='replace')
ev = []; cur = None
for ln in p.stdout:
    s = ln.rstrip('\n')
    if s.startswith('jdk.ExecutionSample'):
        cur = {'t': None, 'th': None, 'st': []}; ev.append(cur)
    elif cur is not None:
        m = re.match(r'\s*startTime = (\S+)', s)
        if m: cur['t'] = m.group(1)
        m = re.match(r'\s*sampledThread = "([^"]+)"', s)
        if m: cur['th'] = m.group(1)
        m = re.match(r'\s+([\w.$<>/]+)\.([\w$<>]+)\(.*?\)\s+line:', s)
        if m: cur['st'].append(m.group(1) + '.' + m.group(2))
srv = [e for e in ev if e['th'] == 'Server thread' and e['st']]
print('samples total', len(ev), 'server thread', len(srv))
def pkg(c):
    c = c.replace('/', '.')
    for pre in ['com.gregguns', 'gregtech', 'gtPlusPlus', 'bartworks', 'com.github.bartimaeusnek', 'micdoodle8', 'cpw.mods.fml', 'net.minecraft', 'net.minecraftforge', 'thaumcraft', 'ic2', 'codechicken', 'cofh', 'appeng', 'buildcraft', 'Reika', 'com.emoniph', 'com.falsepattern', 'ganymedes01', 'biomesoplenty', 'forestry', 'com.gtnewhorizon', 'galaxyspace', 'java.', 'jdk.', 'sun.', 'it.unimi', 'com.google']:
        if c.startswith(pre): return pre
    return '.'.join(c.split('.')[:2])
inc = collections.Counter(); leaf = collections.Counter(); top_non_vanilla = collections.Counter()
for e in srv:
    ps = set(pkg(x) for x in e['st']); [inc.update([x]) for x in ps]
    leaf[e['st'][0]] += 1
    for fr in e['st']:
        pk = pkg(fr)
        if pk not in ('net.minecraft', 'java.', 'jdk.', 'sun.', 'cpw.mods.fml', 'net.minecraftforge'):
            top_non_vanilla[fr] += 1; break
n = len(srv)
print('INCLUSIVE share of server-thread samples by package (any frame in stack):')
for k, v in inc.most_common(22): print('  %-28s %6.2f%%' % (k, 100.0 * v / n))
print('LEAF methods:')
for k, v in leaf.most_common(15): print('  %6.2f%% %s' % (100.0 * v / n, k))
print('FIRST non-vanilla frame from the leaf (who is running):')
for k, v in top_non_vanilla.most_common(20): print('  %6.2f%% %s' % (100.0 * v / n, k))
gg = [e for e in srv if any(x.startswith('com.gregguns') for x in e['st'])]
c = collections.Counter()
for e in gg:
    for fr in e['st']:
        if fr.startswith('com.gregguns'): c[fr] += 1; break
print('GREGGUNS samples (inclusive): %d = %.3f%%; innermost gregguns frame:' % (len(gg), 100.0 * len(gg) / n))
for k, v in c.most_common(15): print('  %6.3f%% %s' % (100.0 * v / n, k))
win = [e for e in srv if e['t'] and '23:18:0' in e['t'] or e['t'] and '23:18:1' in e['t']]
c2 = collections.Counter()
for e in win:
    c2[e['st'][0]] += 1
print('window 23:18:00-23:18:19 server samples', len(win))
for k, v in c2.most_common(8): print('  ', v, k)
c3 = collections.Counter()
for e in win:
    for fr in e['st']:
        if pkg(fr) not in ('net.minecraft', 'java.', 'jdk.', 'sun.', 'cpw.mods.fml', 'net.minecraftforge'): c3[fr] += 1; break
for k, v in c3.most_common(8): print('  nv', v, k)
