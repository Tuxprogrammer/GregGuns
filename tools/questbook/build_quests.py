"""Build the GregGuns BetterQuesting quest line (BQ 3.8.89-GTNH file format) from the quests/ folder.

usage: python -I tools/questbook/build_quests.py --out src/main/resources/assets/gregguns/questbook [--lang src/main/resources/assets/gregguns/lang/en_US.lang] [--data tools/questbook/quests]

The data folder holds _line.json (format, namespace, line, retired) and one <chapter>.json per chapter
({"chapter": name, "retired": [...], "quests": [...]}), so writers working on different chapters never touch the
same file. The quest schema is in docs/gregguns-spec/12-questbook/authoring.md ("Quest data").

Writes, under --out:
  index.json                          the list of every file (jars cannot list directories) and the retired ids
  QuestLines/GregGuns-<line id>/QuestLine.json and one position file per quest
  Quests/GregGuns-<line id>/<Name>-<id>.json, one per quest

The output is a pure function of the data folder: ids are uuid5 of the keys, keys are sorted, no timestamps. Two builds
give byte-identical files (GGS-12-02). Python 3, standard library only.

Spec: docs/gregguns-spec/12-questbook/authoring.md
"""
import argparse
import base64
import glob
import json
import os
import re
import shutil
import struct
import sys
import uuid

TIERS = ['Steam', 'LV', 'MV', 'HV', 'EV', 'IV', 'LuV', 'ZPM']
# Title color per tier, the codes the pack's tier tabs put before every title (with bold, §l). A mixed-tier tab such
# as Multiblock Goals colors each title by its quest's tier; so does ours. Hidden triggers keep a plain title.
TIER_COLORS = {'Steam': '§c', 'LV': '§2', 'MV': '§6', 'HV': '§5', 'EV': '§8', 'IV': '§9', 'LuV': '§d', 'ZPM': '§b'}
# The one loot bag table. A {"type": "bag", "tier": name} reward and a {"bag": name} stack resolve through it, and the
# bag-tier check (GGS-12-07) reads each entry's quest tier from it: a bag is never above its quest's tier. Names are
# matched case-insensitively. Every name is a GregGuns group of EnhancedLootBags' own bag item (GroupMetaID as the
# damage), which GregGuns adds to ELB's group list at startup from assets/gregguns/lootbags/groups.xml
# (docs/gregguns-spec/06-items/pack-loot.md). The tier is the lowest quest tier that may hand the bag out. Group 1700
# is reserved and unused; bags hold GregGuns items and the precursors GregGuns recipes consume, never food (food is a
# separate item reward).
LOOTBAG = 'enhancedlootbags:lootbag'
BAGS = {
    'steam': {'id': LOOTBAG, 'damage': 1701, 'tier': 'Steam'},  # Recruit
    'lv': {'id': LOOTBAG, 'damage': 1702, 'tier': 'LV'},  # Rifleman
    'mv': {'id': LOOTBAG, 'damage': 1703, 'tier': 'MV'},  # Sergeant
    'hv': {'id': LOOTBAG, 'damage': 1704, 'tier': 'HV'},  # Lieutenant
    'ev': {'id': LOOTBAG, 'damage': 1705, 'tier': 'EV'},  # Captain
    'iv': {'id': LOOTBAG, 'damage': 1706, 'tier': 'IV'},  # Major
    'luv': {'id': LOOTBAG, 'damage': 1707, 'tier': 'LuV'},  # Colonel
    'zpm': {'id': LOOTBAG, 'damage': 1708, 'tier': 'ZPM'},  # General
    'spoils': {'id': LOOTBAG, 'damage': 1709, 'tier': 'HV'},  # Spoils of War: boss kills
    'ordnance1': {'id': LOOTBAG, 'damage': 1710, 'tier': 'HV'},  # ICBM HV and EV quests
    'ordnance2': {'id': LOOTBAG, 'damage': 1711, 'tier': 'IV'},  # ICBM IV and LuV quests
    'ordnance3': {'id': LOOTBAG, 'damage': 1712, 'tier': 'LuV'},  # ICBM LuV and ZPM quests
}
# The pack's own generic tier groups (config/EnhancedLootBags/LootBags.xml), which quests written before the GregGuns
# bags name by id and damage. Still tier-checked, but each use is a warning: write {"bag": name} instead.
PACK_BAGS = {
    (LOOTBAG, 2): 'Steam', (LOOTBAG, 4): 'LV', (LOOTBAG, 5): 'MV', (LOOTBAG, 6): 'HV', (LOOTBAG, 7): 'EV',
    (LOOTBAG, 8): 'IV', (LOOTBAG, 41): 'LuV', (LOOTBAG, 42): 'ZPM',
}
# Any bag stack written out by hand (id and damage) is tier-checked the same way.
BAG_STACKS = dict(PACK_BAGS)
BAG_STACKS.update({(b['id'], b['damage']): b['tier'] for b in BAGS.values()})
BAG_IDS = {b['id'] for b in BAGS.values()}
LINE_FOLDER = 'GregGuns'
LINE_FILE = '_line.json'
WELCOME = 'gg.start.welcome'
WILDCARD = 32767  # OreDictionary.WILDCARD_VALUE: "damage": "*" matches every damage value (the pack writes 32767)
# Pack item ids the data may name besides gregguns: ones (regenerate with pack_items.py).
PACK_ITEMS = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'pack_items.txt')
# The GregGuns loot bag groups that ship in the jar; every BAGS damage must be one of their GroupMetaIDs.
GROUPS_XML = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..', 'src', 'main', 'resources', 'assets',
                          'gregguns', 'lootbags', 'groups.xml')
GT_MACHINES = 'gregtech:gt.blockmachines'
SIZES = (24, 28, 48, 72)  # box sizes (GGS-12-15): 24 normal, 28 chapter head, 48 main quest or head, 72 milestone
# Fields the data may carry. Anything else is a typo; keys starting with '_' are writer notes and are ignored.
QUEST_FIELDS = {'key', 'chapter', 'title', 'tier', 'desc', 'pre', 'implicit', 'logic', 'taskLogic', 'tasks', 'rewards',
                'x', 'y', 'size', 'main', 'icon', 'visibility', 'skip', 'completes', 'repeatTime'}
TASK_FIELDS = {'checkbox': {'type'}, 'retrieval': {'type', 'items', 'any', 'matchNBT'},
               'optional_retrieval': {'type', 'items', 'any', 'matchNBT'}, 'crafting': {'type', 'items', 'matchNBT'},
               'hunt': {'type', 'target', 'count'}, 'fluid': {'type', 'fluids'},
               'location': {'type', 'dimension', 'name'}}
ITEM_TASKS = ('retrieval', 'optional_retrieval', 'crafting')
REWARD_FIELDS = {'item': {'type', 'items'}, 'choice': {'type', 'items'}, 'bag': {'type', 'tier', 'count'},
                 'xp': {'type', 'amount', 'levels'}}
STACK_FIELDS = {'id', 'count', 'damage', 'ore', 'tag', 'bag'}
# The description tags the pack uses (GGS-12-14): [note] and [warn] color a passage, [quest] marks a quest or tab
# name, [url] is a link. BetterQuesting's FormattingTag (3.8.89-GTNH) knows more; the pack's text uses these four.
# [questlink]<key>[/questlink] (or "<key> text") links to a quest; the builder writes the key's id in its place.
TEXT_TAGS = {'note', 'warn', 'quest', 'url', 'questlink'}
QUESTLINK = re.compile(r'\[questlink\]([^\s\[]+)')


def load_data(folder):
    """Read the data folder into one dict: the _line.json header plus every chapter's quests, each quest tagged
    with its chapter (the file's "chapter"), and "retired" gathered from _line.json and every chapter file.
    A "desc" given as a list of paragraphs becomes one string with a blank line between paragraphs.
    Files whose name starts with '_' are not chapters."""
    with open(os.path.join(folder, LINE_FILE), encoding='utf8') as fh:
        data = json.load(fh)
    data['quests'] = []
    data['retired'] = list(data.get('retired', []))
    for path in sorted(glob.glob(os.path.join(folder, '*.json'))):
        stem = os.path.basename(path)[:-5]
        if stem.startswith('_'):
            continue
        with open(path, encoding='utf8') as fh:
            chapter = json.load(fh)
        if chapter.get('chapter') != stem:
            raise ValueError('%s: "chapter" must be the file name, %s' % (path, stem))
        data['retired'] += chapter.get('retired', [])
        for q in chapter['quests']:
            if 'chapter' in q:
                raise ValueError('%s: %s carries its own "chapter"; the file sets it' % (path, q.get('key')))
            q = dict(q, chapter=stem)
            if isinstance(q.get('desc'), list):
                q['desc'] = '\n\n'.join(q['desc'])
            data['quests'].append(q)
    return data


def qid(ns, key):
    """Quest or line key -> (high, low, base64) the way BetterQuesting stores a UUID."""
    b = uuid.uuid5(ns, key).bytes
    hi, lo = struct.unpack('>qq', b)
    return hi, lo, base64.urlsafe_b64encode(b).decode()


def file_name(title, b64):
    plain = re.sub('§.', '', title)
    return re.sub('[^a-zA-Z0-9]', '', plain)[:16] + '-' + b64 + '.json'


def bag(name):
    """A bag name from the BAGS table -> its entry. KeyError names the valid ones."""
    entry = BAGS.get(str(name).lower())
    if entry is None:
        raise KeyError('unknown bag tier %r (BAGS: %s)' % (name, ', '.join(BAGS)))
    return entry


def resolve(s):
    """A data stack with the {"bag": name} shorthand -> a plain stack."""
    if 'bag' not in s:
        return s
    entry = bag(s['bag'])
    return {'id': entry['id'], 'damage': entry['damage'], 'count': s.get('count', 1)}


def stack(s):
    s = resolve(s)
    damage = s.get('damage', 0)
    out = {'Count:3': s.get('count', 1), 'Damage:2': WILDCARD if damage == '*' else damage,
           'OreDict:8': s.get('ore', ''), 'id:8': s['id']}
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
    if kind in ('retrieval', 'optional_retrieval'):
        # optional_retrieval lists items (recipe inputs, say) without gating the quest: TaskOptionalRetrieval.
        return {'autoConsume:1': 0, 'consume:1': 0, 'groupDetect:1': 0,
                'ignoreNBT:1': 0 if t.get('matchNBT') else 1, 'partialMatch:1': 1,
                'requireOnlyOneItem:1': int(bool(t.get('any'))), 'requiredItems:9': stacks(t['items']),
                'taskID:8': 'bq_standard:' + kind}
    if kind == 'fluid':
        # Fluid containers (cells, buckets) in the inventory, as the pack's TaskFluid quests write it.
        fluids = {'%d:10' % i: {'Amount:3': f['amount'], 'FluidName:8': f['name']} for i, f in enumerate(t['fluids'])}
        return {'autoConsume:1': 0, 'consume:1': 0, 'groupDetect:1': 0, 'ignoreNBT:1': 1,
                'requiredFluids:9': fluids, 'taskID:8': 'bq_standard:fluid'}
    if kind == 'location':
        # Dimension only (range -1), the one way the pack uses TaskLocation.
        return {'biome:3': -1, 'dimension:3': t['dimension'], 'hideInfo:1': 0, 'invert:1': 0,
                'name:8': t.get('name', ''), 'posX:3': 0, 'posY:3': 0, 'posZ:3': 0, 'range:3': -1, 'structure:8': '',
                'taskID:8': 'bq_standard:location', 'taxiCabDist:1': 0, 'visible:1': 0}
    if kind == 'crafting':
        # NBT is ignored unless matchNBT says otherwise, as for retrieval: an item whose tag varies (a payload, a
        # fill) still counts (ItemComparison.CompareNBTTag fails a tagless requirement against a tagged stack).
        return {'allowAnvil:1': 0, 'allowCraft:1': 1, 'allowCraftedFromStatistics:1': 0, 'allowSmelt:1': 1,
                'ignoreNBT:1': 0 if t.get('matchNBT') else 1, 'partialMatch:1': 1, 'requiredItems:9': stacks(t['items']),
                'taskID:8': 'bq_standard:crafting'}
    if kind == 'hunt':
        return {'damageType:8': '', 'ignoreNBT:1': 1, 'required:3': t['count'], 'subtypes:1': 0,
                'target:8': t['target'], 'targetNBT:10': {}, 'taskID:8': 'bq_standard:hunt'}
    raise ValueError('unknown task type ' + kind)


def reward(r):
    """One data reward -> one BetterQuesting reward. Keys follow bq_standard's RewardItem, RewardChoice, RewardXP."""
    kind = r['type']
    if kind == 'item':
        return {'ignoreDisabled:1': 0, 'rewardID:8': 'bq_standard:item', 'rewards:9': stacks(r['items'])}
    if kind == 'bag':
        return reward({'type': 'item', 'items': [{'bag': r['tier'], 'count': r.get('count', 1)}]})
    if kind == 'choice':
        return {'choices:9': stacks(r['items']), 'ignoreDisabled:1': 0, 'rewardID:8': 'bq_standard:choice'}
    if kind == 'xp':
        return {'amount:3': r['amount'], 'ignoreDisabled:1': 0, 'isLevels:1': int(r.get('levels', True)),
                'rewardID:8': 'bq_standard:xp'}
    raise ValueError('unknown reward type ' + kind)


def reward_stacks(r):
    """Every plain stack a data reward hands out (bags resolved), for the checks."""
    if r['type'] in ('item', 'choice'):
        return [resolve(s) for s in r['items']]
    if r['type'] == 'bag':
        return [resolve({'bag': r['tier'], 'count': r.get('count', 1)})]
    return []


def complete_reward(ns, i, key):
    """bq_standard:questcompletion, the reward GTNH's tier-skip triggers use to complete the quests they skip."""
    return {'ignoreDisabled:1': 1, 'index:3': i, 'isSilent:1': 0, 'questID:8': qid(ns, key)[2],
            'rewardID:8': 'bq_standard:questcompletion'}


def title_text(q):
    """The shown title: tier color and bold before it, unless the quest is a trigger or the title sets its own."""
    if q.get('skip') or q['title'].startswith('§'):
        return q['title']
    return TIER_COLORS[q['tier']] + '§l' + q['title']


def link_id(ns, ref):
    """A [questlink] target -> BetterQuesting's Base64 id: a GregGuns key, or a pack quest number."""
    if ref.isdigit():
        return base64.urlsafe_b64encode(struct.pack('>qq', 0, int(ref))).decode()
    return qid(ns, ref)[2]


def desc_text(ns, q):
    """The shown description: [questlink] keys replaced by the ids BetterQuesting looks up."""
    return QUESTLINK.sub(lambda m: '[questlink]' + link_id(ns, m.group(1)), q['desc'])


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
        'countAsQuest:1': 1, 'desc:8': desc_text(ns, q), 'globalShare:1': int(not skip), 'icon:10': stack(q['icon']),
        'isGlobal:1': 0, 'isMain:1': int(q.get('main', q.get('size', 24) > 24)), 'isSilent:1': int(skip), 'lockedProgress:1': 0,
        'name:8': title_text(q), 'notification_duration:5': -1.0, 'notification_effect:3': -1,
        'notification_fade_in:5': -1.0, 'notification_fade_out:5': -1.0,
        'notification_icon_offset_y:3': -2147483648, 'notification_icon_scale:5': -1.0,
        'notification_pos_x:3': -2147483648, 'notification_pos_y:3': -2147483648,
        'notification_show_icon:8': 'default', 'notification_style:8': 'default',
        'notification_subtitle:8': '', 'notification_subtitle_scale:5': -1.0, 'notification_title:8': '',
        'notification_title_scale:5': -1.0, 'particle_count:3': -1, 'partySingleReward:1': 0,
        'questLogic:8': q.get('logic', 'AND'), 'repeatTime:3': q.get('repeatTime', -1), 'repeat_relative:1': 1, 'simultaneous:1': 0,
        'snd_complete:8': 'random.levelup', 'snd_update:8': 'random.levelup',
        'taskLogic:8': q.get('taskLogic', 'AND'), 'visibility:8': q.get('visibility', 'NORMAL')}
    out = {'preRequisites:9': {'%d:10' % i: pre(p) for i, p in enumerate(q['pre'])},
           'properties:10': {'betterquesting:10': props},
           'questIDHigh:4': hi, 'questIDLow:4': lo, 'rewards:9': {}, 'tasks:9': {}}
    for i, t in enumerate(q['tasks']):
        out['tasks:9']['%d:10' % i] = dict(task(t), **{'index:3': i})
    for i, r in enumerate(q['rewards']):
        out['rewards:9']['%d:10' % i] = dict(reward(r), **{'index:3': i})
    for j, key in enumerate(q.get('completes', [])):
        i = len(q['rewards']) + j
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
    """Quest text -> a .lang value that BQ's translateToLocalFormatted returns unchanged ('%' doubled).
    A .lang value is one line, so a line break is written as %n, the way the pack's own quest lang files
    (config/txloader/load/betterquesting/lang/*.lang) write paragraph breaks (%n%n)."""
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
        rows.append('betterquesting.quest.%s.name=%s' % (b64.rstrip('='), lang_text(title_text(q))))
        rows.append('betterquesting.quest.%s.desc=%s' % (b64.rstrip('='), lang_text(desc_text(ns, q))))
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


def pack_items():
    with open(PACK_ITEMS, encoding='utf8') as fh:
        return {l.strip() for l in fh if l.strip() and not l.startswith('#')}


def _unknown(obj, allowed):
    return sorted(k for k in obj if k not in allowed and not k.startswith('_'))


def check_stack(k, where, s, known):
    """Problems with one data stack: shape, bag name, and a non-GregGuns id that the pack does not use.
    GT machine stacks are left to QuestbookResourcesTest, which knows the GregGuns MTE ids."""
    problems = []
    bad = _unknown(s, STACK_FIELDS)
    if bad:
        problems.append('%s: %s stack has unknown fields %s' % (k, where, bad))
    if 'bag' in s:
        try:
            bag(s['bag'])
        except KeyError as e:
            problems.append('%s: %s' % (k, e.args[0]))
        return problems
    if 'id' not in s:
        return problems + ['%s: %s stack has no id' % (k, where)]
    if s.get('damage', 0) != '*' and not isinstance(s.get('damage', 0), int):
        problems.append('%s: %s damage must be an integer or "*"' % (k, s['id']))
    if s['id'] != GT_MACHINES and not s['id'].startswith('gregguns:') and s['id'] not in known:
        problems.append('%s: %s is not in pack_items.txt' % (k, s['id']))
    return problems


def bag_group_problems(groups_xml=GROUPS_XML):
    """BAGS names a GroupMetaID that the shipped groups file does not define."""
    import xml.etree.ElementTree as ET
    ids = {int(g.get('GroupMetaID')) for g in ET.parse(groups_xml).getroot().iter('LootGroup')}
    return ['BAGS %s: damage %d is not a group in %s' % (n, b['damage'], os.path.basename(groups_xml))
            for n, b in sorted(BAGS.items()) if b['id'] == LOOTBAG and b['damage'] not in ids]


def check(data):
    """Build-time checks for the requirements that the file shape cannot show. Returns (errors, warnings)."""
    problems, warnings = [], []
    problems += bag_group_problems()
    known = pack_items()
    keys = {q['key'] for q in data['quests']}
    if len(keys) != len(data['quests']):
        problems.append('duplicate quest key')
    retired = data.get('retired', [])
    if len(set(retired)) != len(retired):
        problems.append('a retired key is listed twice')
    for k in sorted(keys & set(retired)):
        problems.append('%s: retired keys are never reused (GGS-12-02)' % k)
    for q in data['quests']:
        k = q['key']
        bad = _unknown(q, QUEST_FIELDS)
        if bad:
            problems.append('%s: unknown fields %s' % (k, bad))
        if q['tier'] not in TIERS:
            problems.append('%s: unknown tier %s' % (k, q['tier']))
            continue
        for c in q.get('completes', []):
            if c not in keys:
                problems.append('%s: completes unknown quest %s' % (k, c))
        for p in q['pre']:
            if isinstance(p, str) and p not in keys:
                problems.append('%s: unknown prerequisite %s' % (k, p))
        if not q['tasks']:
            problems.append('%s: no task' % k)
        if q.get('taskLogic', 'AND') not in ('AND', 'OR'):
            problems.append('%s: taskLogic must be AND or OR' % k)
        elif q.get('taskLogic') == 'OR' and len(q['tasks']) < 2:
            problems.append('%s: taskLogic OR needs at least two tasks' % k)
        elif q.get('taskLogic') == 'OR' and any(t.get('type') == 'optional_retrieval' for t in q['tasks']):
            # BetterQuesting counts an ignored (optional) task as done in its periodic update, so OR would complete
            # the quest by itself (QuestInstance.update, BQ 3.8.89-GTNH).
            problems.append('%s: taskLogic OR with an optional_retrieval completes by itself' % k)
        if all(t.get('type') == 'optional_retrieval' for t in q['tasks']) and q['tasks']:
            problems.append('%s: only optional tasks; nothing to complete' % k)
        if not q.get('skip') and q.get('size') not in SIZES:
            problems.append('%s: size %s is not one of %s (GGS-12-15)' % (k, q.get('size'), SIZES))
        if 'repeatTime' in q and not (isinstance(q['repeatTime'], int) and q['repeatTime'] > 0):
            problems.append('%s: repeatTime is a positive number of ticks' % k)
        problems += check_stack(k, 'icon', q['icon'], known)
        for t in q['tasks']:
            kind = t.get('type')
            if kind not in TASK_FIELDS:
                problems.append('%s: unknown task type %s' % (k, kind))
                continue
            bad = _unknown(t, TASK_FIELDS[kind])
            if bad:
                problems.append('%s: %s task has unknown fields %s' % (k, kind, bad))
            if kind == 'checkbox' and k != WELCOME:
                problems.append('%s: checkbox task; only %s may use one (GGS-12-11)' % (k, WELCOME))
            if kind == 'fluid' and not (t.get('fluids') and all(
                    isinstance(f.get('name'), str) and isinstance(f.get('amount'), int) and f['amount'] > 0
                    for f in t['fluids'])):
                problems.append('%s: a fluid task lists {"name", "amount"} entries' % k)
            if kind == 'location' and not isinstance(t.get('dimension'), int):
                problems.append('%s: a location task needs a dimension id' % k)
            if kind == 'hunt' and not (t['target'] == 'Zombie' or t['target'].startswith('gregguns.')):
                problems.append('%s: odd hunt target %s' % (k, t['target']))
            if kind in ITEM_TASKS:
                if not t.get('items'):
                    problems.append('%s: %s task with no items' % (k, kind))
                elif t.get('any') and len(t['items']) < 2:
                    problems.append('%s: "any" needs at least two items' % k)
                for s in t.get('items', []):
                    if 'bag' in s:
                        problems.append('%s: a task cannot ask for a bag' % k)
                    problems += check_stack(k, 'task', s, known)
        for r in q['rewards']:
            kind = r.get('type')
            if kind not in REWARD_FIELDS:
                problems.append('%s: unknown reward type %s' % (k, kind))
                continue
            bad = _unknown(r, REWARD_FIELDS[kind])
            if bad:
                problems.append('%s: %s reward has unknown fields %s' % (k, kind, bad))
            if kind == 'choice' and len(r.get('items', [])) < 2:
                problems.append('%s: a choice needs at least two items' % k)
            if kind == 'item' and not r.get('items'):
                problems.append('%s: item reward with no items' % k)
            if kind == 'xp' and not (isinstance(r.get('amount'), int) and r['amount'] > 0):
                problems.append('%s: xp amount must be a positive integer' % k)
            if kind == 'bag':
                problems += check_stack(k, 'bag', {'bag': r.get('tier')}, known)
                if not (isinstance(r.get('count', 1), int) and r.get('count', 1) > 0):
                    problems.append('%s: bag count must be a positive integer' % k)
                continue
            for s in r.get('items', []):
                problems += check_stack(k, 'reward', s, known)
        try:
            for s in (s for r in q['rewards'] if r.get('type') in REWARD_FIELDS for s in reward_stacks(r)):
                if s['id'] in BAG_IDS:
                    tier = BAG_STACKS.get((s['id'], s.get('damage', 0)))
                    if tier is None:
                        problems.append('%s: unknown bag %s damage %s' % (k, s['id'], s.get('damage')))
                    elif TIERS.index(tier) > TIERS.index(q['tier']):
                        problems.append('%s: bag above the quest tier (GGS-12-07)' % k)
                    if (s['id'], s.get('damage', 0)) in PACK_BAGS:
                        warnings.append('%s: pack tier bag damage %s; write {"bag": name} instead'
                                        % (k, s.get('damage')))
        except KeyError:
            pass  # an unknown bag name, reported above
        for tag in sorted(set(re.findall(r'\[/?([a-zA-Z_]+)\]', q['desc']))):
            if tag not in TEXT_TAGS:
                problems.append('%s: [%s] is not one of the pack tags %s (GGS-12-14)' % (k, tag, sorted(TEXT_TAGS)))
        for ref in QUESTLINK.findall(q['desc']):
            if not ref.isdigit() and ref not in keys:
                problems.append('%s: [questlink] to unknown quest key %s' % (k, ref))
        if q.get('visibility') == 'SECRET' and not (q['chapter'] == 'npc' and k == 'gg.npc.dave'):
            problems.append('%s: SECRET outside the Dictator Dave quest (GGS-12-13)' % k)
        if q.get('logic', 'AND') not in ('AND', 'OR'):
            problems.append('%s: logic must be AND or OR' % k)
    return problems, warnings


def build(data_path, out_dir, lang_path=None):
    data = load_data(data_path)
    problems, warnings = check(data)
    for w in warnings:
        if w not in boxes:
            print('WARNING', w, file=sys.stderr)
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
    # retired: the ids the installer deletes from an existing database (quest and line entry), as UUID strings.
    retired = sorted(str(uuid.uuid5(ns, k)) for k in data.get('retired', []))
    index = {'format': 1, 'line': line_path, 'linePositions': positions, 'quests': quests,
             'orderEntry': '%s: %s' % (line_b64, data['line']['name']), 'retired': retired}
    files['index.json'] = json.dumps(index, indent=2, sort_keys=True) + '\n'
    for rel, text in files.items():
        path = os.path.join(out_dir, *rel.split('/'))
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, 'w', encoding='utf8', newline='\n') as fh:
            fh.write(text)
    if lang_path:
        write_lang(lang_path, lang_block(ns, data))
    print('wrote %d files (%d quests, %d retired) to %s' % (len(files), len(quests), len(retired), out_dir))
    return 0


if __name__ == '__main__':
    here = os.path.dirname(os.path.abspath(__file__))
    ap = argparse.ArgumentParser(description=__doc__.split('\n')[0])
    ap.add_argument('--out', required=True, help='questbook resource root')
    ap.add_argument('--data', default=os.path.join(here, 'quests'), help='quest data folder')
    ap.add_argument('--lang', help='en_US.lang to refresh the generated betterquesting.* block in')
    args = ap.parse_args()
    sys.exit(build(args.data, args.out, args.lang))
