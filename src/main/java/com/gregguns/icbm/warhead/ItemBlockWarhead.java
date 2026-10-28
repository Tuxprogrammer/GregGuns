package com.gregguns.icbm.warhead;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import com.gregguns.core.config.IcbmConfig;
import com.gregguns.core.text.AddedBy;
import com.gregguns.core.text.TierText;
import com.gregguns.icbm.explosive.ExplosiveDef;
import com.gregguns.icbm.explosive.Explosives;
import com.gregguns.icbm.warhead.effects.Effects;
import com.gregguns.icbm.warhead.effects.EnderRules;
import com.gregguns.icbm.warhead.effects.RedMatterModel;

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
    public String getItemStackDisplayName(ItemStack stack) {
        // D58: "<Explosive> T<casing> Warhead". The stack read clamps an unknown explosive to condensed. Without NBT
        // this is still a valid name (the default explosive), so only a null stack falls back to the block name.
        if (stack == null) {
            return super.getItemStackDisplayName(stack);
        }
        WarheadData data = WarheadStacks.read(casing, stack);
        return WarheadTooltipFormat.displayName(
            StatCollector.translateToLocal("gregguns.icbm.warhead.name"),
            StatCollector.translateToLocal("gregguns.icbm.explosive." + data.explosive()),
            casing.number());
    }

    /** Pixels a tooltip line may take before it wraps. */
    private static final int TOOLTIP_WRAP_WIDTH = 200;

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
            : casing == WarheadCasing.T2
                ? TierText.colored(com.gregguns.api.Tier.EV) + "-" + TierText.colored(com.gregguns.api.Tier.IV)
                : casing == WarheadCasing.T3
                    ? TierText.colored(com.gregguns.api.Tier.LUV) + "-" + TierText.colored(com.gregguns.api.Tier.ZPM)
                    : TierText.coloredIndex(
                        casing.tier()
                            .minVoltageTier());
        f.fill = data.fill();
        f.cap = cap;
        f.radius = def == null ? 0 : Math.max(WarheadMath.MIN_RADIUS, def.radius(data.fill()));
        f.fuseTicks = WarheadFuse.litTicks(f.radius, com.gregguns.core.config.IcbmConfig.fuseMultiplier);
        f.centerDamage = def == null ? 0 : def.damage(data.fill());
        f.annihilates = def != null && def.showsAnnihilation();
        WarheadTooltipFormat.Labels l = labels(data.explosive(), blast);
        boolean shift = net.minecraft.client.gui.GuiScreen.isShiftKeyDown();
        // The Shift details are full sentences; wrap them so the tooltip stays inside the screen.
        net.minecraft.client.gui.FontRenderer font = net.minecraft.client.Minecraft.getMinecraft().fontRenderer;
        for (String line : WarheadTooltipFormat.lines(l, f, shift)) {
            tooltip.addAll(font.listFormattedStringToWidth(line, TOOLTIP_WRAP_WIDTH));
        }
        if (shift) {
            tooltip.add(AddedBy.line());
        }
    }

    /**
     * The numbers an effect line prints, read from the config and the effect constants so the text cannot drift from
     * the game (lang strings take them as {@code %s}).
     *
     * @param id a blast id
     * @return the format arguments for {@code gregguns.icbm.effect.<id>}; empty when the line has none
     */
    static Object[] effectArgs(String id) {
        switch (id) {
            case "antimatter":
                return new Object[] { (int) RedMatterModel.ANTIMATTER_REACH };
            case "redmatter":
                return new Object[] { IcbmConfig.redMatterLifetimeSeconds };
            case "ender":
                return new Object[] { EnderRules.DURATION_TICKS / 20, (int) EnderRules.TELEPORT_RANGE,
                    EnderRules.TARGET_MIN, EnderRules.TARGET_SPREAD, IcbmConfig.enderMaxTeleports };
            case "antigrav":
                return new Object[] { IcbmConfig.antigravMaxBlocks };
            case "breaching":
                return new Object[] { Effects.BREACH_DEPTH, (int) Effects.BREACH_MAX_HARDNESS };
            case "debilitation":
            case "chemical":
            case "smoke":
            case "contagious":
                return new Object[] { Effects.GAS_TICKS / 20 };
            default:
                return new Object[0];
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
        l.detailEffect = blast == null ? null
            : StatCollector.translateToLocalFormatted("gregguns.icbm.effect." + id, effectArgs(id));
        return l;
    }
}
