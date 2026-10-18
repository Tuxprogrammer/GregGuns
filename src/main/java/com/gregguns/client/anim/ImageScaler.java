package com.gregguns.client.anim;

/**
 * Pure image arithmetic for loading oversized overlay textures at a sane size: the target size that fits a limit and a
 * box-filter downsample over packed ARGB pixels. No Minecraft or AWT types, so tests can drive it.
 *
 * <p>
 * The Techguns-era scope overlays are 2048 or 3072 pixels square, far more than a screen needs; the client shrinks them
 * to at most {@link #SCOPE_MAX_SIZE} pixels when it loads them. The PNG files on disk are never changed.
 */
public final class ImageScaler {

    /** Largest side, in pixels, of a scope overlay held on the GPU. */
    public static final int SCOPE_MAX_SIZE = 1024;

    private ImageScaler() {}

    /**
     * The size an image should be loaded at: unchanged if it fits, else scaled down to make its longer side
     * {@code max}, keeping the aspect ratio (each side at least 1).
     *
     * @param width  source width, at least 1
     * @param height source height, at least 1
     * @param max    the longest side allowed, at least 1
     * @return width and height to load at
     */
    public static int[] fit(int width, int height, int max) {
        if (width < 1 || height < 1 || max < 1) {
            throw new IllegalArgumentException("sizes must be positive");
        }
        if (width <= max && height <= max) {
            return new int[] { width, height };
        }
        double scale = (double) max / Math.max(width, height);
        return new int[] { Math.max(1, (int) Math.round(width * scale)),
            Math.max(1, (int) Math.round(height * scale)) };
    }

    /**
     * Shrinks an image with a box filter. Colors are averaged weighted by alpha (premultiplied), so a transparent
     * region of the overlay does not bleed its hidden color into the edge of an opaque one.
     *
     * @param argb   source pixels as 0xAARRGGBB, row by row, length {@code width * height}
     * @param width  source width
     * @param height source height
     * @param tw     target width, at most {@code width}
     * @param th     target height, at most {@code height}
     * @return the target pixels, same format
     */
    public static int[] downsample(int[] argb, int width, int height, int tw, int th) {
        if (argb.length != width * height || tw < 1 || th < 1 || tw > width || th > height) {
            throw new IllegalArgumentException("bad sizes");
        }
        int[] out = new int[tw * th];
        for (int ty = 0; ty < th; ty++) {
            int y0 = (int) ((long) ty * height / th);
            int y1 = Math.max(y0 + 1, (int) ((long) (ty + 1) * height / th));
            for (int tx = 0; tx < tw; tx++) {
                int x0 = (int) ((long) tx * width / tw);
                int x1 = Math.max(x0 + 1, (int) ((long) (tx + 1) * width / tw));
                long a = 0;
                long r = 0;
                long g = 0;
                long b = 0;
                for (int y = y0; y < y1; y++) {
                    int row = y * width;
                    for (int x = x0; x < x1; x++) {
                        int p = argb[row + x];
                        int pa = p >>> 24;
                        a += pa;
                        r += (long) ((p >> 16) & 0xFF) * pa;
                        g += (long) ((p >> 8) & 0xFF) * pa;
                        b += (long) (p & 0xFF) * pa;
                    }
                }
                long n = (long) (y1 - y0) * (x1 - x0);
                int oa = (int) ((a + n / 2) / n);
                int or = a == 0 ? 0 : (int) ((r + a / 2) / a);
                int og = a == 0 ? 0 : (int) ((g + a / 2) / a);
                int ob = a == 0 ? 0 : (int) ((b + a / 2) / a);
                out[ty * tw + tx] = oa << 24 | or << 16 | og << 8 | ob;
            }
        }
        return out;
    }
}
