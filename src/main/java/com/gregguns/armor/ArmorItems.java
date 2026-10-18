package com.gregguns.armor;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.item.Item;

import org.jetbrains.annotations.Nullable;

import com.gregguns.armor.logic.ArmorCatalog;
import com.gregguns.armor.logic.ArmorSlot;
import com.gregguns.armor.logic.PieceDef;
import com.gregguns.armor.logic.SetDef;
import com.gregguns.core.registry.ModuleRegistrar;

/**
 * Registers one {@link ItemGregGunsArmor} per piece of every set in {@link ArmorCatalog} and holds them for recipes,
 * scenarios and other code. Adding a set to the catalog is all it takes to get its items.
 *
 * <p>
 * Usage: {@code ItemStack boots = new ItemStack(ArmorItems.get(ArmorCatalog.T1_SCOUT, ArmorSlot.BOOTS));}. Common side,
 * preInit.
 */
// Spec: docs/techguns-spec/05-armor/armor-catalog.md section 5
public final class ArmorItems {

    private static final Map<String, EnumMap<ArmorSlot, ItemGregGunsArmor>> BY_SET = new HashMap<>();

    private ArmorItems() {}

    /**
     * Registers every piece of every catalog set. Call once from {@code ArmorModule.preInit}.
     *
     * @param registrar the armor module's registrar
     */
    public static void register(ModuleRegistrar registrar) {
        for (SetDef set : ArmorCatalog.ALL) {
            EnumMap<ArmorSlot, ItemGregGunsArmor> pieces = new EnumMap<>(ArmorSlot.class);
            for (PieceDef piece : set.pieces()
                .values()) {
                pieces.put(
                    piece.slot(),
                    registrar.registerItem(set.pieceName(piece.slot()), new ItemGregGunsArmor(set, piece)));
            }
            BY_SET.put(set.id(), pieces);
        }
    }

    /**
     * @param set  a catalog set
     * @param slot a slot
     * @return the item, or null before registration, when the armor module is off, or when the set has no such piece
     */
    public static @Nullable ItemGregGunsArmor get(SetDef set, ArmorSlot slot) {
        EnumMap<ArmorSlot, ItemGregGunsArmor> pieces = BY_SET.get(set.id());
        return pieces == null ? null : pieces.get(slot);
    }

    /**
     * @param item any item
     * @return the item as GregGuns armor, or null
     */
    public static @Nullable ItemGregGunsArmor of(@Nullable Item item) {
        return item instanceof ItemGregGunsArmor ? (ItemGregGunsArmor) item : null;
    }

    /**
     * @param set a catalog set
     * @return the registered pieces of the set by slot, empty when not registered
     */
    public static Map<ArmorSlot, ItemGregGunsArmor> piecesOf(SetDef set) {
        EnumMap<ArmorSlot, ItemGregGunsArmor> pieces = BY_SET.get(set.id());
        if (pieces == null) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(pieces);
    }
}
