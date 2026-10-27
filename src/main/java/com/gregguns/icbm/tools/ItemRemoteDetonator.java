package com.gregguns.icbm.tools;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import com.gregguns.api.Tier;
import com.gregguns.api.Tiered;
import com.gregguns.core.text.AddedBy;
import com.gregguns.core.text.TierText;
import com.gregguns.icbm.warhead.TileWarhead;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The remote detonator of one tier (HV, EV or IV): sneak-click a placed warhead to link, click to light its fuse from
 * a distance. It does not detonate anything by itself: it lights the same fuse flint and steel does (D51), so the
 * warhead counts down and beeps and nobody is killed by an instant blast (D118). It fires only inside its range, in
 * the dimension of the link, at a loaded chunk, for the warhead's owner (D119). Both sides; the work is server side.
 */
// Spec: docs/gregguns-icbm-spec/05-detection-defense/handheld-tools.md (Remote detonator)
public final class ItemRemoteDetonator extends Item implements Tiered {

    static final String K_LINKED = "linked";
    static final String K_X = "linkX";
    static final String K_Y = "linkY";
    static final String K_Z = "linkZ";
    static final String K_DIM = "linkDim";

    private final Tier tier;

    /** @param tier HV, EV or IV */
    public ItemRemoteDetonator(Tier tier) {
        this.tier = tier;
        setMaxStackSize(1);
    }

    /**
     * @param tier a detonator tier
     * @return the bare registry name
     */
    public static String name(Tier tier) {
        return "remote_detonator_" + tier.name()
            .toLowerCase(java.util.Locale.ROOT);
    }

    @Override
    public Tier getTier() {
        return tier;
    }

    /** @return the range in blocks */
    public int range() {
        return HandheldRules.range(tier);
    }

    /**
     * @param stack a detonator stack
     * @return true when it holds a link
     */
    public static boolean linked(ItemStack stack) {
        return stack != null && stack.hasTagCompound()
            && stack.getTagCompound()
                .getBoolean(K_LINKED);
    }

    /** Stores a link on the stack. */
    static void link(ItemStack stack, int x, int y, int z, int dim) {
        NBTTagCompound tag = stack.hasTagCompound() ? stack.getTagCompound() : new NBTTagCompound();
        tag.setBoolean(K_LINKED, true);
        tag.setInteger(K_X, x);
        tag.setInteger(K_Y, y);
        tag.setInteger(K_Z, z);
        tag.setInteger(K_DIM, dim);
        stack.setTagCompound(tag);
    }

    /** Clears the link. */
    static void unlink(ItemStack stack) {
        if (stack.hasTagCompound()) {
            stack.setTagCompound(null);
        }
    }

    /**
     * Server side: lights the linked warhead's fuse when every rule allows it.
     *
     * @param holder the player pressing
     * @param stack  the detonator
     * @return what came of it
     */
    public RemoteResult fire(EntityPlayer holder, ItemStack stack) {
        if (!linked(stack)) {
            return RemoteResult.NOT_LINKED;
        }
        NBTTagCompound tag = stack.getTagCompound();
        World world = holder.worldObj;
        if (tag.getInteger(K_DIM) != world.provider.dimensionId) {
            return RemoteResult.WRONG_DIMENSION;
        }
        int x = tag.getInteger(K_X);
        int y = tag.getInteger(K_Y);
        int z = tag.getInteger(K_Z);
        if (!HandheldRules.inRange(x + 0.5 - holder.posX, y + 0.5 - holder.posY, z + 0.5 - holder.posZ, range())) {
            return RemoteResult.OUT_OF_RANGE;
        }
        if (!world.getChunkProvider()
            .chunkExists(x >> 4, z >> 4)) {
            return RemoteResult.UNLOADED;
        }
        TileWarhead warhead = Warheads.main(world, x, y, z);
        if (warhead == null) {
            return RemoteResult.GONE;
        }
        if (!HandheldRules.mayControl(warhead.ownerId(), holder.getUniqueID(), holder.capabilities.isCreativeMode)) {
            return RemoteResult.NOT_OWNER;
        }
        return warhead.arm(warhead.litFuseTicks(), holder) ? RemoteResult.LIT : RemoteResult.ALREADY_LIT;
    }

    /**
     * Server side: links the stack to the warhead at a position.
     *
     * @return true when linked; false (with a chat line) when there is no warhead or it is someone else's
     */
    public boolean linkTo(EntityPlayer holder, ItemStack stack, World world, int x, int y, int z) {
        TileWarhead warhead = Warheads.main(world, x, y, z);
        if (warhead == null) {
            return false;
        }
        if (!HandheldRules.mayControl(warhead.ownerId(), holder.getUniqueID(), holder.capabilities.isCreativeMode)) {
            holder.addChatMessage(new ChatComponentTranslation(RemoteResult.NOT_OWNER.langKey()));
            return true;
        }
        link(stack, warhead.xCoord, warhead.yCoord, warhead.zCoord, world.provider.dimensionId);
        holder.addChatMessage(
            new ChatComponentTranslation(
                "gregguns.icbm.remote.linked",
                warhead.xCoord,
                warhead.yCoord,
                warhead.zCoord));
        return true;
    }

    @Override
    public boolean onItemUseFirst(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {
        if (!player.isSneaking()) {
            return false;
        }
        if (world.isRemote) {
            // The client cannot tell whether the click links (the tile may not be synced); the server answers.
            return Warheads.main(world, x, y, z) != null;
        }
        return linkTo(player, stack, world, x, y, z);
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (world.isRemote) {
            return stack;
        }
        if (player.isSneaking()) {
            if (linked(stack)) {
                unlink(stack);
                player.addChatMessage(new ChatComponentTranslation("gregguns.icbm.remote.cleared"));
            }
            return stack;
        }
        RemoteResult r = fire(player, stack);
        if (r == RemoteResult.OUT_OF_RANGE) {
            player.addChatMessage(new ChatComponentTranslation(r.langKey(), range()));
        } else {
            player.addChatMessage(new ChatComponentTranslation(r.langKey()));
        }
        return stack;
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        boolean shift = net.minecraft.client.gui.GuiScreen.isShiftKeyDown();
        String tierLine = String
            .format(StatCollector.translateToLocal("gregguns.icbm.part.tooltip.tier"), TierText.colored(tier));
        List<String> details = new ArrayList<>();
        details.add(String.format(StatCollector.translateToLocal("gregguns.icbm.remote.tooltip.range"), range()));
        details.add(StatCollector.translateToLocal("gregguns.icbm.remote.tooltip.link"));
        details.add(StatCollector.translateToLocal("gregguns.icbm.remote.tooltip.fuse"));
        details.add(StatCollector.translateToLocal("gregguns.icbm.remote.tooltip.safety"));
        tooltip.addAll(
            HandheldTooltipFormat.lines(
                tierLine,
                StatCollector.translateToLocal("gregguns.icbm.remote.summary"),
                details,
                StatCollector.translateToLocal("gregguns.icbm.part.tooltip.shift"),
                shift));
        if (linked(stack)) {
            NBTTagCompound tag = stack.getTagCompound();
            tooltip.add(
                String.format(
                    StatCollector.translateToLocal("gregguns.icbm.remote.tooltip.linked"),
                    tag.getInteger(K_X),
                    tag.getInteger(K_Y),
                    tag.getInteger(K_Z),
                    tag.getInteger(K_DIM)));
        } else {
            tooltip.add(StatCollector.translateToLocal("gregguns.icbm.remote.tooltip.unlinked"));
        }
        if (shift) {
            tooltip.add(AddedBy.line());
        }
    }
}
