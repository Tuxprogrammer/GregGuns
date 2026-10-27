package com.gregguns.icbm.production.gc;

import java.util.Collections;
import java.util.List;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.StatCollector;

import com.gregguns.icbm.production.MissileBenchLayout;
import com.gregguns.icbm.production.MissileBenchRules;
import com.gregguns.icbm.production.MissileBuildSpec;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Draws the schematic of a missile page: a rocket silhouette (nose, body, fins, engine bell) behind a frame for every
 * slot, in the style of Galacticraft's own rocket workbench pages. Plain rectangles, no texture, so it needs nothing
 * from Galacticraft's jar. Shared by the in-game page and the NEI page. Client only.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/build-and-recipes.md (NASA Workbench page)
@SideOnly(Side.CLIENT)
public final class MissileBenchArt {

    private static final int EDGE = 0xFF373737;
    private static final int SLOT = 0xFF8B8B8B;
    private static final int HULL = 0xFFA8A8B4;
    private static final int HULL_EDGE = 0xFF6E6E7C;
    private static final int SEAT = 0xFFD8B84A;

    private MissileBenchArt() {}

    /**
     * Draws the silhouette and every slot frame, with (ox, oy) as the panel's top-left.
     *
     * @param spec     the page
     * @param ox       panel x
     * @param oy       panel y
     * @param withSlot whether to frame the result slot too
     */
    public static void draw(MissileBuildSpec spec, int ox, int oy, boolean withSlot) {
        int cx = ox + MissileBenchLayout.CENTER;
        int base = oy + MissileBenchLayout.baseY(spec);
        int engine = oy + MissileBenchLayout.engineY(spec);
        int bodyTop = oy + MissileBenchLayout.PLATES_TOP - 2;
        // nose, in steps
        hull(cx - 5, oy + 2, cx + 5, oy + 8);
        hull(cx - 10, oy + 8, cx + 10, oy + 16);
        hull(cx - 15, oy + 16, cx + 15, bodyTop);
        // body
        hull(cx - 14, bodyTop, cx + 14, base + 2);
        // fins: a swept wing either side, reaching from the body to the outer fin slots
        hull(cx - 66, base - 2, cx - 14, base + 20);
        hull(cx - 34, base - 18, cx - 14, base - 2);
        hull(cx + 14, base - 2, cx + 66, base + 20);
        hull(cx + 14, base - 18, cx + 34, base - 2);
        // engine bell
        hull(cx - 14, base + 2, cx + 14, engine + 18);
        hull(cx - 18, engine + 18, cx + 18, engine + 22);
        // the warhead seat: a gold frame, no slot (the warhead is stacked on the pad)
        int sx = ox + MissileBenchLayout.seatX();
        int sy = oy + MissileBenchLayout.seatY();
        Gui.drawRect(sx - 1, sy - 1, sx + 17, sy + 17, SEAT);
        Gui.drawRect(sx, sy, sx + 16, sy + 16, HULL);
        for (int i = 0; i < MissileBenchRules.plateSlots(spec); i++) {
            frame(ox + MissileBenchLayout.plateX(i), oy + MissileBenchLayout.plateY(i));
        }
        for (int i = 0; i < MissileBenchRules.FIN_SLOTS; i++) {
            frame(ox + MissileBenchLayout.finX(i), oy + MissileBenchLayout.finY(spec));
        }
        frame(ox + MissileBenchLayout.engineX(), oy + MissileBenchLayout.engineY(spec));
        frame(ox + MissileBenchLayout.guidanceX(), oy + MissileBenchLayout.guidanceY());
        if (withSlot) {
            frame(ox + MissileBenchLayout.resultX(), oy + MissileBenchLayout.resultY(spec));
        }
    }

    /** Draws the small captions in their boxes (see {@link MissileBenchLayout#captions}), clear of every slot. */
    public static void drawLabels(FontRenderer font, MissileBuildSpec spec, int ox, int oy) {
        String[] keys = { "warhead", "guidance", "fins", "engine" };
        int[][] boxes = MissileBenchLayout.captions(spec);
        for (int i = 0; i < keys.length; i++) {
            String text = StatCollector.translateToLocal("gregguns.icbm.bench." + keys[i]);
            // A box taller than one line wraps its caption to the box width; the other captions fit on one line.
            @SuppressWarnings("unchecked")
            List<String> lines = boxes[i][3] > MissileBenchLayout.CAPTION_HEIGHT
                ? font.listFormattedStringToWidth(text, boxes[i][2])
                : Collections.singletonList(text);
            for (int l = 0; l < lines.size(); l++) {
                String line = lines.get(l);
                int x = ox + boxes[i][0];
                if (MissileBenchLayout.captionRightAligned(i)) {
                    x += boxes[i][2] - font.getStringWidth(line);
                }
                font.drawString(line, x, oy + boxes[i][1] + l * MissileBenchLayout.CAPTION_HEIGHT, 0x404040);
            }
        }
    }

    private static void hull(int x0, int y0, int x1, int y1) {
        Gui.drawRect(x0, y0, x1, y1, HULL_EDGE);
        Gui.drawRect(x0 + 1, y0 + 1, x1 - 1, y1 - 1, HULL);
    }

    private static void frame(int x, int y) {
        Gui.drawRect(x - 1, y - 1, x + 17, y + 17, EDGE);
        Gui.drawRect(x, y, x + 16, y + 16, SLOT);
    }
}
