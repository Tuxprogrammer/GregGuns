package com.gregguns.utility.charge;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.world.WorldServer;

import com.gregguns.core.config.UtilityConfig;
import com.gregguns.explosion.BlockBreakGuard;
import com.gregguns.utility.security.OwnerProfiles;
import com.mojang.authlib.GameProfile;

/**
 * Bores the tunnel of a detonating charge. No {@code Explosion} is created (GGS-09-12): each block is checked with
 * Forge's break event for a real player, its drops are gathered at fortune 0 before it is removed, and a column stops
 * at
 * the first block that is too hard or whose event was canceled. Server side.
 */
// Spec: docs/gregguns-spec/09-machines-utility/explosive-charges.md (GGS-09-10 to GGS-09-14),
// docs/techguns-spec/09-machines-utility/explosive-charges.md (Explosion algorithm), BUGS MUTL-30
// Deviation: liquids are skipped, not removed (a bore does not drain a lake); the baseline treated them as breakable.
public final class ChargeBlast {

    /** What a detonation did, for tests and scenarios. */
    public static final class Result {

        /** Blocks removed. */
        public int broken;
        /** Drop stacks spawned. */
        public int drops;
        /** Columns stopped by a hard block or a canceled event. */
        public int blockedColumns;
        /** Break events posted. */
        public int events;
        /** The player every event carried. */
        public EntityPlayerMP actor;
    }

    private ChargeBlast() {}

    /**
     * Detonates a charge. The charge block is removed first so that the neighbor updates of the boring cannot drop it.
     *
     * @param world       the world
     * @param x           charge x
     * @param y           charge y
     * @param z           charge z
     * @param orientation the placed face, 0 to 5
     * @param params      the settings
     * @param owner       the owner's UUID; may be null
     * @param profile     the owner's profile; may be null
     * @return what happened
     */
    public static Result detonate(WorldServer world, int x, int y, int z, int orientation, ChargeParams params,
        UUID owner, GameProfile profile) {
        Result result = new Result();
        // GGS-09-10: always a real player, so claim protection sees the event (MUTL-30).
        result.actor = OwnerProfiles.playerFor(world, owner, profile);
        world.setBlockToAir(x, y, z);
        float pitch = (1.0F + (world.rand.nextFloat() - world.rand.nextFloat()) * 0.2F) * 0.7F;
        world.playSoundEffect(x + 0.5, y + 0.5, z + 0.5, "random.explode", 4.0F, pitch);
        ChargeType type = params.type();
        for (int[] column : ChargeGeometry.columns(params.radius())) {
            boreColumn(world, x, y, z, orientation, column[0], column[1], params, type, result);
        }
        return result;
    }

    private static void boreColumn(WorldServer world, int cx, int cy, int cz, int orientation, int u, int v,
        ChargeParams params, ChargeType type, Result result) {
        for (int step = 1; step <= params.length(); step++) {
            int[] p = ChargeGeometry.cell(orientation, cx, cy, cz, u, v, step);
            if (p[1] < 0 || p[1] > 255) {
                return;
            }
            Block block = world.getBlock(p[0], p[1], p[2]);
            if (block.isAir(world, p[0], p[1], p[2]) || block.getMaterial()
                .isLiquid()) {
                continue;
            }
            float hardness = block.getBlockHardness(world, p[0], p[1], p[2]);
            if (!type.canBreak(hardness, UtilityConfig.chargeMaxHardness, UtilityConfig.chargeAdvancedMaxHardness)) {
                result.blockedColumns++;
                return;
            }
            int meta = world.getBlockMetadata(p[0], p[1], p[2]);
            result.events++;
            if (!BlockBreakGuard.mayBreak(world, p[0], p[1], p[2], block, meta, result.actor)) {
                // GGS-09-11: this column stops; nothing past the canceled block breaks.
                result.blockedColumns++;
                return;
            }
            // GGS-09-13: drops at fortune 0, gathered before the block goes (GT ores derive them from the meta).
            List<ItemStack> drops = new ArrayList<>(block.getDrops(world, p[0], p[1], p[2], meta, 0));
            world.setBlockToAir(p[0], p[1], p[2]);
            result.broken++;
            for (ItemStack stack : drops) {
                if (stack == null || stack.stackSize <= 0) {
                    continue;
                }
                EntityItem item = new EntityItem(world, p[0] + 0.5, p[1] + 0.5, p[2] + 0.5, stack);
                item.delayBeforeCanPickup = 10;
                world.spawnEntityInWorld(item);
                result.drops++;
            }
        }
    }
}
