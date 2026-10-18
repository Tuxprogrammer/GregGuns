package com.gregguns.armor.oxygen.gc;

import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import com.gregguns.api.Tier;
import com.gregguns.api.Tiered;

import micdoodle8.mods.galacticraft.core.items.ItemOxygenMask;

/**
 * The Oxygen Mask when Galacticraft is loaded: a subclass of Galacticraft's {@code ItemOxygenMask}, so it fits the
 * mask slot of Galacticraft's own inventory, and GC does the air accounting. Lives in this package so it is loaded only
 * when GalacticraftCore is present (GGS-05-23). Server and client.
 */
// Spec: docs/gregguns-spec/05-armor/oxygen-gear.md (GGS-05-22, GGS-05-23, GGS-05-28)
// Source: GC slots accept items by class, mask slot instanceof ItemOxygenMask:
// https://github.com/GTNewHorizons/Galacticraft/blob/3.4.34-GTNH/src/main/java/micdoodle8/mods/galacticraft/core/util/OxygenUtil.java#L375-L385
// Source: ItemOxygenMask(String) is protected and sets the texture and tab; the registrar overrides both:
// https://github.com/GTNewHorizons/Galacticraft/blob/3.4.34-GTNH/src/main/java/micdoodle8/mods/galacticraft/core/items/ItemOxygenMask.java
public final class OxygenMaskGC extends ItemOxygenMask implements Tiered {

    private CreativeTabs tab;

    /** Creates the mask. */
    public OxygenMaskGC() {
        super("oxygen_mask");
    }

    @Override
    public Item setCreativeTab(CreativeTabs tab) {
        this.tab = tab;
        return super.setCreativeTab(tab);
    }

    @Override
    public CreativeTabs getCreativeTab() {
        // Galacticraft's override returns its own tab; GregGuns items belong in the GregGuns tab.
        return tab;
    }

    @Override
    public Tier getTier() {
        return Tier.HV;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        tooltip.add(StatCollector.translateToLocal("gregguns.equipment.tooltip.space_suit_note"));
    }
}
