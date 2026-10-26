package com.gregguns.icbm.production;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class MissileBenchLayoutTest {

    private static List<int[]> slots(MissileBuildSpec s) {
        List<int[]> out = new ArrayList<>();
        for (int i = 0; i < MissileBenchRules.plateSlots(s); i++) {
            out.add(new int[] { MissileBenchLayout.plateX(i), MissileBenchLayout.plateY(i) });
        }
        for (int i = 0; i < MissileBenchRules.FIN_SLOTS; i++) {
            out.add(new int[] { MissileBenchLayout.finX(i), MissileBenchLayout.finY(s) });
        }
        out.add(new int[] { MissileBenchLayout.engineX(), MissileBenchLayout.engineY(s) });
        out.add(new int[] { MissileBenchLayout.guidanceX(), MissileBenchLayout.guidanceY() });
        out.add(new int[] { MissileBenchLayout.resultX(), MissileBenchLayout.resultY(s) });
        out.add(new int[] { MissileBenchLayout.seatX(), MissileBenchLayout.seatY() });
        return out;
    }

    @Test
    void slotsNeverOverlapAndStayInThePanel() {
        for (MissileBuildSpec s : MissileBuildSpec.values()) {
            List<int[]> all = slots(s);
            for (int i = 0; i < all.size(); i++) {
                int[] a = all.get(i);
                assertTrue(a[0] >= 0 && a[0] + 16 <= MissileBenchLayout.WIDTH, s + " slot " + i + " x");
                assertTrue(a[1] >= 0 && a[1] + 16 <= MissileBenchLayout.HEIGHT, s + " slot " + i + " y");
                for (int j = i + 1; j < all.size(); j++) {
                    int[] b = all.get(j);
                    boolean apart = a[0] + 16 <= b[0] || b[0] + 16 <= a[0] || a[1] + 16 <= b[1] || b[1] + 16 <= a[1];
                    assertTrue(apart, s + " slots " + i + " and " + j + " overlap");
                }
            }
        }
    }

    @Test
    void theMissileIsSymmetricAboutItsCenterLine() {
        assertEquals(MissileBenchLayout.CENTER, MissileBenchLayout.engineX() + 8, "engine on the center line");
        assertEquals(MissileBenchLayout.CENTER, MissileBenchLayout.seatX() + 8);
        assertEquals(
            2 * MissileBenchLayout.CENTER,
            MissileBenchLayout.finX(0) + MissileBenchLayout.finX(3) + 16,
            "outer fins mirror");
        assertEquals(
            2 * MissileBenchLayout.CENTER,
            MissileBenchLayout.finX(1) + MissileBenchLayout.finX(2) + 16,
            "inner fins mirror");
    }

    @Test
    void fromTopToBottomSeatPlatesFinsEngine() {
        for (MissileBuildSpec s : MissileBuildSpec.values()) {
            assertTrue(MissileBenchLayout.seatY() + 16 <= MissileBenchLayout.PLATES_TOP);
            assertTrue(
                MissileBenchLayout.baseY(s) >= MissileBenchLayout.plateY(MissileBenchRules.plateSlots(s) - 1) + 16);
            assertTrue(MissileBenchLayout.engineY(s) >= MissileBenchLayout.finY(s) + 16);
        }
    }

    private static boolean apart(int[] a, int aw, int ah, int[] b, int bw, int bh) {
        return a[0] + aw <= b[0] || b[0] + bw <= a[0] || a[1] + ah <= b[1] || b[1] + bh <= a[1];
    }

    @Test
    void captionsNeverTouchASlotOrEachOtherAndStayOnThePage() {
        for (MissileBuildSpec s : MissileBuildSpec.values()) {
            int[][] caps = MissileBenchLayout.captions(s);
            List<int[]> all = slots(s);
            for (int i = 0; i < caps.length; i++) {
                int[] c = caps[i];
                assertTrue(c[0] >= 0 && c[2] > 0, s + " caption " + i + " x");
                assertTrue(c[1] >= 0 && c[1] + c[3] <= MissileBenchLayout.HEIGHT + 24, s + " caption " + i + " y");
                for (int[] slot : all) {
                    // one pixel of frame around a slot counts as the slot
                    assertTrue(
                        apart(c, c[2], c[3], new int[] { slot[0] - 1, slot[1] - 1 }, 18, 18),
                        s + " caption " + i + " overlaps a slot");
                }
                for (int j = i + 1; j < caps.length; j++) {
                    assertTrue(apart(c, c[2], c[3], caps[j], caps[j][2], caps[j][3]), s + " captions " + i + "/" + j);
                }
            }
        }
    }
}
