package com.gregguns.core;

import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.gregguns.core.module.ModuleContent;
import com.gregguns.item.gun.GunItems;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The single GregGuns creative tab, {@code itemGroup.gregguns}.
 *
 * <p>
 * Icon: the 9mm Pistol. The tab leaves out the content of modules that are off (it stays registered, only hidden); the
 * arrow is the fallback if the pistol is somehow missing.
 */
// Spec: docs/techguns-spec/01-core/lifecycle-and-registration.md (Creative tab: one tab for everything)
public final class GregGunsTab extends CreativeTabs {

    /** Tab label. Vanilla looks the display name up under the lang key {@code itemGroup.<label>}. */
    public static final String LABEL = "gregguns";

    /** Creates the tab. Call once, in preInit, before any item that uses it is registered. */
    public GregGunsTab() {
        super(LABEL);
    }

    /** Lists the tab's items, leaving out the content of modules that are off (it stays registered, but hidden). */
    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void displayAllReleventItems(List list) {
        super.displayAllReleventItems(list);
        list.removeIf(o -> o instanceof ItemStack && !ModuleContent.isUsable((ItemStack) o));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Item getTabIconItem() {
        Item pistol = GunItems.pistol();
        return pistol != null ? pistol : Items.arrow;
    }
}
