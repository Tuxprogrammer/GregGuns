import re, sys, collections, subprocess
p = subprocess.Popen(['jfr', 'print', '--events', 'jdk.ExecutionSample', '--stack-depth', '80', sys.argv[1]], stdout=subprocess.PIPE, text=True, errors='replace')
ev = []; cur = None
for ln in p.stdout:
    s = ln.rstrip('\n')
    if s.startswith('jdk.ExecutionSample'): cur = {'th': None, 'st': []}; ev.append(cur)
    elif cur is not None:
        m = re.match(r'\s*sampledThread = "([^"]+)"', s)
        if m: cur['th'] = m.group(1)
        m = re.match(r'\s+([\w.$<>/]+)\.([\w$<>]+)\(.*?\)\s+line:', s)
        if m: cur['st'].append(m.group(1) + '.' + m.group(2))
srv = [e for e in ev if e['th'] == 'Server thread' and e['st']]; n = len(srv)
selfgg = collections.Counter(e['st'][0] for e in srv if e['st'][0].startswith('com.gregguns'))
print('gregguns SELF (leaf) samples: %d = %.3f%%' % (sum(selfgg.values()), 100.0 * sum(selfgg.values()) / n))
for k, v in selfgg.most_common(12): print('  %6.3f%% %s' % (100.0 * v / n, k))
# below Wrapped: what is the callee frame directly above the Wrapped frame (index-1)
callee = collections.Counter(); w = 0
for e in srv:
    for i, fr in enumerate(e['st']):
        if fr.startswith('com.gregguns.worldgen.Ross128ProviderHook$Wrapped'):
            w += 1
            callee[(fr.split('.')[-1], e['st'][i - 1] if i > 0 else '<leaf>')] += 1; break
print('samples in Ross wrapper:', w, '=%.2f%%' % (100.0 * w / n))
for k, v in callee.most_common(10): print('  %5d %s' % (v, k))
sg = [e for e in srv if any('StructureGenerator' in x or 'StructurePopulator' in x or 'Stamper' in x for x in e['st'])]
print('structure generator/populator/stamper inclusive: %d = %.3f%%' % (len(sg), 100.0 * len(sg) / n))
npc = [e for e in srv if any(x.startswith('com.gregguns.entity') for x in e['st'])]
print('gregguns npc inclusive: %d = %.3f%%' % (len(npc), 100.0 * len(npc) / n))
gtp = [e for e in srv if any(x.startswith('com.gregguns.') and not x.startswith('com.gregguns.entity') and 'Ross128' not in x for x in e['st'])]
print('other gregguns inclusive: %d = %.3f%%' % (len(gtp), 100.0 * len(gtp) / n))
