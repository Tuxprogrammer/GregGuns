package com.gregguns.item.coin;

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
import com.gregguns.core.registry.Naming;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The GregGuns Coin: one item, five denominations in the item damage ({@link CoinDenomination}). The name is the lang
 * name plus the value ("GregGuns Coin 100"), as the pack names its coins. Quests pay it and the pack's Vending Machine
 * takes it as the price of GregGuns trades: GregGuns makes it one of the machine's wallet coin kinds
 * ({@link com.gregguns.compat.vending.VendingCoin}), so it goes in the machine's coin slot like the pack's coins. If
 * that patch did not apply, the coin pays from the input slots instead. The tooltip says which. No NBT. Both sides.
 */
// Spec: docs/gregguns-spec/06-items/coins-and-vending.md
public final class ItemCoin extends Item implements Tiered {

    /** The registry name: {@code gregguns:coin}. */
    public static final String NAME = "coin";

    /** The lang key of the tooltip line when the coin is a wallet coin kind. */
    static final String TOOLTIP = "gregguns.tooltip.coin.vending";

    /** The lang key of the tooltip line when it is not (it pays from the input slots). */
    static final String TOOLTIP_ITEMS = "gregguns.tooltip.coin.vending.items";

    @SideOnly(Side.CLIENT)
    private IIcon[] icons;

    public ItemCoin() {
        setHasSubtypes(true);
        setMaxDamage(0);
        setMaxStackSize(CoinDenomination.MAX_STACK);
    }

    /**
     * @return the lowest tier: currency belongs to no tier, and no GregGuns recipe consumes a coin as an ingredient
     */
    @Override
    public Tier getTier() {
        return Tier.STEAM;
    }

    /**
     * @param denomination the denomination
     * @param count        the stack size
     * @return a stack of this item
     */
    public ItemStack stack(CoinDenomination denomination, int count) {
        return new ItemStack(this, count, denomination.meta());
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        String name = StatCollector.translateToLocal(getUnlocalizedName() + ".name")
            .trim();
        CoinDenomination d = CoinDenomination.byMeta(stack.getItemDamage());
        return d == null ? name : name + " " + d.label();
    }

    /** Where the coin goes at the Vending Machine: its coin slot, or the input slots if the wallet patch is off. */
    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        tooltip.add(
            StatCollector
                .translateToLocal(com.gregguns.compat.vending.VendingCoin.inWallet() ? TOOLTIP : TOOLTIP_ITEMS));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister register) {
        CoinDenomination[] all = CoinDenomination.values();
        icons = new IIcon[all.length];
        for (CoinDenomination d : all) {
            icons[d.meta()] = register.registerIcon(Naming.textureName(d.iconName()));
        }
        itemIcon = icons[0];
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconFromDamage(int damage) {
        CoinDenomination d = CoinDenomination.byMeta(damage);
        return d == null || icons == null ? itemIcon : icons[d.meta()];
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void getSubItems(Item item, CreativeTabs tab, List list) {
        for (CoinDenomination d : CoinDenomination.values()) {
            list.add(new ItemStack(item, 1, d.meta()));
        }
    }
}
