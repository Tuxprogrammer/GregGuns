package com.gregguns.entity.npc;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.gregguns.entity.npc.loot.EquipmentProfile;

/**
 * The numbers the NEI mob page (Mobs Info) shows for an NPC's carried gun (D312). Mobs Info would list a rolled weapon
 * as an "Additional" drop at {@value #ADDITIONAL_SCALE} per unit of its roll chance (vanilla's 8.5% equipment drop,
 * hard-coded there), which is wrong for our NPCs, whose weapon never drops that way; and its equipment enumeration does
 * not list our NPCs' gear at all in the dev pack. So the carry chance of each gun comes from the NPC's own
 * {@link EquipmentProfile} ({@link #carryChances}), and {@link #transform} turns it into the drops
 * {@link NpcGunDropsHandler} really makes:
 * <ul>
 * <li>the rare gun drop, at {@link NpcDrops#RARE_GUN_BASE} times the roll chance, player kills only, more with
 * Looting; none for a profile without the rare drop;</li>
 * <li>the gun's ammo, tank or salvage, at its average count per death times the roll chance.</li>
 * </ul>
 * Rows for the same item, type and gun are merged; the same ammo from two guns stays two rows, each with its own
 * gun's count range on its info line (D349). Pure: no Minecraft or Mobs Info types, so it is unit-tested without Mobs
 * Info.
 */
// Spec: docs/gregguns-spec/10-entities-npcs/mob-drops-nei.md (Mobs Info)
public final class NpcDropInfo {

    /** Mobs Info's chance unit: 10000 is 100%. */
    public static final int FULL = 10000;
    /** Mobs Info's Additional (equipment) chance per unit of roll chance, vanilla's 8.5%. */
    // Source:
    // https://github.com/GTNewHorizons/Mobs-Info/blob/0.6.0-GTNH/src/main/java/com/kuba6000/mobsinfo/loader/MobRecipeLoader.java#L966
    public static final int ADDITIONAL_SCALE = 850;

    /** A drop row's kind, as Mobs Info's {@code MobDrop.DropType}. */
    public enum Type {
        NORMAL,
        RARE
    }

    /** A drop row to add. */
    public static final class Row {

        private final Type type;
        private final String ref;
        private final String gun;
        private int chance;
        private final int min;
        private final int max;

        Row(Type type, String ref, String gun, int chance, int min, int max) {
            this.type = type;
            this.ref = ref;
            this.gun = gun;
            this.chance = chance;
            this.min = min;
            this.max = max;
        }

        /** @return the kind: {@link Type#RARE} for the gun, {@link Type#NORMAL} for its ammo or salvage */
        public Type type() {
            return type;
        }

        /**
         * @return the item reference ({@code gregguns:<name>}, {@code gt:<prefix>.<Material>}, a {@code ~full} tank)
         */
        public String ref() {
            return ref;
        }

        /** @return the registry name of the gun the row comes from (the first, when merged) */
        public String gun() {
            return gun;
        }

        /** @return the chance in Mobs Info units for one item: the average count per death times {@link #FULL} */
        public int chance() {
            return chance;
        }

        /** @return fewest items one drop gives */
        public int min() {
            return min;
        }

        /** @return most items one drop gives */
        public int max() {
            return max;
        }

        @Override
        public String toString() {
            return type + " " + ref + " " + chance + " (" + min + "-" + max + ", " + gun + ")";
        }
    }

    private NpcDropInfo() {}

    /**
     * The chance an NPC carries each weapon when its equipment is rolled at a danger drawn evenly from 0 to
     * {@code dangers - 1}, as an egg or Mobs Info's simulation rolls it ({@link EquipmentProfile#roll}: a weighted pick
     * among the weapons whose minimum danger is met).
     *
     * @param profile the NPC's equipment
     * @param dangers how many danger levels, from 0
     * @param exists  which weapons are registered
     * @return each weapon's chance, 0 to 1, in profile order
     */
    public static Map<String, Double> carryChances(EquipmentProfile profile, int dangers,
        java.util.function.Predicate<String> exists) {
        Map<String, Double> out = new LinkedHashMap<>();
        for (int d = 0; d < dangers; d++) {
            int total = 0;
            for (EquipmentProfile.Weapon w : profile.weapons()) {
                if (d >= w.minDifficulty() && exists.test(w.gun())) {
                    total += w.weight();
                }
            }
            for (EquipmentProfile.Weapon w : profile.weapons()) {
                if (total > 0 && d >= w.minDifficulty() && exists.test(w.gun())) {
                    out.merge(w.gun(), w.weight() / (double) total / dangers, Double::sum);
                }
            }
        }
        return out;
    }

    /**
     * @param guns     each gun's registry name, with the chance the NPC carries it (0 to 1)
     * @param rareDrop whether the profile has the rare gun drop
     * @param rules    each gun's drop rule; a gun with none maps to null
     * @return the rows to add for the guns
     */
    public static List<Row> transform(Map<String, Double> guns, boolean rareDrop,
        java.util.function.Function<String, NpcDrops.GunDrop> rules) {
        Map<String, Row> out = new LinkedHashMap<>();
        for (Map.Entry<String, Double> g : guns.entrySet()) {
            String gun = g.getKey();
            double p = g.getValue();
            if (rareDrop) {
                add(out, new Row(Type.RARE, "gregguns:" + gun, gun, round(p * NpcDrops.RARE_GUN_BASE * FULL), 1, 1));
            }
            NpcDrops.GunDrop rule = rules.apply(gun);
            if (rule == null) {
                continue;
            }
            if (rule.kind() == NpcDrops.GunDropKind.SALVAGE) {
                for (GunSalvage.Entry e : rule.salvage()) {
                    double each = GunSalvage.expectedCount(rule.salvage(), e);
                    add(out, new Row(Type.NORMAL, e.ref(), gun, round(p * each * FULL), e.min(), e.max()));
                }
            } else {
                add(
                    out,
                    new Row(
                        Type.NORMAL,
                        rule.ref(),
                        gun,
                        round(p * rule.expectedItems() * FULL),
                        rule.minItems(),
                        rule.maxItems()));
            }
        }
        List<Row> rows = new ArrayList<>();
        for (Row r : out.values()) {
            if (r.chance() > 0) {
                rows.add(r);
            }
        }
        return rows;
    }

    private static void add(Map<String, Row> out, Row row) {
        String key = row.type() + " " + row.ref() + " " + row.gun();
        Row had = out.get(key);
        if (had == null) {
            out.put(key, row);
        } else {
            had.chance += row.chance();
        }
    }

    private static int round(double v) {
        return (int) Math.round(v);
    }
}
