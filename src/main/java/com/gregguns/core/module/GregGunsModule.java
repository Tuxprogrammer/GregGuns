package com.gregguns.core.module;

import com.gregguns.core.registry.ModuleRegistrar;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

/**
 * A feature area (guns, armor, machines, ...) that registers its content through the module system.
 *
 * <p>
 * The {@link ModuleManager} calls {@link #preInit} and {@link #initContent} for every module, whatever its
 * {@code modules.<name>} flag says, so a flag never changes what is registered (items, blocks, entities, tile entities,
 * machines, packets). It calls {@link #init} and {@link #postInit} only for enabled modules, so a disabled module has
 * no
 * recipes, event handlers, spawning or worldgen. Anything a module adds must be added from inside these hooks, never
 * from a static initializer or from {@code GregGuns} directly. Both sides; client-only work goes in a
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
     * Content that must exist whatever the flags say but cannot be registered in preInit: GregTech machines (legal only
     * between GT's preload and postload), GUI handlers, client renderers of the module's blocks. No recipes and no
     * event
     * handlers here: those belong in {@link #init}.
     *
     * @param event the FML event
     */
    default void initContent(FMLInitializationEvent event) {}

    /**
     * Behavior of an enabled module: recipes, event handlers and anything that needs every mod's items registered.
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
