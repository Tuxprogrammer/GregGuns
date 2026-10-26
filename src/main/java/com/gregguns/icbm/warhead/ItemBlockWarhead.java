package com.gregguns.icbm.warhead;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import com.gregguns.core.text.AddedBy;
import com.gregguns.core.text.TierText;
import com.gregguns.icbm.explosive.ExplosiveDef;
import com.gregguns.icbm.explosive.Explosives;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The warhead item: one per casing tier, with the NBT {@code {explosive, fill}} (D23). It is also the item form of the
 * placed block. Both sides.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/warheads-and-fill.md
public final class ItemBlockWarhead extends ItemBlock {

    private final WarheadCasing casing;

    /** @param block a {@link BlockWarhead} */
    public ItemBlockWarhead(Block block) {
        super(block);
        this.casing = ((BlockWarhead) block).casing();
        setMaxStackSize(16);
    }

    /** @return the casing */
    public WarheadCasing casing() {
        return casing;
    }

    @Override
    public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ, int metadata) {
        if (!WarheadFillers.canPlace(world, x, y, z, casing.heightBlocks())) {
            return false;
        }
        if (!super.placeBlockAt(stack, player, world, x, y, z, side, hitX, hitY, hitZ, metadata)) {
            return false;
        }
        if (world.getTileEntity(x, y, z) instanceof TileWarhead) {
            ((TileWarhead) world.getTileEntity(x, y, z)).init(casing, stack.getTagCompound(), player);
        }
        if (!world.isRemote) {
            WarheadFillers.place(world, x, y, z, casing.heightBlocks());
        }
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        WarheadData data = WarheadStacks.read(casing, stack);
        ExplosiveDef def = Explosives.get(data.explosive());
        WarheadBlast blast = WarheadBlast.byId(data.explosive());
        int cap = WarheadStacks.cap(casing, def);
        WarheadTooltipFormat.Facts f = new WarheadTooltipFormat.Facts();
        f.explosiveName = StatCollector.translateToLocal("gregguns.icbm.explosive." + data.explosive());
        f.tierText = casing == WarheadCasing.T1 ? TierText.colored(com.gregguns.api.Tier.HV)
            : TierText.colored(com.gregguns.api.Tier.EV) + "-" + TierText.colored(com.gregguns.api.Tier.IV);
        f.fill = data.fill();
        f.cap = cap;
        f.radius = def == null ? 0 : Math.max(WarheadMath.MIN_RADIUS, def.radius(data.fill()));
        f.fuseTicks = WarheadFuse.litTicks(f.radius, com.gregguns.core.config.IcbmConfig.fuseMultiplier);
        f.centerDamage = def == null ? 0 : def.damage(data.fill());
        f.annihilates = def != null && def.annihilates();
        WarheadTooltipFormat.Labels l = labels(data.explosive(), blast);
        boolean shift = net.minecraft.client.gui.GuiScreen.isShiftKeyDown();
        for (String line : WarheadTooltipFormat.lines(l, f, shift)) {
            tooltip.add(line);
        }
        if (shift) {
            tooltip.add(AddedBy.line());
        }
    }

    @SideOnly(Side.CLIENT)
    private static WarheadTooltipFormat.Labels labels(String id, WarheadBlast blast) {
        WarheadTooltipFormat.Labels l = new WarheadTooltipFormat.Labels();
        l.explosive = StatCollector.translateToLocal("gregguns.icbm.warhead.tooltip.explosive");
        l.tier = StatCollector.translateToLocal("gregguns.icbm.warhead.tooltip.tier");
        l.fill = StatCollector.translateToLocal("gregguns.icbm.warhead.tooltip.fill");
        l.radius = StatCollector.translateToLocal("gregguns.icbm.warhead.tooltip.radius");
        l.damage = StatCollector.translateToLocal("gregguns.icbm.warhead.tooltip.damage");
        l.annihilation = StatCollector.translateToLocal("gregguns.icbm.warhead.tooltip.annihilation");
        l.shiftHint = StatCollector.translateToLocal("gregguns.icbm.warhead.tooltip.shift");
        l.detailCap = StatCollector.translateToLocal("gregguns.icbm.warhead.tooltip.cap");
        l.detailCurve = StatCollector.translateToLocal("gregguns.icbm.warhead.tooltip.curve");
        l.detailFuse = StatCollector.translateToLocal("gregguns.icbm.warhead.tooltip.fuse");
        l.detailCharge = StatCollector.translateToLocal("gregguns.icbm.warhead.tooltip.charge");
        l.detailEffect = blast == null ? null : StatCollector.translateToLocal("gregguns.icbm.effect." + id);
        return l;
    }
}
