package com.gregguns.machine;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.gregguns.block.GregGunsCasings;
import com.gregguns.machine.repair.GantryShape;
import com.gregguns.machine.repair.MTEArmoryGantry;
import com.gregguns.utility.rampart.MTERampart;
import com.gregguns.utility.rampart.RampartShape;
import com.gtnewhorizon.structurelib.alignment.constructable.ChannelDataAccessor;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.common.misc.GTStructureChannels;

/**
 * Dev tooling: forms a GregGuns multiblock around a controller that already stands in the world. Used by
 * {@code /gregguns build} and by the {@code testing.build_multiblocks} scenario.
 *
 * <p>
 * A StructureLib sneak-click on a controller with a trigger item builds only the casings (creative) or what the player
 * carries (survival); it never places hatches. The hatch layout below is the one the machines' own scenarios use, with
 * the controller facing north.
 */
// Source: StructureLib 1.4.45 ConstructableUtility.handle0 (server side, sneaking, creative: constructable.construct(
// stack, false); survival: ISurvivalConstructable.survivalConstruct):
// https://github.com/GTNewHorizons/StructureLib/blob/1.4.45/src/main/java/com/gtnewhorizon/structurelib/alignment/constructable/ConstructableUtility.java#L59-L96
// The trigger stack size picks the bay or mount count in each controller's construct().
public final class DevStructureBuilder {

    private static final String OWNER = "gregguns-test";

    private DevStructureBuilder() {}

    /** What a build did. */
    public static final class Outcome {

        /** Whether the controller reports a formed structure afterwards. */
        public final boolean formed;
        /** A one-line description for chat. */
        public final String message;

        Outcome(boolean formed, String message) {
            this.formed = formed;
            this.message = message;
        }

        @Override
        public String toString() {
            return message;
        }
    }

    /**
     * Builds and forms the multiblock whose controller is at the position.
     *
     * @param world the world
     * @param x     controller x
     * @param y     controller y
     * @param z     controller z
     * @param count bays or mounts, or 0 for the default; the machine clamps it
     * @return the outcome; {@code formed} is false with an explanation when there is no controller or it did not form
     */
    public static Outcome build(World world, int x, int y, int z, int count) {
        return build(world, x, y, z, count, false);
    }

    /**
     * Builds and forms the multiblock whose controller is at the position, with the Ammunition Works' optional
     * filling bench when asked.
     *
     * @param world the world
     * @param x     controller x
     * @param y     controller y
     * @param z     controller z
     * @param count bays or mounts, or 0 for the default; the machine clamps it
     * @param bench whether to add the filling bench (only the Ammunition Works has one; others ignore it)
     * @return the outcome; {@code formed} is false with an explanation when there is no controller or it did not form
     */
    public static Outcome build(World world, int x, int y, int z, int count, boolean bench) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!(tile instanceof IGregTechTileEntity)) {
            return new Outcome(false, "build: no GregTech machine at " + x + " " + y + " " + z);
        }
        IGregTechTileEntity controller = (IGregTechTileEntity) tile;
        IMetaTileEntity mte = controller.getMetaTileEntity();
        controller.setFrontFacing(ForgeDirection.NORTH);
        controller.setOwnerName(OWNER);
        controller.setOwnerUuid(UUID.nameUUIDFromBytes(OWNER.getBytes(StandardCharsets.UTF_8)));
        if (mte instanceof MTEAmmoWorks) {
            int bays = count == 0 ? 3 : AmmoWorksShape.clampBays(count);
            ItemStack trigger = new ItemStack(Items.stick, bays);
            if (bench) {
                // The bench rides on the "height" channel of the trigger stack (MTEAmmoWorks#benchFor).
                ChannelDataAccessor.setChannelData(trigger, GTStructureChannels.STRUCTURE_HEIGHT.get(), 1);
            }
            ((MTEAmmoWorks) mte).construct(trigger, false);
            hatch(world, x, y + 1, z, ItemList.Hatch_Input_Bus_HV, ForgeDirection.NORTH);
            hatch(world, x, y + 2, z, ItemList.Hatch_Energy_HV, ForgeDirection.NORTH);
            hatch(world, x + 1, y, z, ItemList.Hatch_Maintenance, ForgeDirection.NORTH);
            hatch(world, x - 1, y, z, ItemList.Hatch_Muffler_HV, ForgeDirection.NORTH);
            hatch(
                world,
                x,
                y,
                z + AmmoWorksShape.totalLength(bays) - 1,
                ItemList.Hatch_Output_Bus_HV,
                ForgeDirection.SOUTH);
            if (bench) {
                // Bench casings on the second aisle's side columns at the controller's height: the input bus on the
                // left, the output bus on the right (the bench starts right behind the station's back wall).
                int benchZ = z + AmmoWorksShape.benchDepth(bays) + 1;
                hatch(world, x - 2, y, benchZ, ItemList.Hatch_Input_Bus_HV, ForgeDirection.WEST);
                hatch(world, x + 2, y, benchZ, ItemList.Hatch_Output_Bus_HV, ForgeDirection.EAST);
            }
            return finish(world, x, y, z, "Ammunition Works, " + bays + " bays" + (bench ? " with bench" : ""));
        }
        if (mte instanceof MTEArmoryGantry) {
            int bays = count == 0 ? 3 : GantryShape.clampBays(count);
            ((MTEArmoryGantry) mte).construct(new ItemStack(Items.stick, bays), false);
            hatch(world, x - 1, y, z, ItemList.Hatch_Input_Bus_HV, ForgeDirection.NORTH);
            hatch(world, x + 1, y, z, ItemList.Hatch_Maintenance, ForgeDirection.NORTH);
            hatch(world, x, y - 1, z, ItemList.Hatch_Energy_HV, ForgeDirection.NORTH);
            hatch(world, x + 1, y - 1, z, ItemList.Hatch_Energy_HV, ForgeDirection.NORTH);
            hatch(
                world,
                x,
                y,
                z + GantryShape.totalLength(bays) - 1,
                ItemList.Hatch_Output_Bus_HV,
                ForgeDirection.SOUTH);
            return finish(world, x, y, z, "Armory Gantry, " + bays + " bays");
        }
        if (mte instanceof com.gregguns.icbm.silo.MTEMissileSilo) {
            return buildSilo(world, x, y, z, ItemList.Hatch_Energy_LuV);
        }
        if (mte instanceof MTERampart) {
            int mounts = count == 0 ? RampartShape.DEFAULT_MOUNTS : RampartShape.clampMounts(count);
            ((MTERampart) mte).construct(new ItemStack(Items.stick, mounts), false);
            // The wall runs to one side of the controller; the first bay's solid sill row tells which.
            int dir = world.getBlock(x + RampartShape.hatchDistance(0), y, z) == GregGunsCasings.INSTANCE ? 1 : -1;
            hatch(world, x - 1, y, z, ItemList.Hatch_Input_Bus_HV, ForgeDirection.NORTH);
            hatch(world, x + 1, y, z, ItemList.Hatch_Maintenance, ForgeDirection.NORTH);
            hatch(world, x, y - 1, z, ItemList.Hatch_Energy_HV, ForgeDirection.NORTH);
            hatch(world, x, y - 1, z + 1, ItemList.Hatch_Output_Bus_HV, ForgeDirection.NORTH);
            for (int i = 0; i < mounts; i++) {
                place(
                    world,
                    x + dir * RampartShape.hatchDistance(i),
                    y + RampartShape.hatchRise(),
                    z,
                    GregGunsMTEIDs.GUN_MOUNT_HATCH_MV,
                    ForgeDirection.NORTH);
            }
            return finish(world, x, y, z, "Rampart, " + mounts + " mounts");
        }
        return new Outcome(false, "build: the machine at " + x + " " + y + " " + z + " is not a GregGuns multiblock");
    }

    /**
     * Builds the Missile Silo around the controller at the position, with the hatches in the glass rings (the bus and
     * the maintenance hatch beside the controller, the energy and fuel hatches below them).
     *
     * @param world  the world
     * @param x      controller x
     * @param y      controller y
     * @param z      controller z
     * @param energy the Energy Hatch to place, which sets the heaviest missile the silo handles
     * @return the outcome
     */
    public static Outcome buildSilo(World world, int x, int y, int z, ItemList energy) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!(tile instanceof IGregTechTileEntity)
            || !(((IGregTechTileEntity) tile).getMetaTileEntity() instanceof com.gregguns.icbm.silo.MTEMissileSilo)) {
            return new Outcome(false, "build: no Missile Silo controller at " + x + " " + y + " " + z);
        }
        IGregTechTileEntity controller = (IGregTechTileEntity) tile;
        controller.setFrontFacing(ForgeDirection.NORTH);
        controller.setOwnerName(OWNER);
        controller.setOwnerUuid(UUID.nameUUIDFromBytes(OWNER.getBytes(StandardCharsets.UTF_8)));
        // A rebuild over a formed pad: Galacticraft's dummy blocks tear the whole pad down when one is replaced, which
        // eats cells construct() has just placed. Clear the nine cells first (front is north, so the shaft is behind).
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = 1; dz <= 3; dz++) {
                world.setBlockToAir(x + dx, y - 2, z + dz);
            }
        }
        ((com.gregguns.icbm.silo.MTEMissileSilo) controller.getMetaTileEntity())
            .construct(new ItemStack(Items.stick, 1), false);
        hatch(world, x - 1, y, z, ItemList.Hatch_Input_Bus_HV, ForgeDirection.NORTH);
        hatch(world, x + 1, y, z, ItemList.Hatch_Maintenance, ForgeDirection.NORTH);
        hatch(world, x, y - 1, z, energy, ForgeDirection.NORTH);
        hatch(world, x + 1, y - 1, z, ItemList.Hatch_Input_HV, ForgeDirection.NORTH);
        return finish(world, x, y, z, "Missile Silo");
    }

    /**
     * Checks the structure of the controller at the position and reports its formed flag, from a fresh tile lookup (GT
     * may replace a tile entity during its first ticks).
     *
     * @param world the world
     * @param x     controller x
     * @param y     controller y
     * @param z     controller z
     * @return whether the controller is a formed multiblock
     */
    public static boolean check(World world, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!(tile instanceof IGregTechTileEntity)) {
            return false;
        }
        IGregTechTileEntity controller = (IGregTechTileEntity) tile;
        if (!(controller.getMetaTileEntity() instanceof MTEMultiBlockBase)) {
            return false;
        }
        MTEMultiBlockBase mte = (MTEMultiBlockBase) controller.getMetaTileEntity();
        mte.mWrench = true;
        mte.mScrewdriver = true;
        mte.mSoftMallet = true;
        mte.mHardHammer = true;
        mte.mSolderingTool = true;
        mte.mCrowbar = true;
        mte.checkStructure(true, controller);
        return mte.mMachine;
    }

    private static Outcome finish(World world, int x, int y, int z, String what) {
        boolean formed = check(world, x, y, z);
        return new Outcome(
            formed,
            "build: " + what
                + (formed ? " formed"
                    : " built but NOT formed (GT may need a few ticks to initialize the hatches; run build again)"));
    }

    private static void hatch(World world, int x, int y, int z, ItemList item, ForgeDirection facing) {
        place(
            world,
            x,
            y,
            z,
            item.get(1)
                .getItemDamage(),
            facing);
    }

    private static void place(World world, int x, int y, int z, int id, ForgeDirection facing) {
        IMetaTileEntity prototype = GregTechAPI.METATILEENTITIES[id];
        Block machines = GregTechAPI.sBlockMachines;
        world.setBlock(x, y, z, machines, prototype.getTileEntityBaseType(), 3);
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!(tile instanceof IGregTechTileEntity)) {
            throw new IllegalStateException("no GT tile entity at " + x + " " + y + " " + z);
        }
        IGregTechTileEntity base = (IGregTechTileEntity) tile;
        base.setInitialValuesAsNBT(null, (short) id);
        base.setFrontFacing(facing);
        base.setOwnerName(OWNER);
        base.setOwnerUuid(UUID.nameUUIDFromBytes(OWNER.getBytes(StandardCharsets.UTF_8)));
    }
}
