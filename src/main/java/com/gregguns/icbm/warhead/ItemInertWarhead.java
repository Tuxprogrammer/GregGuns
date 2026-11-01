package com.gregguns.icbm.warhead;

import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import com.gregguns.api.Tier;
import com.gregguns.api.Tiered;
import com.gregguns.core.text.AddedBy;
import com.gregguns.icbm.TooltipWrap;
import com.gregguns.icbm.explosive.ExplosiveDef;
import com.gregguns.icbm.explosive.Explosives;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The inert warhead (D282): the first step of the warhead chain, one item per casing ({@code inert_warhead_t1} to
 * {@code _t5}), with an optional payload in NBT. It holds no charges. It is a plain item, not a block, so it cannot be
 * placed, cannot
 * sit on a missile, is ignored by the silo's input bus and nothing can set it off: it is inert by construction. Loading
 * Plastic Explosive charges ({@link WarheadChargeRecipe} or the Assembler) turns it into the live warhead of its
 * casing.
 * Both sides.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/warheads-and-fill.md (The inert warhead)
public final class ItemInertWarhead extends Item implements Tiered {

    /** Pixels a tooltip line may take before it wraps. */
    private static final int TOOLTIP_WRAP_WIDTH = TooltipWrap.WIDTH;

    private final WarheadCasing casing;

    /**
     * Creates the item: stacks of 16 like the live warhead.
     *
     * @param casing the casing this inert warhead is
     */
    public ItemInertWarhead(WarheadCasing casing) {
        this.casing = casing;
        setMaxDamage(0);
        setMaxStackSize(16);
    }

    /** @return the casing */
    public WarheadCasing casing() {
        return casing;
    }

    /** @return the live warhead's tier, as {@link BlockWarhead#getTier()} gives it: HV, EV, then ZPM from T3 up */
    @Override
    public Tier getTier() {
        return casing == WarheadCasing.T1 ? Tier.HV : casing == WarheadCasing.T2 ? Tier.EV : Tier.ZPM;
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        if (stack == null) {
            return super.getItemStackDisplayName(stack);
        }
        String payload = InertWarheads.payload(stack);
        return InertWarheadTooltipFormat.displayName(
            StatCollector.translateToLocal("gregguns.icbm.inert.name"),
            StatCollector.translateToLocal("gregguns.icbm.inert.name_payload"),
            payload == null ? null : StatCollector.translateToLocal("gregguns.icbm.explosive." + payload),
            casing.number());
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void getSubItems(Item item, CreativeTabs tab, List list) {
        // The bare casing first, then every payload it takes, so NEI lists each step of the chain.
        list.add(InertWarheads.stack(casing, null));
        for (WarheadBlast blast : WarheadBlast.values()) {
            if (blast != WarheadBlast.CONDENSED && blast.tier() == casing.tier()) {
                list.add(InertWarheads.stack(casing, blast.id()));
            }
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        String payload = InertWarheads.payload(stack);
        String explosive = WarheadCharging.explosiveFor(casing, payload);
        ExplosiveDef def = explosive == null ? null : Explosives.get(explosive);
        InertWarheadTooltipFormat.Facts f = new InertWarheadTooltipFormat.Facts();
        f.payloadName = payload == null ? null : StatCollector.translateToLocal("gregguns.icbm.explosive." + payload);
        f.t1 = casing == WarheadCasing.T1;
        f.tierText = ItemBlockWarhead.tierText(casing);
        f.cap = WarheadStacks.cap(casing, def);
        InertWarheadTooltipFormat.Labels l = new InertWarheadTooltipFormat.Labels();
        l.payload = StatCollector.translateToLocal("gregguns.icbm.inert.tooltip.payload");
        l.payloadNoneT1 = StatCollector.translateToLocal("gregguns.icbm.inert.tooltip.payload_none_t1");
        l.payloadNone = StatCollector.translateToLocal("gregguns.icbm.inert.tooltip.payload_none");
        l.tier = StatCollector.translateToLocal("gregguns.icbm.warhead.tooltip.tier");
        l.charges = StatCollector.translateToLocal("gregguns.icbm.warhead.tooltip.fill");
        l.inert = StatCollector.translateToLocal("gregguns.icbm.inert.tooltip.inert");
        l.shiftHint = StatCollector.translateToLocal("gregguns.icbm.warhead.tooltip.shift");
        l.nextBareT1 = StatCollector.translateToLocal("gregguns.icbm.inert.tooltip.next_bare_t1");
        l.nextBare = StatCollector.translateToLocal("gregguns.icbm.inert.tooltip.next_bare");
        l.nextFitted = StatCollector.translateToLocal("gregguns.icbm.inert.tooltip.next_fitted");
        l.howToCharge = StatCollector.translateToLocal("gregguns.icbm.inert.tooltip.how");
        l.dearmFitted = StatCollector.translateToLocal("gregguns.icbm.inert.tooltip.dearm_fitted");
        l.dearmBare = StatCollector.translateToLocal("gregguns.icbm.inert.tooltip.dearm_bare");
        boolean shift = net.minecraft.client.gui.GuiScreen.isShiftKeyDown();
        net.minecraft.client.gui.FontRenderer font = net.minecraft.client.Minecraft.getMinecraft().fontRenderer;
        for (String line : InertWarheadTooltipFormat.lines(l, f, shift)) {
            tooltip.addAll(font.listFormattedStringToWidth(line, TOOLTIP_WRAP_WIDTH));
        }
        if (shift) {
            tooltip.add(AddedBy.line());
        }
    }
}
