package com.gregguns.icbm.production;

import java.util.List;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;

import com.gregguns.api.Tier;
import com.gregguns.api.Tiered;
import com.gregguns.core.text.AddedBy;
import com.gregguns.core.text.TierText;
import com.gregguns.icbm.recipe.PartTooltipFormat;

import cpw.mods.fml.common.Optional;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import micdoodle8.mods.galacticraft.api.recipe.ISchematicItem;

/**
 * The Missile Schematic: damage 0 unlocks the T1 page of the NASA Workbench, damage 1 the T2 page. It goes in the
 * workbench's schematic slot, as Galacticraft's own schematics do, so it implements Galacticraft's marker interface
 * when the mod is present. Both sides.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/build-and-recipes.md (Chip and schematic)
@Optional.Interface(iface = "micdoodle8.mods.galacticraft.api.recipe.ISchematicItem", modid = "GalacticraftCore")
public final class ItemMissileSchematic extends Item implements Tiered, ISchematicItem {

    /** Registry name without the mod id. */
    public static final String NAME = "missile_schematic";

    /** PLACEHOLDER art until the guides track delivers licensed icons: the NTM fuze icons stand in. */
    private static final String PLACEHOLDER_ICON_DIR = "gregguns:thirdparty/ntm/";

    private IIcon[] icons;

    public ItemMissileSchematic() {
        setHasSubtypes(true);
        setMaxDamage(0);
        setMaxStackSize(1);
    }

    /** @return the spec of a stack's damage, T1 for anything out of range */
    public static MissileBuildSpec specOf(ItemStack stack) {
        MissileBuildSpec s = MissileBuildSpec.byNumber(stack == null ? 1 : stack.getItemDamage() + 1);
        return s == null ? MissileBuildSpec.T1 : s;
    }

    @Override
    public Tier getTier() {
        return MissileBuildSpec.T1.tier();
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        return "item.gregguns." + NAME + "_t" + specOf(stack).number();
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void getSubItems(Item item, CreativeTabs tab, List list) {
        for (MissileBuildSpec s : MissileBuildSpec.values()) {
            list.add(new ItemStack(item, 1, s.number() - 1));
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister reg) {
        icons = new IIcon[MissileBuildSpec.values().length];
        for (MissileBuildSpec s : MissileBuildSpec.values()) {
            icons[s.number() - 1] = reg
                .registerIcon(PLACEHOLDER_ICON_DIR + (s.number() == 1 ? "fuze_assembly_mk1" : "fuze_assembly_mk2"));
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconFromDamage(int damage) {
        if (icons == null) {
            return null;
        }
        return icons[Math.max(0, Math.min(icons.length - 1, damage))];
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        MissileBuildSpec spec = specOf(stack);
        boolean shift = net.minecraft.client.gui.GuiScreen.isShiftKeyDown();
        String tierLine = String
            .format(StatCollector.translateToLocal("gregguns.icbm.part.tooltip.tier"), TierText.colored(spec.tier()));
        String summary = StatCollector.translateToLocal("gregguns.icbm.schematic.summary");
        String usedIn = String.format(
            StatCollector.translateToLocal("gregguns.icbm.part.tooltip.used"),
            StatCollector.translateToLocal("gregguns.icbm.schematic.used"));
        String hint = StatCollector.translateToLocal("gregguns.icbm.part.tooltip.shift");
        tooltip.addAll(PartTooltipFormat.lines(tierLine, summary, usedIn, hint, shift));
        if (shift) {
            tooltip.add(AddedBy.line());
        }
    }
}
