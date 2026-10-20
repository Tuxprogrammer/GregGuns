package com.gregguns.client.gun;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import com.gregguns.client.GunsClientModule;
import com.gregguns.gun.net.PacketGunCharge;
import com.gregguns.gun.net.PacketGunTrigger;
import com.gregguns.gun.server.GunKeybinds;
import com.gregguns.item.gun.ItemGun;
import com.gregguns.item.gun.ItemToolGun;
import com.gregguns.network.GregGunsNetwork;
import com.gtnewhorizon.gtnhlib.keybind.SyncedKeybind;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Reads the local player's gun input once per client tick and feeds both the server and the prediction mirror.
 *
 * <ul>
 * <li><b>Fire</b> is the vanilla attack button while a gun is held, no GUI is open and the window has focus. Only the
 * press and the release are sent ({@code PacketGunTrigger}); the server owns the shots.</li>
 * <li><b>Reload</b> is the synced reload key. GTNHLib already tells the server; this handler only starts the local
 * prediction.</li>
 * <li><b>Zoom</b> is the right-click, sent by {@link ItemGun#onItemRightClick}.</li>
 * </ul>
 *
 * <p>
 * Reads {@code KeyBinding} state, never the raw key, so a GUI or another mod can block the input (GGS-01-16). Client
 * only.
 */
// Spec: docs/techguns-spec/02-guns/gun-lifecycle.md section 4; docs/gregguns-spec/01-core/keybinds.md.
// Deviation: Techguns cancels the mouse event and mirrors the button itself; GregGuns reads the attack key binding.
@SideOnly(Side.CLIENT)
public final class GunInputHandler {

    private final ClientGunMirror mirror;
    private boolean triggerDown;
    private boolean chargeDown;
    private int lastSlot = -1;
    private ItemGun lastGun;
    private boolean autotestAttack;

    /** @param mirror the local prediction mirror */
    public GunInputHandler(ClientGunMirror mirror) {
        this.mirror = mirror;
    }

    /**
     * Dev autotest only ({@code com.gregguns.client.autotest}): holds the fire button down without a real key press
     * and without window focus, so the real input path (mirror plus trigger packet) can be driven by a script.
     *
     * @param down true to hold the button
     */
    public void setAutotestAttack(boolean down) {
        this.autotestAttack = down;
    }

    /** Runs for the local player at the end of each client tick. */
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.side != Side.CLIENT || event.phase != TickEvent.Phase.END || event.player != mc.thePlayer) {
            return;
        }
        EntityPlayer player = event.player;
        ItemStack held = player.getHeldItem();
        ItemGun gun = held != null && held.getItem() instanceof ItemGun ? (ItemGun) held.getItem() : null;
        boolean canAct = gun != null && mc.currentScreen == null
            && (mc.inGameHasFocus || autotestAttack)
            && player.isEntityAlive();

        // A switch makes the server drop its input state, so forget ours and let a still-held button press again.
        if (player.inventory.currentItem != lastSlot || gun != lastGun) {
            lastSlot = player.inventory.currentItem;
            lastGun = gun;
            triggerDown = false;
            mirror.trigger(false);
            chargeDown = false;
            mirror.chargeButton(false, false);
        }

        boolean wantDown = canAct && (autotestAttack || triggerKey(mc, gun));
        if (wantDown != triggerDown) {
            triggerDown = wantDown;
            mirror.trigger(wantDown);
            if (gun != null) {
                GregGunsNetwork.channel()
                    .sendToServer(
                        new PacketGunTrigger(
                            player.worldObj.provider.dimensionId,
                            player.inventory.currentItem,
                            wantDown));
            }
        }

        // Charge guns (Bio Gun, Power Hammer): the right button charges while held and fires on release.
        boolean wantCharge = canAct && gun.stats()
            .isCharge() && mc.gameSettings.keyBindUseItem.getIsKeyPressed();
        if (wantCharge != chargeDown) {
            chargeDown = wantCharge;
            mirror.chargeButton(
                wantCharge,
                player.capabilities.isCreativeMode || gun.stateOf(held)
                    .loaded() > 0);
            GregGunsNetwork.channel()
                .sendToServer(
                    new PacketGunCharge(
                        player.worldObj.provider.dimensionId,
                        player.inventory.currentItem,
                        wantCharge));
        }
        if (gun != null) {
            mirror.charge()
                .tick(gun.stats());
        }
        // The first-person charge shift follows the local charge fraction; 0 with no charge gun in hand.
        GunsClientModule.animations()
            .chargeShift()
            .tick(
                gun == null ? 0.0
                    : mirror.charge()
                        .fraction(gun.stats()));

        SyncedKeybind reload = GunKeybinds.reload();
        KeyBinding reloadBinding = reload == null ? null : reload.getKeybinding();
        if (reloadBinding != null) {
            // isPressed() consumes one press; drain them all so presses made in a GUI do not fire later.
            boolean pressed = false;
            while (reloadBinding.isPressed()) {
                pressed = true;
            }
            if (pressed && canAct) {
                mirror.reloadKey(player);
            }
        }

        mirror.tick(player);
    }

    /**
     * The button that pulls the trigger. A tool gun's left button is melee and mining, so its shot is the right
     * button; a charge tool gun (Power Hammer) has no plain trigger at all, its right button is the charge.
     */
    private static boolean triggerKey(Minecraft mc, ItemGun gun) {
        if (gun instanceof ItemToolGun) {
            return !gun.stats()
                .isCharge() && mc.gameSettings.keyBindUseItem.getIsKeyPressed();
        }
        return mc.gameSettings.keyBindAttack.getIsKeyPressed();
    }
}
