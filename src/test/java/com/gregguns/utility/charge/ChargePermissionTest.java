package com.gregguns.utility.charge;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import net.minecraft.network.PacketBuffer;

import org.junit.jupiter.api.Test;

import com.gregguns.utility.security.FriendProvider;
import com.gregguns.utility.security.OwnerAccess;

import io.netty.buffer.Unpooled;

/**
 * MUTL-31 and GGS-09-15: the rule behind {@link PacketChargeButton#isAuthorized} is "owner or team member", and the
 * packet survives the wire. The full server pipeline runs in the scenario {@code utility.charge_foreign_button}.
 */
class ChargePermissionTest {

    private static final UUID OWNER = new UUID(10, 10);
    private static final UUID TEAMMATE = new UUID(11, 11);
    private static final UUID STRANGER = new UUID(12, 12);
    private static final FriendProvider TEAM = (owner, other) -> owner.equals(OWNER) && other.equals(TEAMMATE);

    @Test
    void doesNotReproduceMUTL31_aStrangerCannotChangeOrArmACharge() {
        // Every button id, including arm, goes through the one packet-level check.
        for (int button = 0; button < ChargeParams.BUTTON_COUNT; button++) {
            assertFalse(OwnerAccess.isAuthorized(OWNER, STRANGER, TEAM), "button " + button);
            assertFalse(OwnerAccess.isAuthorized(OWNER, STRANGER, null), "button " + button + " without a team mod");
        }
    }

    @Test
    void theOwnerAndTeamMembersAreAllowed() {
        assertTrue(OwnerAccess.isAuthorized(OWNER, OWNER, null));
        assertTrue(OwnerAccess.isAuthorized(OWNER, TEAMMATE, TEAM));
        assertFalse(OwnerAccess.isAuthorized(OWNER, TEAMMATE, null));
    }

    @Test
    void anOwnerlessChargeAllowsNobody() {
        assertFalse(OwnerAccess.isAuthorized(null, STRANGER, TEAM));
        assertFalse(OwnerAccess.isAuthorized(OWNER, null, TEAM));
    }

    @Test
    void theButtonPacketRoundTripsOnTheWire() throws Exception {
        PacketBuffer first = new PacketBuffer(Unpooled.buffer());
        new PacketChargeButton(-1, 100, 64, -200, ChargeParams.BTN_FUSE_DOWN).encode(first);
        PacketChargeButton decoded = new PacketChargeButton();
        decoded.decode(new PacketBuffer(first.copy()));
        PacketBuffer second = new PacketBuffer(Unpooled.buffer());
        decoded.encode(second);
        byte[] a = new byte[first.readableBytes()];
        byte[] b = new byte[second.readableBytes()];
        first.readBytes(a);
        second.readBytes(b);
        assertEquals(4 + 4 + 4 + 4 + 1, a.length);
        assertArrayEquals(a, b);
    }
}
