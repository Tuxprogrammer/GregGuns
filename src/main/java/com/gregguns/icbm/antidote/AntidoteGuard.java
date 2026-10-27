package com.gregguns.icbm.antidote;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraftforge.event.entity.living.LivingEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * The antidote's protection (D151). The ailments of ICBM gas and of GT's radioactivity effect are potion effects, and
 * GT's hazard check reads armor only, so the antidote cannot answer it as a hazard protector. It does the next best
 * thing: a dose clears those effects at once, and for {@link AntidoteRules#PROTECTION_TICKS} ticks the guard clears
 * them again every {@link AntidoteRules#SWEEP_PERIOD} ticks, a player at a time, with no world scan. The end time is
 * kept on the player's own data. Server side.
 */
// Spec: docs/gregguns-icbm-spec/06-blocks-items/antidote.md
public final class AntidoteGuard {

    /** The key on the player's entity data. */
    public static final String KEY = "gg_antidote_until";

    /** Creates the guard; registered once on the Forge bus. */
    public AntidoteGuard() {}

    /** @return the potions the antidote clears: poison, hunger, weakness, slowness, nausea and mining fatigue */
    static int[] cured() {
        return new int[] { Potion.poison.id, Potion.hunger.id, Potion.weakness.id, Potion.moveSlowdown.id,
            Potion.confusion.id, Potion.digSlowdown.id };
    }

    /**
     * Gives a dose: clears the ailments and starts the protection.
     *
     * @param player the player
     * @param now    the world time
     */
    public static void dose(EntityPlayer player, long now) {
        player.getEntityData()
            .setLong(KEY, AntidoteRules.protectedUntil(now));
        clear(player);
    }

    /**
     * @param player a player
     * @param now    the world time
     * @return whether the player is protected now
     */
    public static boolean isProtected(EntityPlayer player, long now) {
        return AntidoteRules.isProtected(
            player.getEntityData()
                .getLong(KEY),
            now);
    }

    /**
     * Removes the cured effects.
     *
     * @return how many were active
     */
    public static int clear(EntityPlayer player) {
        int n = 0;
        for (int id : cured()) {
            if (player.getActivePotionEffect(Potion.potionTypes[id]) != null) {
                player.removePotionEffect(id);
                n++;
            }
        }
        return n;
    }

    /** Sweeps a protected player; a player at a time, every few ticks. */
    @SubscribeEvent
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        if (!(event.entityLiving instanceof EntityPlayer) || event.entityLiving.worldObj.isRemote) {
            return;
        }
        long now = event.entityLiving.worldObj.getTotalWorldTime();
        if (!AntidoteRules.sweepNow(now)) {
            return;
        }
        EntityPlayer p = (EntityPlayer) event.entityLiving;
        if (isProtected(p, now)) {
            clear(p);
        }
    }

}
