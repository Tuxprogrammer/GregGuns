package com.gregguns.core;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.item.Item;

import com.gregguns.item.gun.GunItems;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The single GregGuns creative tab, {@code itemGroup.gregguns}.
 *
 * <p>
 * Icon: the 9mm Pistol when the guns module is enabled. With the module off no GregGuns item exists, so the tab falls
 * back to a vanilla arrow.
 */
// Spec: docs/techguns-spec/01-core/lifecycle-and-registration.md (Creative tab: one tab for everything)
public final class GregGunsTab extends CreativeTabs {

    /** Tab label. Vanilla looks the display name up under the lang key {@code itemGroup.<label>}. */
    public static final String LABEL = "gregguns";

    /** Creates the tab. Call once, in preInit, before any item that uses it is registered. */
    public GregGunsTab() {
        super(LABEL);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Item getTabIconItem() {
        Item pistol = GunItems.pistol();
        return pistol != null ? pistol : Items.arrow;
    }
}
