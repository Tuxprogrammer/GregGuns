package com.gregguns.icbm.defense;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.StatCollector;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import com.gregguns.api.Tier;
import com.gregguns.api.Tiered;
import com.gregguns.core.text.AddedBy;
import com.gregguns.core.text.TierText;
import com.gregguns.icbm.control.LaunchTarget;
import com.gregguns.icbm.control.TargetStick;
import com.gregguns.icbm.recipe.PartTooltipFormat;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The radar gun, the kept handheld tool (D93): a laser range finder that stores a target. Right-click aims at the block
 * under the crosshair (up to {@link #RANGE} blocks) and keeps its X and Z in the same tag a GT data stick carries
 * ({@link TargetStick}); sneaking and right-clicking clears it. Right-clicking a Launch Control with it copies the
 * target
 * there. The trace runs on the server from the player's eyes, so a client cannot send a position (ICBM-05-07 and
 * ICBM-05-12). Both sides.
 */
// Spec: docs/gregguns-icbm-spec/05-detection-defense/handheld-tools.md (Radar gun)
public final class ItemRadarGun extends Item implements Tiered {

    /** The bare registry name. */
    public static final String NAME = "radar_gun";
    /** How far the laser reaches, blocks. */
    public static final double RANGE = 200.0;

    /** Creates the item; it does not stack. */
    public ItemRadarGun() {
        setMaxStackSize(1);
    }

    @Override
    public Tier getTier() {
        return Tier.HV;
    }

    /**
     * @param stack any stack
     * @return the stored target, or {@link LaunchTarget#NONE}
     */
    public static LaunchTarget target(ItemStack stack) {
        return stack != null && stack.getItem() instanceof ItemRadarGun ? TargetStick.read(stack.getTagCompound())
            : LaunchTarget.NONE;
    }

    /**
     * @param stack any stack
     * @param dim   the dimension to report when the tag has none
     * @return the dimension the target was taken in
     */
    public static int dimension(ItemStack stack, int dim) {
        return stack == null ? dim : TargetStick.dimension(stack.getTagCompound(), dim);
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (world.isRemote) {
            return stack;
        }
        if (player.isSneaking()) {
            if (stack.hasTagCompound()) {
                stack.getTagCompound()
                    .removeTag(TargetStick.TAG);
            }
            player.addChatMessage(new ChatComponentTranslation("gregguns.icbm.radargun.cleared"));
            return stack;
        }
        Vec3 eye = Vec3.createVectorHelper(player.posX, player.posY + player.getEyeHeight(), player.posZ);
        Vec3 look = player.getLookVec();
        // Stop the trace at the first unloaded chunk: rayTraceBlocks would load or generate chunks on demand.
        double reach = 0;
        for (double d = 1; d <= RANGE; d += 1) {
            int cx = MathHelper.floor_double(eye.xCoord + look.xCoord * d) >> 4;
            int cz = MathHelper.floor_double(eye.zCoord + look.zCoord * d) >> 4;
            if (!world.getChunkProvider()
                .chunkExists(cx, cz)) {
                break;
            }
            reach = d;
        }
        Vec3 end = eye.addVector(look.xCoord * reach, look.yCoord * reach, look.zCoord * reach);
        MovingObjectPosition hit = world.rayTraceBlocks(eye, end);
        if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            player.addChatMessage(new ChatComponentTranslation("gregguns.icbm.radargun.miss", (int) RANGE));
            return stack;
        }
        store(stack, world, hit.blockX, hit.blockZ);
        player.addChatMessage(new ChatComponentTranslation("gregguns.icbm.radargun.set", hit.blockX, hit.blockZ));
        return stack;
    }

    /**
     * Stores a target on the gun.
     *
     * @param stack the gun
     * @param world the world the target is in
     * @param x     block X
     * @param z     block Z
     */
    public static void store(ItemStack stack, World world, int x, int z) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        TargetStick.write(stack.getTagCompound(), LaunchTarget.of(x, z), world.provider.dimensionId);
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        boolean shift = net.minecraft.client.gui.GuiScreen.isShiftKeyDown();
        String tierLine = String
            .format(StatCollector.translateToLocal("gregguns.icbm.part.tooltip.tier"), TierText.colored(Tier.HV));
        LaunchTarget t = target(stack);
        String summary = t.isSet()
            ? StatCollector.translateToLocalFormatted("gregguns.icbm.radargun.tooltip.target", t.x(), t.z())
            : StatCollector.translateToLocal("gregguns.icbm.radargun.tooltip.empty");
        String details = StatCollector.translateToLocalFormatted("gregguns.icbm.radargun.tooltip.details", (int) RANGE);
        String hint = StatCollector.translateToLocal("gregguns.icbm.part.tooltip.shift");
        tooltip.addAll(PartTooltipFormat.lines(tierLine, summary, details, hint, shift));
        if (shift) {
            tooltip.add(AddedBy.line());
        }
    }
}
