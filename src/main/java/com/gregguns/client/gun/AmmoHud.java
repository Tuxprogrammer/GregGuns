package com.gregguns.client.gun;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import com.gregguns.core.config.ClientConfig;
import com.gregguns.gun.logic.EnergyMath;
import com.gregguns.gun.logic.GunState;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.item.gun.ItemEnergyGun;
import com.gregguns.item.gun.ItemGun;
import com.gregguns.item.gun.PlayerAmmoStore;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The ammo overlay: rounds in the held gun, magazines carried, and a reload bar. Bottom-right, offset by
 * {@link ClientConfig#hudOffsetX} and {@link ClientConfig#hudOffsetY}. All text comes from lang keys.
 *
 * <p>
 * The values are read from the player's state at draw time and nothing is counted per frame, so the overlay stays
 * correct if Angelica's HUD cache is switched on (GGS-01-31). It draws inside a {@code RenderGameOverlayEvent.Post}
 * handler only and never cancels an event. The rounds come from the stack NBT the server synced, not from the
 * prediction mirror, so the display is never ahead of the server.
 *
 * <p>
 * Client only.
 */
// Spec: docs/gregguns-spec/01-core/hud-overlays.md; docs/techguns-spec/01-core/input-and-hud.md (GuiOverlayAmmoCount).
// Deviation: drawn after the HOTBAR element, not EXPERIENCE: the experience element is skipped in creative mode,
// which would hide the ammo from creative players. Fixes CORE-18 (alignment by measured width, codes stripped).
@SideOnly(Side.CLIENT)
public final class AmmoHud {

    private static final int LINE_HEIGHT = 10;
    private static final int BAR_WIDTH = 60;
    private static final int BAR_HEIGHT = 3;
    private static final int BAR_BACK = 0x80000000;
    private static final int BAR_FILL = 0xFF55FF55;
    private static final int TEXT_COLOR = 0xFFFFFF;

    private final ClientGunMirror mirror;

    /** @param mirror the prediction mirror, for the reload progress */
    public AmmoHud(ClientGunMirror mirror) {
        this.mirror = mirror;
    }

    /** Draws the overlay after the hotbar. */
    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.HOTBAR || !ClientConfig.showAmmoHud) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.thePlayer;
        if (player == null) {
            return;
        }
        ItemStack held = player.getHeldItem();
        if (held == null || !(held.getItem() instanceof ItemGun)) {
            return;
        }
        ItemGun gun = (ItemGun) held.getItem();
        GunStats stats = gun.stats();
        GunState state = gun.stateOf(held);
        boolean reloading = mirror.isReloading();

        List<String> lines = new ArrayList<>();
        if (gun instanceof ItemEnergyGun) {
            // Q5: an energy gun has no clip; the HUD shows its charge percent (GGS-02-13).
            ItemEnergyGun energy = (ItemEnergyGun) gun;
            long charge = energy.chargeOf(held);
            EnumChatFormatting color = EnergyMath.canFire(charge, stats.euPerShot()) ? EnumChatFormatting.WHITE
                : EnumChatFormatting.RED;
            lines.add(
                StatCollector.translateToLocalFormatted(
                    "gregguns.hud.charge",
                    color.toString() + EnergyMath.percent(charge, energy.capacityEu()) + EnumChatFormatting.RESET));
        } else {
            EnumChatFormatting roundColor = state.canFire(stats) ? EnumChatFormatting.WHITE : EnumChatFormatting.RED;
            lines.add(
                StatCollector.translateToLocalFormatted(
                    "gregguns.hud.rounds",
                    roundColor.toString() + state.loaded() + EnumChatFormatting.RESET,
                    stats.magazineSize()));
            int magazines = new PlayerAmmoStore(player).count(stats.ammoType());
            lines.add(StatCollector.translateToLocalFormatted("gregguns.hud.magazines", magazines));
        }
        if (reloading) {
            lines.add(
                StatCollector
                    .translateToLocalFormatted("gregguns.hud.reloading", Math.round(mirror.reloadProgress() * 100.0)));
        }

        ScaledResolution res = event.resolution;
        FontRenderer font = mc.fontRenderer;
        for (int i = 0; i < lines.size(); i++) {
            String text = lines.get(i);
            int width = font.getStringWidth(HudLayout.stripFormatting(text));
            int x = HudLayout.rightAlignedX(res.getScaledWidth(), width, ClientConfig.hudOffsetX);
            int y = HudLayout.lineY(res.getScaledHeight(), ClientConfig.hudOffsetY, lines.size(), i, LINE_HEIGHT);
            font.drawStringWithShadow(text, x, y, TEXT_COLOR);
        }
        if (reloading) {
            int right = res.getScaledWidth() - Math.max(0, ClientConfig.hudOffsetX);
            int top = HudLayout.lineY(res.getScaledHeight(), ClientConfig.hudOffsetY, lines.size(), 0, LINE_HEIGHT)
                - BAR_HEIGHT
                - 2;
            Gui.drawRect(right - BAR_WIDTH, top, right, top + BAR_HEIGHT, BAR_BACK);
            Gui.drawRect(
                right - BAR_WIDTH,
                top,
                right - BAR_WIDTH + HudLayout.filledWidth(mirror.reloadProgress(), BAR_WIDTH),
                top + BAR_HEIGHT,
                BAR_FILL);
        }
    }
}
