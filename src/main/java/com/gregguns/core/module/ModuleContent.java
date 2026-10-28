package com.gregguns.core.module;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.IntFunction;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.gregguns.block.GregGunsCasings;
import com.gregguns.machine.MteModules;

import gregtech.api.GregTechAPI;

/**
 * Remembers which module each registered item and block belongs to, so the rest of the mod can ask "is this stack's
 * module off?" without knowing the content. Filled by {@code ModuleRegistrar} (and by the few blocks that register
 * themselves); GregTech machine stacks and the casing block are resolved by metadata.
 *
 * <p>
 * A module being off never removes content (see {@link ModuleState}); it only hides it and stops it working. Every
 * query here is therefore pure behavior: safe to call from any thread after preInit.
 */
// Spec: docs/gregguns-spec/01-core/config-gtnhlib.md (module switches)
public final class ModuleContent {

    private static final Map<Item, ModuleId> ITEMS = new IdentityHashMap<>();
    /** Items whose owner depends on the damage value (a stack that only another module can make). */
    private static final Map<Item, IntFunction<ModuleId>> BY_DAMAGE = new IdentityHashMap<>();

    private ModuleContent() {}

    /**
     * @param item   an item
     * @param module the module that registered it
     */
    public static void assign(Item item, ModuleId module) {
        ITEMS.put(item, module);
    }

    /**
     * Gives an item a per-damage owner: the function's answer wins over the item's own module, and {@code null} from it
     * falls back to that module. Used for a stack only another module can make (hot rubble comes only from ICBM
     * blasts).
     *
     * @param item     an item already assigned to a module
     * @param byDamage the owner of a damage value, or {@code null} for the item's own module
     */
    public static void assignByDamage(Item item, IntFunction<ModuleId> byDamage) {
        BY_DAMAGE.put(item, byDamage);
    }

    /**
     * @param block  a block (its item form is recorded)
     * @param module the module that registered it
     */
    public static void assign(Block block, ModuleId module) {
        Item item = Item.getItemFromBlock(block);
        if (item != null) {
            ITEMS.put(item, module);
        }
    }

    /**
     * @param stack a stack, may be {@code null}
     * @return the GregGuns module that owns it, or {@code null} for anything else (also for an empty stack)
     */
    public static ModuleId moduleOf(ItemStack stack) {
        if (stack == null) {
            return null;
        }
        Item item = stack.getItem();
        if (item == null) {
            return null;
        }
        if (GregGunsCasings.INSTANCE != null && item == Item.getItemFromBlock(GregGunsCasings.INSTANCE)) {
            return MteModules.casingModule(stack.getItemDamage());
        }
        IntFunction<ModuleId> byDamage = BY_DAMAGE.get(item);
        if (byDamage != null) {
            ModuleId special = byDamage.apply(stack.getItemDamage());
            if (special != null) {
                return special;
            }
        }
        ModuleId id = ITEMS.get(item);
        if (id != null) {
            return id;
        }
        if (GregTechAPI.sBlockMachines != null && item == Item.getItemFromBlock(GregTechAPI.sBlockMachines)) {
            return MteModules.moduleOf(stack.getItemDamage());
        }
        return null;
    }

    /**
     * @param stack a stack, may be {@code null}
     * @return the owning module if the stack is GregGuns content whose module is off, else {@code null}
     */
    public static ModuleId disabledModuleOf(ItemStack stack) {
        ModuleId id = moduleOf(stack);
        return id != null && !ModuleState.isActive(id) ? id : null;
    }

    /**
     * @param stack a stack, may be {@code null}
     * @return false for GregGuns content whose module is off; true for everything else
     */
    public static boolean isUsable(ItemStack stack) {
        return disabledModuleOf(stack) == null;
    }
}
