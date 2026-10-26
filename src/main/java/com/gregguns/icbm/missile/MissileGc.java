package com.gregguns.icbm.missile;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

import com.gregguns.GregGuns;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import micdoodle8.mods.galacticraft.api.recipe.RocketFuels;
import micdoodle8.mods.galacticraft.core.event.EventLandingPadRemoval;
import micdoodle8.mods.galacticraft.core.tile.TileEntityLandingPad;
import micdoodle8.mods.galacticraft.core.tile.TileEntityMulti;

/**
 * The Galacticraft side of the missiles: the fuel lists, the pad lookup and the pad-removal guard. This class names
 * Galacticraft types directly, so it is only ever touched after the module gate and a {@code galacticraftPresent()}
 * check (see {@code MissileRegistration.init} and {@code ItemBlockMissileBody}). Server and client.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/galacticraft-pad.md, fuel.md
// Source:
// https://github.com/GTNewHorizons/Galacticraft/blob/3.4.34-GTNH/src/main/java/micdoodle8/mods/galacticraft/api/recipe/RocketFuels.java
// Source:
// https://github.com/GTNewHorizons/Galacticraft/blob/3.4.34-GTNH/src/main/java/micdoodle8/mods/galacticraft/core/tile/TileEntityLandingPad.java
public final class MissileGc {

    private MissileGc() {}

    /** Registers each tier's fuels (D19) and the pad-removal guard (D32). */
    public static void init() {
        registerFuels();
        MinecraftForge.EVENT_BUS.register(new PadGuard());
    }

    /** Puts every tier's accepted fluids in {@code RocketFuels}, keyed by the tier's entity class. */
    static void registerFuels() {
        for (String fluid : MissileTier.T1.fuels()) {
            RocketFuels.addFuel(EntityMissileT1.class, fluid);
        }
        for (String fluid : MissileTier.T2.fuels()) {
            RocketFuels.addFuel(EntityMissileT2.class, fluid);
        }
        GregGuns.LOG.info("ICBM missile fuels registered with Galacticraft");
    }

    /**
     * @param world a world
     * @param x     X of a block
     * @param y     Y
     * @param z     Z
     * @return the {x, y, z} of the centre block of the pad this block belongs to, or null when it is no pad block
     */
    public static int[] padCenter(World world, int x, int y, int z) {
        TileEntity t = world.getTileEntity(x, y, z);
        if (t instanceof TileEntityLandingPad) {
            return new int[] { x, y, z };
        }
        if (t instanceof TileEntityMulti && ((TileEntityMulti) t).mainBlockPosition != null) {
            micdoodle8.mods.galacticraft.api.vector.BlockVec3 main = ((TileEntityMulti) t).mainBlockPosition;
            if (world.getTileEntity(main.x, main.y, main.z) instanceof TileEntityLandingPad) {
                return new int[] { main.x, main.y, main.z };
            }
        }
        return null;
    }

    /**
     * @param missile a missile entity
     * @param fluid   a fluid name
     * @return whether the missile's tank accepts it, as the fuel loader asks
     */
    public static boolean accepts(EntityMissile missile, String fluid) {
        return RocketFuels.isCorrectFuel(missile, fluid);
    }

    /** Denies Galacticraft's pad removal for a pad that holds one of our missiles (D32, 0018 §9). */
    public static final class PadGuard {

        /** @param event the removal request GC posts before it deletes a pad */
        @SubscribeEvent
        public void onPadRemoval(EventLandingPadRemoval event) {
            AxisAlignedBB box = AxisAlignedBB
                .getBoundingBox(event.x - 1.0, event.y, event.z - 1.0, event.x + 2.0, event.y + 3.0, event.z + 2.0);
            if (!event.world.getEntitiesWithinAABB(EntityMissile.class, box)
                .isEmpty()) {
                event.allow = false;
            }
        }
    }
}
