package com.gregguns.gun.net;

import java.io.IOException;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;

import com.gregguns.item.gun.ItemGun;
import com.gregguns.network.PlayerSender;
import com.gregguns.network.ServerboundPacket;
import com.gregguns.network.validation.Sender;
import com.gregguns.network.validation.SenderRules;

/**
 * Base of the client-to-server gun packets. They carry <b>intents only</b>: "trigger down", "zoom toggle". They never
 * carry an ammo count, a zoom flag or a position, so a modified client cannot dictate state (GUN-12, CORE-41). The
 * server derives everything from its own controller.
 *
 * <p>
 * Every intent names the dimension and hotbar slot the player believes it is in, and the server checks both plus that
 * a gun is in that slot ({@link GunPacketChecks}). The base {@link ServerboundPacket} already rejects dead senders.
 *
 * <p>
 * Wire: dimension (int), slot (byte), then the subclass's fields. Side: server validates and handles.
 */
public abstract class GunIntentPacket extends ServerboundPacket {

    /** Dimension id the client claims to be in. */
    protected int dimension;
    /** Hotbar slot the client claims is selected. */
    protected int slot;

    /** Required by the channel. */
    protected GunIntentPacket() {}

    /**
     * @param dimension the sender's dimension
     * @param slot      the sender's selected hotbar slot
     */
    protected GunIntentPacket(int dimension, int slot) {
        this.dimension = dimension;
        this.slot = slot;
    }

    @Override
    public void encode(PacketBuffer buf) throws IOException {
        buf.writeInt(dimension);
        buf.writeByte(slot);
        encodeBody(buf);
    }

    @Override
    public void decode(PacketBuffer buf) throws IOException {
        dimension = buf.readInt();
        slot = buf.readByte();
        decodeBody(buf);
    }

    /** Writes the subclass's own fields. */
    protected void encodeBody(PacketBuffer buf) throws IOException {}

    /** Reads the subclass's own fields. */
    protected void decodeBody(PacketBuffer buf) throws IOException {}

    @Override
    protected void validate(SenderRules rules) {
        rules.sameDimension(dimension)
            .indexInRange("slot", slot, GunPacketChecks.HOTBAR_SLOTS)
            .custom(() -> {
                GunPacketChecks.Held held = held(rules.sender());
                return GunPacketChecks.check(slot, held.selectedSlot(), held.holdsGun());
            });
    }

    /**
     * Reads what the sender holds. Overridden by tests; the default reads the real player.
     *
     * @param sender the packet's sender, a {@link PlayerSender} in the game
     */
    protected GunPacketChecks.Held held(Sender sender) {
        EntityPlayerMP player = ((PlayerSender) sender).player();
        return new GunPacketChecks.Held(
            player.inventory.currentItem,
            player.getHeldItem() != null && player.getHeldItem()
                .getItem() instanceof ItemGun);
    }
}
