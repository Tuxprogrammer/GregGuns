package com.gregguns.client.gun;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import com.gregguns.gun.GunClientHooks;
import com.gregguns.gun.GunClientSync;
import com.gregguns.gun.logic.AmmoInventory;
import com.gregguns.gun.logic.GunController;
import com.gregguns.gun.logic.GunNbt;
import com.gregguns.gun.logic.GunState;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.GunTickResult;
import com.gregguns.gun.logic.ReloadStateMachine;
import com.gregguns.item.gun.GunItems;
import com.gregguns.item.gun.ItemGun;
import com.gregguns.item.gun.PlayerAmmoStore;
import com.gregguns.item.gun.StoreAmmoInventory;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The local player's prediction mirror. It runs the same pure {@link GunController} as the server, on a private
 * {@link GunState}, so the shooter sees and hears a shot or a reload the instant they act instead of one round trip
 * later. It predicts <b>animation and sound only</b>: it never writes the stack, never spends real ammo and never
 * spawns anything.
 *
 * <h2>Staying honest</h2>
 * <ul>
 * <li>The mirror's rounds are re-read from the synced stack NBT whenever that changes, and after a quiet spell, so a
 * shot the server refused cannot leave the mirror permanently off.</li>
 * <li>Its ammo view only counts: the reload "withdraws" nothing from the real inventory.</li>
 * <li>The server corrects it through {@link GunClientSync}: an automatic reload the client did not predict starts the
 * mirror's reload, and a refused or canceled reload stops it.</li>
 * </ul>
 *
 * <p>
 * Every effect goes out through {@link GunClientHooks#events()}, so the renderer sees the same events for the local
 * player and for everyone else. Client thread only.
 */
// Spec: docs/techguns-spec/02-guns/gun-lifecycle.md section 12 (client prediction: recoil, muzzle flash, reload
// animation, sound). Deviation: Techguns predicts the ammo NBT too; GregGuns predicts only effects (CONTRIBUTING:
// server authority).
@SideOnly(Side.CLIENT)
public final class ClientGunMirror implements GunClientSync {

    /** Ticks without a local change after which the mirror re-reads the server's round count. */
    static final int RESYNC_QUIET_TICKS = 10;

    private final GunController controller = new GunController();
    private final GunState state = new GunState(0);
    private ItemGun activeGun;
    private int activeSlot = -1;
    private int lastServerLoaded = -1;
    private int quietTicks;

    /** @return true if a reload is running (predicted or confirmed) */
    public boolean isReloading() {
        return controller.reload()
            .isReloading();
    }

    /** @return reload progress in {@code [0, 1]}; 0 when not reloading */
    public double reloadProgress() {
        return controller.reload()
            .progress();
    }

    /** @return the gun the mirror is currently following, or null */
    public ItemGun activeGun() {
        return activeGun;
    }

    /** The fire button changed on the client. */
    public void trigger(boolean down) {
        if (down) {
            controller.triggerPressed();
        } else {
            controller.triggerReleased();
        }
    }

    /**
     * One client tick of the local player.
     *
     * @param player the local player
     */
    public void tick(EntityPlayer player) {
        ItemStack held = player.getHeldItem();
        ItemGun gun = held != null && held.getItem() instanceof ItemGun ? (ItemGun) held.getItem() : null;
        if (activeGun != null && (gun != activeGun || player.inventory.currentItem != activeSlot)) {
            deselect(player);
        }
        if (gun == null) {
            return;
        }
        follow(player, held, gun);
        GunStats stats = gun.stats();
        syncRounds(stats, held);
        int before = state.loaded();
        GunTickResult result = controller
            .tick(stats, state, ammoView(player, stats), true, player.capabilities.isCreativeMode);
        quietTicks = state.loaded() != before || isReloading() ? 0 : quietTicks + 1;
        if (result.shot()
            .wasFired()) {
            GunClientHooks.events()
                .onShotFired(
                    player,
                    stats,
                    result.shot()
                        .attackType());
        }
        announce(player, stats, result.reload());
        if (result.outOfAmmo()) {
            GunClientHooks.events()
                .onDryFire(player, stats);
        }
    }

    /**
     * The reload key was pressed. Predicts the same decision the server will make.
     *
     * @param player the local player
     */
    public void reloadKey(EntityPlayer player) {
        ItemStack held = player.getHeldItem();
        if (held == null || !(held.getItem() instanceof ItemGun)) {
            return;
        }
        ItemGun gun = (ItemGun) held.getItem();
        follow(player, held, gun);
        GunStats stats = gun.stats();
        syncRounds(stats, held);
        ReloadStateMachine.Event event = controller.forceReload(stats, state, ammoView(player, stats));
        if (event == ReloadStateMachine.Event.REJECTED_NO_AMMO) {
            GunClientHooks.events()
                .onDryFire(player, stats);
        } else {
            announce(player, stats, event);
        }
    }

    // ----- server corrections (GunClientSync) ----------------------------------------------------------------

    @Override
    public void serverReloadStarted(EntityLivingBase entity, int ticks) {
        if (!(entity instanceof EntityPlayer) || isReloading()) {
            return;
        }
        EntityPlayer player = (EntityPlayer) entity;
        ItemStack held = player.getHeldItem();
        if (held == null || !(held.getItem() instanceof ItemGun)) {
            return;
        }
        ItemGun gun = (ItemGun) held.getItem();
        follow(player, held, gun);
        GunStats stats = gun.stats();
        syncRounds(stats, held);
        // The server says a reload is running; start the mirror's even if its own checks would disagree.
        announce(
            player,
            stats,
            controller.reload()
                .start(stats, state, ammoView(player, stats)));
        controller.zoom()
            .reset();
    }

    @Override
    public void serverReloadEnded(EntityLivingBase entity) {
        if (isReloading()) {
            controller.reload()
                .cancel(ReloadStateMachine.CancelReason.OTHER);
            GunClientHooks.events()
                .onReloadCanceled(entity);
        }
    }

    @Override
    public void serverZoom(EntityLivingBase entity, boolean zoomed) {
        double factor = 1.0;
        if (zoomed && entity.getHeldItem() != null
            && entity.getHeldItem()
                .getItem() instanceof ItemGun) {
            factor = ((ItemGun) entity.getHeldItem()
                .getItem()).stats()
                    .zoomFactor();
        }
        GunClientHooks.events()
            .setZoom(entity, zoomed, factor);
    }

    // ----- internals -----------------------------------------------------------------------------------------

    private void follow(EntityPlayer player, ItemStack held, ItemGun gun) {
        if (activeGun != gun || activeSlot != player.inventory.currentItem) {
            activeGun = gun;
            activeSlot = player.inventory.currentItem;
            lastServerLoaded = -1;
            quietTicks = 0;
        }
    }

    private void deselect(EntityPlayer player) {
        boolean wasReloading = isReloading();
        controller.reset();
        activeGun = null;
        activeSlot = -1;
        lastServerLoaded = -1;
        if (wasReloading) {
            GunClientHooks.events()
                .onReloadCanceled(player);
        }
        GunClientHooks.events()
            .setZoom(player, false, 1.0);
    }

    /** Copies the synced round count into the mirror when it changed, or after a quiet spell. */
    private void syncRounds(GunStats stats, ItemStack held) {
        int serverLoaded = GunNbt.read(held.getTagCompound(), stats)
            .loaded();
        if (serverLoaded != lastServerLoaded || quietTicks >= RESYNC_QUIET_TICKS && !isReloading()) {
            state.setLoaded(stats, serverLoaded);
            lastServerLoaded = serverLoaded;
            quietTicks = 0;
        }
    }

    private static void announce(EntityPlayer player, GunStats stats, ReloadStateMachine.Event event) {
        if (event == ReloadStateMachine.Event.STARTED) {
            GunClientHooks.events()
                .onReloadStarted(player, stats, stats.reloadTicks());
        } else if (event == ReloadStateMachine.Event.CANCELED) {
            GunClientHooks.events()
                .onReloadCanceled(player);
        }
    }

    /** A counting-only ammo source: it reports the real inventory but never changes it. */
    private static AmmoInventory ammoView(EntityPlayer player, GunStats stats) {
        final AmmoInventory real = new StoreAmmoInventory(
            new PlayerAmmoStore(player),
            GunItems.roundsPerLooseItem(stats));
        return new AmmoInventory() {

            @Override
            public int count(String ammoType) {
                return real.count(ammoType);
            }

            @Override
            public int withdraw(String ammoType, int maxItems) {
                return Math.min(real.count(ammoType), Math.max(0, maxItems));
            }

            @Override
            public void depositItems(String ammoType, int items) {}

            @Override
            public void depositRounds(String ammoType, int rounds) {}
        };
    }
}
