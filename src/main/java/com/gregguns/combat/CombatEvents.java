package com.gregguns.combat;

import net.minecraft.util.DamageSource;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * The Forge event handlers of the damage system. GregGuns never cancels {@code LivingAttackEvent} and never
 * re-implements vanilla's {@code attackEntityFrom}: other mods' listeners see every hit (GGS-02-18), and the model runs
 * inside {@code LivingHurtEvent} by setting the final amount ({@code docs/design/0009} decision 3). Server side only;
 * both handlers ignore the client world and every source that is not a {@link GunDamageSource}.
 */
// Spec: docs/gregguns-spec/02-guns/damage-interop.md; docs/design/0009 decision 3
public final class CombatEvents {

    /**
     * Lets a hit that ignores invulnerability frames land. Vanilla's {@code attackEntityFrom} would otherwise drop or
     * shrink it ({@code damage - lastDamage}) while the frames run. The frames are cleared for this call and restored
     * in {@link #onLivingHurt}, so the hit neither checks nor sets them.
     *
     * <p>
     * Runs last, so a mod that cancels the attack earlier leaves the target's frames alone. A hit that vanilla still
     * rejects after this point (dead or invulnerable target, Fire Resistance against fire) leaves the frames cleared.
     *
     * @param event the attack event; never cancelled here
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onLivingAttack(LivingAttackEvent event) {
        if (event.entityLiving.worldObj.isRemote || !(event.source instanceof GunDamageSource)) {
            return;
        }
        HitProperties hit = ((GunDamageSource) event.source).hit();
        hit.setSavedHurtResistantTime(-1);
        if (hit.ignoresInvulnerabilityFrames()) {
            hit.setSavedHurtResistantTime(event.entityLiving.hurtResistantTime);
            event.entityLiving.hurtResistantTime = 0;
        }
    }

    /**
     * Applies the damage model to a GregGuns hit: penetration, armour (once), per-type values, then the secondary
     * effect. Sets {@code event.ammount} to the final amount. Runs at LOW priority so other mods' pre-armour changes
     * to the amount are included.
     *
     * @param event the hurt event; its amount is the raw damage before armour
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public void onLivingHurt(LivingHurtEvent event) {
        DamageSource source = event.source;
        if (event.entityLiving.worldObj.isRemote || !(source instanceof GunDamageSource)) {
            return;
        }
        GunDamageSource gun = (GunDamageSource) source;
        HitProperties hit = gun.hit();
        if (hit.savedHurtResistantTime() >= 0) {
            // attackEntityFrom set the frames before damageEntity; put back what was there before this hit.
            event.entityLiving.hurtResistantTime = hit.savedHurtResistantTime();
            hit.setSavedHurtResistantTime(-1);
        }
        event.ammount = HitResolver.resolve(event.entityLiving, gun, event.ammount);
        if (event.ammount > 0.0F) {
            SecondaryEffectApplier.apply(event.entityLiving, hit);
        }
    }
}
