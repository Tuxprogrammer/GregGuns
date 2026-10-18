package com.gregguns.item.part;

import net.minecraft.item.Item;

import com.gregguns.api.GregGunsComponent;
import com.gregguns.api.Tier;
import com.gregguns.api.Tiered;

/**
 * A GregGuns-only component made from GT parts (a barrel, mechanical parts). It has a tier and nothing else; guns
 * and other recipes consume it as an ingredient.
 *
 * <p>
 * Spec: docs/gregguns-spec/06-items/recipes.md (component index). Both sides.
 */
public final class ItemPart extends Item implements Tiered, GregGunsComponent {

    private final Tier tier;

    /**
     * @param tier the D1 tier the part belongs to
     */
    public ItemPart(Tier tier) {
        this.tier = tier;
        setMaxStackSize(64);
    }

    @Override
    public Tier getTier() {
        return tier;
    }
}
