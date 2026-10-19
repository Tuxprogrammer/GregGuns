package com.gregguns.client.fx;

import com.gregguns.explosion.BlastFxHooks;
import com.gregguns.explosion.BlastFxSelector;
import com.gregguns.fx.FxHooks;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Installs the client sink behind {@code PacketBlastFx}: a nuke-size blast spawns the {@code NukeExplosion} list at the
 * blast centre, a smaller one {@code RocketExplosion}, both through the one FX path ({@link FxHooks#sink()}).
 *
 * <p>
 * Side: client only. Called from {@link FxClient#init()}.
 */
// Spec: docs/techguns-spec/04-projectiles-fx/fx-catalog.md (Triggers);
// docs/gregguns-spec/04-projectiles-fx/m5-exit-gate.md
@SideOnly(Side.CLIENT)
final class BlastFxClient {

    private BlastFxClient() {}

    /** Installs the blast sink. */
    static void install() {
        BlastFxHooks.install(
            (x, y, z, radius) -> FxHooks.sink()
                .spawnAt(BlastFxSelector.listFor(radius), x, y, z, 0, 0, 0));
    }
}
