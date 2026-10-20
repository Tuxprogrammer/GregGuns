package com.gregguns.worldgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

/**
 * Reads and writes a {@link PlanLedger} as NBT. Tolerant on read, because saved data outlives code: unknown tags are
 * ignored, an entry with no key, no decision flag, or a placed entry with no ground Y is skipped (an unrecorded plan is
 * decided afresh, which is safer than guessing a Y), and a flag array is cut to whole triples.
 */
// Spec: docs/gregguns-spec/11-worldgen/structure-placement.md (GGS-11-10); PLAN-M8 Q3
public final class StateNbt {

    static final String VERSION = "Version";
    static final String PLANS = "Plans";
    static final String KEY = "Key";
    static final String PLACED = "Placed";
    static final String REASON = "Reason";
    static final String GROUND_Y = "GroundY";
    static final String FLAGS = "Flags";

    static final int CURRENT_VERSION = 2;
    // Version 1 keys had no structure id ("SIZE:x:z"). None shipped; they still load as opaque strings and never
    // match a v2 key, so those plans are decided afresh.

    private static final int TAG_BYTE = 1;
    private static final int TAG_INT = 3;
    private static final int TAG_STRING = 8;
    private static final int TAG_LIST = 9;
    private static final int TAG_COMPOUND = 10;
    private static final int TAG_INT_ARRAY = 11;

    private StateNbt() {}

    public static void write(PlanLedger ledger, NBTTagCompound out) {
        out.setInteger(VERSION, CURRENT_VERSION);
        NBTTagList list = new NBTTagList();
        for (Map.Entry<String, Decision> e : ledger.all()
            .entrySet()) {
            Decision d = e.getValue();
            NBTTagCompound t = new NBTTagCompound();
            t.setString(KEY, e.getKey());
            t.setBoolean(PLACED, d.isPlaced());
            if (d.isPlaced()) {
                t.setInteger(GROUND_Y, d.groundY());
                int[] flat = new int[d.flags()
                    .size() * 3];
                int i = 0;
                for (FlagPos f : d.flags()) {
                    flat[i++] = f.x;
                    flat[i++] = f.y;
                    flat[i++] = f.z;
                }
                t.setIntArray(FLAGS, flat);
            } else {
                t.setString(REASON, d.reason());
            }
            list.appendTag(t);
        }
        out.setTag(PLANS, list);
    }

    /** Reads into a fresh ledger. Never throws on missing or foreign tags. */
    public static PlanLedger read(NBTTagCompound in) {
        PlanLedger ledger = new PlanLedger();
        if (in == null || !in.hasKey(PLANS, TAG_LIST)) {
            return ledger;
        }
        NBTTagList list = in.getTagList(PLANS, TAG_COMPOUND);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound t = list.getCompoundTagAt(i);
            if (!t.hasKey(KEY, TAG_STRING) || !t.hasKey(PLACED, TAG_BYTE)) {
                continue;
            }
            String key = t.getString(KEY);
            if (key.isEmpty()) {
                continue;
            }
            Decision d;
            if (t.getBoolean(PLACED)) {
                if (!t.hasKey(GROUND_Y, TAG_INT)) {
                    continue;
                }
                List<FlagPos> flags = new ArrayList<FlagPos>();
                if (t.hasKey(FLAGS, TAG_INT_ARRAY)) {
                    int[] flat = t.getIntArray(FLAGS);
                    for (int j = 0; j + 2 < flat.length; j += 3) {
                        flags.add(new FlagPos(flat[j], flat[j + 1], flat[j + 2]));
                    }
                }
                d = Decision.placed(t.getInteger(GROUND_Y), flags);
            } else {
                d = Decision.rejected(t.hasKey(REASON, TAG_STRING) ? t.getString(REASON) : null);
            }
            ledger.restore(key, d);
        }
        return ledger;
    }
}
