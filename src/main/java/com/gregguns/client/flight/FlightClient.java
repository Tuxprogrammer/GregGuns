package com.gregguns.client.flight;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.Vec3;

import com.gregguns.armor.flight.Flight;
import com.gregguns.armor.flight.logic.BackKind;
import com.gregguns.armor.flight.logic.FlightMode;
import com.gregguns.armor.flight.logic.FlightPhysics;
import com.gregguns.armor.flight.logic.FlightPolicy;
import com.gregguns.armor.flight.logic.FlightStatus;
import com.gregguns.armor.slots.EquipmentKeybinds;
import com.gtnewhorizon.gtnhlib.keybind.SyncedKeybind;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Applies the Glider and Anti Gravity Device motion to the local player. In 1.7.10 the client owns its player's
 * motion (the server follows position packets), so the physics must run here; the server decides the status (device
 * worn, switched on, EU available) and sends it ({@code PacketFlightStatus}). The formulas are the shared
 * {@link FlightPhysics}, and the decision is the shared {@link FlightPolicy}, so the server tick and this agree.
 *
 * <p>
 * Runs at the end of the client tick, so the motion set here is what the next player update moves by. It never touches
 * the player's flight ability: while the player is flying through the vanilla ability (creative or another mod), it
 * does nothing. Client only. Feel check owed: see implementation-notes (FLT-DEV-05).
 */
// Spec: docs/gregguns-spec/05-armor/powered-flight.md (GGS-05-17, GGS-05-18)
// Source: Entity.moveFlying, the acceleration FlightPhysics.steer reproduces (vanilla 1.7.10)
public final class FlightClient {

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        EntityClientPlayerMP player = mc.thePlayer;
        if (player == null || mc.theWorld == null || mc.isGamePaused()) {
            return;
        }
        FlightStatus status = Flight.mirror()
            .status(player);
        if (status.kind() == BackKind.NONE) {
            return;
        }
        boolean inputOpen = mc.currentScreen == null;
        boolean engaged = status.engaged();
        if (status.kind() == BackKind.GLIDER) {
            // The server's copy of the held key lags by a round trip; the local key is the truth for feel.
            SyncedKeybind key = EquipmentKeybinds.backDevice();
            KeyBinding binding = key == null ? null : key.getKeybinding();
            engaged = inputOpen && binding != null && binding.getIsKeyPressed();
        }
        FlightMode mode = FlightPolicy.mode(
            status.kind(),
            engaged,
            status.powered(),
            Flight.airborne(player),
            player.isSneaking(),
            player.capabilities.isFlying);
        double forward = player.movementInput == null ? 0.0 : player.movementInput.moveForward;
        double strafe = player.movementInput == null ? 0.0 : player.movementInput.moveStrafe;
        if (mode == FlightMode.GLIDE) {
            Vec3 look = player.getLookVec();
            FlightPhysics.Impulse glide = FlightPhysics.glide(player.motionY, look.xCoord, look.zCoord);
            player.motionX += glide.x;
            player.motionY += glide.y;
            player.motionZ += glide.z;
            addSteer(player, forward, strafe, FlightPhysics.GLIDE_AIR_CONTROL);
            player.fallDistance = 0.0F;
        } else if (mode == FlightMode.FLY) {
            boolean up = inputOpen && mc.gameSettings.keyBindJump.getIsKeyPressed();
            player.motionY = FlightPhysics.verticalFlight(player.motionY, up, player.isSneaking());
            addSteer(player, forward, strafe, FlightPhysics.flyAcceleration());
            player.fallDistance = 0.0F;
        }
    }

    private static void addSteer(EntityClientPlayerMP player, double forward, double strafe, double strength) {
        FlightPhysics.Impulse steer = FlightPhysics.steer(forward, strafe, player.rotationYaw, strength);
        player.motionX += steer.x;
        player.motionZ += steer.z;
    }
}
