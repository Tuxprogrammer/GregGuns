package com.gregguns.icbm.fx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.network.PacketBuffer;

import org.junit.jupiter.api.Test;

import io.netty.buffer.Unpooled;

/** The annihilation orbs: bounded numbers, the swell and fade curves and the packet round trip (I5). */
class OrbPlanTest {

    @Test
    void aFlashSwellsToItsRadiusAndFadesOut() {
        double r0 = OrbPlan.drawRadius(OrbKind.ANTIMATTER, 80, 0, 60);
        double r30 = OrbPlan.drawRadius(OrbKind.ANTIMATTER, 80, 30, 60);
        double r60 = OrbPlan.drawRadius(OrbKind.ANTIMATTER, 80, 60, 60);
        assertEquals(0.0, r0, 1e-9);
        assertTrue(r30 > 40.0 && r30 < 80.0, "ease-out: past the halfway radius at half time");
        assertEquals(80.0, r60, 1e-9);
        assertTrue(OrbPlan.alpha(OrbKind.ANTIMATTER, 0, 60) > OrbPlan.alpha(OrbKind.ANTIMATTER, 50, 60));
        assertEquals(0.0F, OrbPlan.alpha(OrbKind.ANTIMATTER, 60, 60), 0.0F);
    }

    @Test
    void aBodyShowsItsRadiusAtOnceAndFadesOnlyAtTheEnd() {
        assertEquals(12.0, OrbPlan.drawRadius(OrbKind.RED_MATTER, 12, 0, 1200), 0.0);
        assertEquals(
            OrbPlan.alpha(OrbKind.RED_MATTER, 0, 1200),
            OrbPlan.alpha(OrbKind.RED_MATTER, 1000, 1200),
            0.0F,
            "steady until the last second");
        assertTrue(OrbPlan.alpha(OrbKind.RED_MATTER, 1190, 1200) < OrbPlan.alpha(OrbKind.RED_MATTER, 1000, 1200));
        assertEquals(0.0F, OrbPlan.alpha(OrbKind.RED_MATTER, 1200, 1200), 0.0F);
    }

    @Test
    void aBodyIsAnnouncedOnAShortLease() {
        assertEquals(40, OrbPlan.lease(12_000, 20));
        assertEquals(15, OrbPlan.lease(15, 20));
        assertEquals(1, OrbPlan.lease(0, 20));
        // Refreshed every 20 ticks, the client's time left never drops below 20, so the alpha stays steady.
        assertEquals(0.9F, OrbPlan.alpha(OrbKind.RED_MATTER, 20, OrbPlan.lease(12_000, 20)), 0.0F);
    }

    @Test
    void theCostBoundsAreSmall() {
        assertTrue(OrbPlan.MAX_ACTIVE <= 4);
        assertTrue(OrbPlan.SEGMENTS <= 32);
    }

    @Test
    void idsRoundTripAndRejectOutOfRange() {
        assertNull(OrbKind.byId(-1));
        assertNull(OrbKind.byId(2));
        assertEquals(OrbKind.RED_MATTER, OrbKind.byId(OrbKind.RED_MATTER.ordinal()));
    }

    @Test
    void thePacketRoundTrips() throws Exception {
        PacketBlastOrb p = new PacketBlastOrb(OrbKind.RED_MATTER, 10.5, 64, -3.25, 32, 1200);
        PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
        p.encode(buf);
        assertEquals(17, buf.readableBytes());
        PacketBlastOrb q = new PacketBlastOrb();
        q.decode(buf);
        assertEquals(OrbKind.RED_MATTER.ordinal(), q.kindId());
        assertEquals(32, q.radius());
        assertEquals(1200, q.life());
    }
}
