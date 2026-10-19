package com.gregguns.client.armor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;

import com.gregguns.armor.Armor;
import com.gregguns.armor.logic.ArmorBonus;
import com.gregguns.armor.logic.StepHeightPolicy;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Applies the step-assist bonus to the local player's step height with {@link StepHeightPolicy}. Step height is
 * client-authoritative in 1.7.10, which is why this runs on the client. It raises the height only when it is lower than
 * the assist height and puts back only what it set, so another mod's step height is never lowered (CORE-14).
 *
 * <p>
 * No T1 set grants step assist; the handler is ready for the sets that do. A new player entity (respawn, relog) starts
 * from a clean state. Client only.
 */
// Spec: docs/techguns-spec/01-core/index.md CORE-14; docs/techguns-spec/05-armor/set-bonuses.md section 3.1 step 5
public final class StepHeightClient {

    private StepHeightPolicy.State state = StepHeightPolicy.State.UNTOUCHED;
    private EntityClientPlayerMP lastPlayer;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
        if (player != lastPlayer) {
            lastPlayer = player;
            state = StepHeightPolicy.State.UNTOUCHED;
        }
        if (player == null) {
            return;
        }
        boolean wanted = Armor.bonus(player, ArmorBonus.STEP_ASSIST) > 0.0;
        if (!wanted && !state.owned()) {
            return;
        }
        StepHeightPolicy.Decision d = StepHeightPolicy.decide(player.stepHeight, wanted, state);
        if (d.set()) {
            player.stepHeight = (float) d.height();
        }
        state = d.state();
    }
}
