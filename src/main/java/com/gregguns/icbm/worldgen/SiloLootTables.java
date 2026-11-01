package com.gregguns.icbm.worldgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.gregguns.api.Tier;
import com.gregguns.entity.npc.loot.LootDrop;
import com.gregguns.icbm.complex.LootKeys;
import com.gregguns.worldgen.loot.LootCategory;
import com.gregguns.worldgen.loot.LootEntry;
import com.gregguns.worldgen.loot.LootMix;
import com.gregguns.worldgen.loot.LootPools;

/**
 * The silo complex's room tables (D316): one table per room theme and per ceiling, mixed by {@link LootMix} from the
 * shared {@link SiloPools} and the complex's issue arms ({@link SiloArms}). The mess and cafeteria are food heavy, the
 * bunks armor heavy, the shooting range guns and ammo heavy, the missile assembly floor missile body and warhead parts
 * heavy, and every table carries a little everyday clutter. Pure data, no Minecraft types; {@link SiloChests} registers
 * every table with Forge in postInit as {@code gregguns:silo_<key>_<ceiling>}, plus {@code gregguns:silo_junk}.
 *
 * <p>
 * Ceilings (D38, D129): every line declares the tier of what it holds, and a table built at a ceiling drops every line
 * above it. GregGuns' own content stops at ZPM, so above ZPM the only richer lines are the GT material ladder rungs
 * (plates and ingots), each tagged with its voltage ({@link Line#voltage()}). An Overworld complex (EV) holds nothing
 * above EV.
 *
 * <p>
 * Usage: {@code SiloLootTables.category("galley", SiloCeiling.EV)} for the {@link LootCategory} the chest draws from,
 * {@code SiloLootTables.lines(...)} for the checked lines with their voltages, {@code categoryName(...)} for the
 * registry name.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/loot-and-ceilings.md (Room tables, GGI-08-30..45); D38, D129, D316
public final class SiloLootTables {

    /** The GT material of each voltage, index 3 (HV) to 12 (UMV): the plate and ingot ladder. */
    private static final String[] LADDER = { null, null, null, "StainlessSteel", "Titanium", "TungstenSteel", "HSSG",
        "NaquadahAlloy", "Neutronium", "CosmicNeutronium", "Infinity", "TranscendentMetal", "SpaceTime" };

    /** Every table key, from {@link RoomLoot#tables()}. */
    public static final String[] KEYS = RoomLoot.tables()
        .toArray(new String[0]);

    /** The registry name of the junk category every abandoned and mothballed chest draws its junk from. */
    public static final String JUNK = "gregguns:silo_junk";

    /** The nominal picks of a category as Forge sees it; the real count comes from {@link RoomLootDraw}. */
    private static final int MIN_PICKS = 5, MAX_PICKS = 8;

    private SiloLootTables() {}

    /** One table line with the GT voltage index of its contents. */
    public static final class Line {

        private final String ref;
        private final int min;
        private final int max;
        private final int weight;
        private final int voltage;

        Line(String ref, int min, int max, int weight, int voltage) {
            this.ref = ref;
            this.min = min;
            this.max = max;
            this.weight = weight;
            this.voltage = voltage;
        }

        /** @return the item reference (see {@code LootEntry}) */
        public String ref() {
            return ref;
        }

        /** @return smallest stack */
        public int min() {
            return min;
        }

        /** @return largest stack */
        public int max() {
            return max;
        }

        /** @return the weight */
        public int weight() {
            return weight;
        }

        /** @return the GT voltage index of the line's contents; never above the table's ceiling */
        public int voltage() {
            return voltage;
        }
    }

    /**
     * @param key     a table key
     * @param ceiling a ceiling, {@link SiloCeiling#EV} to {@link SiloCeiling#UMV}
     * @return the registry name, for example {@code gregguns:silo_safe_ev}
     */
    public static String categoryName(String key, int ceiling) {
        return "gregguns:silo_" + key
            + "_"
            + SiloCeiling.name(ceiling)
                .toLowerCase(Locale.ROOT);
    }

    /** @return the GT material of a voltage's ladder rung, or null below HV or above UMV */
    public static String material(int voltage) {
        return voltage >= 0 && voltage < LADDER.length ? LADDER[voltage] : null;
    }

    /**
     * @param key     a table key; an unknown key gets the stockpile table
     * @param ceiling the ceiling
     * @return the lines with their voltages, all at or below {@code ceiling}
     */
    public static List<Line> lines(String key, int ceiling) {
        List<Line> out = new ArrayList<>();
        for (LootEntry e : category(key, ceiling).entries()) {
            out.add(
                new Line(
                    e.ref(),
                    e.drop()
                        .min(),
                    e.drop()
                        .max(),
                    e.weight(),
                    voltageOf(e)));
        }
        return out;
    }

    /**
     * @param e a table line
     * @return the voltage of what it holds: a ladder rung's own voltage (above ZPM too), else its declared tier's
     */
    public static int voltageOf(LootEntry e) {
        String ref = e.ref();
        if (ref.startsWith("gt:")) {
            String material = ref.substring(ref.indexOf('.') + 1);
            for (int v = 0; v < LADDER.length; v++) {
                if (material.equals(LADDER[v])) {
                    return v;
                }
            }
        }
        return e.tier()
            .voltageIndex();
    }

    /**
     * @param key     a table key; an unknown key gets the stockpile table
     * @param ceiling the ceiling
     * @return the table: every line at or below the ceiling, the arms of the complex's issue, the ladder up to the
     *         ceiling's rung
     */
    public static LootCategory category(String key, int ceiling) {
        String k = RoomLoot.tables()
            .contains(key) ? key : LootKeys.STOCKPILE;
        Tier cap = tierOf(Math.min(ceiling, Tier.ZPM.voltageIndex()));
        LootMix m = LootMix.of(categoryName(k, ceiling), cap, MIN_PICKS, MAX_PICKS);
        mix(m, k, ceiling);
        return m.build();
    }

    /**
     * Every item reference a chest of a table may hold (D317): the table's lines and their companions, the junk lines,
     * the spoil items its lines can turn into, and the never-empty fallback. Bonus items come on top (the contents
     * pass knows them per chest).
     *
     * @param key     a table key
     * @param ceiling the ceiling
     * @return the references
     */
    public static java.util.Set<String> chestRefs(String key, int ceiling) {
        java.util.Set<String> out = new java.util.LinkedHashSet<>();
        for (LootEntry e : category(key, ceiling).entries()) {
            out.add(e.ref());
            if (e.companion() != null) {
                out.add(
                    e.companion()
                        .ref());
            }
            String spoil = SiloPools.spoilOf(e.pool(), e.ref());
            if (spoil != null) {
                out.add(spoil);
            }
        }
        for (LootEntry e : junk().entries()) {
            out.add(e.ref());
        }
        out.add(RoomLootDraw.FALLBACK.ref());
        return out;
    }

    /** @return the junk category (no ceiling: junk is junk on every planet) */
    public static LootCategory junk() {
        return LootMix.of(JUNK, Tier.STEAM, 1, 3)
            .pool(100, SiloPools.junk())
            .build();
    }

    /** The room tables as pool shares (points; a table's shares add to 100). */
    private static void mix(LootMix m, String key, int c) {
        LootPools.Pool common = SiloPools.common();
        switch (key) {
            case LootKeys.GALLEY:
                m.pool(30, SiloPools.ingredients())
                    .pool(25, SiloPools.meals())
                    .pool(15, SiloPools.produce())
                    .pool(15, SiloPools.cookware())
                    .pool(5, SiloPools.rawmeat())
                    .pool(10, common);
                break;
            case LootKeys.CANTEEN:
                m.pool(35, SiloPools.meals())
                    .pool(20, SiloPools.snacks())
                    .pool(20, SiloPools.drinks())
                    .pool(10, SiloPools.ingredients())
                    .pool(5, SiloPools.cookware())
                    .pool(10, common);
                break;
            case LootKeys.PANTRY:
                m.pool(30, SiloPools.rawmeat())
                    .pool(25, SiloPools.preserved())
                    .pool(20, SiloPools.produce())
                    .pool(15, SiloPools.ingredients())
                    .pool(5, SiloPools.drinks())
                    .pool(5, common);
                break;
            case LootKeys.FARM:
                m.pool(30, SiloPools.seeds())
                    .pool(30, SiloPools.produce())
                    .pool(25, SiloPools.farming())
                    .pool(15, common);
                break;
            case LootKeys.CELLAR:
                m.pool(60, SiloPools.mushroom())
                    .pool(15, SiloPools.farming())
                    .pool(10, SiloPools.produce())
                    .pool(15, common);
                break;
            case LootKeys.BUNKS:
                m.pool(50, SiloPools.bunkarmor())
                    .pool(20, SiloPools.personal())
                    .pool(10, SiloArms.sidearmAmmo(c))
                    .pool(10, SiloPools.snacks())
                    .pool(10, common);
                break;
            case LootKeys.LAUNDRY:
                m.pool(40, SiloPools.linens())
                    .pool(30, SiloPools.bunkarmor())
                    .pool(15, SiloPools.personal())
                    .pool(15, common);
                break;
            case LootKeys.LOUNGE:
                m.pool(30, SiloPools.snacks())
                    .pool(25, SiloPools.drinks())
                    .pool(30, SiloPools.lounge())
                    .pool(15, common);
                break;
            case LootKeys.MEDICAL:
                m.pool(60, SiloPools.medical())
                    .pool(5, SiloPools.hazmat())
                    .pool(10, SiloPools.soups())
                    .pool(5, SiloPools.personal())
                    .pool(20, common);
                break;
            case LootKeys.DECON:
                m.pool(55, SiloPools.hazmat())
                    .pool(25, SiloPools.medical())
                    .pool(5, SiloPools.pumps())
                    .pool(15, common);
                break;
            case LootKeys.GUARD:
                m.pool(15, SiloArms.sidearms(c))
                    .pool(35, SiloArms.sidearmAmmo(c))
                    .pool(10, SiloPools.drinks())
                    .pool(10, SiloPools.snacks())
                    .pool(10, SiloPools.medical())
                    .pool(5, SiloPools.control())
                    .pool(15, common);
                break;
            case LootKeys.BRIG:
                m.pool(50, SiloPools.evidence())
                    .pool(20, SiloPools.personal())
                    .pool(10, SiloArms.sidearmAmmo(c))
                    .pool(20, common);
                break;
            case LootKeys.RANGE:
                m.pool(45, SiloArms.ammo(c))
                    .pool(15, SiloArms.guns(c))
                    .pool(5, SiloArms.grenades(c))
                    .pool(10, SiloArms.parts(c))
                    .pool(10, SiloPools.targets())
                    .pool(15, common);
                break;
            case LootKeys.ARMORY:
            case LootKeys.ARMORY_AMMO: // an energy gun's display chest, which burns no ammo, draws the armory table
                m.pool(40, SiloArms.ammo(c))
                    .pool(15, SiloArms.guns(c))
                    .pool(10, SiloArms.grenades(c))
                    .pool(10, SiloArms.parts(c))
                    .pool(10, SiloPools.combatArmor())
                    .pool(5, SiloPools.tools())
                    .pool(10, common);
                break;
            case LootKeys.CONTROL:
                m.pool(35, SiloPools.control())
                    .pool(30, SiloPools.electronics())
                    .pool(10, SiloPools.office())
                    .pool(10, SiloPools.drinks())
                    .pool(15, common);
                break;
            case LootKeys.RADAR:
                m.pool(35, SiloPools.electronics())
                    .pool(30, SiloPools.control())
                    .pool(10, SiloPools.power())
                    .pool(10, SiloPools.drinks())
                    .pool(15, common);
                break;
            case LootKeys.COMMS:
                m.pool(45, SiloPools.electronics())
                    .pool(20, SiloPools.office())
                    .pool(10, SiloPools.power())
                    .pool(10, SiloPools.drinks())
                    .pool(15, common);
                break;
            case LootKeys.SERVER:
                m.pool(55, SiloPools.electronics())
                    .pool(20, SiloPools.power())
                    .pool(10, SiloPools.hvac())
                    .pool(15, common);
                break;
            case LootKeys.OFFICE:
                m.pool(60, SiloPools.office())
                    .pool(10, SiloPools.drinks())
                    .pool(5, SiloPools.snacks())
                    .pool(10, SiloPools.personal())
                    .pool(15, common);
                break;
            case LootKeys.SAFE:
                m.pool(25, ladder("plate", 2, 8, c))
                    .pool(25, ladder("ingot", 2, 8, c))
                    .pool(20, valuables())
                    .pool(10, SiloArms.guns(c))
                    .pool(20, SiloArms.ammo(c));
                break;
            case LootKeys.ASSEMBLY:
                m.pool(50, SiloPools.missilebody(c, true))
                    .pool(15, SiloPools.warheadparts())
                    .pool(15, SiloPools.hardware())
                    .pool(10, SiloPools.components())
                    .pool(10, common);
                break;
            case LootKeys.MAGAZINE:
                m.pool(45, SiloPools.warheadparts())
                    .pool(30, SiloPools.explosives())
                    .pool(5, SiloPools.missilebody(c, false))
                    .pool(10, SiloPools.hardware())
                    .pool(10, common);
                break;
            case LootKeys.WORKSHOP:
                m.pool(25, SiloPools.hardware())
                    .pool(5, ladder("plate", 2, 8, c))
                    .pool(20, SiloPools.components())
                    .pool(15, SiloPools.tools())
                    .pool(10, SiloArms.parts(c))
                    .pool(10, SiloPools.electronics())
                    .pool(15, common);
                break;
            case LootKeys.MOTORPOOL:
                m.pool(25, SiloPools.components())
                    .pool(10, SiloPools.vehicle())
                    .pool(15, SiloPools.power())
                    .pool(15, SiloPools.fuel())
                    .pool(15, SiloPools.hardware())
                    .pool(5, SiloPools.tools())
                    .pool(15, common);
                break;
            case LootKeys.FUEL:
                m.pool(70, SiloPools.fuel())
                    .pool(10, SiloPools.hardware())
                    .pool(20, common);
                break;
            case LootKeys.POWER:
                m.pool(60, SiloPools.power())
                    .pool(10, SiloPools.components())
                    .pool(15, SiloPools.hardware())
                    .pool(15, common);
                break;
            case LootKeys.HVAC:
                m.pool(60, SiloPools.hvac())
                    .pool(15, SiloPools.components())
                    .pool(10, SiloPools.hardware())
                    .pool(15, common);
                break;
            case LootKeys.PUMPS:
                m.pool(60, SiloPools.pumps())
                    .pool(10, SiloPools.components())
                    .pool(15, SiloPools.hardware())
                    .pool(15, common);
                break;
            case LootKeys.CHEMLAB:
                m.pool(65, SiloPools.chem())
                    .pool(10, SiloPools.hazmat())
                    .pool(5, SiloPools.medical())
                    .pool(5, SiloPools.electronics())
                    .pool(15, common);
                break;
            case LootKeys.INCINERATOR:
                m.pool(75, SiloPools.incinerator())
                    .pool(10, SiloPools.hardware())
                    .pool(15, common);
                break;
            case LootKeys.SALVAGE:
                m.pool(20, SiloPools.bulk())
                    .pool(5, ladder("plate", 1, 4, c))
                    .pool(20, SiloPools.hardware())
                    .pool(10, SiloPools.components())
                    .pool(10, SiloPools.missilebody(c, false))
                    .pool(10, SiloPools.preserved())
                    .pool(10, SiloPools.tools())
                    .pool(15, common);
                break;
            case LootKeys.STOCKPILE:
            default:
                m.pool(50, SiloPools.bulk())
                    .pool(10, ladder("ingot", 4, 12, c))
                    .pool(10, SiloPools.preserved())
                    .pool(10, SiloPools.hardware())
                    .pool(5, SiloPools.tools())
                    .pool(15, common);
                break;
        }
    }

    /** @return the safe's valuables: gold, silver, coins and the odd diamond */
    private static LootPools.Pool valuables() {
        List<LootEntry> lines = new ArrayList<>();
        lines.add(line("gt:ingot.Gold", 4, 12, 3, Tier.STEAM));
        lines.add(line("gt:ingot.Silver", 4, 12, 3, Tier.STEAM));
        lines.add(line("gregguns:coin", 8, 32, 3, Tier.STEAM));
        lines.add(line("minecraft:diamond", 1, 3, 1, Tier.STEAM));
        return LootPools.Pool.of("valuables", lines);
    }

    /**
     * One line per ladder rung from HV up to the ceiling. The top rung weighs three times as much, so a rich
     * dimension's chests are mostly its own tier, with the rungs below thinning out. A rung above ZPM declares ZPM, the
     * cap of GregGuns' tiers; its real voltage is {@link #voltageOf}.
     */
    private static LootPools.Pool ladder(String prefix, int min, int max, int ceiling) {
        List<LootEntry> lines = new ArrayList<>();
        int top = Math.min(ceiling, LADDER.length - 1);
        for (int v = 3; v <= top; v++) {
            lines.add(
                line(
                    "gt:" + prefix + "." + LADDER[v],
                    min,
                    max,
                    v == top ? 3 : 1,
                    tierOf(Math.min(v, Tier.ZPM.voltageIndex()))));
        }
        return LootPools.Pool.of("ladder_" + prefix, lines);
    }

    private static LootEntry line(String ref, int min, int max, int weight, Tier tier) {
        return new LootEntry(LootDrop.range(ref, min, max), weight, tier, "silo", null);
    }

    private static Tier tierOf(int voltage) {
        Tier best = Tier.STEAM;
        for (Tier t : Tier.values()) {
            if (t.voltageIndex() <= voltage) {
                best = t;
            }
        }
        return best;
    }
}
