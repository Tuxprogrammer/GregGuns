package com.gregguns.item.melee;

import java.util.List;
import java.util.Locale;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.StatCollector;

import com.gregguns.combat.DamageType;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The tooltip lines of the melee tools. Every string is a lang key. Client only.
 */
@SideOnly(Side.CLIENT)
final class MeleeTooltips {

    private MeleeTooltips() {}

    @SuppressWarnings({ "unchecked", "rawtypes" })
    static void add(List tooltip, float totalDamage, boolean crowbar) {
        tooltip.add(
            StatCollector.translateToLocalFormatted(
                "gregguns.melee.tooltip.damage",
                String.format(Locale.ROOT, "%.1f", totalDamage)));
        if (!GuiScreen.isShiftKeyDown()) {
            tooltip.add(StatCollector.translateToLocal("gregguns.gun.tooltip.shift"));
            return;
        }
        tooltip.add(
            StatCollector.translateToLocalFormatted(
                "gregguns.gun.tooltip.damage_type",
                StatCollector.translateToLocal(DamageType.PHYSICAL.nameLangKey())));
        if (crowbar) {
            tooltip.add(StatCollector.translateToLocal("gregguns.melee.tooltip.crowbar_tool"));
        }
    }
}
