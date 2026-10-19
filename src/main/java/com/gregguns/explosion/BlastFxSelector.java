package com.gregguns.explosion;

/**
 * Picks the FX list for a long-range blast packet. Pure: no Minecraft types.
 *
 * <p>
 * {@link PacketBlastFx} is only sent for blasts at or above {@link ExplosionRules#NUKE_FX_MIN_SIZE}, which get the
 * {@value #NUKE} list; anything smaller that reaches the client by this path gets {@value #ROCKET}.
 */
// Spec: docs/techguns-spec/04-projectiles-fx/fx-catalog.md (Triggers: RocketExplosion, NukeExplosion)
public final class BlastFxSelector {

    /** FX list for a nuke-size blast. */
    public static final String NUKE = "NukeExplosion";
    /** FX list for any smaller blast. */
    public static final String ROCKET = "RocketExplosion";

    private BlastFxSelector() {}

    /**
     * @param radius the blast's vanilla power, blocks
     * @return the FX list name to spawn at the blast centre
     */
    public static String listFor(float radius) {
        return radius >= ExplosionRules.NUKE_FX_MIN_SIZE ? NUKE : ROCKET;
    }
}
