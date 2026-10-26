package com.gregguns.core.module;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * Makes existing GregGuns items and blocks of a module that is off do nothing instead of crashing or vanishing. The
 * content is always registered (see {@link ModuleState}); this handler is what a flipped flag actually switches for a
 * player holding such an item: right clicks, placing and melee swings are cancelled, with one chat line per attempt.
 * Guns are gated at the trigger ({@code ServerGunHandler}), machines in their tick.
 *
 * <p>
 * Registered on the Forge bus by {@code CommonProxy} whatever the flags say, so it also works on a client that joined a
 * server whose flags differ.
 */
// Spec: docs/gregguns-spec/01-core/config-gtnhlib.md (module switches)
public final class ModuleGate {

    /** Handler instance for the event bus. */
    public static final ModuleGate INSTANCE = new ModuleGate();

    private ModuleGate() {}

    /** @param event a click; denied when the held stack's module is off */
    @SubscribeEvent
    public void onInteract(PlayerInteractEvent event) {
        ItemStack held = event.entityPlayer.getHeldItem();
        ModuleId off = ModuleContent.disabledModuleOf(held);
        if (off != null) {
            event.useItem = Event.Result.DENY;
            if (event.action == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) {
                event.useBlock = Event.Result.DENY;
            }
            tell(event.entityPlayer, off);
        }
    }

    /** @param event a melee swing; cancelled when the held stack's module is off */
    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        ModuleId off = ModuleContent.disabledModuleOf(event.entityPlayer.getHeldItem());
        if (off != null) {
            event.setCanceled(true);
            tell(event.entityPlayer, off);
        }
    }

    private static void tell(EntityPlayer player, ModuleId off) {
        if (!player.worldObj.isRemote) {
            // The client resolves the key in its own language.
            player.addChatMessage(
                new ChatComponentText("[GregGuns] ").appendSibling(
                    new ChatComponentTranslation(ModuleState.DISABLED_KEY, ModuleState.disabledFlag(off))));
        }
    }
}
