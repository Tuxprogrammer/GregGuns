package com.gregguns.utility.camp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;

import com.gregguns.core.config.UtilityConfig;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The Camp Flag's tile entity: the NPC spawner of a generated camp. World generation calls {@link #init} once; the
 * server tick then follows {@link CampSpawnPlan} and asks the {@link CampSpawnPorts active port} to count and spawn.
 * An uninitialized flag does nothing (MUTL-39) and the cap is saved (MUTL-38). Server logic; the client only draws the
 * post.
 *
 * <p>
 * Usage: {@code tile.init(32, 32, 16, 2, 16, spawnPoints, names, -1)}, where {@code spawnPoints} is a flat array of
 * (x, y, z) offsets from the camp origin and a negative cap means "use the size formula".
 */
// Spec: docs/techguns-spec/09-machines-utility/misc-tile-entities.md section 2; fixes MUTL-38, MUTL-39
// Deviation: sizes and offsets are saved as ints, not shorts.
public class TileCampFlag extends TileEntity {

    private static final int HEIGHT = 20;
    private static final int MARGIN = 8;
    private static final int BELOW = 3;

    private boolean initialized;
    private int sizeX;
    private int sizeZ;
    private int offsetX;
    private int offsetY;
    private int offsetZ;
    private int[] spawnPoints = new int[0];
    private final List<String> npcNames = new ArrayList<>();
    private int maxNpcs;
    private CampSpawnPlan plan;

    /**
     * Initializes the flag. Server side; world generation calls it right after placing the block.
     *
     * @param sizeX       camp size on X
     * @param sizeZ       camp size on Z
     * @param offsetX     flag position minus camp origin, X
     * @param offsetY     flag position minus camp origin, Y
     * @param offsetZ     flag position minus camp origin, Z
     * @param spawnPoints flat (x, y, z) offsets from the origin; a length that is not a multiple of 3 drops the tail
     * @param names       NPC entity names to choose from
     * @param maxNpcs     the cap, or a negative number for {@link CampSpawnPlan#defaultCap}
     */
    public void init(int sizeX, int sizeZ, int offsetX, int offsetY, int offsetZ, int[] spawnPoints, List<String> names,
        int maxNpcs) {
        this.sizeX = sizeX;
        this.sizeZ = sizeZ;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        int usable = spawnPoints == null ? 0 : spawnPoints.length / 3 * 3;
        this.spawnPoints = usable == 0 ? new int[0] : Arrays.copyOf(spawnPoints, usable);
        this.npcNames.clear();
        if (names != null) {
            this.npcNames.addAll(names);
        }
        this.maxNpcs = maxNpcs >= 0 ? maxNpcs : CampSpawnPlan.defaultCap(sizeX, sizeZ, UtilityConfig.campFlagCapFactor);
        this.initialized = true;
        this.plan = null;
        markDirty();
    }

    /** @return whether {@link #init} was called (or the NBT said so) */
    public boolean isInitialized() {
        return initialized;
    }

    /** @return the NPC cap; kept across saves */
    public int maxNpcs() {
        return maxNpcs;
    }

    /** @return the NPC names, read-only */
    public List<String> npcNames() {
        return Collections.unmodifiableList(npcNames);
    }

    /** @return the number of spawn points */
    public int spawnPointCount() {
        return spawnPoints.length / 3;
    }

    @Override
    public void updateEntity() {
        if (worldObj == null || worldObj.isRemote) {
            return;
        }
        if (plan == null) {
            plan = new CampSpawnPlan(UtilityConfig.campFlagSpawnIntervalTicks);
        }
        boolean spawn = plan.tick(initialized, maxNpcs, spawnPoints.length / 3, npcNames.size(), this::countNpcs);
        if (spawn) {
            spawnOne();
        }
    }

    private int countNpcs() {
        int ox = xCoord - offsetX;
        int oy = yCoord - offsetY;
        int oz = zCoord - offsetZ;
        return CampSpawnPorts.get()
            .countNpcs(
                worldObj,
                npcNames,
                ox - MARGIN,
                oy - BELOW,
                oz - MARGIN,
                ox + sizeX + MARGIN,
                oy - BELOW + HEIGHT,
                oz + sizeZ + MARGIN);
    }

    private void spawnOne() {
        int points = spawnPoints.length / 3;
        int p = worldObj.rand.nextInt(points) * 3;
        String name = npcNames.get(worldObj.rand.nextInt(npcNames.size()));
        double x = xCoord - offsetX + spawnPoints[p] + 0.5;
        double y = yCoord - offsetY + spawnPoints[p + 1];
        double z = zCoord - offsetZ + spawnPoints[p + 2] + 0.5;
        CampSpawnPorts.get()
            .spawn(worldObj, name, x, y, z);
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        writeFlag(tag);
    }

    /** Writes the flag's own fields; split from {@link #writeToNBT} so tests need no tile entity registry. */
    void writeFlag(NBTTagCompound tag) {
        tag.setBoolean("initialized", initialized);
        tag.setInteger("sizeX", sizeX);
        tag.setInteger("sizeZ", sizeZ);
        tag.setInteger("xoffset", offsetX);
        tag.setInteger("yoffset", offsetY);
        tag.setInteger("zoffset", offsetZ);
        tag.setIntArray("spawnPositions", spawnPoints);
        tag.setInteger("maxNPCs", maxNpcs);
        NBTTagList names = new NBTTagList();
        for (String name : npcNames) {
            names.appendTag(new NBTTagString(name));
        }
        tag.setTag("npcNames", names);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        readFlag(tag);
    }

    /** Reads the flag's own fields; see {@link #writeFlag}. */
    void readFlag(NBTTagCompound tag) {
        sizeX = tag.getInteger("sizeX");
        sizeZ = tag.getInteger("sizeZ");
        offsetX = tag.getInteger("xoffset");
        offsetY = tag.getInteger("yoffset");
        offsetZ = tag.getInteger("zoffset");
        int[] points = tag.getIntArray("spawnPositions");
        spawnPoints = Arrays.copyOf(points, points.length / 3 * 3);
        npcNames.clear();
        NBTTagList names = tag.getTagList("npcNames", 8);
        for (int i = 0; i < names.tagCount(); i++) {
            npcNames.add(names.getStringTagAt(i));
        }
        maxNpcs = tag.getInteger("maxNPCs");
        initialized = tag.getBoolean("initialized");
        plan = null;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox() {
        double top = yCoord + FlagPostGeometry.HEIGHT / 16.0;
        double reach = FlagPostGeometry.horizontalReach() / 16.0;
        return AxisAlignedBB.getBoundingBox(
            xCoord + 0.5 - reach,
            yCoord,
            zCoord + 0.5 - reach,
            xCoord + 0.5 + reach,
            top,
            zCoord + 0.5 + reach);
    }
}
