package com.gregguns.core.config;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import com.gregguns.core.module.ModuleId;
import com.gregguns.icbm.IcbmAvailability;

/**
 * Reads the {@code modules.*} switches. Kept apart from {@link ModulesConfig} so that class stays fields-only. The
 * {@code icbm} switch reads as off when Galacticraft is not loaded, even if the config says on (D9).
 */
public final class ModuleFlags {

    private ModuleFlags() {}

    /**
     * @param id a module
     * @return whether its flag is on
     * @throws IllegalStateException if {@link ModulesConfig} has no static boolean field named after the module (the
     *                               unit tests guard against this)
     */
    public static boolean isEnabled(ModuleId id) {
        try {
            Field field = ModulesConfig.class.getField(id.configKey());
            if (field.getType() != boolean.class || !Modifier.isStatic(field.getModifiers())) {
                throw new IllegalStateException("modules." + id.configKey() + " is not a static boolean");
            }
            boolean flag = field.getBoolean(null);
            // The ICBM module also needs Galacticraft (D9). Short-circuit: with the flag off nothing is probed, so unit
            // tests and the default config never touch Forge's Loader.
            if (flag && id == ModuleId.ICBM) {
                return IcbmAvailability.galacticraftPresent();
            }
            return flag;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException("No readable config flag modules." + id.configKey(), e);
        }
    }
}
