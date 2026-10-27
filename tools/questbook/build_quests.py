"""Build the GregGuns BetterQuesting quest line (BQ 3.8.89-GTNH file format) from quests.json.

usage: python -I tools/questbook/build_quests.py --out src/main/resources/assets/gregguns/questbook [--lang src/main/resources/assets/gregguns/lang/en_US.lang] [--data quests.json]

Writes, under --out:
  index.json                          the list of every file (jars cannot list directories)
  QuestLines/GregGuns-<line id>/QuestLine.json and one position file per quest
  Quests/GregGuns-<line id>/<Name>-<id>.json, one per quest

The output is a pure function of quests.json: ids are uuid5 of the keys, keys are sorted, no timestamps. Two builds
give byte-identical files (GGS-12-02). Python 3, standard library only.
Spec: docs/gregguns-spec/12-questbook/authoring.md
"""
import argparse
import base64
import json
import os
import re
import shutil
import struct
import sys
import uuid

TIERS = ['Steam', 'LV', 'MV', 'HV', 'EV', 'IV', 'LuV', 'ZPM']
# EnhancedLootBags GroupMetaID per tier (config/EnhancedLootBags/LootBags.xml in the pack)
BAG_META = {'Steam': 2, 'LV': 4, 'MV': 5, 'HV': 6, 'EV': 7, 'IV': 8, 'LuV': 41, 'ZPM': 42}
LINE_FOLDER = 'GregGuns'


def qid(ns, key):
    """Quest or line key -> (high, low, base64) the way BetterQuesting stores a UUID."""
    b = uuid.uuid5(ns, key).bytes
    hi, lo = struct.unpack('>qq', b)
    return hi, lo, base64.urlsafe_b64encode(b).decode()


def file_name(title, b64):
    plain = re.sub('§.', '', title)
    return re.sub('[^a-zA-Z0-9]', '', plain)[:16] + '-' + b64 + '.json'


def stack(s):
    out = {'Count:3': s.get('count', 1), 'Damage:2': s.get('damage', 0), 'OreDict:8': s.get('ore', ''), 'id:8': s['id']}
    if s.get('tag'):
        # Item NBT in BetterQuesting's typed-key JSON: a string value is "<key>:8".
        out['tag:10'] = {k + ':8': v for k, v in s['tag'].items()}
    return out


def stacks(items):
    return {'%d:10' % i: stack(s) for i, s in enumerate(items)}


def task(t):
    kind = t['type']
    if kind == 'checkbox':
        return {'taskID:8': 'bq_standard:checkbox'}
    if kind == 'retrieval':
        return {'autoConsume:1': 0, 'consume:1': 0, 'groupDetect:1': 0,
                'ignoreNBT:1': 0 if t.get('matchNBT') else 1, 'partialMatch:1': 1,
                'requireOnlyOneItem:1': int(bool(t.get('any'))), 'requiredItems:9': stacks(t['items']),
                'taskID:8': 'bq_standard:retrieval'}
    if kind == 'crafting':
        return {'allowAnvil:1': 0, 'allowCraft:1': 1, 'allowCraftedFromStatistics:1': 0, 'allowSmelt:1': 1,
                'ignoreNBT:1': 0, 'partialMatch:1': 1, 'requiredItems:9': stacks(t['items']),
                'taskID:8': 'bq_standard:crafting'}
    if kind == 'hunt':
        return {'damageType:8': '', 'ignoreNBT:1': 1, 'required:3': t['count'], 'subtypes:1': 0,
                'target:8': t['target'], 'targetNBT:10': {}, 'taskID:8': 'bq_standard:hunt'}
    raise ValueError('unknown task type ' + kind)


def reward(r):
    if r['type'] == 'item':
        return {'ignoreDisabled:1': 0, 'rewardID:8': 'bq_standard:item', 'rewards:9': stacks(r['items'])}
    raise ValueError('unknown reward type ' + r['type'])


def complete_reward(ns, i, key):
    """bq_standard:questcompletion, the reward GTNH's tier-skip triggers use to complete the quests they skip."""
    return {'ignoreDisabled:1': 1, 'index:3': i, 'isSilent:1': 0, 'questID:8': qid(ns, key)[2],
            'rewardID:8': 'bq_standard:questcompletion'}


def quest_json(ns, q):
    hi, lo, _ = qid(ns, q['key'])
    skip = bool(q.get('skip'))

    def pre(ref):
        if isinstance(ref, int):  # pack quest: high 0, low = number
            return {'questIDHigh:4': 0, 'questIDLow:4': ref}
        h, l, _ = qid(ns, ref)
        out = {'questIDHigh:4': h, 'questIDLow:4': l}
        if ref in q.get('implicit', ()):  # BQ RequirementType.IMPLICIT (1): still gates, line drawn on hover only
            out['type:1'] = 1
        return out

    props = {  # every key the exporter writes, with the pack's defaults
        'autoClaim:1': int(skip), 'completion_animation:8': 'default', 'completion_particle:8': 'default',
        'confetti_icon:10': {'Count:3': 0, 'Damage:2': 0, 'OreDict:8': '', 'id:8': 'minecraft:stick'},
        'countAsQuest:1': 1, 'desc:8': q['desc'], 'globalShare:1': int(not skip), 'icon:10': stack(q['icon']),
        'isGlobal:1': 0, 'isMain:1': int(q.get('main', q.get('size', 24) > 24)), 'isSilent:1': int(skip), 'lockedProgress:1': 0,
        'name:8': q['title'], 'notification_duration:5': -1.0, 'notification_effect:3': -1,
        'notification_fade_in:5': -1.0, 'notification_fade_out:5': -1.0,
        'notification_icon_offset_y:3': -2147483648, 'notification_icon_scale:5': -1.0,
        'notification_pos_x:3': -2147483648, 'notification_pos_y:3': -2147483648,
        'notification_show_icon:8': 'default', 'notification_style:8': 'default',
        'notification_subtitle:8': '', 'notification_subtitle_scale:5': -1.0, 'notification_title:8': '',
        'notification_title_scale:5': -1.0, 'particle_count:3': -1, 'partySingleReward:1': 0,
        'questLogic:8': q.get('logic', 'AND'), 'repeatTime:3': -1, 'repeat_relative:1': 1, 'simultaneous:1': 0,
        'snd_complete:8': 'random.levelup', 'snd_update:8': 'random.levelup', 'taskLogic:8': 'AND',
        'visibility:8': q.get('visibility', 'NORMAL')}
    out = {'preRequisites:9': {'%d:10' % i: pre(p) for i, p in enumerate(q['pre'])},
           'properties:10': {'betterquesting:10': props},
           'questIDHigh:4': hi, 'questIDLow:4': lo, 'rewards:9': {}, 'tasks:9': {}}
    for i, t in enumerate(q['tasks']):
        out['tasks:9']['%d:10' % i] = dict(task(t), **{'index:3': i})
    for i, r in enumerate(q['rewards']):
        out['rewards:9']['%d:10' % i] = dict(reward(r), **{'index:3': i})
    for i, key in enumerate(q.get('completes', [])):
        out['rewards:9']['%d:10' % i] = complete_reward(ns, i, key)
    return out


def position_json(ns, q):
    hi, lo, _ = qid(ns, q['key'])
    return {'questIDHigh:4': hi, 'questIDLow:4': lo, 'sizeX:3': q['size'], 'sizeY:3': q['size'],
            'x:3': q['x'], 'y:3': q['y']}


def line_json(ns, line):
    hi, lo, _ = qid(ns, line['key'])
    props = {'bg_image:8': '', 'bg_size:3': 256, 'desc:8': line['desc'], 'icon:10': stack(line['icon']),
             'name:8': line['name'], 'visibility:8': line['visibility']}
    return {'properties:10': {'betterquesting:10': props}, 'questLineIDHigh:4': hi, 'questLineIDLow:4': lo}


LANG_BEGIN = '# BEGIN questbook (generated by tools/questbook/build_quests.py --lang; do not edit by hand)'
LANG_END = '# END questbook'


def lang_text(v):
    """Quest text -> a .lang value that BQ's translateToLocalFormatted returns unchanged ('%' doubled)."""
    return v.replace('%', '%%').replace('\r', '').replace('\n', '%n')


def lang_block(ns, data):
    """BetterQuesting looks up betterquesting.quest.<id>.name/.desc (and questline) before the inline text
    (QuestTranslation, BQ 3.8.89-GTNH). <id> is the url-safe base64 of the UUID with '=' removed."""
    rows = []
    _, _, line_b64 = qid(ns, data['line']['key'])
    line = data['line']
    rows.append('betterquesting.questline.%s.name=%s' % (line_b64.rstrip('='), lang_text(line['name'])))
    rows.append('betterquesting.questline.%s.desc=%s' % (line_b64.rstrip('='), lang_text(line['desc'])))
    for q in sorted(data['quests'], key=lambda q: q['key']):
        _, _, b64 = qid(ns, q['key'])
        rows.append('betterquesting.quest.%s.name=%s' % (b64.rstrip('='), lang_text(q['title'])))
        rows.append('betterquesting.quest.%s.desc=%s' % (b64.rstrip('='), lang_text(q['desc'])))
    return '\n'.join([LANG_BEGIN] + rows + [LANG_END]) + '\n'


def write_lang(path, block):
    """Replace the generated block in the lang file, or append it."""
    with open(path, encoding='utf8') as fh:
        text = fh.read()
    if LANG_BEGIN in text:
        head, rest = text.split(LANG_BEGIN, 1)
        tail = rest.split(LANG_END, 1)[1].lstrip('\n')
        text = head + block + ('\n' + tail if tail else '')
    else:
        text = text.rstrip('\n') + '\n\n' + block
    with open(path, 'w', encoding='utf8', newline='\n') as fh:
        fh.write(text)


def dump(obj):
    """Two-space JSON with sorted keys, like the exporter (which also escapes ' and =)."""
    text = json.dumps(obj, indent=2, sort_keys=True, ensure_ascii=False)
    return text.replace("'", '\\u0027').replace('=', '\\u003d') + '\n'


def check(data):
    """Build-time checks for the requirements that the file shape cannot show. Returns a list of problems."""
    problems = []
    keys = {q['key'] for q in data['quests']}
    if len(keys) != len(data['quests']):
        problems.append('duplicate quest key')
    for q in data['quests']:
        k = q['key']
        if q['tier'] not in TIERS:
            problems.append('%s: unknown tier %s' % (k, q['tier']))
            continue
        for c in q.get('completes', []):
            if c not in keys:
                problems.append('%s: completes unknown quest %s' % (k, c))
        for p in q['pre']:
            if isinstance(p, str) and p not in keys:
                problems.append('%s: unknown prerequisite %s' % (k, p))
        kinds = {t['type'] for t in q['tasks']}
        if 'crafting' in kinds and q['chapter'] != 'steam':
            problems.append('%s: crafting task outside the Steam chapter (GGS-12-09)' % k)
        for t in q['tasks']:
            if t['type'] == 'hunt' and not (t['target'] == 'Zombie' or t['target'].startswith('gregguns.')):
                problems.append('%s: odd hunt target %s' % (k, t['target']))
        for r in q['rewards']:
            for s in r['items']:
                if s['id'] == 'enhancedlootbags:lootbag':
                    tier = [t for t, m in BAG_META.items() if m == s.get('damage')]
                    if not tier:
                        problems.append('%s: unknown bag meta %s' % (k, s.get('damage')))
                    elif TIERS.index(tier[0]) > TIERS.index(q['tier']):
                        problems.append('%s: bag above the quest tier (GGS-12-07)' % k)
        if q.get('visibility') == 'SECRET' and not (q['chapter'] == 'npc' and k in (
                'gg.npc.dave', 'gg.npc.cyberdemon', 'gg.npc.alien_bug')):
            problems.append('%s: SECRET outside the three boss quests (GGS-12-13)' % k)
        if q.get('logic') == 'OR' and k != 'gg.start.welcome':
            problems.append('%s: OR logic (GGS-12-03)' % k)
    return problems


def build(data_path, out_dir, lang_path=None):
    with open(data_path, encoding='utf8') as fh:
        data = json.load(fh)
    problems = check(data)
    if problems:
        for p in problems:
            print('ERROR', p, file=sys.stderr)
        return 1
    ns = uuid.UUID(data['namespace'])
    _, _, line_b64 = qid(ns, data['line']['key'])
    folder = '%s-%s' % (LINE_FOLDER, line_b64)
    for sub in ('QuestLines', 'Quests'):
        shutil.rmtree(os.path.join(out_dir, sub), ignore_errors=True)
    files = {}
    line_path = 'QuestLines/%s/QuestLine.json' % folder
    files[line_path] = dump(line_json(ns, data['line']))
    positions, quests = [], []
    for q in sorted(data['quests'], key=lambda q: q['key']):
        _, _, b64 = qid(ns, q['key'])
        name = file_name(q['title'], b64)
        if q.get('skip'):  # a trigger: not in any line, like GTNH's Quests/NoQuestLine
            qp = 'Quests/NoQuestLine/%s' % name
            files[qp] = dump(quest_json(ns, q))
            quests.append(qp)
            continue
        pp = 'QuestLines/%s/%s' % (folder, name)
        qp = 'Quests/%s/%s' % (folder, name)
        files[pp] = dump(position_json(ns, q))
        files[qp] = dump(quest_json(ns, q))
        positions.append(pp)
        quests.append(qp)
    index = {'format': 1, 'line': line_path, 'linePositions': positions, 'quests': quests,
             'orderEntry': '%s: %s' % (line_b64, data['line']['name'])}
    files['index.json'] = json.dumps(index, indent=2, sort_keys=True) + '\n'
    for rel, text in files.items():
        path = os.path.join(out_dir, *rel.split('/'))
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, 'w', encoding='utf8', newline='\n') as fh:
            fh.write(text)
    if lang_path:
        write_lang(lang_path, lang_block(ns, data))
    print('wrote %d files (%d quests) to %s' % (len(files), len(quests), out_dir))
    return 0


if __name__ == '__main__':
    here = os.path.dirname(os.path.abspath(__file__))
    ap = argparse.ArgumentParser(description=__doc__.split('\n')[0])
    ap.add_argument('--out', required=True, help='questbook resource root')
    ap.add_argument('--data', default=os.path.join(here, 'quests.json'))
    ap.add_argument('--lang', help='en_US.lang to refresh the generated betterquesting.* block in')
    args = ap.parse_args()
    sys.exit(build(args.data, args.out, args.lang))
