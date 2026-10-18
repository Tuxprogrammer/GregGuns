package com.gregguns.core.module;

import com.gregguns.core.registry.ModuleRegistrar;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

/**
 * A feature area (guns, armour, machines, ...) that registers its content through the module system.
 *
 * <p>
 * The {@link ModuleManager} calls these hooks only when {@code modules.<name>} is on, so a disabled module registers
 * nothing: no items, no recipes, no event handlers. Anything a module adds must therefore be added from inside these
 * hooks, never from a static initialiser or from {@code GregGuns} directly. Both sides; client-only work goes in a
 * {@link ClientModule} registered by {@code ClientProxy}.
 */
// Spec: docs/ROADMAP.md (M0 scope: registries); CONTRIBUTING.md (Registration order)
public interface GregGunsModule {

    /** @return which module this is; also selects the config flag */
    ModuleId id();

    /**
     * Config-dependent registration: items, blocks, entities, tile entities (use {@code registrar}). Runs after the
     * config is loaded.
     *
     * @param event     the FML event
     * @param registrar registers content under this module
     */
    default void preInit(FMLPreInitializationEvent event, ModuleRegistrar registrar) {}

    /**
     * Recipes, event handlers and anything that needs every mod's items registered.
     *
     * @param event the FML event
     */
    default void init(FMLInitializationEvent event) {}

    /**
     * Cross-mod work: compatibility recipes and the like.
     *
     * @param event the FML event
     */
    default void postInit(FMLPostInitializationEvent event) {}
}
