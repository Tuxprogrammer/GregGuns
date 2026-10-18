package com.gregguns.machine.reaction;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.gregguns.GregGuns;
import com.gregguns.api.Tier;
import com.gregguns.core.registry.ModuleRegistrar;
import com.gregguns.item.part.ItemPart;

import cpw.mods.fml.common.registry.GameRegistry;

/**
 * The three components the Reaction Chamber makes or consumes: the Heat Ray Focus (its non-consumed input), the Laser
 * Focus (EV) and the Anti Gravity Core (IV). Registered by the machines module, so the machines work without the rest
 * of the component index.
 *
 * <p>
 * Usage: {@code ReactionParts.stack(ReactionParts.LASER_FOCUS, 1)}. Common side, preInit.
 */
// Spec: docs/gregguns-spec/08-machines-production/recipes.md (Heat Ray Focus, Laser Focus, Anti Gravity Core)
public final class ReactionParts {

    /** Registry name of the Heat Ray Focus. */
    public static final String HEAT_RAY_FOCUS = "part_heat_ray_focus";
    /** Registry name of the Laser Focus. */
    public static final String LASER_FOCUS = "part_laser_focus";
    /** Registry name of the Anti Gravity Core. */
    public static final String ANTI_GRAVITY_CORE = "part_anti_gravity_core";

    private ReactionParts() {}

    /**
     * Registers the three items.
     *
     * @param registrar the machines module's registrar
     */
    public static void register(ModuleRegistrar registrar) {
        registrar.registerItem(HEAT_RAY_FOCUS, new ItemPart(Tier.EV));
        registrar.registerItem(LASER_FOCUS, new ItemPart(Tier.EV));
        registrar.registerItem(ANTI_GRAVITY_CORE, new ItemPart(Tier.IV));
    }

    /**
     * @param registryName one of the constants above
     * @param count        stack size (0 makes a GT non-consumed input)
     * @return the stack, or {@code null} if the item is not registered
     */
    public static ItemStack stack(String registryName, int count) {
        Item item = GameRegistry.findItem(GregGuns.MODID, registryName);
        return item == null ? null : new ItemStack(item, count);
    }
}
