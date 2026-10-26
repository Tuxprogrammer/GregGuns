package com.gregguns.icbm.warhead;

import com.gregguns.icbm.explosive.ExplosiveDef;
import com.gregguns.icbm.explosive.Explosives;
import com.gregguns.icbm.warhead.effects.Effects;

/**
 * Registers the T1 and T2 blast types in the shared explosive registry (D12), with the radius curve of
 * {@link WarheadMath}. Idempotent. Both sides.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/blast-types.md
public final class WarheadBlasts {

    private static boolean done;

    private WarheadBlasts() {}

    /**
     * @param blast a catalog entry
     * @return the registry definition for it
     */
    public static ExplosiveDef define(WarheadBlast blast) {
        int cap = blast.fillCap();
        return ExplosiveDef.builder(blast.id(), blast.tier())
            .shape(blast.shape())
            .maxFill(cap)
            .baseRadius(WarheadMath.baseRadius(blast.fullRadius(), cap))
            .radiusExponent(WarheadMath.RADIUS_EXPONENT)
            .centerDamage(blast.fullDamage())
            .effect(Effects.forKind(blast.kind()))
            .build();
    }

    /** Registers every catalog entry once. */
    public static synchronized void registerAll() {
        if (done) {
            return;
        }
        for (WarheadBlast blast : WarheadBlast.values()) {
            if (Explosives.get(blast.id()) == null) {
                Explosives.register(define(blast));
            }
        }
        done = true;
    }
}
