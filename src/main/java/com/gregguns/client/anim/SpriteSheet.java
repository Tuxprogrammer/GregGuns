package com.gregguns.client.anim;

/**
 * Frame arithmetic for an FX sprite sheet: a grid of equal cells read left to right, top to bottom, of which the
 * first {@code frames} are used.
 *
 * <p>
 * Spec: rendering-framework.md section 7 (frame = floor(frames x progress); nothing is drawn once frame >= frames).
 */
public final class SpriteSheet {

    private final int columns;
    private final int rows;
    private final int frames;

    /**
     * @param columns grid columns, at least 1
     * @param rows    grid rows, at least 1
     * @param frames  used cells, from 1 to {@code columns * rows}
     */
    public SpriteSheet(int columns, int rows, int frames) {
        if (columns < 1 || rows < 1 || frames < 1 || frames > columns * rows) {
            throw new IllegalArgumentException("bad sheet " + columns + "x" + rows + " with " + frames + " frames");
        }
        this.columns = columns;
        this.rows = rows;
        this.frames = frames;
    }

    /** @return the frame to show at {@code progress}, or -1 if progress is outside [0, 1) */
    public int frameAt(double progress) {
        if (progress < 0.0 || progress >= 1.0) {
            return -1;
        }
        int frame = (int) Math.floor(frames * progress);
        return frame >= frames ? -1 : frame;
    }

    /** @return left texture coordinate of a frame, 0 to 1 */
    public double u0(int frame) {
        return (frame % columns) / (double) columns;
    }

    /** @return right texture coordinate of a frame */
    public double u1(int frame) {
        return (frame % columns + 1) / (double) columns;
    }

    /** @return top texture coordinate of a frame */
    public double v0(int frame) {
        return (frame / columns) / (double) rows;
    }

    /** @return bottom texture coordinate of a frame */
    public double v1(int frame) {
        return (frame / columns + 1) / (double) rows;
    }

    /** @return number of used frames */
    public int frames() {
        return frames;
    }
}
