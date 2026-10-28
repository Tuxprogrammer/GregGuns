package com.gregguns.icbm.antidote;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraftforge.event.entity.living.LivingEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * The antidote's protection (D151). The ailments of ICBM gas and of GT's radioactivity effect are potion effects, and
 * GT's hazard check reads armor only, so the antidote cannot answer it as a hazard protector. It does the next best
 * thing: a dose clears those effects at once, and for {@link AntidoteRules#PROTECTION_TICKS} ticks the guard clears
 * them again every {@link AntidoteRules#SWEEP_PERIOD} ticks, a player at a time, with no world scan. The end time is
 * kept under the player's persisted data ({@link EntityPlayer#PERSISTED_NBT_TAG}), which Forge copies on death and on
 * leaving the End, so dying does not end the protection. Server side.
 */
// Spec: docs/gregguns-icbm-spec/06-blocks-items/antidote.md
public final class AntidoteGuard {

    /** The key on the player's entity data. */
    public static final String KEY = "gg_antidote_until";

    /** Creates the guard; registered once on the Forge bus. */
    public AntidoteGuard() {}

    /**
     * @return the potions the antidote clears, every one of them harmful: poison, hunger, weakness, slowness, nausea,
     *         mining fatigue and, when IC2 is loaded, IC2 radiation, the effect that actually damages (D151). Never a
     *         beneficial effect.
     */
    static int[] cured() {
        int radiation = radiationId();
        int[] base = { Potion.poison.id, Potion.hunger.id, Potion.weakness.id, Potion.moveSlowdown.id,
            Potion.confusion.id, Potion.digSlowdown.id };
        if (radiation < 0) {
            return base;
        }
        int[] all = java.util.Arrays.copyOf(base, base.length + 1);
        all[base.length] = radiation;
        return all;
    }

    /** @return IC2's radiation potion id, or -1 when IC2 or its potion is not there */
    private static int radiationId() {
        try {
            // Source:
            // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTUtility.java#L2063-L2070
            // (applyRadioactivity adds IC2Potion.radiation, a static set by IC2 at init, with the other five effects)
            ic2.core.IC2Potion potion = ic2.core.IC2Potion.radiation;
            return potion == null ? -1 : potion.id;
        } catch (LinkageError e) {
            return -1;
        }
    }

    private static NBTTagCompound persisted(EntityPlayer player) {
        NBTTagCompound data = player.getEntityData();
        if (!data.hasKey(EntityPlayer.PERSISTED_NBT_TAG, 10)) {
            data.setTag(EntityPlayer.PERSISTED_NBT_TAG, new NBTTagCompound());
        }
        return data.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
    }

    /**
     * Gives a dose: clears the ailments and starts the protection.
     *
     * @param player the player
     * @param now    the world time
     */
    public static void dose(EntityPlayer player, long now) {
        persisted(player).setLong(KEY, AntidoteRules.protectedUntil(now));
        clear(player);
    }

    /**
     * @param player a player
     * @param now    the world time
     * @return whether the player is protected now
     */
    public static boolean isProtected(EntityPlayer player, long now) {
        return AntidoteRules.isProtected(persisted(player).getLong(KEY), now);
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
