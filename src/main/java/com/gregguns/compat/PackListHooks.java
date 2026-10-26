package com.gregguns.compat;

import java.util.Collections;
import java.util.List;

import com.gregguns.GregGuns;

/**
 * Two more in-memory list hooks, built like {@link BackhandCompat}: append GregGuns entries to a public static
 * {@code String[]} of another mod's loaded config, never to its file.
 *
 * <ul>
 * <li><b>AngerMod</b> {@code KamikazeConfig.butcherItems}: a player holding an item whose lowercase unlocalized name
 * contains an entry does not trigger the kamikaze explosion. Our unlocalized names are {@code item.gregguns.*}, so
 * {@code gregguns.} covers every GregGuns tool and gun.</li>
 * <li><b>Mobs Info</b> {@code Config.MobHandler.mobBlacklist}: exact {@code EntityList} names skipped when building
 * the mob drop recipes. {@code gregguns.SimpleTurret} has no drops and would list as an empty mob.</li>
 * </ul>
 */
// Source:
// https://github.com/GTNewHorizons/AngerMod/blob/1.0.7/src/main/java/com/github/namikon/angermod/events/KamikazeRevenge.java#L26-L35
// (itemName.contains(s.toLowerCase()) over KamikazeConfig.butcherItems) and
// https://github.com/GTNewHorizons/AngerMod/blob/1.0.7/src/main/java/com/github/namikon/angermod/config/KamikazeConfig.java#L24-L26
// https://github.com/GTNewHorizons/Mobs-Info/blob/0.6.0-GTNH/src/main/java/com/kuba6000/mobsinfo/config/Config.java#L127-L139
// (public static String[] mobBlacklist) and docs/mob-blacklist.md there (exact entity registry names).
// Spec: docs/gregguns-spec/PACK-CHANGES.md
public final class PackListHooks {

    /** AngerMod's mod id. */
    public static final String ANGERMOD = "angermod";
    /** Mobs Info's mod id. */
    public static final String MOBSINFO = "mobsinfo";

    private PackListHooks() {}

    /** @return true if AngerMod's list was extended */
    public static boolean angerModButcherItems() {
        return run(
            "com.github.namikon.angermod.config.KamikazeConfig",
            "butcherItems",
            Collections.singletonList("gregguns."));
    }

    /** @return true if Mobs Info's blacklist was extended */
    public static boolean mobsInfoBlacklist() {
        return run(
            "com.kuba6000.mobsinfo.config.Config$MobHandler",
            "mobBlacklist",
            Collections.singletonList(com.gregguns.entity.npc.NpcMob.SIMPLE_TURRET.fullName()));
    }

    private static boolean run(String className, String field, List<String> add) {
        try {
            boolean ok = BackhandCompat.extend(Class.forName(className), field, add);
            if (ok) {
                GregGuns.LOG.info("{}.{}: GregGuns entries added (in memory)", className, field);
            }
            return ok;
        } catch (ReflectiveOperationException | RuntimeException e) {
            GregGuns.LOG.warn("{}.{} could not be extended; see PACK-CHANGES.md", className, field, e);
            return false;
        }
    }
}
