package com.gregguns.explosion;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.BlockEvent;

/**
 * Asks the protection mods whether a player may break a block, by posting Forge's {@code BlockEvent.BreakEvent} for
 * that player. Any GregGuns code that removes blocks on a player's behalf calls this first and skips the block when it
 * says no (fixes PFX-14: Techguns' mining projectile broke blocks with no checks). Server side.
 */
// Spec: docs/gregguns-spec/04-projectiles-fx/explosions-pack.md (GGS-04-12)
// Source: ServerUtilities cancels BreakEvent in claimed chunks:
// https://github.com/GTNewHorizons/ServerUtilities/blob/2.4.14/src/main/java/serverutils/data/ClaimedChunks.java#L480-L485
public final class BlockBreakGuard {

    private BlockBreakGuard() {}

    /**
     * @param world  the world
     * @param x      block X
     * @param y      block Y
     * @param z      block Z
     * @param block  the block there
     * @param meta   its metadata
     * @param player the player the break is on behalf of
     * @return false when a listener cancelled the break
     */
    public static boolean mayBreak(World world, int x, int y, int z, Block block, int meta, EntityPlayer player) {
        BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(x, y, z, world, block, meta, player);
        return !MinecraftForge.EVENT_BUS.post(event);
    }
}
