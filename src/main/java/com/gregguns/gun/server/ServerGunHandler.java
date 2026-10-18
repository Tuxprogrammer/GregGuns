package com.gregguns.gun.server;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

import com.gregguns.gun.logic.AmmoInventory;
import com.gregguns.gun.logic.GunController;
import com.gregguns.gun.logic.GunNbt;
import com.gregguns.gun.logic.GunState;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.GunTickResult;
import com.gregguns.gun.logic.ReloadStateMachine;
import com.gregguns.gun.logic.ShotContext;
import com.gregguns.gun.logic.ShotOffset;
import com.gregguns.gun.logic.ShotResult;
import com.gregguns.gun.logic.SpreadModel;
import com.gregguns.gun.net.GunEventKind;
import com.gregguns.gun.net.PacketGunEvent;
import com.gregguns.gun.sound.GunSoundPlayer;
import com.gregguns.item.gun.GunItems;
import com.gregguns.item.gun.ItemGun;
import com.gregguns.item.gun.PlayerAmmoStore;
import com.gregguns.item.gun.StoreAmmoInventory;
import com.gregguns.network.GregGunsNetwork;
import com.gregguns.projectile.Projectiles;

/**
 * The server half of every gun: drives each player's {@link GunController} once per tick and turns its results into
 * world changes. The server decides every shot, every ammo change and every reload; clients only send intents and
 * predict animation and sound.
 *
 * <h2>Per tick</h2>
 * {@link #onPlayerTick} reads the held gun's state from its NBT, ticks the controller, writes the NBT back when the
 * rounds changed, then acts on the result: launches one projectile per {@link ShotOffset}, plays sounds to everyone
 * except the shooter (who predicted them), and sends {@link PacketGunEvent}s. Switching away from a gun, dying or
 * dropping it resets the controller, which cancels a running reload.
 *
 * <h2>Intents</h2>
 * {@link #onTrigger} and {@link #onZoomToggle} are called from validated packets; {@link #onReloadKey} from the
 * synced reload keybind. Each re-checks that the player is alive and holds a gun, because a key listener has no packet
 * rules in front of it.
 *
 * <p>
 * Spec: docs/techguns-spec/02-guns/gun-lifecycle.md sections 5, 7 and 12. Fixes GUN-11 (one dry click per pull, from
 * the controller), GUN-12 (zoom is server state), GUN-13 (a forced reload keeps the loaded rounds), GUN-32 (the server
 * fire control grants the packet latency grace). Server only, main thread.
 */
public final class ServerGunHandler {

    private ServerGunHandler() {}

    /**
     * One server tick for one player. Called from the END phase of the player tick.
     *
     * @param player the player
     */
    public static void onPlayerTick(EntityPlayerMP player) {
        GunPlayerData data = GunPlayerData.get(player);
        ItemStack held = player.getHeldItem();
        ItemGun gun = gunOf(held);
        boolean gone = !isAlive(player);
        if (data.activeGun() != null
            && (gun != data.activeGun() || player.inventory.currentItem != data.activeSlot() || gone)) {
            deselect(player, data);
        }
        if (gun == null || gone) {
            return;
        }
        data.setActive(gun, player.inventory.currentItem);
        GunStats stats = gun.stats();
        ItemGun.ensureTag(held, stats);
        GunState state = GunNbt.read(held.getTagCompound(), stats);
        int before = state.loaded();
        GunTickResult result = data.controller()
            .tick(stats, state, ammoOf(player, stats), true, player.capabilities.isCreativeMode);
        if (state.loaded() != before) {
            GunNbt.write(held.getTagCompound(), state);
        }
        if (result.shot()
            .wasFired()) {
            fire(player, data, gun, result.shot());
        }
        announceReload(player, gun, result.reload());
        if (result.outOfAmmo()) {
            GunSoundPlayer.playDryFire(player, player);
        }
        syncZoom(player, data, stats);
    }

    /**
     * The fire button changed. The controller decides on its next tick whether that is a shot.
     *
     * @param player the player, validated to hold a gun
     * @param down   true for pressed
     */
    public static void onTrigger(EntityPlayerMP player, boolean down) {
        if (gunOf(player.getHeldItem()) == null || !isAlive(player)) {
            return;
        }
        GunController controller = GunPlayerData.get(player)
            .controller();
        if (down) {
            controller.triggerPressed();
        } else {
            controller.triggerReleased();
        }
    }

    /**
     * The player asked to toggle zoom. The controller refuses it while reloading or on an unscoped gun.
     *
     * @param player the player, validated to hold a gun
     */
    public static void onZoomToggle(EntityPlayerMP player) {
        ItemGun gun = gunOf(player.getHeldItem());
        if (gun == null || !isAlive(player)) {
            return;
        }
        GunPlayerData data = GunPlayerData.get(player);
        data.controller()
            .toggleZoom(gun.stats());
        syncZoom(player, data, gun.stats());
    }

    /**
     * The reload key was pressed. Starts a reload if the cooldown allows, the gun is not full and ammo is available;
     * otherwise tells the shooter's client to drop its prediction.
     *
     * @param player the player who pressed it
     */
    public static void onReloadKey(EntityPlayerMP player) {
        ItemStack held = player.getHeldItem();
        ItemGun gun = gunOf(held);
        if (gun == null || !isAlive(player)) {
            return;
        }
        GunPlayerData data = GunPlayerData.get(player);
        GunStats stats = gun.stats();
        ItemGun.ensureTag(held, stats);
        GunState state = GunNbt.read(held.getTagCompound(), stats);
        ReloadStateMachine.Event event = data.controller()
            .forceReload(stats, state, ammoOf(player, stats));
        if (event == ReloadStateMachine.Event.STARTED) {
            announceReload(player, gun, event);
            syncZoom(player, data, stats);
        } else if (event != ReloadStateMachine.Event.NONE) {
            // The client may have predicted a reload that the server refused: roll it back.
            sendTo(player, new PacketGunEvent(player, GunEventKind.RELOAD_ENDED, 0));
        }
    }

    // ----- internals -----------------------------------------------------------------------------------------

    private static void fire(EntityPlayerMP player, GunPlayerData data, ItemGun gun, ShotResult shot) {
        GunStats stats = gun.stats();
        // Deviation: no armor accuracy bonus until the armor module (M3) exists; zoom is the server's own state.
        ShotContext context = ShotContext.player(
            data.controller()
                .zoom()
                .isZoomed(stats),
            0.0);
        ShotOffset[] offsets = SpreadModel.roll(player.getRNG(), stats, context, shot.attackType());
        for (ShotOffset offset : offsets) {
            Projectiles.launcher()
                .launch(player.worldObj, player, stats, offset, 1.0f);
        }
        GunSoundPlayer.playFire(player, gun.fireEvent(), player);
        if (gun.rechamberEvent() != null) {
            // Spec: the rechamber sound plays with every shot (sheets-rifles-shotguns.md, Bolt Action Rifle, 4).
            GunSoundPlayer.playReload(player, gun.rechamberEvent(), player);
        }
        GregGunsNetwork.channel()
            .sendToAllTracking(new PacketGunEvent(player, GunEventKind.SHOT_FIRED, shot.attackType()), player);
    }

    private static void announceReload(EntityPlayerMP player, ItemGun gun, ReloadStateMachine.Event event) {
        switch (event) {
            case STARTED:
                GunSoundPlayer.playReload(player, gun.reloadEvent(), player);
                GregGunsNetwork.channel()
                    .sendToAllTrackingAndSelf(
                        new PacketGunEvent(
                            player,
                            GunEventKind.RELOAD_STARTED,
                            gun.stats()
                                .reloadTicks()),
                        player);
                break;
            case CANCELED:
                GregGunsNetwork.channel()
                    .sendToAllTrackingAndSelf(new PacketGunEvent(player, GunEventKind.RELOAD_ENDED, 0), player);
                break;
            default:
                break;
        }
    }

    private static void deselect(EntityPlayerMP player, GunPlayerData data) {
        boolean wasReloading = data.controller()
            .reload()
            .isReloading();
        data.controller()
            .reset();
        data.setActive(null, -1);
        if (wasReloading) {
            GregGunsNetwork.channel()
                .sendToAllTrackingAndSelf(new PacketGunEvent(player, GunEventKind.RELOAD_ENDED, 0), player);
        }
        if (data.zoomSent()) {
            data.setZoomSent(false);
            sendTo(player, new PacketGunEvent(player, GunEventKind.ZOOM, 0));
        }
    }

    /**
     * Sends a packet to one player. A player with no connection (a scenario's fake player) has no channel to send on
     * and is skipped; a real player always has one.
     */
    private static void sendTo(EntityPlayerMP player, PacketGunEvent packet) {
        if (player.playerNetServerHandler != null) {
            GregGunsNetwork.channel()
                .sendTo(packet, player);
        }
    }

    private static void syncZoom(EntityPlayerMP player, GunPlayerData data, GunStats stats) {
        boolean zoomed = data.controller()
            .zoom()
            .isZoomed(stats);
        if (zoomed != data.zoomSent()) {
            data.setZoomSent(zoomed);
            sendTo(player, new PacketGunEvent(player, GunEventKind.ZOOM, zoomed ? 1 : 0));
        }
    }

    private static AmmoInventory ammoOf(EntityPlayerMP player, GunStats stats) {
        return new StoreAmmoInventory(new PlayerAmmoStore(player), GunItems.roundsPerLooseItem(stats));
    }

    private static ItemGun gunOf(ItemStack stack) {
        return stack != null && stack.getItem() instanceof ItemGun ? (ItemGun) stack.getItem() : null;
    }

    private static boolean isAlive(EntityPlayerMP player) {
        return !player.isDead && player.getHealth() > 0.0f;
    }
}
