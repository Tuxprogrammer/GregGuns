package com.gregguns.core.config;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import com.gregguns.core.module.ModuleId;

/**
 * Reads the {@code modules.*} switches. Kept apart from {@link ModulesConfig} so that class stays fields-only.
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
            return field.getBoolean(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException("No readable config flag modules." + id.configKey(), e);
        }
    }
}
