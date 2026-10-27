package com.gregguns.icbm.warhead.effects;

import com.gregguns.icbm.blast.BlastEngine;
import com.gregguns.icbm.explosive.BlastContext;
import com.gregguns.icbm.explosive.BlastEffect;

/**
 * The antimatter warhead (UV, D30, D130, D133): the engine already digs the crater and annihilates the kill zone
 * (bosses
 * included) through the shared blast engine. The effect adds one thing: antimatter and red matter cannot coexist, so
 * every red matter body within {@link RedMatterModel#ANTIMATTER_REACH} ends the moment antimatter goes off (the ICBM
 * rule). It leaves no fallout: it never calls the fallout system (D21).
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/antimatter.md
final class AntimatterEffect implements BlastEffect {

    @Override
    public void onDetonate(BlastContext ctx) {
        if (ctx.world().isRemote) {
            return;
        }
        // The flash: one swelling ball to the blast radius, cheap on the client (no particles, no block scan).
        com.gregguns.icbm.fx.OrbBroadcast.send(
            ctx.world(),
            com.gregguns.icbm.fx.OrbKind.ANTIMATTER,
            ctx.x(),
            ctx.y(),
            ctx.z(),
            (int) Math.ceil(ctx.radius()),
            com.gregguns.icbm.fx.OrbPlan.FLASH_TICKS);
        BlastEngine.instance()
            .cancelNear(ctx.world(), "redmatter", ctx.x(), ctx.y(), ctx.z(), RedMatterModel.ANTIMATTER_REACH);
    }
}
