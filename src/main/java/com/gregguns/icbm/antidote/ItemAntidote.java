package com.gregguns.icbm.antidote;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import com.gregguns.api.Tier;
import com.gregguns.api.Tiered;
import com.gregguns.core.text.AddedBy;
import com.gregguns.core.text.TierText;
import com.gregguns.icbm.TooltipWrap;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The antidote (D10, D151): a syringe. Using it takes {@value AntidoteRules#USE_TICKS} ticks, then it cures the
 * ailments ICBM gas and fallout cause and guards against them for three minutes ({@link AntidoteGuard}). Both sides.
 */
// Spec: docs/gregguns-icbm-spec/06-blocks-items/antidote.md
public final class ItemAntidote extends Item implements Tiered {

    /** The registry name. */
    public static final String NAME = "antidote";

    /** Creates the antidote. */
    public ItemAntidote() {
        setMaxStackSize(8);
    }

    @Override
    public Tier getTier() {
        return Tier.HV;
    }

    @Override
    public EnumAction getItemUseAction(ItemStack stack) {
        return EnumAction.drink;
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return AntidoteRules.USE_TICKS;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        player.setItemInUse(stack, getMaxItemUseDuration(stack));
        return stack;
    }

    @Override
    public ItemStack onEaten(ItemStack stack, World world, EntityPlayer player) {
        if (!world.isRemote) {
            AntidoteGuard.dose(player, world.getTotalWorldTime());
            player.addChatMessage(new ChatComponentTranslation("gregguns.icbm.antidote.dosed"));
        }
        if (!player.capabilities.isCreativeMode) {
            stack.stackSize--;
        }
        return stack;
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        boolean shift = net.minecraft.client.gui.GuiScreen.isShiftKeyDown();
        String tierLine = String
            .format(StatCollector.translateToLocal("gregguns.icbm.part.tooltip.tier"), TierText.colored(getTier()));
        List<String> details = new ArrayList<>();
        details.add(StatCollector.translateToLocal("gregguns.icbm.antidote.tooltip.cures"));
        details.add(
            String.format(
                StatCollector.translateToLocal("gregguns.icbm.antidote.tooltip.guard"),
                AntidoteRules.PROTECTION_TICKS / 20L));
        details.add(StatCollector.translateToLocal("gregguns.icbm.antidote.tooltip.armor"));
        TooltipWrap.addAll(
            tooltip,
            com.gregguns.icbm.tools.HandheldTooltipFormat.lines(
                tierLine,
                StatCollector.translateToLocal("gregguns.icbm.antidote.summary"),
                details,
                StatCollector.translateToLocal("gregguns.icbm.part.tooltip.shift"),
                shift));
        if (shift) {
            TooltipWrap.add(tooltip, AddedBy.line());
        }
    }
}
