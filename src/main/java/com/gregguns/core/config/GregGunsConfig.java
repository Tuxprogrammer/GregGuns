package com.gregguns.core.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;

/**
 * Lists and registers GregGuns' {@code @Config} classes. All of them share {@code config/gregguns.cfg}.
 *
 * <p>
 * Adding a config class: create it with {@code @Config(modid = "gregguns", category = "...")}, add it to
 * {@link #CLASSES}, and document every key in {@code docs/config.md}. For a field the client reads or that gates server
 * behaviour, annotate it {@code @Config.Sync} (GGS-01-03); the tests then require it to be listed as expected.
 */
// Spec: docs/gregguns-spec/01-core/config-gtnhlib.md (GGS-01-01)
public final class GregGunsConfig {

    /** Every config class, in registration order. */
    public static final List<Class<?>> CLASSES = Collections
        .unmodifiableList(Arrays.<Class<?>>asList(ModulesConfig.class, DebugConfig.class, ClientConfig.class));

    private GregGunsConfig() {}

    /** Loads (and on first run creates) the config file. Call in preInit before anything reads a config field. */
    public static void register() {
        for (Class<?> configClass : CLASSES) {
            // Source:
            // https://github.com/GTNewHorizons/GTNHLib/blob/0.11.52/src/main/java/com/gtnewhorizon/gtnhlib/config/ConfigurationManager.java#L66
            ConfigurationManager.registerConfig(configClass);
        }
    }
}
