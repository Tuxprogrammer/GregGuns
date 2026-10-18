package com.gregguns.item.ammo;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import com.gregguns.api.Tier;
import com.gregguns.api.Tiered;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * An empty magazine. Filled by a crafting recipe (empty plus loose rounds) or on a GT map (wave 3), not by right-click.
 *
 * <p>
 * Spec: docs/gregguns-spec/06-items/ammo.md (refill kept), docs/techguns-spec/06-items/ammo.md. Both sides.
 */
public final class ItemMagazineEmpty extends Item implements Tiered {

    private final MagazineType type;

    /**
     * @param type the magazine family
     */
    public ItemMagazineEmpty(MagazineType type) {
        this.type = type;
        setMaxStackSize(64);
    }

    /** @return the magazine family */
    public MagazineType type() {
        return type;
    }

    @Override
    public Tier getTier() {
        return type.tier();
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        tooltip
            .add(StatCollector.translateToLocalFormatted("gregguns.tooltip.magazine.empty", type.roundsPerMagazine()));
    }
}
