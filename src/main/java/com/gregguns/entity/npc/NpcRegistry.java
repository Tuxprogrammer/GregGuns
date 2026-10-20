package com.gregguns.entity.npc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.entity.EntityLiving;

import com.gregguns.core.DevEnvironment;
import com.gregguns.core.registry.ModuleRegistrar;
import com.gregguns.entity.npc.mob.ArmySoldier;
import com.gregguns.entity.npc.mob.Commando;
import com.gregguns.entity.npc.mob.CyberDemon;
import com.gregguns.entity.npc.mob.SkeletonSoldier;
import com.gregguns.entity.npc.mob.ZombieFarmer;
import com.gregguns.entity.npc.mob.ZombieMiner;
import com.gregguns.entity.npc.mob.ZombiePigmanSoldier;
import com.gregguns.entity.npc.mob.ZombieSoldier;

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

    /** Mobs that deliberately have no spawn egg: the camp turret is placed by the turret base only. */
    public static final Set<NpcMob> EGG_EXEMPT = Collections.unmodifiableSet(EnumSet.of(NpcMob.SIMPLE_TURRET));

    /**
     * Catalog mobs that have an ID and egg colors but no entity class yet. This is a recorded M7 gap (see the M7 exit
     * gate page), not an exemption: {@link #eggAudit(boolean)} fails when a mob listed here becomes bound, so the entry
     * must be removed in the same change.
     */
    public static final Set<NpcMob> NOT_YET_IMPLEMENTED = Collections
        .unmodifiableSet(EnumSet.of(NpcMob.OUTCAST, NpcMob.STORMTROOPER));

    /**
     * Checks that every catalog mob has a bound class, egg colors and (when {@code live}) a registered entity and an
     * egg subtype. The dev-only test NPC and {@link #EGG_EXEMPT} are skipped; {@link #NOT_YET_IMPLEMENTED} mobs must
     * stay unbound.
     *
     * @param live true to also check registration and the creative egg listing (after {@link #register})
     * @return one message naming each failing mob; empty when all is well
     */
    public static List<String> eggAudit(boolean live) {
        List<String> problems = new ArrayList<>();
        for (NpcMob mob : NpcMob.values()) {
            if (mob.isDevOnly()) {
                continue;
            }
            if (EGG_EXEMPT.contains(mob)) {
                if (mob.hasEgg()) {
                    problems.add(mob + ": exempt from eggs but has egg colors");
                }
                continue;
            }
            if (NOT_YET_IMPLEMENTED.contains(mob)) {
                if (BOUND.containsKey(mob)) {
                    problems.add(mob + ": now bound, remove it from NOT_YET_IMPLEMENTED");
                }
                continue;
            }
            if (!BOUND.containsKey(mob)) {
                problems.add(mob + ": no bound entity class");
            }
            if (!mob.hasEgg()) {
                problems.add(mob + ": no egg colors");
            }
            if (NpcMob.byId(mob.entityId()) != mob) {
                problems.add(mob + ": egg subtype (item damage " + mob.entityId() + ") does not resolve to it");
            }
            if (live && !REGISTERED.contains(mob)) {
                problems.add(mob + ": entity not registered");
            }
            if (live && !eggMobs().contains(mob)) {
                problems.add(mob + ": not offered as an egg subtype");
            }
        }
        return problems;
    }

    /** The mobs implemented so far; each batch appends one {@link #bind} line. */
    static void declareMobs() {
        // Batches B1 onward add lines here, for example: bind(NpcMob.BANDIT, Bandit.class);
        bind(NpcMob.PSYCHO_STEVE, PsychoSteve.class);
        bind(NpcMob.DICTATOR_DAVE, DictatorDave.class);
        bind(NpcMob.SUPER_MUTANT_BASIC, SuperMutantBasic.class);
        bind(NpcMob.SUPER_MUTANT_HEAVY, SuperMutantHeavy.class);
        bind(NpcMob.SUPER_MUTANT_ELITE, SuperMutantElite.class);
        bind(NpcMob.CYBER_DEMON, CyberDemon.class);
        bind(NpcMob.ATTACK_HELICOPTER, AttackHelicopter.class);
        bind(NpcMob.ALIEN_BUG, AlienBug.class);
        bind(NpcMob.SIMPLE_TURRET, NpcTurret.class);
        bind(NpcMob.ARMY_SOLDIER, ArmySoldier.class);
        bind(NpcMob.COMMANDO, Commando.class);
        bind(NpcMob.ZOMBIE_SOLDIER, ZombieSoldier.class);
        bind(NpcMob.ZOMBIE_FARMER, ZombieFarmer.class);
        bind(NpcMob.ZOMBIE_MINER, ZombieMiner.class);
        bind(NpcMob.BANDIT, Bandit.class);
        bind(NpcMob.SKELETON_SOLDIER, SkeletonSoldier.class);
        bind(NpcMob.ZOMBIE_PIGMAN_SOLDIER, ZombiePigmanSoldier.class);
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
     * @param mob a kind
     * @return its bound class, or null if no batch has bound it yet
     */
    public static Class<? extends EntityLiving> boundClass(NpcMob mob) {
        return BOUND.get(mob);
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
