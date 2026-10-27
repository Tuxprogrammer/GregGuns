package com.gregguns.icbm.tools;

import java.util.UUID;

import com.gregguns.api.Tier;

/**
 * The numbers and decisions of the remote detonator and the defuser, as pure logic (no Minecraft types, both sides,
 * unit tested): the detonator's range by tier, the range test, who may link a placed warhead, and the defuser's
 * durability.
 */
// Spec: docs/gregguns-icbm-spec/05-detection-defense/handheld-tools.md (D118 to D121)
public final class HandheldRules {

    /** Range of the HV remote detonator, blocks. */
    public static final int RANGE_HV = 128;
    /** Range of the EV remote detonator, blocks. */
    public static final int RANGE_EV = 256;
    /** Range of the IV remote detonator, blocks. */
    public static final int RANGE_IV = 512;
    /** Uses in a new defuser; each defused device costs one and the last use breaks it (D121). */
    public static final int DEFUSER_USES = 32;

    private HandheldRules() {}

    /**
     * Whether a handheld tool's client-side {@code onItemUseFirst} may report the click as used. It must not:
     * {@code PlayerControllerMP} returns before sending the block-placement packet when it does, so the server would
     * never run the tool. Guarded by a unit test; the client path cannot be driven from a scenario.
     *
     * @return always false
     */
    public static boolean clientConsumesUse() {
        return false;
    }

    /**
     * @param tier a detonator tier (HV, EV or IV)
     * @return the range in blocks, measured from the holder to the center of the warhead; 0 for any other tier
     */
    public static int range(Tier tier) {
        switch (tier) {
            case HV:
                return RANGE_HV;
            case EV:
                return RANGE_EV;
            case IV:
                return RANGE_IV;
            default:
                return 0;
        }
    }

    /**
     * @param dx    X distance from the holder to the warhead
     * @param dy    Y distance
     * @param dz    Z distance
     * @param range the range, blocks
     * @return true when the warhead is within a sphere of that radius (edge included)
     */
    public static boolean inRange(double dx, double dy, double dz, int range) {
        return range > 0 && dx * dx + dy * dy + dz * dz <= (double) range * range;
    }

    /**
     * Who may link a detonator to a placed warhead and trigger it (D119): the owner, anyone for an unowned warhead
     * (one placed without a player), and a creative player.
     *
     * @param owner    the warhead's owner, null when unowned
     * @param player   the player linking or firing
     * @param creative true for a creative player
     * @return true when allowed
     */
    public static boolean mayControl(UUID owner, UUID player, boolean creative) {
        return creative || owner == null || owner.equals(player);
    }
}
