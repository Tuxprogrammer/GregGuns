package com.gregguns.entity.npc.loot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.gregguns.api.Tier;

/**
 * The GregGuns items an infernal NPC adds to its drops, and the pure roll that picks them (GGS-10-10 to GGS-10-12).
 * Every entry carries its D1 tier. A roll takes the entries at or under the ceiling that {@link InfernalBands} gives
 * for the mob's modifier count, capped by the NPC's own tier ceiling. The infernal band's rare roll (one in
 * {@link #RARE_ONE_IN}) draws only from entries at exactly its HV ceiling when any fit. Ammunition and magazines
 * only, apart from one MV gun; the references are resolved later by {@code NpcItems}, which skips unknown ones.
 * Pure: no Minecraft types, the caller's {@link Random} is the only source of chance, and the draw count per roll is
 * fixed.
 */
// Spec: docs/gregguns-spec/10-entities-npcs/infernal-mobs.md (GGS-10-10 to GGS-10-12),
// docs/gregguns-spec/06-items/pack-loot.md (Infernal Mobs handler), docs/design/0007 (decision 2)
public final class InfernalLootTable {

    /** One in this many infernal-band rolls is the rare (HV) roll. */
    public static final int RARE_ONE_IN = 8;

    private static final String G = "gregguns:";
    private static final int PICK_RANGE = 1 << 16;

    private static final List<Entry> ENTRIES = Collections.unmodifiableList(
        Arrays.asList(
            new Entry(LootDrop.range(G + "ammo_9mm_bullet", 4, 8), Tier.LV),
            new Entry(LootDrop.range(G + "ammo_shotgun_shell", 4, 8), Tier.LV),
            new Entry(LootDrop.range(G + "ammo_rifle_bullet", 2, 6), Tier.LV),
            new Entry(LootDrop.one(G + "mag_9mm_pistol"), Tier.LV),
            new Entry(LootDrop.one(G + "medkit"), Tier.LV),
            new Entry(LootDrop.one(G + "mag_smg"), Tier.MV),
            new Entry(LootDrop.one(G + "mag_assault_rifle"), Tier.MV),
            new Entry(LootDrop.range(G + "ammo_40mm_grenade", 1, 3), Tier.MV),
            new Entry(LootDrop.one(G + "thompson_smg"), Tier.MV),
            new Entry(LootDrop.one(G + "mag_lmg"), Tier.HV),
            new Entry(LootDrop.one(G + "mag_advanced"), Tier.HV)));

    private InfernalLootTable() {}

    /** A table entry: a drop and the tier of the item. */
    public static final class Entry {

        private final LootDrop drop;
        private final Tier tier;

        Entry(LootDrop drop, Tier tier) {
            this.drop = drop;
            this.tier = tier;
        }

        /** @return the drop */
        public LootDrop drop() {
            return drop;
        }

        /** @return the item's tier */
        public Tier tier() {
            return tier;
        }
    }

    /** @return every entry, in order */
    public static List<Entry> entries() {
        return ENTRIES;
    }

    /**
     * Rolls an infernal NPC's extra drops: {@link InfernalBands#rolls} picks. Each pick makes exactly two draws.
     *
     * @param rand         the entity's own random source
     * @param modifierSize the mob's Infernal Mobs modifier count
     * @param npcCeiling   the NPC's tier ceiling from its profile
     * @return the drops, one per roll that found an eligible entry
     */
    public static List<LootDrop> roll(Random rand, int modifierSize, Tier npcCeiling) {
        InfernalBands band = InfernalBands.forModifiers(modifierSize);
        int rolls = InfernalBands.rolls(modifierSize);
        List<LootDrop> out = new ArrayList<>();
        for (int i = 0; i < rolls; i++) {
            boolean rare = rand.nextInt(RARE_ONE_IN) == 0 && band.rareCeiling() != band.commonCeiling();
            int pick = rand.nextInt(PICK_RANGE);
            Tier ceiling = band.ceilingFor(npcCeiling, rare);
            List<Entry> pool = pool(ceiling, rare);
            if (!pool.isEmpty()) {
                out.add(
                    pool.get(pick % pool.size())
                        .drop());
            }
        }
        return out;
    }

    private static List<Entry> pool(Tier ceiling, boolean rare) {
        List<Entry> under = new ArrayList<>();
        List<Entry> exact = new ArrayList<>();
        for (Entry e : ENTRIES) {
            if (e.tier.ordinal() <= ceiling.ordinal()) {
                under.add(e);
                if (e.tier == ceiling) {
                    exact.add(e);
                }
            }
        }
        return rare && !exact.isEmpty() ? exact : under;
    }
}
