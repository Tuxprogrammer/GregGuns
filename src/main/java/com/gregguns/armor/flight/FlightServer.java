package com.gregguns.armor.flight;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

import org.jetbrains.annotations.Nullable;

import com.gregguns.armor.flight.logic.FlightStatus;
import com.gregguns.network.GregGunsNetwork;

/**
 * Server-side flight bookkeeping per player: the held back-device key, the last tick a device assisted the fall, and
 * the last status sent to the client. Runtime tracking only, never saved. The status is re-sent when it changes and
 * every {@link #RESEND_TICKS} ticks while a device is worn, so a client that joined late or swapped entities
 * (respawn, dimension change) recovers without a request packet. Server thread only.
 */
// Spec: docs/gregguns-spec/05-armor/powered-flight.md (GGS-05-18); docs/gregguns-spec/05-armor/baubles-slots.md
// (GGS-05-05: effects run on the server, the client is told)
public final class FlightServer {

    /** Ticks between repeats of an unchanged status. */
    public static final int RESEND_TICKS = 100;
    /** A fall within this many ticks of an assisted tick counts as assisted. */
    public static final int ASSIST_WINDOW = 3;

    private final Map<UUID, FlightStatus> sent = new HashMap<>();
    private final Map<UUID, Long> sentAt = new HashMap<>();
    private final Map<UUID, Long> assistedAt = new HashMap<>();
    private final Set<UUID> keysHeld = new HashSet<>();

    /** @param player a player; true while the back-device key is held */
    public boolean keyHeld(EntityPlayer player) {
        return keysHeld.contains(player.getUniqueID());
    }

    /**
     * Records the back-device key.
     *
     * @param player a player
     * @param down   true on press, false on release
     */
    public void setKey(EntityPlayer player, boolean down) {
        if (down) {
            keysHeld.add(player.getUniqueID());
        } else {
            keysHeld.remove(player.getUniqueID());
        }
    }

    /**
     * Notes that a device kept the player aloft or gliding this tick, so the fall that ends it is not charged as a
     * plain fall.
     *
     * @param player a player
     */
    public void markAssisted(EntityPlayer player) {
        assistedAt.put(player.getUniqueID(), player.worldObj.getTotalWorldTime());
    }

    /**
     * @param player a player
     * @return whether a device assisted within the last {@link #ASSIST_WINDOW} ticks
     */
    public boolean assistedRecently(EntityPlayer player) {
        Long at = assistedAt.get(player.getUniqueID());
        return at != null && player.worldObj.getTotalWorldTime() - at <= ASSIST_WINDOW;
    }

    /**
     * Tells the client the device state when it changed or is due for a repeat. A fake player has no connection; the
     * status is still recorded so scenarios can read it.
     *
     * @param player a server player
     * @param status the current status
     */
    public void publish(EntityPlayer player, FlightStatus status) {
        UUID id = player.getUniqueID();
        long now = player.worldObj.getTotalWorldTime();
        FlightStatus before = sent.getOrDefault(id, FlightStatus.NONE);
        Long at = sentAt.get(id);
        boolean changed = !status.equals(before);
        boolean due = status != FlightStatus.NONE && (at == null || now - at >= RESEND_TICKS);
        if (!changed && !due) {
            return;
        }
        sent.put(id, status);
        sentAt.put(id, now);
        if (player instanceof EntityPlayerMP && ((EntityPlayerMP) player).playerNetServerHandler != null) {
            GregGunsNetwork.channel()
                .sendTo(new PacketFlightStatus(status), (EntityPlayerMP) player);
        }
    }

    /**
     * @param player a player
     * @return the last status published for the player, or null when none was
     */
    public @Nullable FlightStatus lastPublished(EntityPlayer player) {
        return sent.get(player.getUniqueID());
    }

    /**
     * Drops everything known about a player and tells the client no device is worn. Call when the device comes off,
     * and on logout, death, respawn and dimension change.
     *
     * @param player a server player
     */
    public void forget(EntityPlayer player) {
        UUID id = player.getUniqueID();
        keysHeld.remove(id);
        assistedAt.remove(id);
        if (sent.containsKey(id) && !sent.get(id)
            .equals(FlightStatus.NONE)) {
            publish(player, FlightStatus.NONE);
        }
        sent.remove(id);
        sentAt.remove(id);
    }
}
