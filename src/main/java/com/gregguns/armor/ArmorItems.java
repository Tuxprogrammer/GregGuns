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
     * Registers every piece of every catalog set in catalog order. Kept for callers that do not care about the
     * creative and NEI order; the armor module registers through {@code ArmorItemRegistry} instead.
     *
     * @param registrar the armor module's registrar
     */
    public static void register(ModuleRegistrar registrar) {
        for (SetDef set : ArmorCatalog.ALL) {
            for (PieceDef piece : set.pieces()
                .values()) {
                registerPiece(registrar, set, piece.slot());
            }
        }
    }

    /**
     * Registers one piece of a catalog set, so the caller picks the registration order (see {@code ArmorItemOrder}).
     *
     * @param registrar the armor module's registrar
     * @param set       a catalog set
     * @param slot      the slot of one of its pieces
     */
    public static void registerPiece(ModuleRegistrar registrar, SetDef set, ArmorSlot slot) {
        PieceDef piece = set.pieces()
            .get(slot);
        ItemGregGunsArmor item = set.power()
            .kind() == com.gregguns.armor.logic.PowerSpec.PowerKind.ELECTRIC && slot == ArmorSlot.CHESTPLATE
                ? new ItemElectricArmor(set, piece)
                : new ItemGregGunsArmor(set, piece);
        BY_SET.computeIfAbsent(set.id(), k -> new EnumMap<>(ArmorSlot.class))
            .put(slot, registrar.registerItem(set.pieceName(slot), item));
    }

    /**
     * @param set  a catalog set
     * @param slot a slot
     * @return the item, or null before registration, or when the set has no such piece
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
