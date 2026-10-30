package com.gregguns.compat.lootbags;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Keeps a GregGuns loot bag from crashing EnhancedLootBags when its group is gone. ELB opens a bag by looking its group
 * up and, when that is null, logs and then dereferences it: the player is kicked on a server and single player
 * crashes. Registered when ELB is loaded, whatever {@code compat.lootBags} says.
 *
 * <ul>
 * <li>Server, right click: before the bag is used, a missing group is put back; if it still is missing, the use is
 * denied with a chat line.</li>
 * <li>Server, {@code /lootbags reload} (alias {@code /lbag}): the command runs here, and our groups go back in the same
 * call, so no tick ever sees them missing; then every player gets the headers again.</li>
 * <li>Client, every second: see {@link LootBagInjector#ensureClientSafe()} (the tooltip path).</li>
 * </ul>
 */
// Source:
// https://github.com/GTNewHorizons/EnhancedLootBags/blob/1.3.5/src/main/java/eu/usrv/enhancedlootbags/core/LootGroupsHandler.java#L181-L220
// (getMergedGroupFromID: a null group is logged at L215, then tReturnGroup.shuffleLoot() at L218 throws)
// Source:
// https://github.com/MinecraftForge/MinecraftForge/blob/9274e4fe435cb415099a8216c1b42235f185443e/patches/minecraft/net/minecraft/network/NetHandlerPlayServer.java.patch
// (1.7.10 branch; processPlayerBlockPlacement, direction 255: PlayerInteractEvent RIGHT_CLICK_AIR is posted on the
// server and tryUseItem runs only when event.useItem != DENY)
// Source:
// https://github.com/MinecraftForge/MinecraftForge/blob/9274e4fe435cb415099a8216c1b42235f185443e/patches/minecraft/net/minecraft/command/CommandHandler.java.patch
// (CommandEvent is posted after the permission check; a cancelled event skips the command and rethrows
// event.exception)
// Source:
// https://github.com/GTNewHorizons/EnhancedLootBags/blob/1.3.5/src/main/java/eu/usrv/enhancedlootbags/server/LootBagCommand.java#L45-L75
// (command "lootbags", alias "lbag"; "reload" calls LootGroupHandler.reload(), which re-reads LootBags.xml only)
public final class LootBagGuard {

    /** Lang key of the chat line when a bag cannot open. */
    public static final String MISSING_KEY = "gregguns.lootbag.missing";
    private static final String COMMAND = "lootbags";
    private int clientTicks;

    /**
     * Server: denies using a GregGuns bag whose group ELB does not have, after one attempt to put it back.
     *
     * @param event the interaction
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.action != PlayerInteractEvent.Action.RIGHT_CLICK_AIR || event.entityPlayer.worldObj.isRemote) {
            return;
        }
        ItemStack held = event.entityPlayer.getCurrentEquippedItem();
        if (!LootBagInjector.isGregGunsBag(held)) return;
        if (deny(event.entityPlayer, held)) {
            event.useItem = Event.Result.DENY;
        }
    }

    /**
     * @param player the player using the bag
     * @param bag    the bag
     * @return true if the use must be denied; the player has been told why
     */
    static boolean deny(EntityPlayer player, ItemStack bag) {
        if (LootBagInjector.hasGroup(bag.getItemDamage())) return false;
        LootBagInjector.ensurePresent();
        if (LootBagInjector.hasGroup(bag.getItemDamage())) return false;
        player.addChatMessage(
            new ChatComponentTranslation(MISSING_KEY).setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
        return true;
    }

    /**
     * Server: runs {@code /lootbags reload} itself and puts our groups back straight after. Lowest priority, and only
     * when no other handler cancelled the command; the permission check has already passed when the event fires.
     *
     * @param event the command
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onCommand(CommandEvent event) {
        if (!COMMAND.equals(event.command.getCommandName()) || event.parameters.length < 1
            || !"reload".equalsIgnoreCase(event.parameters[0])) {
            return;
        }
        event.setCanceled(true);
        try {
            event.command.processCommand(event.sender, event.parameters);
        } catch (RuntimeException e) {
            event.exception = e;
        } finally {
            LootBagInjector.afterReload();
        }
    }

    /**
     * Client: once a second, makes sure every GregGuns bag group is in ELB's local list (the tooltip reads it).
     *
     * @param event the client tick
     */
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ++clientTicks < 20) return;
        clientTicks = 0;
        LootBagInjector.ensureClientSafe();
    }
}
