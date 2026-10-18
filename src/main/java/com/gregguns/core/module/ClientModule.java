package com.gregguns.core.module;

/**
 * The client half of a {@link GregGunsModule}: renderers, models, HUD and sounds.
 *
 * <p>
 * Implementations live under {@code com.gregguns.client}, and {@code ClientProxy} is the only code that names them.
 * This interface has no Minecraft types so common code can hold it without loading client classes. The manager runs a
 * hook only when its module is enabled.
 */
public interface ClientModule {

    /** @return the module this belongs to */
    ModuleId id();

    /** Client side of preInit: for example texture and model registration. */
    default void preInit() {}

    /** Client side of init: renderers, keybinds, HUD overlays. */
    default void init() {}

    /** Client side of postInit. */
    default void postInit() {}
}
