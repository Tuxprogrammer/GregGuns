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
        registerOne(registrar, COMBAT_KNIFE);
        registerOne(registrar, CROWBAR);
    }

    /**
     * Registers one melee tool by name, so the caller picks the registration order (see {@code GunsItemOrder}).
     *
     * @param registrar the guns module's registrar
     * @param name      {@link #COMBAT_KNIFE} or {@link #CROWBAR}
     */
    public static void registerOne(ModuleRegistrar registrar, String name) {
        if (COMBAT_KNIFE.equals(name)) {
            combatKnife = registrar.registerItem(COMBAT_KNIFE, new ItemCombatKnife());
        } else if (CROWBAR.equals(name)) {
            crowbar = registrar.registerItem(CROWBAR, new ItemCrowbar());
        } else {
            throw new IllegalArgumentException("not a melee item: " + name);
        }
    }

    /** @return the Combat Knife; {@code null} before registration */
    public static ItemCombatKnife combatKnife() {
        return combatKnife;
    }

    /** @return the Crowbar; {@code null} before registration */
    public static ItemCrowbar crowbar() {
        return crowbar;
    }
}
