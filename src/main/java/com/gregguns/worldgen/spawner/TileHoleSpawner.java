package com.gregguns.worldgen.spawner;

import java.util.List;

import net.minecraft.entity.EntityLiving;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import com.gregguns.core.module.ModuleId;
import com.gregguns.core.module.ModuleState;

/**
 * The tile of a hole spawner. It ticks only while its chunk is loaded (a tile never ticks otherwise), waits
 * {@link HoleCaps#INTERVAL_TICKS} between attempts and leaves everything to {@link HoleSpawning}. Server logic only;
 * the client never acts. The kind is saved; the countdown is not.
 */
// Spec: PLAN-M8 G11
public class TileHoleSpawner extends TileEntity {

    private HoleKind kind = HoleKind.ZOMBIE;
    private int countdown = 40;

    /** For the NBT loader; the kind is read from the save. */
    public TileHoleSpawner() {}

    /** @param kind which NPCs this hole places */
    public TileHoleSpawner(HoleKind kind) {
        this.kind = kind;
    }

    /** @return which NPCs this hole places */
    public HoleKind kind() {
        return kind;
    }

    @Override
    public void updateEntity() {
        if (worldObj == null || worldObj.isRemote) {
            return;
        }
        if (!ModuleState.isActive(ModuleId.NPCS)) {
            return; // the npcs module is off: no spawning, the spawner block stays as it is
        }
        if (--countdown > 0) {
            return;
        }
        // A little jitter, so holes placed together do not all attempt on the same tick.
        countdown = HoleCaps.INTERVAL_TICKS + worldObj.rand.nextInt(20);
        attempt(HoleSpawning.MARKER, worldObj::spawnEntityInWorld);
    }

    /**
     * Runs one attempt now, ignoring the countdown. The tick and scenarios both use it.
     *
     * @param marker the Infernal opt-out step
     * @param placer adds each NPC to the world
     * @return the NPCs placed
     */
    public List<EntityLiving> attempt(HoleSpawning.Marker marker, HoleSpawning.Placer placer) {
        return HoleSpawning.attempt(worldObj, xCoord, yCoord, zCoord, kind, marker, placer);
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setString("kind", kind.name());
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        HoleKind read = HoleKind.byName(tag.getString("kind"));
        kind = read != null ? read : HoleKind.ZOMBIE;
    }
}
