package com.gregguns.icbm.warhead;

import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import com.gregguns.core.text.AddedBy;
import com.gregguns.icbm.TooltipWrap;
import com.gregguns.icbm.explosive.ExplosiveDef;
import com.gregguns.icbm.explosive.Explosives;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The inert warhead (D282): the first step of the warhead chain, one subtype per casing (damage 0 for T1 to 4 for T5),
 * with an optional payload in NBT. It holds no charges. It is a plain item, not a block, so it cannot be placed, cannot
 * sit on a missile, is ignored by the silo's input bus and nothing can set it off: it is inert by construction. Loading
 * Plastic Explosive charges ({@link WarheadChargeRecipe} or the Assembler) turns it into the live warhead of its
 * casing.
 * Both sides.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/warheads-and-fill.md (The inert warhead)
public final class ItemInertWarhead extends Item {

    /** Pixels a tooltip line may take before it wraps. */
    private static final int TOOLTIP_WRAP_WIDTH = TooltipWrap.WIDTH;

    /** Creates the item: subtypes by casing, stacks of 16 like the live warhead. */
    public ItemInertWarhead() {
        setHasSubtypes(true);
        setMaxDamage(0);
        setMaxStackSize(16);
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        if (stack == null) {
            return super.getItemStackDisplayName(stack);
        }
        WarheadCasing casing = InertWarheads.casing(stack);
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
        // The bare casings first, then every payload of each casing, so NEI lists each step of the chain.
        for (WarheadCasing casing : WarheadCasing.values()) {
            list.add(InertWarheads.stack(casing, null));
        }
        for (WarheadCasing casing : WarheadCasing.values()) {
            for (WarheadBlast blast : WarheadBlast.values()) {
                if (blast != WarheadBlast.CONDENSED && blast.tier() == casing.tier()) {
                    list.add(InertWarheads.stack(casing, blast.id()));
                }
            }
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        WarheadCasing casing = InertWarheads.casing(stack);
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
