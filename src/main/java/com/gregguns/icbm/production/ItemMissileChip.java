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

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The Missile Chip: damage 0 is the T1 chip (HV), damage 1 the T2 chip (EV). The Laser Engraver turns a chip into the
 * matching schematic. A plain item with a tier and a tooltip; deliberately not a {@code GregGunsComponent}, like the
 * ICBM parts, because its recipe only exists for an enabled module. Both sides.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/build-and-recipes.md (Chip and schematic)
public final class ItemMissileChip extends Item implements Tiered {

    /** Registry name without the mod id. */
    public static final String NAME = "missile_chip";

    /** PLACEHOLDER art until the guides track delivers licensed icons: the NTM fuze icons stand in. */
    private static final String PLACEHOLDER_ICON_DIR = "gregguns:thirdparty/ntm/";

    private IIcon[] icons;

    public ItemMissileChip() {
        setHasSubtypes(true);
        setMaxDamage(0);
        setMaxStackSize(64);
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
        String summary = StatCollector.translateToLocal("gregguns.icbm.chip.summary");
        String usedIn = String.format(
            StatCollector.translateToLocal("gregguns.icbm.part.tooltip.used"),
            StatCollector.translateToLocal("gregguns.icbm.chip.used"));
        String hint = StatCollector.translateToLocal("gregguns.icbm.part.tooltip.shift");
        tooltip.addAll(PartTooltipFormat.lines(tierLine, summary, usedIn, hint, shift));
        if (shift) {
            tooltip.add(AddedBy.line());
        }
    }
}
