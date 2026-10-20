package com.gregguns.client.npc;

import net.minecraft.entity.boss.BossStatus;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.common.MinecraftForge;

import com.gregguns.entity.npc.AttackHelicopter;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Shows the vanilla boss health bar while an Attack Helicopter is rendered, the way the original renderer did every
 * frame. The shared mob renderer is final and knows nothing about bosses, so this listens to the render event instead.
 * Client only.
 */
// Spec: docs/techguns-spec/10-entities-npcs/per-mob.md (16. Attack Helicopter, boss bar)
@SideOnly(Side.CLIENT)
public final class HelicopterBossBar {

    private HelicopterBossBar() {}

    /** Registers the listener. */
    public static void register() {
        MinecraftForge.EVENT_BUS.register(new HelicopterBossBar());
    }

    @SubscribeEvent
    public void onRender(RenderLivingEvent.Pre event) {
        if (event.entity instanceof AttackHelicopter) {
            BossStatus.setBossStatus((AttackHelicopter) event.entity, true);
        }
    }
}
