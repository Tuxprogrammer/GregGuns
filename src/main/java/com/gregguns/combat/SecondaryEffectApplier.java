package com.gregguns.combat;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

/**
 * Applies a hit's secondary effect to the target, unless full GT hazmat for the matching hazard protects it. Direct
 * damage is never affected by hazmat (GGS-02-22). Server side.
 */
// Spec: docs/gregguns-spec/02-guns/damage-interop.md (GGS-02-22)
final class SecondaryEffectApplier {

    private SecondaryEffectApplier() {}

    /**
     * @param target the entity that took damage
     * @param hit    the hit's properties
     */
    static void apply(EntityLivingBase target, HitProperties hit) {
        if (!hit.secondaryEffectsEnabled()) {
            return;
        }
        SecondaryEffect effect = SecondaryEffects.forType(hit.type());
        if (effect == null) {
            return;
        }
        HazardKind hazard = hit.type()
            .hazard();
        if (hazard != null && GregTechHazmat.isProtected(target, hazard)) {
            return;
        }
        int ticks = effect.durationTicks();
        int amp = effect.amplifier();
        switch (effect.kind()) {
            case IGNITE:
                target.setFire(Math.max(1, ticks / 20));
                break;
            case SLOWNESS:
                target.addPotionEffect(new PotionEffect(Potion.moveSlowdown.id, ticks, amp));
                break;
            case POISON:
                target.addPotionEffect(new PotionEffect(Potion.poison.id, ticks, amp));
                break;
            case SHOCK:
                target.addPotionEffect(new PotionEffect(Potion.digSlowdown.id, ticks, amp));
                break;
            case RADIATION_SICKNESS:
                // Source:
                // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTUtility.java#L2040-L2060
                // GT's own radioactivity skips undead and arthropods; so do we.
                EnumCreatureAttribute kind = target.getCreatureAttribute();
                if (kind == EnumCreatureAttribute.UNDEAD || kind == EnumCreatureAttribute.ARTHROPOD) {
                    return;
                }
                target.addPotionEffect(new PotionEffect(Potion.weakness.id, ticks, amp));
                target.addPotionEffect(new PotionEffect(Potion.hunger.id, ticks, amp));
                target.addPotionEffect(new PotionEffect(Potion.confusion.id, ticks, amp));
                break;
            default:
                break;
        }
    }
}
