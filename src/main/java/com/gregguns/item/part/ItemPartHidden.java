package com.gregguns.item.part;

import net.minecraft.item.Item;

import com.gregguns.api.GregGunsComponent;
import com.gregguns.api.HiddenFromCreativeTab;
import com.gregguns.api.Tier;
import com.gregguns.api.Tiered;

/**
 * A technical component that is made by use and never crafted, so it is kept out of the creative tab: the depleted
 * Nuclear Power Cell. Spec: docs/gregguns-spec/06-items/recipes.md (GGS-06-42), nei-visibility.md. Both sides.
 */
public final class ItemPartHidden extends Item implements Tiered, HiddenFromCreativeTab, GregGunsComponent {

    private final Tier tier;

    /**
     * @param tier the D1 tier
     */
    public ItemPartHidden(Tier tier) {
        this.tier = tier;
        setMaxStackSize(64);
    }

    @Override
    public Tier getTier() {
        return tier;
    }
}
