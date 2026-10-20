package com.gregguns.explosion;

import java.util.ArrayList;

import net.minecraft.world.ChunkPosition;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;

import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.SkipUnless;

/**
 * Asks the pack whether a blast may remove blocks at a spot, by posting a throwaway {@code ExplosionEvent.Detonate}
 * with one block listed and seeing whether a listener empties the list. Used by scenarios that expect blocks to break:
 * a server that disables explosions (ServerUtilities {@code enable_explosions=FALSE}, or {@code safe_spawn} around the
 * world spawn, or a claimed chunk) is configured correctly, and those scenarios skip there. Test hooks only; server
 * side.
 */
// Spec: docs/gregguns-spec/02-guns/damage-interop.md (explosions and protection mods)
// Source: ServerUtilities empties the Detonate list for disabled, spawn-protected and claimed areas:
// https://github.com/GTNewHorizons/ServerUtilities/blob/2.4.14/src/main/java/serverutils/handlers/ServerUtilitiesWorldEventHandler.java#L105-L139
public final class PackExplosionProbe {

    /** Why a scenario that needs block damage was skipped. */
    public static final String SKIP_REASON = "the pack removes explosion blocks here (ServerUtilities"
        + " enable_explosions=FALSE, safe_spawn or a claim); GregGuns honors that on purpose, the pack default allows"
        + " them";

    private PackExplosionProbe() {}

    /**
     * @param world the world
     * @param x     block X of the spot
     * @param y     block Y of the spot
     * @param z     block Z of the spot
     * @return true when a Detonate listener leaves a listed block at that spot alone
     */
    @SuppressWarnings("unchecked")
    public static boolean blocksSurvive(World world, int x, int y, int z) {
        Explosion probe = new Explosion(world, null, x + 0.5, y + 0.5, z + 0.5, 1.0F);
        probe.affectedBlockPositions.add(new ChunkPosition(x, y, z));
        ForgeEventFactory.onExplosionDetonate(world, probe, new ArrayList<net.minecraft.entity.Entity>(), 2.0);
        return !probe.affectedBlockPositions.isEmpty();
    }

    /**
     * @param scenario a scenario that needs explosions to break blocks
     * @return it, skipping with {@link #SKIP_REASON} where the pack protects the scenario's area
     */
    public static Scenario skipIfProtected(Scenario scenario) {
        return SkipUnless.inContext(
            scenario,
            (ScenarioContext ctx) -> blocksSurvive(ctx.world(), ctx.originX(), ctx.originY(), ctx.originZ()),
            SKIP_REASON);
    }
}
