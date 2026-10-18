package com.gregguns.armor.flight;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;

import com.gregguns.armor.flight.logic.BackKind;
import com.gregguns.armor.flight.logic.FlightPhysics;
import com.gregguns.armor.flight.logic.FlightStatus;
import com.gregguns.armor.flight.logic.JumpPackFall;
import com.gregguns.armor.slots.ElectricGear;
import com.gregguns.core.config.FlightConfig;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * Fall and jump hooks of the back devices, registered on the Forge bus by the armor module.
 *
 * <p>
 * Fall: priority {@code LOW}, so it runs after the pack's own fall handlers and after the armor module's boot bonuses,
 * and sees the distance they left. A canceled event is never received, so a fall another mod already negated costs no
 * EU. The Glider and the Anti Gravity Device zero the distance only for a fall that ended within a few ticks of their
 * own assistance; the Jump Pack shortens it only when it can pay ({@link JumpPackFall}). An empty device changes
 * nothing (GGS-05-19).
 *
 * <p>
 * Jump: the Jump Pack boost is applied on both sides of the jump event. The client side adds the motion (it owns the
 * motion) when the server's last status says the pack can pay; the server side spends the EU.
 *
 * <p>
 * Server and client.
 */
// Spec: docs/gregguns-spec/05-armor/powered-flight.md (GGS-05-18, GGS-05-19); docs/techguns-spec/05-armor/
// special-equipment.md 4.4
// Source: the pack's server-side fall handler shortens a 5-block fall to 4 before ours (implementation-notes,
// "Pack fall handlers"), so numbers here are relative to that baseline.
public final class FlightEvents {

    /**
     * Applies fall assistance to a falling player.
     *
     * @param event the fall; handled on the server only
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public void onFall(LivingFallEvent event) {
        if (!(event.entityLiving instanceof EntityPlayer) || event.entityLiving.worldObj.isRemote
            || event.distance <= 0.0F) {
            return;
        }
        EntityPlayer player = (EntityPlayer) event.entityLiving;
        ItemStack stack = Flight.wornDevice(player);
        if (stack == null) {
            return;
        }
        if (stack.getItem() instanceof ItemJumpPack) {
            JumpPackFall.Outcome out = JumpPackFall
                .resolve(event.distance, ElectricGear.charge(stack), FlightConfig.jumpPackFallCostEu);
            if (out.paid() && ElectricGear.use(stack, FlightConfig.jumpPackFallCostEu, player)) {
                event.distance = (float) out.distance();
            }
        } else if (Flight.server()
            .assistedRecently(player)) {
                // Glider: free by design. Anti Gravity Device: the flight ticks that led here were paid for.
                event.distance = 0.0F;
            }
    }

    /**
     * Adds the Jump Pack boost to a jump.
     *
     * @param event the jump, on either side
     */
    @SubscribeEvent
    public void onJump(LivingJumpEvent event) {
        if (!(event.entityLiving instanceof EntityPlayer)) {
            return;
        }
        EntityPlayer player = (EntityPlayer) event.entityLiving;
        if (player.worldObj.isRemote) {
            FlightStatus status = Flight.mirror()
                .status(player);
            if (status.kind() == BackKind.JUMP_PACK && status.powered()) {
                player.motionY += FlightPhysics.JUMP_BOOST;
            }
            return;
        }
        ItemStack stack = Flight.wornDevice(player);
        if (stack != null && stack.getItem() instanceof ItemJumpPack
            && ItemJumpPack.canJump(stack)
            && ElectricGear.use(stack, FlightConfig.jumpPackJumpCostEu, player)) {
            player.motionY += FlightPhysics.JUMP_BOOST;
        }
    }
}
