package com.gregguns.core.selfcheck;

import java.io.IOException;

/**
 * Read access to the mod's resources by classpath path, so the self-check works the same on a dedicated server (where
 * the resource manager does not load mod assets) and in unit tests (where a fake supplies the files).
 *
 * <p>
 * Both sides.
 */
public interface ResourceSource {

    /**
     * @param path classpath path without a leading slash, for example {@code "assets/gregguns/sounds.json"}
     * @return whether the resource exists
     */
    boolean exists(String path);

    /**
     * @param path classpath path without a leading slash
     * @return the resource decoded as UTF-8
     * @throws IOException if the resource is missing or unreadable
     */
    String readText(String path) throws IOException;
}
