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
 * A loose round (for example the 9mm bullet). One item per {@link AmmoType}; guns and magazine recipes find it by the
 * type's id.
 *
 * <p>
 * Spec: docs/techguns-spec/06-items/ammo.md, docs/gregguns-spec/06-items/ammo.md. Both sides.
 */
public final class ItemAmmoRound extends Item implements Tiered {

    private final AmmoType type;

    /**
     * @param type the caliber this item represents
     */
    public ItemAmmoRound(AmmoType type) {
        this.type = type;
        setMaxStackSize(64);
    }

    /** @return the ammo type */
    public AmmoType type() {
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
        tooltip.add(StatCollector.translateToLocal("gregguns.tooltip.ammo.round"));
        String flavor = "gregguns.tooltip.ammo.flavor." + type.id();
        if (StatCollector.canTranslate(flavor)) {
            tooltip.add(StatCollector.translateToLocal(flavor));
        }
        AmmoTooltips.forRound(tooltip, type.roundItemName());
    }
}
