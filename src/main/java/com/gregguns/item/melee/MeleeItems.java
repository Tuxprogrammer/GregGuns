package com.gregguns.item.melee;

import com.gregguns.core.registry.ModuleRegistrar;

/**
 * Registers the LV melee tools and holds them for recipes and scenarios. Common side, preInit.
 *
 * <p>
 * Usage: {@code new ItemStack(MeleeItems.combatKnife())}. Spec: docs/techguns-spec/06-items/special-items.md.
 */
public final class MeleeItems {

    /** Registry name of the Combat Knife. */
    public static final String COMBAT_KNIFE = "combat_knife";
    /** Registry name of the Crowbar. */
    public static final String CROWBAR = "crowbar";

    private static ItemCombatKnife combatKnife;
    private static ItemCrowbar crowbar;

    private MeleeItems() {}

    /**
     * Registers the Combat Knife and the Crowbar. Call once from {@code GunsModule.preInit}.
     *
     * @param registrar the guns module's registrar
     */
    public static void register(ModuleRegistrar registrar) {
        combatKnife = registrar.registerItem(COMBAT_KNIFE, new ItemCombatKnife());
        crowbar = registrar.registerItem(CROWBAR, new ItemCrowbar());
    }

    /** @return the Combat Knife; {@code null} before registration or when the guns module is off */
    public static ItemCombatKnife combatKnife() {
        return combatKnife;
    }

    /** @return the Crowbar; {@code null} before registration or when the guns module is off */
    public static ItemCrowbar crowbar() {
        return crowbar;
    }
}
