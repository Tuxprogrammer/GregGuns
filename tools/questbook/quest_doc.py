"""Regenerate the generated tables of docs/gregguns-spec/12-questbook/quest-line.md from the quest data.

usage: python -I tools/questbook/quest_doc.py [--check] [--data tools/questbook/quests] [--doc <quest-line.md>]

Rewrites, in place, only what the data decides: the quest count in the opening line, every chapter's quest table, the
reward budget, the totals and the hidden-trigger table. Prose around them (gates, what a chapter teaches, the coverage
matrix) is left alone. With --check nothing is written; the exit code is 1 when the doc is stale.

Python 3, standard library only. Reads the data through build_quests.load_data, so the rows match what the builder
ships.
"""
import argparse
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))  # -I leaves the script's folder off sys.path
import build_quests  # noqa: E402

HERE = os.path.dirname(os.path.abspath(__file__))
DOC = os.path.join(HERE, '..', '..', 'docs', 'gregguns-spec', '12-questbook', 'quest-line.md')

# Chapter section heading in the doc -> chapter file. The order is the doc's.
CHAPTERS = [('Start here', 'start'), ('Steam / ULV', 'steam'), ('LV', 'lv'), ('MV', 'mv'), ('HV', 'hv'), ('EV', 'ev'),
            ('IV', 'iv'), ('LuV', 'luv'), ('ZPM', 'zpm'), ('NPCs and bosses', 'npc'), ('Structures', 'site'),
            ('Machines', 'mach'), ('Armor and equipment', 'gear'), ('Ammo logistics', 'ammo'),
            ('ICBM warheads and missiles', 'icbm')]
QUEST_HEADER = '| Key | Title | Tier | Hint | Task | Reward | Prerequisites | Layout |'
TRIGGER_HEADER = '| Key | Tier | Detects (not consumed) | Prerequisites | Completes |'
TOTALS_HEADER = '| Chapter | Quests | Checkbox | Retrieval | Crafting | Hunt | Bags |'
BUDGET_HEADER = '| Bag | Group id (`Damage`) | Lowest tier | Bags | Quests |'
COINS = ['1', '10', '100', '1K', '10K']  # gregguns:coin damage -> face value (CoinDenomination)


def plain(text):
    """Description text without the BetterQuesting tags, on one line."""
    text = re.sub(r'\[/?[a-z]+\]', '', text)
    return ' '.join(text.split())


def hint(q):
    """The first sentence of the description."""
    text = plain(q['desc'].split('\n\n')[0])
    m = re.match(r'(.+?[.!?])(\s|$)', text)
    return (m.group(1) if m else text).replace('|', '/')


def item(s):
    """One data stack -> a short name: GregGuns ids bare, GT materials by ore name, GT machines by MTE id."""
    if 'bag' in s:
        return 'bag %s' % s['bag'] + (' x%d' % s['count'] if s.get('count', 1) != 1 else '')
    i = s['id']
    damage = s.get('damage', 0)
    if i.startswith('gregguns:'):
        name = '`%s`' % i[len('gregguns:'):]
        if i == 'gregguns:coin':
            name = '`coin %s`' % COINS[damage]
        elif damage:
            name = '`%s@%s`' % (i[len('gregguns:'):], damage)
    elif i == build_quests.GT_MACHINES:
        name = '`mte %s`' % damage
    elif s.get('ore'):
        name = '`%s`' % s['ore']
    else:
        name = '`%s%s`' % (i, '' if not damage else '@%s' % ('*' if damage == '*' else damage))
    if s.get('tag'):
        name += ' (%s)' % ', '.join('%s=%s' % kv for kv in sorted(flat(s['tag'])))
    count = s.get('count', 1)
    return name + (' x%d' % count if count != 1 else '')


def flat(tag, prefix=''):
    """A nested tag -> (dotted key, value) pairs."""
    out = []
    for k, v in tag.items():
        if isinstance(v, dict):
            out += flat(v, prefix + k + '.')
        else:
            out.append((prefix + k, v))
    return out


def task(t):
    kind = t['type']
    if kind == 'checkbox':
        return 'checkbox'
    if kind == 'hunt':
        return 'hunt `%s` x%d' % (t['target'], t['count'])
    if kind == 'fluid':
        return 'fluid ' + ' + '.join('`%s` %d mB' % (f['name'], f['amount']) for f in t['fluids'])
    if kind == 'location':
        return 'location dimension %d' % t['dimension']
    joiner = ' or ' if t.get('any') else ' + '
    if kind == 'optional_retrieval':
        joiner = ' / '
    label = {'retrieval': 'retrieval', 'optional_retrieval': 'optional', 'crafting': 'crafting'}[kind]
    nbt = ', NBT matched' if t.get('matchNBT') else ''
    return '%s %s%s' % (label, joiner.join(item(s) for s in t['items']), nbt)


def reward(r):
    kind = r['type']
    if kind == 'bag':
        return 'bag %s x%d' % (r['tier'], r.get('count', 1))
    if kind == 'xp':
        return '%d %s' % (r['amount'], 'levels' if r.get('levels', True) else 'XP')
    text = ', '.join(item(s) for s in r['items'])
    return ('choice: ' + text) if kind == 'choice' else text


def prereqs(q):
    out = []
    for p in q['pre']:
        if isinstance(p, int):
            out.append('pack %d' % p)
        else:
            out.append('`%s`%s' % (p, ' (hover)' if p in q.get('implicit', ()) else ''))
    text = ', '.join(out) or 'none'
    return ('any of ' + text) if q.get('logic') == 'OR' and len(out) > 1 else text


def layout(q):
    main = q.get('main', q.get('size', 24) > 24)
    return '%d, %d (%d)%s' % (q['x'], q['y'], q['size'], ', main' if main else '')


def quest_row(q):
    tasks = '; '.join(task(t) for t in q['tasks'])
    if q.get('taskLogic') == 'OR':
        tasks = 'any one of: ' + tasks
    rewards = '; '.join(reward(r) for r in q['rewards']) or 'none'
    title = q['title'] + (' (SECRET)' if q.get('visibility') == 'SECRET' else '')
    return '| `%s` | %s | %s | %s | %s | %s | %s | %s |' % (
        q['key'], title, q['tier'], hint(q), tasks, rewards, prereqs(q), layout(q))


def trigger_row(q):
    detects = '; '.join(task(t) for t in q['tasks'])
    return '| `%s` | %s | %s | %s | %s |' % (
        q['key'], q['tier'], detects, prereqs(q), ', '.join('`%s`' % k for k in q.get('completes', [])))


def bags(q):
    """(bag name, count) for every bag the quest hands out, from a bag reward or a {"bag": name} stack."""
    out = []
    for r in q['rewards']:
        if r['type'] == 'bag':
            out.append((r['tier'].lower(), r.get('count', 1)))
        elif r['type'] in ('item', 'choice'):
            out += [(s['bag'].lower(), s.get('count', 1)) for s in r['items'] if 'bag' in s]
    return out


def replace_table(lines, start, header, rows):
    """Replace the table whose header is the first line equal to `header` at or after `start`."""
    i = lines.index(header, start)
    j = i + 2
    while j < len(lines) and lines[j].startswith('|'):
        j += 1
    lines[i:j] = [header, '|' + '---|' * header.count(' | ') + '---|'] + rows
    return i


def render(data, text):
    lines = text.split('\n')
    by_chapter = {}
    for q in data['quests']:  # data order, which is each chapter file's order
        by_chapter.setdefault(q['chapter'], []).append(q)
    shown = [q for q in data['quests'] if not q.get('skip')]
    triggers = sorted((q for q in data['quests'] if q.get('skip')),
                      key=lambda q: (q['chapter'] != 'skip', build_quests.TIERS.index(q['tier']), q['key']))
    # Opening line.
    count = 'tab: %d quests in %d chapters, plus %d hidden tier-skip triggers' % (len(shown), len(CHAPTERS),
                                                                                  len(triggers))
    for n, line in enumerate(lines):
        if line.startswith('The full quest list for the GregGuns tab:'):
            lines[n] = re.sub(r'tab: \d+ quests in \d+ chapters(, plus \d+ hidden tier-skip triggers)?', count, line)
            break
    # Chapter tables.
    for heading, chapter in CHAPTERS:
        start = lines.index('### ' + heading)
        rows = [quest_row(q) for q in by_chapter.get(chapter, []) if not q.get('skip')]
        replace_table(lines, start, QUEST_HEADER, rows)
    # Reward budget: bags per bag group.
    paid = {}
    for q in shown:
        for name, count in bags(q):
            entry = paid.setdefault(name, [0, []])
            entry[0] += count
            if q['key'] not in entry[1]:
                entry[1].append(q['key'])
    budget = []
    for name, b in sorted(build_quests.BAGS.items(), key=lambda kv: kv[1]['damage']):
        count, keys = paid.get(name, [0, []])
        budget.append('| %s | %d | %s | %d | %s |' % (
            name, b['damage'], b['tier'], count, ', '.join('`%s`' % k for k in sorted(keys)) or 'none'))
    start = lines.index('### Reward budget')
    for n in range(start, len(lines)):
        if re.match(r'^\d+ quests pay a bag\.', lines[n]):
            lines[n] = re.sub(r'^\d+ quests pay a bag\.', '%d quests pay a bag.' % sum(1 for q in shown if bags(q)),
                              lines[n])
            break
    replace_table(lines, start, BUDGET_HEADER, budget)
    # Totals.
    totals = []
    sums = [0] * 6
    for heading, chapter in CHAPTERS:
        qs = [q for q in by_chapter.get(chapter, []) if not q.get('skip')]
        kinds = [{t['type'] for t in q['tasks']} for q in qs]
        row = [len(qs), sum('checkbox' in k for k in kinds), sum('retrieval' in k for k in kinds),
               sum('crafting' in k for k in kinds), sum('hunt' in k for k in kinds), sum(1 for q in qs if bags(q))]
        sums = [a + b for a, b in zip(sums, row)]
        totals.append('| %s | %s |' % (heading, ' | '.join(str(x) for x in row)))
    totals.append('| **Total** | %s |' % ' | '.join('**%d**' % x for x in sums))
    replace_table(lines, lines.index('## Totals'), TOTALS_HEADER, totals)
    # Hidden triggers.
    replace_table(lines, lines.index('## Hidden tier-skip triggers'), TRIGGER_HEADER,
                  [trigger_row(q) for q in triggers])
    return '\n'.join(lines)


def main():
    ap = argparse.ArgumentParser(description=__doc__.split('\n')[0])
    ap.add_argument('--data', default=os.path.join(HERE, 'quests'))
    ap.add_argument('--doc', default=DOC)
    ap.add_argument('--check', action='store_true', help='exit 1 if the doc is stale; write nothing')
    args = ap.parse_args()
    data = build_quests.load_data(args.data)
    with open(args.doc, encoding='utf8') as fh:
        old = fh.read()
    new = render(data, old)
    if args.check:
        if new != old:
            print('stale: %s (run tools/questbook/quest_doc.py)' % os.path.relpath(args.doc))
            return 1
        print('up to date')
        return 0
    if new != old:
        with open(args.doc, 'w', encoding='utf8', newline='\n') as fh:
            fh.write(new)
        print('wrote %s' % os.path.relpath(args.doc))
    else:
        print('up to date')
    return 0


if __name__ == '__main__':
    sys.exit(main())
