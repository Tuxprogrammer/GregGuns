package com.gregguns.entity.npc;

import java.lang.reflect.Method;

import net.minecraft.entity.EntityLivingBase;

import com.gregguns.GregGuns;
import com.gregguns.core.config.NpcConfig;
import com.gregguns.entity.npc.loot.InfernalOptOut;

import cpw.mods.fml.common.Loader;

/**
 * The soft link to Infernal Mobs. Common code never names an Infernal Mobs class: this class reaches the three calls
 * it needs by reflection and does nothing when the mod is absent or its API has changed (logged once). Server side.
 */
// Spec: docs/gregguns-spec/10-entities-npcs/infernal-mobs.md (GGS-10-10, GGS-10-13)
public final class InfernalHooks {

    /** The Infernal Mobs mod id. */
    public static final String MOD_ID = "InfernalMobs";

    private static final String CORE = "atomicstryker.infernalmobs.common.InfernalMobsCore";
    private static final String MODIFIER = "atomicstryker.infernalmobs.common.modifiers.MobModifier";

    private static boolean resolved;
    private static Method getMobModifiers;
    private static Method getModSize;
    private static String nbtKey;

    private InfernalHooks() {}

    /** @return true when Infernal Mobs is loaded and its API resolved */
    public static boolean available() {
        if (!Loader.isModLoaded(MOD_ID)) {
            return false;
        }
        if (!resolved) {
            resolved = true;
            try {
                // Source:
                // https://github.com/GTNewHorizons/Infernal-Mobs/blob/1.10.7-GTNH/src/main/java/atomicstryker/infernalmobs/common/InfernalMobsCore.java#L121-L127
                // Source:
                // https://github.com/GTNewHorizons/Infernal-Mobs/blob/1.10.7-GTNH/src/main/java/atomicstryker/infernalmobs/common/InfernalMobsCore.java#L621-L624
                Class<?> core = Class.forName(CORE);
                getMobModifiers = core.getMethod("getMobModifiers", EntityLivingBase.class);
                getModSize = Class.forName(MODIFIER)
                    .getMethod("getModSize");
                nbtKey = (String) core.getMethod("getNBTTag")
                    .invoke(null);
            } catch (ReflectiveOperationException | RuntimeException e) {
                GregGuns.LOG.warn("Infernal Mobs is loaded but its API did not resolve; infernal hooks are off", e);
                getMobModifiers = null;
            }
        }
        return getMobModifiers != null;
    }

    /**
     * @param entity a living entity
     * @return its Infernal Mobs modifier count, or 0 when it is not an infernal mob (or the API is unavailable)
     */
    public static int modifierSize(EntityLivingBase entity) {
        if (!available()) {
            return 0;
        }
        try {
            Object mod = getMobModifiers.invoke(null, entity);
            return mod == null ? 0 : (Integer) getModSize.invoke(mod);
        } catch (ReflectiveOperationException | RuntimeException e) {
            GregGuns.LOG.warn("Infernal Mobs modifier lookup failed", e);
            return 0;
        }
    }

    /**
     * Marks an NPC placed by a spawner block so Infernal Mobs does not roll it, unless the config allows spawner NPCs.
     * Call before the entity joins the world. Writes one key of the entity's persistent data and never replaces the
     * compound (GGS-10-14).
     *
     * @param npc the NPC about to be spawned by a spawner block
     * @return true when the marker was written
     */
    public static boolean markSpawnerNpc(EntityLivingBase npc) {
        if (!InfernalOptOut.shouldOptOut(true, NpcConfig.infernalAllowSpawnerNpcs) || !available()) {
            return false;
        }
        // Source:
        // https://github.com/GTNewHorizons/Infernal-Mobs/blob/1.10.7-GTNH/src/main/java/atomicstryker/infernalmobs/common/events/EntityEventHandler.java#L69-L82
        // A non-empty saved string skips processEntitySpawn; an unparseable one adds no modifiers (core L582-L595).
        npc.getEntityData()
            .setString(nbtKey, InfernalOptOut.MARKER);
        return true;
    }
}
