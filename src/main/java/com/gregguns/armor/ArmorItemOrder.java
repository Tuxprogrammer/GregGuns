package com.gregguns.armor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.gregguns.api.Tier;
import com.gregguns.armor.flight.ItemAntiGravityDevice;
import com.gregguns.armor.flight.ItemGlider;
import com.gregguns.armor.flight.ItemJumpPack;
import com.gregguns.armor.logic.ArmorCatalog;
import com.gregguns.armor.logic.ArmorSlot;
import com.gregguns.armor.logic.SetDef;
import com.gregguns.armor.slots.EquipmentItems;
import com.gregguns.armor.slots.ItemAmmoPouch;
import com.gregguns.armor.slots.ItemGasMask;
import com.gregguns.armor.slots.ItemNightVisionGoggles;
import com.gregguns.armor.slots.ItemTacticalMask;

/**
 * The one registration order of the armor module's items, derived from {@link ArmorCatalog}. Item ids follow
 * registration order in 1.7.10, and NEI's default sort and the creative tab follow the ids, so this is what a player
 * scrolls through in three blocks, each tier by tier (Steam to ZPM): every armor set in catalog order (helmet,
 * chestplate, leggings, boots, contiguous); then the accessories (special-slot gear and flight devices); then the
 * crafting parts of the armor.
 *
 * <p>
 * Accessories get their own block, not a place beside one set: a gas mask, goggles or a glider belong to no single
 * set. A new catalog
 * set slots itself in; a new accessory or part is one line in the lists below. FML remaps ids by registry name, so
 * reordering never breaks an existing world. Pure data, no Minecraft types, both sides.
 */
// Spec: docs/gregguns-spec/05-armor/index.md (registration order); owner request 2026-10-20 (NEI id order)
public final class ArmorItemOrder {

    /** What a slot registers, which decides the class that builds the item. */
    public enum Kind {
        /** One piece of a catalog set ({@code ArmorItems}). */
        ARMOR_PIECE,
        /** A special-slot item ({@code EquipmentItems}). */
        EQUIPMENT,
        /** A flight back device ({@code FlightItems}). */
        FLIGHT,
        /** A crafting part ({@code ArmorParts}). */
        PART
    }

    /** One item to register. */
    public static final class Slot {

        private final String name;
        private final Tier tier;
        private final Kind kind;
        private final SetDef set;
        private final ArmorSlot slot;

        Slot(String name, Tier tier, Kind kind, SetDef set, ArmorSlot slot) {
            this.name = name;
            this.tier = tier;
            this.kind = kind;
            this.set = set;
            this.slot = slot;
        }

        /** @return the bare registry name */
        public String name() {
            return name;
        }

        /** @return the tier group the item sits in */
        public Tier tier() {
            return tier;
        }

        /** @return what kind of item it is */
        public Kind kind() {
            return kind;
        }

        /** @return for an armor piece, its set; otherwise {@code null} */
        public SetDef set() {
            return set;
        }

        /** @return for an armor piece, its slot; otherwise {@code null} */
        public ArmorSlot armorSlot() {
            return slot;
        }
    }

    private static final class Extra {

        final String name;
        final Tier tier;
        final Kind kind;

        Extra(String name, Tier tier, Kind kind) {
            this.name = name;
            this.tier = tier;
            this.kind = kind;
        }
    }

    /**
     * Special-slot gear and flight devices with the tier each item declares ({@code getTier()}), in the order they sit
     * inside a tier: face gear first, then the back.
     */
    private static final List<Extra> ACCESSORIES = Collections.unmodifiableList(
        Arrays.asList(
            new Extra(ItemNightVisionGoggles.NAME, Tier.MV, Kind.EQUIPMENT),
            new Extra(ItemAmmoPouch.NAME, Tier.MV, Kind.EQUIPMENT),
            new Extra(ItemGasMask.NAME, Tier.MV, Kind.EQUIPMENT),
            new Extra(ItemTacticalMask.NAME, Tier.HV, Kind.EQUIPMENT),
            new Extra(EquipmentItems.OXYGEN_MASK, Tier.HV, Kind.EQUIPMENT),
            new Extra(EquipmentItems.OXYGEN_TANKS, Tier.HV, Kind.EQUIPMENT),
            new Extra(ItemGlider.NAME, Tier.HV, Kind.FLIGHT),
            new Extra(ItemJumpPack.NAME, Tier.HV, Kind.FLIGHT),
            new Extra(ItemAntiGravityDevice.NAME, Tier.IV, Kind.FLIGHT)));

    /** The tier each {@link ArmorParts#NAMES} part declares. */
    private static final List<Extra> PARTS = Collections.unmodifiableList(
        Arrays.asList(
            new Extra(ArmorCatalog.HEAVY_CLOTH, Tier.STEAM, Kind.PART),
            new Extra(ArmorCatalog.TREATED_LEATHER, Tier.LV, Kind.PART),
            new Extra(ArmorParts.GAS_MASK_FILTER, Tier.MV, Kind.PART),
            new Extra(ArmorParts.TANK_AIR, Tier.HV, Kind.PART),
            new Extra(ArmorParts.GLIDER_BACKPACK, Tier.HV, Kind.PART),
            new Extra(ArmorParts.GLIDER_WING, Tier.HV, Kind.PART),
            new Extra(ArmorCatalog.STEAM_ARMOR_PLATING, Tier.EV, Kind.PART)));

    private ArmorItemOrder() {}

    /** @return every item the armor module registers, in registration order; the tier never decreases down the list */
    public static List<Slot> build() {
        List<Slot> all = new ArrayList<>();
        // Block 1: every armor set, tier by tier.
        for (Tier tier : Tier.values()) {
            for (SetDef set : ArmorCatalog.ALL) {
                if (set.tier() != tier) {
                    continue;
                }
                for (ArmorSlot slot : ArmorSlot.values()) {
                    if (set.pieces()
                        .containsKey(slot)) {
                        all.add(new Slot(set.pieceName(slot), tier, Kind.ARMOR_PIECE, set, slot));
                    }
                }
            }
        }
        // Block 2: accessories, then block 3: crafting parts, each tier by tier.
        for (Tier tier : Tier.values()) {
            addExtras(all, ACCESSORIES, tier);
        }
        for (Tier tier : Tier.values()) {
            addExtras(all, PARTS, tier);
        }
        return Collections.unmodifiableList(all);
    }

    private static void addExtras(List<Slot> all, List<Extra> extras, Tier tier) {
        for (Extra extra : extras) {
            if (extra.tier == tier) {
                all.add(new Slot(extra.name, tier, extra.kind, null, null));
            }
        }
    }
}
