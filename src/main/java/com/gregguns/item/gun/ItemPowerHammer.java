package com.gregguns.item.gun;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;

import com.gregguns.combat.DamageSources;
import com.gregguns.combat.DamageType;
import com.gregguns.gun.def.GunDefinition;
import com.gregguns.gun.logic.ChargeRelease;
import com.gregguns.gun.logic.Shockwave;
import com.gregguns.gun.logic.ShotOffset;

/**
 * The Power Hammer and its Advanced version: a {@link ItemToolGun} whose charged right click is a shockwave. The
 * release spends 1 to 10 rounds of air (the shot handler does that), then this class hits every entity in the
 * {@link Shockwave} box for {@link Shockwave#damage} and breaks the cells of the box nearest-first through
 * {@link ItemToolGun#breakBlock}, so every block is asked of {@code BlockBreakGuard} and paid for from the air left.
 *
 * <p>
 * Only blocks the hammer could mine are broken: not air, not liquids, not tile entities (a shockwave must not undo a
 * machine or a chest), and not a block whose material needs a tool the hammer's level does not harvest.
 */
// Spec: docs/techguns-spec/02-guns/sheets-launchers-tools-grenades.md (Power Hammer, Power Hammer (Advanced)),
// docs/gregguns-spec/02-guns/tool-guns.md
public class ItemPowerHammer extends ItemToolGun {

    /**
     * @param definition a tool definition with a charge
     */
    public ItemPowerHammer(GunDefinition definition) {
        super(definition);
    }

    @Override
    public void onChargedShot(EntityPlayerMP player, ChargeRelease release, ShotOffset[] offsets) {
        World world = player.worldObj;
        ItemStack stack = player.getHeldItem();
        Vec3 look = player.getLookVec();
        double ex = player.posX;
        double ey = player.posY + player.getEyeHeight();
        double ez = player.posZ;
        double distance = Shockwave.centerDistance(solidDistance(world, ex, ey, ez, look));

        double[] b = Shockwave.box(ex, ey, ez, look.xCoord, look.yCoord, look.zCoord, distance);
        @SuppressWarnings("unchecked")
        List<Entity> inBox = world.getEntitiesWithinAABBExcludingEntity(
            player,
            AxisAlignedBB.getBoundingBox(b[0], b[1], b[2], b[3], b[4], b[5]));
        float damage = (float) Shockwave.damage(release.units());
        for (Entity target : inBox) {
            if (target instanceof EntityLivingBase && target.isEntityAlive()) {
                DamageSources.hit(target, DamageType.PHYSICAL, null, player, damage, 0.0);
            }
        }

        if (stack == null || stack.getItem() != this) {
            return;
        }
        int feetX = (int) Math.floor(player.posX);
        int feetY = (int) Math.floor(player.posY);
        int feetZ = (int) Math.floor(player.posZ);
        for (int[] c : Shockwave.cells(ex, ey, ez, look.xCoord, look.yCoord, look.zCoord, distance)) {
            boolean playerCell = c[0] == feetX && c[2] == feetZ && (c[1] == feetY || c[1] == feetY + 1);
            if (!playerCell && shockable(world, stack, c[0], c[1], c[2])
                && breakBlock(player, stack, c[0], c[1], c[2]) == BreakResult.EMPTY) {
                break;
            }
        }
    }

    private static double solidDistance(World world, double ex, double ey, double ez, Vec3 look) {
        Vec3 from = Vec3.createVectorHelper(ex, ey, ez);
        Vec3 to = Vec3.createVectorHelper(
            ex + look.xCoord * Shockwave.REACH,
            ey + look.yCoord * Shockwave.REACH,
            ez + look.zCoord * Shockwave.REACH);
        MovingObjectPosition hit = world.rayTraceBlocks(from, to);
        return hit == null ? -1.0 : hit.hitVec.distanceTo(from);
    }

    private static boolean shockable(World world, ItemStack stack, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        if (block.isAir(world, x, y, z) || block.getMaterial()
            .isLiquid() || world.getTileEntity(x, y, z) != null) {
            return false;
        }
        int meta = world.getBlockMetadata(x, y, z);
        return block.getMaterial()
            .isToolNotRequired() || ForgeHooks.canToolHarvestBlock(block, meta, stack);
    }
}
