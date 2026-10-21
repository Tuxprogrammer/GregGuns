"""Validate a BetterQuesting quest folder: a pack DefaultQuests folder, or the in-jar questbook resource root.

usage: python -I tools/questbook/validate_quests.py <dir> [--only GregGuns-] [--registry registry.txt] [--entities entities.txt]

registry.txt: one item registry name per line (modid:name).
entities.txt: one EntityList string name per line.
--only: report only errors whose message contains this text (the shipped pack has older findings of its own).
Checks: file names match ids, prerequisites exist, no cycles, line entries point at quests, no overlapping boxes,
QuestLinesOrder.txt entries exist (when the file is present), index.json lists exactly the files on disk (when
present), and optionally registry and entity names. Exit code 1 if any check fails. Python 3, standard library only.
"""
import base64
import glob
import json
import os
import re
import struct
import sys


def decode(b64):
    return struct.unpack('>qq', base64.urlsafe_b64decode(b64))


def load(path):
    with open(path, encoding='utf8') as fh:
        return json.load(fh)


def main(root, registry=None, entities=None, only=None):
    errors = []
    quests = {}

    def rel(p):
        return os.path.relpath(p, root).replace(os.sep, '/')

    for path in glob.glob(os.path.join(root, 'Quests', '**', '*.json'), recursive=True):
        d = load(path)
        qid = (d['questIDHigh:4'], d['questIDLow:4'])
        if qid in quests:
            errors.append('duplicate quest id in %s' % rel(path))
        quests[qid] = (path, d)
        m = re.search(r'-([A-Za-z0-9_-]{22}==)\.json$', path)
        if not m or decode(m.group(1)) != qid:
            errors.append('file name does not match quest id: %s' % rel(path))
    graph = {}
    for qid, (path, d) in quests.items():
        deps = [(p['questIDHigh:4'], p['questIDLow:4']) for p in d.get('preRequisites:9', {}).values()]
        for dep in deps:
            if dep not in quests and dep[0] != 0:  # High 0 is a pack quest, outside this folder set
                errors.append('missing prerequisite %s in %s' % (dep, rel(path)))
        graph[qid] = [x for x in deps if x in quests]
    state = {}
    sys.setrecursionlimit(10000)

    def visit(n):
        state[n] = 1
        for m in graph[n]:
            if state.get(m) == 1:
                errors.append('prerequisite cycle through %s' % rel(quests[m][0]))
            elif m not in state:
                visit(m)
        state[n] = 2

    for n in graph:
        if n not in state:
            visit(n)
    lines = {}
    for qlpath in glob.glob(os.path.join(root, 'QuestLines', '*', 'QuestLine.json')):
        d = load(qlpath)
        lines[(d['questLineIDHigh:4'], d['questLineIDLow:4'])] = os.path.dirname(qlpath)
    order_path = os.path.join(root, 'QuestLinesOrder.txt')
    if os.path.exists(order_path):
        with open(order_path, encoding='utf8') as fh:
            order = [l.split(':')[0] for l in fh.read().splitlines() if l]
        for b64 in order:
            if decode(b64) not in lines:
                errors.append('QuestLinesOrder.txt entry without a line: %s' % b64)
    for folder in lines.values():
        boxes = []
        for path in glob.glob(os.path.join(folder, '*.json')):
            if path.endswith('QuestLine.json'):
                continue
            d = load(path)
            if (d['questIDHigh:4'], d['questIDLow:4']) not in quests:
                errors.append('line entry for unknown quest: %s' % rel(path))
            boxes.append((d['x:3'], d['y:3'], d['sizeX:3'], d['sizeY:3'], rel(path)))
        for i, a in enumerate(boxes):
            for b in boxes[i + 1:]:
                if a[0] < b[0] + b[2] and b[0] < a[0] + a[2] and a[1] < b[1] + b[3] and b[1] < a[1] + a[3]:
                    errors.append('overlap in %s: %s / %s' % (rel(folder), a[4], b[4]))
    index_path = os.path.join(root, 'index.json')
    if os.path.exists(index_path):
        idx = load(index_path)
        listed = set(idx['linePositions'] + idx['quests'] + [idx['line']])
        on_disk = {rel(p) for p in glob.glob(os.path.join(root, '**', '*.json'), recursive=True)} - {'index.json'}
        for p in sorted(listed - on_disk):
            errors.append('index.json lists a missing file: %s' % p)
        for p in sorted(on_disk - listed):
            errors.append('file not listed in index.json: %s' % p)
    if registry or entities:
        reg = set(open(registry, encoding='utf8').read().split()) if registry else None
        ents = set(open(entities, encoding='utf8').read().split()) if entities else None
        for qid, (path, d) in quests.items():
            blob = json.dumps(d)
            if reg is not None:
                for name in set(re.findall(r'"id:8": "([a-zA-Z0-9_]+:[^"]+)"', blob)):
                    if name.startswith('gregguns:') and name not in reg:
                        errors.append('%s: unknown item %s' % (rel(path), name))
            if ents is not None:
                for t in d.get('tasks:9', {}).values():
                    if t.get('taskID:8') == 'bq_standard:hunt' and t['target:8'] not in ents:
                        errors.append('%s: unknown entity %s' % (rel(path), t['target:8']))
    if only:
        errors = [e for e in errors if only in e]
    print('quests %d, lines %d, errors %d' % (len(quests), len(lines), len(errors)))
    for e in errors[:50]:
        print('ERROR', e)
    return 1 if errors else 0


if __name__ == '__main__':
    args = sys.argv[1:]
    reg = ents = None
    if '--registry' in args:
        reg = args[args.index('--registry') + 1]
    if '--entities' in args:
        ents = args[args.index('--entities') + 1]
    only = args[args.index('--only') + 1] if '--only' in args else None
    sys.exit(main(args[0], reg, ents, only))
