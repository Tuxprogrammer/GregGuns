package com.gregguns.entity.npc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;

import net.minecraft.entity.EntityLiving;
import net.minecraft.item.ItemStack;

import com.gregguns.entity.npc.loot.LootDrop;
import com.gregguns.entity.npc.loot.LootTable;
import com.gregguns.entity.npc.loot.NpcProfile;
import com.gregguns.gun.logic.GunCatalog;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.item.gun.GunItems;
import com.gregguns.worldgen.loot.LootCategories;
import com.gregguns.worldgen.loot.ObtainTier;

/**
 * Builds an NPC's drops from its {@link NpcProfile}. {@link #dropFewItems} is called from the {@code dropFewItems}
 * override of {@link NpcEntity}; the drops tied to the carried gun ({@link #rareGunChance}, {@link #gunDrops}) are
 * added by {@link NpcGunDropsHandler}.
 *
 * <p>
 * Simulation-friendly (Mobs Info replaces the entity's {@code rand} and walks every branch): every draw uses the
 * entity's own {@link Random}, nothing reads world state, and the number of draws does not depend on whether an item
 * resolves. Fixed drops fall whatever killed the mob; table rolls are {@link LootTable#rolls}. References resolve as
 * structure loot does ({@link LootCategories#stack}: {@code gt:} parts, {@code ~full} tanks). Server side.
 *
 * <p>
 * The carried gun (D312): it never drops through vanilla's equipment roll. On a player kill it drops as a fresh copy
 * (count 1, no NBT) at {@link #rareGunChance}, for profiles that allow it. Every death that drops loot also drops what
 * the gun fires ({@link #gunDrops}): a quarter to three quarters of a magazine as loose ammo items, one item a
 * round as the player counts them ({@link #magazineUnits}, D349), a full tank, or for an energy gun a few parts of its
 * recipe ({@link GunSalvage}).
 */
// Spec: docs/techguns-spec/10-entities-npcs/per-mob.md (Drops), docs/gregguns-spec/10-entities-npcs/mob-drops-nei.md
// (GGS-10-15, GGS-10-16, GGS-10-19, GGS-10-38 to GGS-10-40), BUGS NPC-04, NPC-05
public final class NpcDrops {

    private NpcDrops() {}

    /**
     * Drops the fixed items and the table rolls.
     *
     * @param npc     the dying NPC
     * @param profile its profile
     * @param looting looting level of the kill
     */
    public static void dropFewItems(EntityLiving npc, NpcProfile profile, int looting) {
        Random rand = npc.getRNG();
        for (LootDrop fixed : profile.fixedDrops()) {
            drop(npc, LootCategories.stack(fixed.ref(), fixed.count(rand)));
        }
        LootTable table = profile.table();
        if (table == null) {
            return;
        }
        int rolls = LootTable.rolls(rand, looting);
        for (int i = 0; i < rolls; i++) {
            LootDrop pick = table.pick(rand);
            if (pick != null) {
                drop(npc, LootCategories.stack(pick.ref(), pick.count(rand)));
            }
        }
    }

    /** Rare gun drop chance with no Looting. */
    public static final double RARE_GUN_BASE = 0.02;
    /** Rare gun drop chance added per Looting level. */
    public static final double RARE_GUN_PER_LOOTING = 0.01;
    /** Fewest loose ammo items an NPC's death drops, as a share of the gun's magazine (D312, D349). */
    public static final double AMMO_MIN_SHARE = 0.25;
    /** Most loose ammo items an NPC's death drops, as a share of the gun's magazine (D312, D349). */
    public static final double AMMO_MAX_SHARE = 0.75;
    /** Most loose ammo items one death drops: one stack. */
    public static final int MAX_AMMO_ITEMS = 64;
    /**
     * Loose ammo items that are each several shots by design rather than one round (the Nether Charge: ten Nether
     * Blaster shots). Their drop is a share of the items one full magazine takes, not of its rounds (D349).
     */
    static final Set<String> MULTI_SHOT_ITEMS = Collections.singleton(GunCatalog.AmmoIds.NETHER_CHARGE);
    /** The suffix that makes a tank reference resolve full (the loot core's own rule). */
    public static final String FULL_TANK = "~full";

    /**
     * @param looting the kill's Looting level
     * @return the chance a player kill drops a copy of the carried gun: 2% plus 1% per Looting level
     */
    public static double rareGunChance(int looting) {
        return RARE_GUN_BASE + RARE_GUN_PER_LOOTING * Math.max(0, looting);
    }

    /** How a carried gun's own drop is made. */
    public enum GunDropKind {
        /** Loose ammo items: {@link #AMMO_MIN_SHARE} to {@link #AMMO_MAX_SHARE} of a magazine, one item a round. */
        LOOSE,
        /** One full tank (flame and bio guns, the chainsaw). */
        TANK,
        /** An energy gun: parts of its recipe ({@link GunSalvage}). */
        SALVAGE
    }

    /** The drop rule of one carried gun. */
    public static final class GunDrop {

        private final GunDropKind kind;
        private final String ref;
        private final int lo;
        private final int hi;
        private final List<GunSalvage.Entry> salvage;

        private GunDrop(GunDropKind kind, String ref, int lo, int hi, List<GunSalvage.Entry> salvage) {
            this.kind = kind;
            this.ref = ref;
            this.lo = lo;
            this.hi = hi;
            this.salvage = salvage;
        }

        /** @return how the drop is made */
        public GunDropKind kind() {
            return kind;
        }

        /** @return the ammo item reference (a {@code ~full} tank for {@link GunDropKind#TANK}); null for salvage */
        public String ref() {
            return ref;
        }

        /** @return the salvage pool; empty unless {@link GunDropKind#SALVAGE} */
        public List<GunSalvage.Entry> salvage() {
            return salvage;
        }

        /** @return fewest ammo items one death drops (0 for salvage) */
        public int minItems() {
            return lo;
        }

        /** @return most ammo items one death drops (0 for salvage) */
        public int maxItems() {
            return hi;
        }

        /** @return the average ammo items one death drops (0 for salvage): every count in range is equally likely */
        public double expectedItems() {
            return (lo + hi) / 2.0;
        }

        /**
         * Rolls one death's drop.
         *
         * @param rand the NPC's random source
         * @return the item references and counts
         */
        public List<GunSalvage.Pick> roll(Random rand) {
            if (kind == GunDropKind.SALVAGE) {
                return GunSalvage.roll(salvage, rand);
            }
            return Collections.singletonList(new GunSalvage.Pick(ref, lo + rand.nextInt(hi - lo + 1)));
        }
    }

    /**
     * @param gunName the carried gun's registry name
     * @param stats   its stats
     * @return what its death drop is made of, or null for a gun with nothing to drop
     */
    public static GunDrop gunDrop(String gunName, GunStats stats) {
        if (stats.isEnergy()) {
            List<GunSalvage.Entry> pool = GunSalvage.pool(gunName);
            return pool.isEmpty() ? null : new GunDrop(GunDropKind.SALVAGE, null, 0, 0, pool);
        }
        String ammo = stats.looseAmmoType();
        if (ammo == null || stats.magazineSize() < 1) {
            return null;
        }
        if (ammo.startsWith("tank_")) {
            // A partly filled tank is not ammo (FluidAmmo#isFullTank): the smallest unit is one full tank.
            return new GunDrop(GunDropKind.TANK, "gregguns:" + ammo + FULL_TANK, 1, 1, Collections.emptyList());
        }
        int units = magazineUnits(stats);
        int lo = Math.min(MAX_AMMO_ITEMS, Math.max(1, (int) Math.ceil(AMMO_MIN_SHARE * units)));
        int hi = Math.min(MAX_AMMO_ITEMS, Math.max(lo, (int) Math.floor(AMMO_MAX_SHARE * units)));
        return new GunDrop(GunDropKind.LOOSE, "gregguns:" + ammo, lo, hi, Collections.emptyList());
    }

    /**
     * The size of a full magazine in the unit the death drop counts (D349): its rounds, since the player reads one
     * loose bullet, shell or rocket as one round, whatever a magazine refill or the forced-reload refund
     * ({@link GunItems#roundsPerLooseItem}) makes of it; for a {@link #MULTI_SHOT_ITEMS} item, the loose items one full
     * magazine takes.
     *
     * @param stats the gun
     * @return the magazine in drop units, at least 1
     */
    static int magazineUnits(GunStats stats) {
        int mag = stats.magazineSize();
        if (!MULTI_SHOT_ITEMS.contains(stats.looseAmmoType())) {
            return Math.max(1, mag);
        }
        int perItem = GunItems.roundsPerLooseItem(stats);
        return Math.max(1, (mag + perItem - 1) / perItem);
    }

    /**
     * Rolls the drop of a carried gun.
     *
     * @param gunName the gun's registry name
     * @param stats   its stats
     * @param rand    the NPC's random source
     * @return item references and counts; empty for a gun with nothing to drop
     */
    public static List<GunSalvage.Pick> gunDrops(String gunName, GunStats stats, Random rand) {
        GunDrop rule = gunDrop(gunName, stats);
        return rule == null ? new ArrayList<>() : rule.roll(rand);
    }

    private static void drop(EntityLiving npc, ItemStack stack) {
        // Nothing above loot.maxTier by its whole crafting chain, nothing of unknown tier (D364).
        if (stack != null && stack.stackSize > 0 && ObtainTier.allowed(stack)) {
            npc.entityDropItem(stack, 0.0F);
        }
    }
}
