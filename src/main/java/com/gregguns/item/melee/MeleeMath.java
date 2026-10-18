package com.gregguns.item.melee;

/**
 * The numbers of the two melee tools as pure logic: damage from the sword or pickaxe base and the steel material
 * bonus, the critical hit, and the hit's knockback. No Minecraft types, both sides.
 */
// Spec: docs/techguns-spec/06-items/special-items.md (Combat Knife, Crowbar, TG_STEEL)
public final class MeleeMath {

    /** Durability of the steel tools, uses. */
    public static final int STEEL_DURABILITY = 1000;
    /** Harvest level of the steel tools (iron). */
    public static final int STEEL_HARVEST_LEVEL = 2;
    /** Mining speed of the steel tools. */
    public static final float STEEL_EFFICIENCY = 7.5F;
    /** Damage bonus of the steel material. */
    public static final float STEEL_DAMAGE_BONUS = 2.5F;
    /** Enchantability of the steel tools. */
    public static final int STEEL_ENCHANTABILITY = 12;
    /** Base damage of a vanilla sword. */
    public static final float SWORD_BASE = 4.0F;
    /** Base damage of a vanilla pickaxe. */
    public static final float PICKAXE_BASE = 2.0F;
    /** The bare-handed damage every player starts with. */
    public static final float HAND_DAMAGE = 1.0F;
    /** Multiplier of a critical hit (falling, not on the ground). */
    public static final float CRIT_MULTIPLIER = 1.5F;

    private MeleeMath() {}

    /** @return the attribute bonus the knife adds to the player: 4 + 2.5 = 6.5 */
    public static float knifeBonus() {
        return SWORD_BASE + STEEL_DAMAGE_BONUS;
    }

    /** @return the attribute bonus the crowbar adds to the player: 2 + 2.5 = 4.5 */
    public static float crowbarBonus() {
        return PICKAXE_BASE + STEEL_DAMAGE_BONUS;
    }

    /**
     * @param attributeDamage the player's attack-damage attribute (hand plus item plus effects)
     * @param enchantBonus    the enchantment bonus against this target
     * @param critical        whether the hit is a critical
     * @return the raw damage of a melee hit before armor: a critical multiplies the attribute only (vanilla order)
     */
    public static float damage(float attributeDamage, float enchantBonus, boolean critical) {
        float base = critical && attributeDamage > 0.0F ? attributeDamage * CRIT_MULTIPLIER : attributeDamage;
        return base + enchantBonus;
    }

    /**
     * @param knockbackEnchant the Knockback enchantment level
     * @param sprinting        whether the attacker is sprinting
     * @return the knockback strength in vanilla units (0 for none)
     */
    public static int knockback(int knockbackEnchant, boolean sprinting) {
        return knockbackEnchant + (sprinting ? 1 : 0);
    }
}
