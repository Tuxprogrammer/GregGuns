package com.gregguns.icbm.complex.gen;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;

/**
 * The silo mouths of one dimension still waiting for their finish (D319), saved with the dimension so a mouth whose
 * surroundings populate after a restart is still finished. A mouth leaves the record once finished, so the file holds
 * only the frontier of explored land. Server side.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Depth, stamp and surface: the mouth finish)
public class SiloMouths extends WorldSavedData {

    public static final String NAME = "gregguns_silo_mouths";

    private final Map<String, SiloMouth> pending = new LinkedHashMap<String, SiloMouth>();

    /** Required by {@code MapStorage}, which builds the instance by reflection from its name. */
    public SiloMouths(String name) {
        super(name);
    }

    /**
     * @param create whether to make the record when the dimension has none yet
     * @return the dimension's record, or null when it has none and {@code create} is false
     */
    public static SiloMouths forWorld(World world, boolean create) {
        SiloMouths s = (SiloMouths) world.perWorldStorage.loadData(SiloMouths.class, NAME);
        if (s == null && create) {
            s = new SiloMouths(NAME);
            world.perWorldStorage.setData(NAME, s);
        }
        return s;
    }

    /** @return the pending mouths by id; the keeper marks the record dirty when it changes them */
    public Map<String, SiloMouth> pending() {
        return pending;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        pending.clear();
        NBTTagList list = tag.getTagList("mouths", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound t = list.getCompoundTagAt(i);
            SiloMouth m = SiloMouth.fromNbt(t);
            if (m != null) pending.put(t.getString("id"), m);
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        NBTTagList list = new NBTTagList();
        for (Map.Entry<String, SiloMouth> e : pending.entrySet()) {
            NBTTagCompound t = e.getValue()
                .toNbt();
            t.setString("id", e.getKey());
            list.appendTag(t);
        }
        tag.setTag("mouths", list);
    }
}
