package com.gregguns.item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.gregguns.armor.ArmorItemOrder;
import com.gregguns.block.GregGunsCasings;
import com.gregguns.entity.npc.NpcRegistry;
import com.gregguns.gun.def.ToolProbeDef;
import com.gregguns.utility.UtilityItemOrder;
import com.gregguns.worldgen.nest.NestBlockRegistration;
import com.gregguns.worldgen.spawner.HoleKind;

/**
 * The one list of every GregGuns item and block, in the order a player should meet them in NEI and the creative tab.
 * It stitches the per-module orders ({@link GunsItemOrder}, {@link ArmorItemOrder}, {@link UtilityItemOrder}) together
 * in module registration order (guns, armor, utility, npcs, worldgen), so the sequence is also the order Forge assigns
 * numeric ids in a NEW world. An existing world keeps the ids it saved, so NEI cannot rely on them: the client reads
 * this list through {@code NeiOrderRemap} and sorts by it instead. The table in
 * {@code docs/gregguns-spec/06-items/nei-order.md} documents the same blocks, and a unit test keeps the two equal.
 *
 * <p>
 * GT meta tile entities (the Ammo Presses, the Ammunition Works, the benches) are not here: they are GT items whose
 * order is GT's meta id, which is reserved. Pure data, no Minecraft types, both sides.
 */
// Spec: docs/gregguns-spec/06-items/nei-order.md; owner request 2026-10-20 (NEI order in existing worlds)
public final class NeiOrder {

    /** The blocks of the list, in order. The doc table has one row per constant. */
    public enum Category {
        /** Guns, their magazines and rounds, melee tools and grenades, tier by tier. */
        WEAPONS_AND_AMMO,
        /** Gun parts and components, tier by tier. */
        GUN_PARTS,
        /** The Ammunition Works casings (the one GregGuns machine block outside GT's MTE list). */
        MACHINE_BLOCKS,
        /** Armor pieces, set by set, tier by tier. */
        ARMOR,
        /** Special-slot gear and flight devices, tier by tier. */
        ACCESSORIES,
        /** Armor crafting parts, tier by tier. */
        ARMOR_PARTS,
        /** Turret plates, the charge, chests, the flag and the blob. */
        UTILITY,
        /** The NPC spawn egg. */
        NPC_EGG,
        /** Zombie and Bug Hole spawners, the bug nest and its slime. */
        WORLD_BLOCKS,
        /** Dev-only items, last. */
        DEV
    }

    private NeiOrder() {}

    /**
     * @return every GregGuns item and block name (bare registry names) grouped by category, in list order; each
     *         category's names are in registration order
     */
    public static Map<Category, List<String>> byCategory() {
        Map<Category, List<String>> out = new LinkedHashMap<>();
        for (Category category : Category.values()) {
            out.put(category, new ArrayList<String>());
        }
        for (GunsItemOrder.Slot slot : GunsItemOrder.build()) {
            boolean part = slot.kind() == GunsItemOrder.Kind.PART || slot.kind() == GunsItemOrder.Kind.COMPONENT;
            out.get(part ? Category.GUN_PARTS : Category.WEAPONS_AND_AMMO)
                .add(slot.name());
        }
        out.get(Category.MACHINE_BLOCKS)
            .add(GregGunsCasings.NAME);
        for (ArmorItemOrder.Slot slot : ArmorItemOrder.build()) {
            Category category = slot.kind() == ArmorItemOrder.Kind.ARMOR_PIECE ? Category.ARMOR
                : slot.kind() == ArmorItemOrder.Kind.PART ? Category.ARMOR_PARTS : Category.ACCESSORIES;
            out.get(category)
                .add(slot.name());
        }
        for (UtilityItemOrder.Slot slot : UtilityItemOrder.build()) {
            out.get(Category.UTILITY)
                .add(slot.name());
        }
        out.get(Category.NPC_EGG)
            .add(NpcRegistry.EGG_NAME);
        for (HoleKind kind : HoleKind.values()) {
            out.get(Category.WORLD_BLOCKS)
                .add(kind.blockName());
        }
        out.get(Category.WORLD_BLOCKS)
            .add(NestBlockRegistration.NEST_NAME);
        out.get(Category.WORLD_BLOCKS)
            .add(NestBlockRegistration.SLIME_NAME);
        out.get(Category.DEV)
            .add(ToolProbeDef.NAME);
        return out;
    }

    /** @return every name, flattened in list order */
    public static List<String> names() {
        List<String> all = new ArrayList<>();
        for (List<String> names : byCategory().values()) {
            all.addAll(names);
        }
        return Collections.unmodifiableList(all);
    }
}
