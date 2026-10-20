package com.gregguns.worldgen;

import java.util.function.Supplier;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;

/**
 * Per-dimension saved record of every structure plan's decision, ground Y and camp flag positions. Stored in
 * {@code World.perWorldStorage}, which Forge keeps in each dimension's own data folder: the plan key then needs no
 * dimension, a dimension that is deleted takes its record with it, and one dimension's save never rewrites another's.
 * (The shared {@code mapStorage} would need the dimension in every key and one growing file for all of them.)
 *
 * <p>
 * All logic lives in {@link PlanLedger} and {@link StateNbt}; this class only adapts them to {@link WorldSavedData}.
 */
// Spec: docs/gregguns-spec/11-worldgen/structure-placement.md (GGS-11-10); PLAN-M8 Q3
public class StructureState extends WorldSavedData {

    public static final String NAME = "gregguns_structures";

    private PlanLedger ledger = new PlanLedger();
    private int savedRevision;

    /** Required by {@code MapStorage}, which builds the instance by reflection from its name. */
    public StructureState(String name) {
        super(name);
    }

    /** The loaded or new state of this world's dimension. Server side only. */
    public static StructureState forWorld(World world) {
        StructureState s = (StructureState) world.perWorldStorage.loadData(StructureState.class, NAME);
        if (s == null) {
            s = new StructureState(NAME);
            world.perWorldStorage.setData(NAME, s);
        }
        return s;
    }

    /** See {@link PlanLedger#decideOnce}; marks the data dirty when a new decision is recorded. */
    public Decision decideOnce(String planKey, Supplier<Decision> decider) {
        Decision d = ledger.decideOnce(planKey, decider);
        syncDirty();
        return d;
    }

    public Decision get(String planKey) {
        return ledger.get(planKey);
    }

    public boolean addFlag(String planKey, FlagPos pos) {
        boolean added = ledger.addFlag(planKey, pos);
        syncDirty();
        return added;
    }

    public PlanLedger ledger() {
        return ledger;
    }

    private void syncDirty() {
        if (ledger.revision() != savedRevision) {
            savedRevision = ledger.revision();
            markDirty();
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        ledger = StateNbt.read(tag);
        savedRevision = ledger.revision();
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        StateNbt.write(ledger, tag);
    }
}
