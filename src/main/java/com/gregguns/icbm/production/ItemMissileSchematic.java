package com.gregguns.icbm.production;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import com.gregguns.api.Tier;
import com.gregguns.api.Tiered;
import com.gregguns.core.text.AddedBy;
import com.gregguns.core.text.TierText;
import com.gregguns.icbm.TooltipWrap;
import com.gregguns.icbm.recipe.PartTooltipFormat;

import cpw.mods.fml.common.Optional;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import micdoodle8.mods.galacticraft.api.recipe.ISchematicItem;

/**
 * The Missile Schematic of one body tier: a plain item with a tier and a tooltip, registered once per tier as
 * {@code missile_schematic_t1} and {@code missile_schematic_t2}. It goes in a NASA Workbench's schematic slot, as
 * Galacticraft's own schematics do, so it implements Galacticraft's marker interface when the mod is present.
 * Deliberately not a {@code GregGunsComponent}, like the ICBM parts, because its recipe only exists for an enabled
 * module. Both sides.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/build-and-recipes.md (Chip and schematic)
@Optional.Interface(iface = "micdoodle8.mods.galacticraft.api.recipe.ISchematicItem", modid = "GalacticraftCore")
public final class ItemMissileSchematic extends Item implements Tiered, ISchematicItem {

    /**
     * @param spec a body tier
     * @return the bare registry name of that tier's item
     */
    public static String name(MissileBuildSpec spec) {
        return "missile_schematic_t" + spec.number();
    }

    private final MissileBuildSpec spec;

    /** @param spec which body tier this schematic is for */
    public ItemMissileSchematic(MissileBuildSpec spec) {
        this.spec = spec;
        setMaxStackSize(1);
    }

    @Override
    public Tier getTier() {
        return spec.tier();
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        boolean shift = net.minecraft.client.gui.GuiScreen.isShiftKeyDown();
        String tierLine = String
            .format(StatCollector.translateToLocal("gregguns.icbm.part.tooltip.tier"), TierText.colored(spec.tier()));
        String summary = StatCollector.translateToLocal("gregguns.icbm.schematic.summary");
        String usedIn = String.format(
            StatCollector.translateToLocal("gregguns.icbm.part.tooltip.used"),
            StatCollector.translateToLocal("gregguns.icbm.schematic.used"));
        String hint = StatCollector.translateToLocal("gregguns.icbm.part.tooltip.shift");
        TooltipWrap.addAll(tooltip, PartTooltipFormat.lines(tierLine, summary, usedIn, hint, shift));
        if (shift) {
            TooltipWrap.add(tooltip, AddedBy.line());
        }
    }
}
