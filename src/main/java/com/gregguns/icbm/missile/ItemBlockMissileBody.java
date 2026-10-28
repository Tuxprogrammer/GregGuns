package com.gregguns.icbm.missile;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import com.gregguns.core.text.AddedBy;
import com.gregguns.core.text.TierText;
import com.gregguns.icbm.IcbmAvailability;
import com.gregguns.icbm.warhead.TileWarheadFiller;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The missile body item and the way it is placed: onto the centre of a Galacticraft pad (a click on any of the pad's
 * nine blocks lands on the centre), or onto the top of any solid block, standing full height. Both sides.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/stack-and-pad.md (Placing the body)
public final class ItemBlockMissileBody extends ItemBlock {

    private final MissileTier tier;

    /** @param block a {@link BlockMissileBody} */
    public ItemBlockMissileBody(Block block) {
        super(block);
        this.tier = ((BlockMissileBody) block).missileTier();
        setMaxStackSize(1);
    }

    /**
     * @param stack a body stack
     * @return true when it is a generated complex's damaged body: item damage {@link #SALVAGE_DAMAGE}, so GT recipes
     *         (which match item and damage, not NBT) never take it for an intact body, or the legacy NBT flag
     */
    public static boolean isSalvage(ItemStack stack) {
        return stack != null
            && (stack.getItemDamage() == SALVAGE_DAMAGE || stack.hasTagCompound() && stack.getTagCompound()
                .getBoolean(com.gregguns.icbm.worldgen.SiloBonusItems.K_DAMAGED));
    }

    /** The item damage of a damaged body: a different item for every GT recipe, with its own recycling data. */
    public static final int SALVAGE_DAMAGE = 1;

    /** @return the tier */
    public MissileTier tier() {
        return tier;
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {
        if (isSalvage(stack)) {
            return false; // a damaged body is salvage only (D17)
        }
        int[] pad = IcbmAvailability.galacticraftPresent() ? MissileGc.padCenter(world, x, y, z) : null;
        int bx;
        int by;
        int bz;
        if (pad != null) {
            // On a pad only a click on its top places, and always on the centre cell. A T3 body is too big for a
            // free-standing pad, but fits the floor of a formed silo (D87).
            if (side != 1
                || tier.siloOnly() && !com.gregguns.icbm.silo.MTEMissileSilo.isFloorOf(world, pad[0], pad[1], pad[2])) {
                return false;
            }
            bx = pad[0];
            by = pad[1] + 1;
            bz = pad[2];
        } else {
            if (side != 1) {
                return false;
            }
            bx = x;
            by = y + 1;
            bz = z;
            if (!world.getBlock(x, y, z)
                .isSideSolid(world, x, y, z, net.minecraftforge.common.util.ForgeDirection.UP)) {
                return false;
            }
        }
        if (!canPlace(world, bx, by, bz) || !player.canPlayerEdit(bx, by, bz, side, stack)) {
            return false;
        }
        if (!world.isRemote) {
            world.setBlock(bx, by, bz, field_150939_a, 0, 3);
            TileEntityHolder.place(world, bx, by, bz, tier, MissileBodies.guidanceOf(stack), pad != null, player);
            if (!player.capabilities.isCreativeMode) {
                stack.stackSize--;
            }
            world.playSoundEffect(bx + 0.5, by + 0.5, bz + 0.5, "dig.stone", 1.0F, 0.8F);
        }
        return true;
    }

    private boolean canPlace(World world, int bx, int by, int bz) {
        for (int i = 0; i < tier.heightBlocks() + 1; i++) {
            if (by + i > 255) {
                return false;
            }
            Block b = world.getBlock(bx, by + i, bz);
            if (!b.isReplaceable(world, bx, by + i, bz) && !b.isAir(world, bx, by + i, bz)) {
                return false;
            }
        }
        return true;
    }

    /** Tile setup kept apart so the placement method stays short. */
    private static final class TileEntityHolder {

        static void place(World world, int bx, int by, int bz, MissileTier tier, int guidance, boolean pad,
            EntityPlayer owner) {
            net.minecraft.tileentity.TileEntity t = world.getTileEntity(bx, by, bz);
            if (t instanceof TileMissileBody) {
                ((TileMissileBody) t).init(guidance, pad, owner);
            }
            for (int i = 1; i < tier.heightBlocks(); i++) {
                world.setBlock(bx, by + i, bz, MissileBlocks.filler(), 0, 2);
                net.minecraft.tileentity.TileEntity f = world.getTileEntity(bx, by + i, bz);
                if (f instanceof TileWarheadFiller) {
                    ((TileWarheadFiller) f).setOffset(0, -i, 0);
                }
            }
        }
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        return StatCollector.translateToLocalFormatted("gregguns.icbm.missile_body.name", tier.number());
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        if (isSalvage(stack)) {
            tooltip.add(StatCollector.translateToLocal("gregguns.icbm.missile_body.damaged"));
        }
        MissileTooltipFormat.Facts f = new MissileTooltipFormat.Facts();
        f.tierText = tier == MissileTier.T1 ? TierText.colored(com.gregguns.api.Tier.HV)
            : tier == MissileTier.T2
                ? TierText.colored(com.gregguns.api.Tier.EV) + "-" + TierText.colored(com.gregguns.api.Tier.IV)
                : TierText.colored(com.gregguns.api.Tier.LUV) + "-" + TierText.colored(com.gregguns.api.Tier.ZPM);
        f.fuelMb = FuelMath.tank(tier.baseFuelMb(), com.gregguns.core.config.IcbmConfig.missileFuelFactor);
        f.minRange = tier.minRange();
        f.maxRange = (int) Math.round(tier.baseMaxRange() * com.gregguns.core.config.IcbmConfig.missileRangeFactor);
        f.guidance = MissileBodies.guidanceOf(stack);
        f.warheadTier = tier.warheadTier()
            .number();
        MissileTooltipFormat.Labels l = new MissileTooltipFormat.Labels();
        l.tier = StatCollector.translateToLocal("gregguns.icbm.missile_body.tooltip.tier");
        l.fuel = StatCollector.translateToLocal("gregguns.icbm.missile_body.tooltip.fuel");
        l.range = StatCollector.translateToLocal("gregguns.icbm.missile_body.tooltip.range");
        l.guidance = StatCollector.translateToLocal("gregguns.icbm.missile_body.tooltip.guidance");
        l.shiftHint = StatCollector.translateToLocal("gregguns.icbm.warhead.tooltip.shift");
        l.detailWarhead = StatCollector.translateToLocal("gregguns.icbm.missile_body.tooltip.warhead");
        l.detailStack = StatCollector.translateToLocal("gregguns.icbm.missile_body.tooltip.stack");
        l.detailBonus = StatCollector.translateToLocal("gregguns.icbm.missile_body.tooltip.bonus");
        boolean shift = net.minecraft.client.gui.GuiScreen.isShiftKeyDown();
        net.minecraft.client.gui.FontRenderer font = net.minecraft.client.Minecraft.getMinecraft().fontRenderer;
        for (String line : MissileTooltipFormat.lines(l, f, shift)) {
            tooltip.addAll(font.listFormattedStringToWidth(line, 200));
        }
        if (shift) {
            tooltip.add(AddedBy.line());
        }
    }
}
