package com.gregguns.icbm.control;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.gregguns.icbm.control.gc.LaunchPads;
import com.gregguns.icbm.missile.LaunchSite;
import com.gregguns.icbm.missile.MissileStatus;
import com.gregguns.icbm.missile.PadMissiles;
import com.gregguns.icbm.silo.MTEMissileSilo;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/**
 * Finds what a Launch Control fires (D285). There is no search for silos and no election: a Launch Control built into
 * a Missile Silo's structure is bound to that silo by the silo's own structure check
 * ({@link MTELaunchControl#bindSilo}), and fires that silo only. A Launch Control that is not part of a silo fires a
 * Galacticraft pad it touches ({@link PadAdjacency}, Galacticraft's own attach rule); with several, the one
 * {@link SiteChoice} prefers. A formed silo's shaft floor is a pad too, but it is never a bare pad: only the silo's
 * own Launch Control fires it (D70). Called only after the ICBM module gate. Server side.
 */
// Spec: docs/gregguns-icbm-spec/04-launch-control/launch-control-block.md (Finding the pad or the silo)
final class LaunchSites {

    /** What a Launch Control is bound to right now. */
    static final class Found {

        /** Bound to nothing: no silo, no touching pad. */
        static final Found NOTHING = new Found(null, SiteLine.NONE, false);

        /** The site to fire, or null. */
        final LaunchSite site;
        /** The screen's site line. */
        final SiteLine line;
        /** Bound to a silo that is not formed right now: nothing fires until it is. */
        final boolean siloUnformed;

        Found(LaunchSite site, SiteLine line, boolean siloUnformed) {
            this.site = site;
            this.line = line;
            this.siloUnformed = siloUnformed;
        }
    }

    private LaunchSites() {}

    /**
     * @param control the Launch Control
     * @param world   its world
     * @param x       X of the launch-control block
     * @param y       Y
     * @param z       Z
     * @return its silo when it is part of one, else the touching pad it fires, else {@link Found#NOTHING}
     */
    static Found find(MTELaunchControl control, World world, int x, int y, int z) {
        MTEMissileSilo silo = control.boundSilo();
        if (silo == null && control.hasSilo()) {
            // Bound, but the silo's chunk is not loaded: it is not a free block, so it fires nothing.
            int[] p = control.siloPos();
            return new Found(null, new SiteLine(SiteLine.Kind.SILO, p[0], p[2], ""), true);
        }
        if (silo != null) {
            LaunchSite site = silo.site();
            SiteLine line = new SiteLine(
                SiteLine.Kind.SILO,
                (int) Math.floor(site.centerX()),
                (int) Math.floor(site.centerZ()),
                silo.signName());
            return silo.isFormed() ? new Found(site, line, false) : new Found(null, line, true);
        }
        return pad(world, x, y, z);
    }

    private static Found pad(World world, int x, int y, int z) {
        List<LaunchSite> sites = new ArrayList<>(2);
        List<int[]> centers = new ArrayList<>(2);
        List<Integer> ranks = new ArrayList<>(2);
        for (int[] c : LaunchPads.touchingPadCenters(world, x, y, z)) {
            if (MTEMissileSilo.isFloorOf(world, c[0], c[1], c[2])) {
                continue; // a formed silo's floor: only its own Launch Control fires it
            }
            LaunchSite s = PadMissiles.site(world, c[0], c[1], c[2]);
            MissileStatus ms = s.status();
            sites.add(s);
            centers.add(c);
            ranks.add(SiteChoice.rank(s.countdownTicks() >= 0 || s.ignited(), ms.present(), ms.armed(), ms.fueled()));
        }
        int i = SiteChoice.best(ranks);
        if (i < 0) {
            return Found.NOTHING;
        }
        int[] c = centers.get(i);
        return new Found(sites.get(i), new SiteLine(SiteLine.Kind.PAD, c[0], c[2], ""), false);
    }

    /**
     * @param world a world
     * @param x     X
     * @param y     Y
     * @param z     Z
     * @return the Missile Silo controller there, or null
     */
    static MTEMissileSilo siloAt(World world, int x, int y, int z) {
        if (y < 0 || y > 255 || !world.blockExists(x, y, z)) {
            return null;
        }
        TileEntity t = world.getTileEntity(x, y, z);
        return t instanceof IGregTechTileEntity
            && ((IGregTechTileEntity) t).getMetaTileEntity() instanceof MTEMissileSilo
                ? (MTEMissileSilo) ((IGregTechTileEntity) t).getMetaTileEntity()
                : null;
    }
}
