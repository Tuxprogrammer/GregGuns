package com.gregguns.client.autotest;

import java.io.File;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ScreenShotHelper;

import com.gregguns.GregGuns;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Takes a screenshot with the vanilla {@link ScreenShotHelper} (the F2 code) and renames the file to
 * {@code <player>-<label>.png}, so a script can find it by name. Call at the end of the render tick, after the frame
 * (including any open GUI) has been drawn. Dev autotest only. Client only.
 */
@SideOnly(Side.CLIENT)
final class Screenshots {

    private Screenshots() {}

    /**
     * Saves the current frame.
     *
     * @param player the local player's name
     * @param label  what the shot shows
     */
    static void take(String player, String label) {
        Minecraft mc = Minecraft.getMinecraft();
        File dir = new File(mc.mcDataDir, "screenshots");
        long before = System.currentTimeMillis() - 1;
        ScreenShotHelper.saveScreenshot(mc.mcDataDir, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
        File newest = null;
        File[] files = dir.listFiles((d, name) -> name.endsWith(".png") && !name.startsWith(player + "-"));
        if (files != null) {
            for (File file : files) {
                if (file.lastModified() >= before && (newest == null || file.lastModified() > newest.lastModified())) {
                    newest = file;
                }
            }
        }
        if (newest == null) {
            ClientAutotest.logLine("screenshot " + label + " FAILED (no file written)");
            return;
        }
        File target = new File(dir, player + "-" + label + ".png");
        if (target.exists() && !target.delete()) {
            GregGuns.LOG.warn("[gregguns] AUTOTEST could not replace {}", target);
        }
        boolean renamed = newest.renameTo(target);
        ClientAutotest.logLine(
            "screenshot " + label
                + " "
                + (renamed ? target : newest)
                + " [yaw "
                + mc.thePlayer.rotationYaw
                + " pitch "
                + mc.thePlayer.rotationPitch
                + " fov "
                + mc.gameSettings.fovSetting
                + " view "
                + mc.gameSettings.thirdPersonView
                + "]");
    }
}
