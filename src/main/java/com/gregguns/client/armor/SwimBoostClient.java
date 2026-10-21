package com.gregguns.client.armor;

import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;

import com.gregguns.armor.Armor;
import com.gregguns.armor.logic.ArmorBonus;
import com.gregguns.armor.logic.SwimBoost;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * The HEV's swim boost for the local player. Movement is client-authoritative in 1.7.10 and vanilla swimming ignores
 * the movement speed attribute, so the extra acceleration is added to the motion here, after each client tick, where
 * the next tick's movement picks it up. The server needs nothing: it takes the client's position. Client only.
 */
// Spec: docs/techguns-spec/05-armor/index.md ARM-05; math in SwimBoost
public final class SwimBoostClient {

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
        if (player == null || player.capabilities.isFlying
            || !(player.isInWater() || player.isInsideOfMaterial(Material.lava))) {
            return;
        }
        double mult = SwimBoost.multiplier(Armor.bonus(player, ArmorBonus.SPEED_WATER));
        if (mult <= 1.0) {
            return;
        }
        double[] extra = SwimBoost.extraMotion(player.moveStrafing, player.moveForward, player.rotationYaw, mult);
        double[] capped = SwimBoost.cap(player.motionX + extra[0], player.motionZ + extra[1]);
        player.motionX = capped[0];
        player.motionZ = capped[1];
    }
}
