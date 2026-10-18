package com.gregguns.item.part;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.world.World;

import com.gregguns.api.GregGunsComponent;
import com.gregguns.api.Tier;
import com.gregguns.api.Tiered;

/**
 * A healing consumable: Bandages or the Medkit. Right-click starts using it only below full health; holding it for the
 * use time heals at once and adds Regeneration, and consumes one item.
 *
 * <p>
 * Server-authoritative: the heal and the effect are applied on the server only. Common side.
 */
// Spec: docs/gregguns-spec/06-items/recipes.md (RC-61, RC-62), docs/techguns-spec/06-items/special-items.md (Medkit)
public final class ItemConsumable extends Item implements Tiered, GregGunsComponent {

    private final Tier tier;
    private final ConsumableSpec spec;

    /**
     * @param tier the D1 tier
     * @param spec the heal numbers
     */
    public ItemConsumable(Tier tier, ConsumableSpec spec) {
        this.tier = tier;
        this.spec = spec;
        setMaxStackSize(16);
    }

    @Override
    public Tier getTier() {
        return tier;
    }

    /** @return the heal numbers */
    public ConsumableSpec spec() {
        return spec;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (ConsumableSpec.canUse(player.getHealth(), player.getMaxHealth())) {
            player.setItemInUse(stack, getMaxItemUseDuration(stack));
        }
        return stack;
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return spec.useTicks();
    }

    @Override
    public EnumAction getItemUseAction(ItemStack stack) {
        return EnumAction.none;
    }

    @Override
    public ItemStack onEaten(ItemStack stack, World world, EntityPlayer player) {
        if (!player.capabilities.isCreativeMode) {
            stack.stackSize--;
        }
        if (!world.isRemote) {
            player.heal(spec.healHp());
            player.addPotionEffect(new PotionEffect(Potion.regeneration.id, spec.regenTicks(), spec.regenAmplifier()));
            world.playSoundAtEntity(player, "random.burp", 0.5F, 0.9F + world.rand.nextFloat() * 0.1F);
        }
        return stack;
    }
}
