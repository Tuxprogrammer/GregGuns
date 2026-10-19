package com.gregguns.gun.server;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import com.gregguns.item.gun.ItemGun;
import com.gregguns.item.gun.ItemToolGun;
import com.gregguns.recipe.AkimboRecipes;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;

/**
 * Event wiring for guns, registered once from the guns module. Both sides:
 * <ul>
 * <li>the server ticks each player's gun at the end of the player tick;</li>
 * <li>holding a gun, the attack button must not break blocks (the click is the trigger). The break speed is canceled
 * and a left-click on a block is canceled, which also covers creative mode.</li>
 * </ul>
 *
 * <p>
 * Spec: docs/techguns-spec/02-guns/gun-lifecycle.md section 4 item 3 (BreakSpeed at highest priority cancels breaking).
 */
public final class GunEvents {

    private GunEvents() {}

    /** Registers the handlers on the FML and Forge buses. */
    public static void register() {
        GunEvents handler = new GunEvents();
        FMLCommonHandler.instance()
            .bus()
            .register(handler);
        MinecraftForge.EVENT_BUS.register(handler);
        AkimboRecipes.registerHandler();
    }

    /** Ticks the held gun on the server. */
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.side == Side.SERVER && event.phase == TickEvent.Phase.END && event.player instanceof EntityPlayerMP) {
            ServerGunHandler.onPlayerTick((EntityPlayerMP) event.player);
        }
    }

    /** Stops the attack button from digging while a gun is held. */
    @SubscribeEvent
    public void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (holdsGun(event.entityPlayer.getHeldItem())) {
            event.setCanceled(true);
        }
    }

    /** Stops a left-click on a block from starting a break while a gun is held. */
    @SubscribeEvent
    public void onInteract(PlayerInteractEvent event) {
        if (event.action == PlayerInteractEvent.Action.LEFT_CLICK_BLOCK && holdsGun(event.entityPlayer.getHeldItem())) {
            event.setCanceled(true);
        }
    }

    private static boolean holdsGun(ItemStack stack) {
        // A tool gun's left button is melee and mining, so it must keep digging (I11).
        return stack != null && stack.getItem() instanceof ItemGun && !(stack.getItem() instanceof ItemToolGun);
    }
}
