package com.gregguns.icbm.blast;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;

/**
 * The per-dimension save record of the blasts in progress. It reads the saved list when the world loads and, when the
 * world saves, writes the live blasts straight from the engine, so what is on disk is always the state at save time.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/blast-engine.md (save and load)
public final class BlastSavedData extends WorldSavedData {

    /** The map-storage name. */
    public static final String NAME = "gregguns_icbm_blasts";

    private NBTTagList pending = new NBTTagList();
    private World world;

    /** @param name the storage name; Forge's map storage calls this by reflection */
    public BlastSavedData(String name) {
        super(name);
    }

    /** @param world the world this record belongs to; set by the engine */
    void attach(World world) {
        this.world = world;
    }

    /** @return the saved blasts not yet resumed; empties the list */
    NBTTagList takePending() {
        NBTTagList out = pending;
        pending = new NBTTagList();
        return out;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        pending = tag.getTagList("blasts", 10);
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        NBTTagList list = world == null ? pending
            : BlastEngine.instance()
                .save(world);
        tag.setTag("blasts", list);
    }

    /** Marks the record changed so the next world save writes it. */
    void touch() {
        markDirty();
    }
}
