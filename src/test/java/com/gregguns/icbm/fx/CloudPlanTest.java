package com.gregguns.icbm.fx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import net.minecraft.network.PacketBuffer;

import org.junit.jupiter.api.Test;

import io.netty.buffer.Unpooled;

/** The blast cloud numbers: bounded rates, shapes, the bounded registry and the packet's size. */
class CloudPlanTest {

    @Test
    void particleRatesStayInsideTheirBoundsForEveryKindAndRadius() {
        for (CloudKind k : CloudKind.values()) {
            for (int r = 1; r <= 255; r++) {
                int n = CloudPlan.perTick(k, r);
                assertTrue(n >= CloudPlan.MIN_PER_TICK && n <= CloudPlan.MAX_PER_TICK, k + " r" + r + " -> " + n);
            }
        }
    }

    @Test
    void aBiggerCloudNeverSpawnsFewer() {
        for (CloudKind k : CloudKind.values()) {
            int last = 0;
            for (int r = 1; r <= 64; r++) {
                int n = CloudPlan.perTick(k, r);
                assertTrue(n >= last);
                last = n;
            }
        }
        assertTrue(CloudPlan.perTick(CloudKind.SMOKE, 8) > CloudPlan.perTick(CloudKind.CHEMICAL, 8));
    }

    @Test
    void worstCaseLoadIsBoundedByTheCapsAlone() {
        int worst = CloudPlan.MAX_ACTIVE * CloudPlan.MAX_PER_TICK;
        assertTrue(worst <= 100, "particles per client tick across every cloud: " + worst);
    }

    @Test
    void everyGasKindHasItsOwnColorAndTheyAreNotRealAgentsTints() {
        java.util.Set<String> colors = new java.util.HashSet<>();
        for (CloudKind k : CloudKind.values()) {
            assertTrue(
                k.red() >= 0 && k.red() <= 1 && k.green() >= 0 && k.green() <= 1 && k.blue() >= 0 && k.blue() <= 1);
            assertTrue(colors.add(k.red() + "," + k.green() + "," + k.blue()), k + " repeats a color");
        }
        assertEquals(5, CloudKind.values().length);
        assertFalse(CloudKind.PLUME.isGas());
        assertNull(CloudKind.byId(-1));
        assertNull(CloudKind.byId(99));
        assertEquals(CloudKind.SMOKE, CloudKind.byId(CloudKind.SMOKE.ordinal()));
    }

    @Test
    void gasParticlesFillTheSphereAndNoMore() {
        Random rnd = new Random(7);
        double[] o = new double[3];
        int inner = 0;
        int radius = 12;
        for (int i = 0; i < 4000; i++) {
            CloudPlan.sample(CloudKind.CHEMICAL, radius, 0, 600, rnd, o);
            double d = Math.sqrt(o[0] * o[0] + o[1] * o[1] + o[2] * o[2]);
            assertTrue(d <= radius + 1e-9);
            if (d < radius / 2.0) {
                inner++;
            }
        }
        // A uniform sphere puts 1/8 of its points inside half the radius: the cloud is filled, not a shell.
        assertTrue(inner > 4000 / 8 * 0.7 && inner < 4000 / 8 * 1.3, "inner " + inner);
    }

    @Test
    void thePlumeRisesAndWidensWithAge() {
        Random rnd = new Random(3);
        double[] o = new double[3];
        double earlyTop = 0;
        double lateTop = 0;
        for (int i = 0; i < 2000; i++) {
            CloudPlan.sample(CloudKind.PLUME, 10, 0, 200, rnd, o);
            assertTrue(o[1] >= 0);
            earlyTop = Math.max(earlyTop, o[1]);
            CloudPlan.sample(CloudKind.PLUME, 10, 200, 200, rnd, o);
            lateTop = Math.max(lateTop, o[1]);
        }
        assertTrue(lateTop > earlyTop * 2.0, earlyTop + " vs " + lateTop);
    }

    @Test
    void plumeLengthScalesWithRadiusUpToACap() {
        assertTrue(CloudPlan.duration(CloudKind.PLUME, 8, 0) < CloudPlan.duration(CloudKind.PLUME, 16, 0));
        assertEquals(CloudPlan.PLUME_MAX_TICKS, CloudPlan.duration(CloudKind.PLUME, 255, 0));
        assertEquals(600, CloudPlan.duration(CloudKind.SMOKE, 8, 600));
    }

    @Test
    void aGasBlastWithNoCraterGetsNoPlume() {
        assertFalse(CloudPlan.wantsPlume(80, false, false), "a shape-NONE blast has its own cloud");
    }

    @Test
    void onlyLargeConventionalBlastsGetAPlume() {
        assertFalse(CloudPlan.wantsPlume(CloudPlan.PLUME_MIN_RADIUS - 1, false, true));
        assertTrue(CloudPlan.wantsPlume(CloudPlan.PLUME_MIN_RADIUS, false, true));
        assertFalse(CloudPlan.wantsPlume(80, true, true), "an annihilating blast keeps the long-range flash instead");
    }

    @Test
    void aGasCloudCostsAFewPacketsPerPlayer() {
        assertEquals(1, CloudPlan.packetsPerPlayer(1));
        assertEquals(1, CloudPlan.packetsPerPlayer(100));
        assertEquals(2, CloudPlan.packetsPerPlayer(101));
        assertTrue(CloudPlan.packetsPerPlayer(600) <= 7);
        assertTrue(CloudPlan.packetsPerPlayer(600) * PacketBlastCloud.WIRE_BYTES < 200, "bytes per player per cloud");
    }

    @Test
    void theRegistryIsBoundedRefreshesAndExpires() {
        ActiveClouds a = new ActiveClouds();
        for (int i = 0; i < 20; i++) {
            a.add(CloudKind.CHEMICAL, i * 100, 64, 0, 8, 600, 600);
        }
        assertEquals(CloudPlan.MAX_ACTIVE, a.size());
        // the oldest were replaced: the survivors are the last ones added
        assertEquals(
            1900.0,
            a.clouds()
                .get(CloudPlan.MAX_ACTIVE - 1).x);
        a.clear();
        a.add(CloudKind.SMOKE, 5, 5, 5, 8, 600, 100);
        a.add(CloudKind.SMOKE, 5.4, 5, 5, 8, 600, 500);
        assertEquals(1, a.size(), "a re-announcement refreshes the cloud at that place");
        assertEquals(
            500,
            a.clouds()
                .get(0).ticksLeft);
        a.add(CloudKind.CONTAGIOUS, 5, 5, 5, 8, 600, 100);
        assertEquals(2, a.size(), "another kind at the same place is another cloud");
        for (int i = 0; i < 100; i++) {
            a.tick();
        }
        assertEquals(1, a.size());
    }

    @Test
    void thePacketIsNineteenBytesAndRoundTrips() throws Exception {
        PacketBlastCloud p = new PacketBlastCloud(CloudKind.DEBILITATION, 10.5, 64, -3.25, 14, 600, 450);
        PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
        p.encode(buf);
        assertEquals(PacketBlastCloud.WIRE_BYTES, buf.readableBytes());
        assertEquals(19, PacketBlastCloud.WIRE_BYTES);
        PacketBlastCloud q = new PacketBlastCloud();
        q.decode(buf);
        assertEquals(CloudKind.DEBILITATION.ordinal(), q.kindId());
        assertEquals(14, q.radius());
        assertEquals(450, q.left());
    }
}
