package com.gregguns.machine;

import com.gregguns.GregGuns;

import gregtech.api.enums.Textures;

/**
 * Requests machine overlay icons while GT can still stitch them.
 *
 * <p>
 * GT queues an icon container's {@code run()} (which fetches the sprite) when the container is first created, and
 * drains that queue at texture stitch. A container created later, for example inside a {@code getTexture} call, keeps a
 * null icon and the machine's item or block render throws a NullPointerException. Controllers (which, unlike basic
 * machines, build no textures in their constructor) call this from their registration. Common code; the icons are
 * only dereferenced on the client.
 */
// Source:
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/client/iconContainers/blocks/GTCustomOptionalBlockIconContainer.java#L25-L40
// (create caches by name; run() fills mIcon and is queued only at creation)
public final class OverlayIcons {

    private OverlayIcons() {}

    /**
     * @param dir   the texture folder under {@code textures/blocks/}, with a trailing slash
     * @param files the overlay file names without extension
     */
    public static void preload(String dir, String... files) {
        for (String file : files) {
            Textures.BlockIcons.customOptional(GregGuns.MODID, dir + file);
        }
    }
}
