package com.gregguns.entity.npc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import net.minecraft.entity.EntityLiving;

import com.gregguns.core.DevEnvironment;
import com.gregguns.core.registry.ModuleRegistrar;
import com.gregguns.entity.npc.mob.CyberDemon;

/**
 * The one place NPC entities and their spawn egg are registered. Each mob batch adds one line to
 * {@link #declareMobs()} binding its {@link NpcMob} to its class; {@link #register} then registers every bound mob
 * under {@code gregguns.<Name>} with its reserved ID, plus the dev-only test NPC, plus the {@code npc_egg} item.
 *
 * <p>
 * Usage (a batch adding a mob): in {@code declareMobs()}, {@code bind(NpcMob.BANDIT, Bandit.class);}, and in
 * {@code NpcClientModule.init()}, {@code MobRenderers.biped(Bandit.class, "biped", "textures/entity/bandit.png", ...)}.
 * The egg, the lang name ({@code entity.gregguns.Bandit.name}) and the creative listing follow from the binding.
 * Common side, preInit only.
 */
// Spec: docs/techguns-spec/10-entities-npcs/mob-catalog.md, docs/gregguns-spec/10-entities-npcs/index.md
public final class NpcRegistry {

    /** Registry name of the spawn egg item. */
    public static final String EGG_NAME = "npc_egg";

    /** Tracking range of every NPC (mob-catalog.md). */
    private static final int TRACKING_RANGE = 80;
    /** Ticks between position updates of every NPC (mob-catalog.md). */
    private static final int UPDATE_FREQUENCY = 3;

    private static final Map<NpcMob, Class<? extends EntityLiving>> BOUND = new EnumMap<>(NpcMob.class);
    private static final List<NpcMob> REGISTERED = new ArrayList<>();
    private static ItemNpcEgg egg;

    private NpcRegistry() {}

    /** The mobs implemented so far; each batch appends one {@link #bind} line. */
    private static void declareMobs() {
        // Batches B1 onward add lines here, for example: bind(NpcMob.BANDIT, Bandit.class);
        bind(NpcMob.PSYCHO_STEVE, PsychoSteve.class);
        bind(NpcMob.DICTATOR_DAVE, DictatorDave.class);
        bind(NpcMob.SUPER_MUTANT_BASIC, SuperMutantBasic.class);
        bind(NpcMob.SUPER_MUTANT_HEAVY, SuperMutantHeavy.class);
        bind(NpcMob.SUPER_MUTANT_ELITE, SuperMutantElite.class);
        bind(NpcMob.CYBER_DEMON, CyberDemon.class);
        if (DevEnvironment.isActive()) {
            // The base NPC, for scenarios and the test renderer only.
            bind(NpcMob.NPC_TEST, NpcEntity.class);
        }
    }

    private static void bind(NpcMob mob, Class<? extends EntityLiving> clazz) {
        BOUND.put(mob, clazz);
    }

    /**
     * Registers every bound mob and the spawn egg.
     *
     * @param registrar the npcs module's registrar
     */
    public static void register(ModuleRegistrar registrar) {
        declareMobs();
        for (Map.Entry<NpcMob, Class<? extends EntityLiving>> e : BOUND.entrySet()) {
            NpcMob mob = e.getKey();
            registrar.registerMob(
                mob.registryName(),
                e.getValue(),
                mob.entityId(),
                TRACKING_RANGE,
                UPDATE_FREQUENCY,
                mob == NpcMob.NPC_TEST);
            REGISTERED.add(mob);
        }
        egg = registrar.registerItem(EGG_NAME, new ItemNpcEgg());
        // The egg draws the shared spawn-egg textures, not textures/items/npc_egg.png.
        egg.setTextureName(ItemNpcEgg.ICON_BASE);
    }

    /**
     * @param mob a kind
     * @return true if its class is bound and registered
     */
    public static boolean isRegistered(NpcMob mob) {
        return REGISTERED.contains(mob);
    }

    /**
     * @return the registered kinds that have a spawn egg, in ID order
     */
    public static List<NpcMob> eggMobs() {
        List<NpcMob> out = new ArrayList<>();
        for (NpcMob mob : NpcMob.values()) {
            if (mob.hasEgg() && isRegistered(mob)) {
                out.add(mob);
            }
        }
        return Collections.unmodifiableList(out);
    }

    /**
     * @return the registered kinds, in registration order
     */
    public static List<NpcMob> registered() {
        return Collections.unmodifiableList(REGISTERED);
    }

    /**
     * @return the spawn egg item, or null before registration
     */
    public static ItemNpcEgg egg() {
        return egg;
    }
}
