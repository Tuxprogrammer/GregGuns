package com.gregguns.icbm.worldgen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.gregguns.GregGuns;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicMachine;

/**
 * The machine facing audit: a GT machine a complex places must face one of north, south, east or west, unless its
 * meta tile entity allows no horizontal side. A basic machine (a pump) must also have a horizontal main facing. It
 * runs on every machine the writer places ({@link #check}, with the code path that placed it) and reads placed
 * machines back from a world ({@link #scan}). A violation is one WARN line and counts in {@link #total()}. Separate
 * from the role audit, which checks blocks. Server side; {@link #problem} is plain Java.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/palette.md (Machine facing)
public final class MachineFacingAudit {

    private static final AtomicInteger TOTAL = new AtomicInteger();
    private static final AtomicInteger CHECKED = new AtomicInteger();
    private static final List<String> RECENT = Collections.synchronizedList(new ArrayList<String>());
    private static final int MAX_RECENT = 64;

    private MachineFacingAudit() {}

    /** @return the violations since the server started */
    public static int total() {
        return TOTAL.get();
    }

    /** @return the machines checked since the server started */
    public static int checked() {
        return CHECKED.get();
    }

    /** @return the last violation lines (at most 64), oldest first */
    public static List<String> recent() {
        synchronized (RECENT) {
            return new ArrayList<>(RECENT);
        }
    }

    /**
     * @param front the machine's front facing
     * @param main  a basic machine's main facing, or null for a machine without one
     * @param valid whether the machine allows a facing
     * @return what is wrong, or null when the machine faces a side it should
     */
    public static String problem(ForgeDirection front, ForgeDirection main, Predicate<ForgeDirection> valid) {
        boolean horizontalPossible = false;
        for (ForgeDirection d : new ForgeDirection[] { ForgeDirection.NORTH, ForgeDirection.EAST, ForgeDirection.SOUTH,
            ForgeDirection.WEST }) {
            horizontalPossible |= valid.test(d);
        }
        if (main != null && !horizontal(main)) return "its main facing is " + main;
        if (!horizontal(front) && horizontalPossible) return "it faces " + front + " though a horizontal side is valid";
        return null;
    }

    private static boolean horizontal(ForgeDirection d) {
        return d != ForgeDirection.UP && d != ForgeDirection.DOWN && d != ForgeDirection.UNKNOWN;
    }

    /**
     * Checks one placed machine and reports a violation.
     *
     * @param tile the machine's tile
     * @param path the code path that placed it, for the report
     * @return true when the machine is fine
     */
    public static boolean check(IGregTechTileEntity tile, String path, int x, int y, int z) {
        CHECKED.incrementAndGet();
        gregtech.api.interfaces.metatileentity.IMetaTileEntity mte = tile.getMetaTileEntity();
        if (mte == null) return true;
        ForgeDirection main = mte instanceof MTEBasicMachine ? ((MTEBasicMachine) mte).mMainFacing : null;
        String why = problem(tile.getFrontFacing(), main, mte::isFacingValid);
        if (why == null) return true;
        report(
            "Silo complex machine facing: " + mte
                .getMetaName() + " at " + x + "," + y + "," + z + ": " + why + "; placed by " + path);
        return false;
    }

    /**
     * Reads every GT machine in a box of a world back and checks it.
     *
     * @return the violation lines of this scan
     */
    public static List<String> scan(World world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ,
        int[] machines) {
        List<String> out = new ArrayList<>();
        List<?> all = new ArrayList<Object>(world.loadedTileEntityList);
        for (Object o : all) {
            TileEntity t = (TileEntity) o;
            if (!(t instanceof IGregTechTileEntity) || t.xCoord < minX
                || t.xCoord > maxX
                || t.yCoord < minY
                || t.yCoord > maxY
                || t.zCoord < minZ
                || t.zCoord > maxZ) {
                continue;
            }
            IGregTechTileEntity g = (IGregTechTileEntity) t;
            if (g.getMetaTileEntity() == null) continue;
            if (machines != null) machines[0]++;
            int before = TOTAL.get();
            check(g, "world read-back", t.xCoord, t.yCoord, t.zCoord);
            if (TOTAL.get() != before) {
                out.add(RECENT.get(RECENT.size() - 1));
            }
        }
        return out;
    }

    private static void report(String line) {
        TOTAL.incrementAndGet();
        synchronized (RECENT) {
            RECENT.add(line);
            if (RECENT.size() > MAX_RECENT) RECENT.remove(0);
        }
        GregGuns.LOG.warn(line);
    }
}
