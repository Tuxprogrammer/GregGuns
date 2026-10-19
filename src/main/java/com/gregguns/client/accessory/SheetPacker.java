package com.gregguns.client.accessory;

import java.util.ArrayList;
import java.util.List;

/**
 * Shelf-packs the box unwraps of a model onto its texture sheet, tallest first, left to right, so a model author lists
 * boxes and never computes texture origins. Deterministic. Pure.
 */
final class SheetPacker {

    private SheetPacker() {}

    /**
     * Assigns {@code (u, v)} to every box.
     *
     * @param boxes  the boxes, in any order
     * @param width  sheet width in texels
     * @param height sheet height in texels
     * @throws IllegalStateException if the unwraps do not fit
     */
    static void pack(List<AccessoryBox> boxes, int width, int height) {
        List<AccessoryBox> order = new ArrayList<>(boxes);
        // Stable sort: equal heights keep the author's order.
        order.sort((a, b) -> Integer.compare(b.unwrapHeight(), a.unwrapHeight()));
        int x = 0;
        int y = 0;
        int rowHeight = 0;
        for (AccessoryBox box : order) {
            if (x + box.unwrapWidth() > width) {
                x = 0;
                y += rowHeight;
                rowHeight = 0;
            }
            if (y + box.unwrapHeight() > height || box.unwrapWidth() > width) {
                throw new IllegalStateException(
                    "box " + box.name() + " does not fit on a " + width + "x" + height + " sheet");
            }
            box.place(x, y);
            x += box.unwrapWidth();
            rowHeight = Math.max(rowHeight, box.unwrapHeight());
        }
    }
}
