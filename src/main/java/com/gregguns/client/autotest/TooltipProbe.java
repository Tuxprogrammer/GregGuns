package com.gregguns.client.autotest;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.GuiScreenEvent;

import org.lwjgl.input.Keyboard;

import com.gregguns.GregGuns;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Dev autotest helper for tooltip shots on a display nobody hovers: {@code tip <slot|off>} draws the tooltip of one
 * container slot after the open container GUI, and {@code shift <on|off>} makes the game see Shift as held while it
 * is drawn (the tooltips of the item classes read {@code GuiScreen.isShiftKeyDown}). Client only.
 */
@SideOnly(Side.CLIENT)
public final class TooltipProbe {

    private int slot = -1;
    private boolean shift;
    /** The key buffer is missing under lwjgl3ify; say so once, not on every draw. */
    private static boolean pressWarned;

    public void setSlot(String arg) {
        slot = "off".equals(arg) ? -1 : Integer.parseInt(arg);
    }

    public void setShift(boolean on) {
        shift = on;
        if (!on) {
            press(false);
        }
    }

    /**
     * Writes the Shift state where {@code Keyboard.isKeyDown} reads it. LWJGL 2 rewrites its buffer on every poll, so
     * this is redone per draw.
     */
    private static void press(boolean down) {
        try {
            ByteBuffer keys = (ByteBuffer) ReflectionHelper.findField(Keyboard.class, "keyDownBuffer")
                .get(null);
            keys.put(Keyboard.KEY_LSHIFT, (byte) (down ? 1 : 0));
            return;
        } catch (ReflectiveOperationException | RuntimeException e) {
            // Not LWJGL 2: try lwjgl3ify below.
        }
        try {
            pressLwjgl3ify(down);
        } catch (ReflectiveOperationException | RuntimeException e) {
            if (!pressWarned) {
                pressWarned = true;
                GregGuns.LOG.warn("[gregguns] AUTOTEST cannot set the shift key here (no known key buffer)", e);
            }
        }
    }

    /**
     * Under lwjgl3ify, {@code org.lwjgl.input.Keyboard.isKeyDown} forwards to {@code org.lwjglx.input.Keyboard}, which
     * reads SDL's key-state array by SDL scancode. SDL only rewrites an entry on a key event, so the written state
     * holds
     * until {@code shift off} clears it.
     */
    // Source: lwjgl3ify 3.0.37 org.lwjglx.input.Keyboard#isKeyDown and #sdlKeyPressedArray,
    // https://github.com/GTNewHorizons/lwjgl3ify/blob/3.0.37/src/main/java/org/lwjglx/input/Keyboard.java#L281-L291
    // and org.lwjglx.input.KeyCodes#lwjglToSdlScancode (same tag).
    private static void pressLwjgl3ify(boolean down) throws ReflectiveOperationException {
        Class<?> keyboard = Class.forName("org.lwjglx.input.Keyboard");
        ByteBuffer keys = (ByteBuffer) keyboard.getField("sdlKeyPressedArray")
            .get(null);
        if (keys == null) {
            throw new IllegalStateException("lwjgl3ify has no key state array yet");
        }
        int scancode = (Integer) Class.forName("org.lwjglx.input.KeyCodes")
            .getMethod("lwjglToSdlScancode", int.class)
            .invoke(null, Keyboard.KEY_LSHIFT);
        keys.put(scancode, (byte) (down ? 1 : 0));
    }

    @SubscribeEvent
    public void before(GuiScreenEvent.DrawScreenEvent.Pre event) {
        if (shift) {
            press(true);
        }
    }

    @SubscribeEvent
    public void after(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (slot < 0 || !(event.gui instanceof GuiContainer)) {
            return;
        }
        GuiContainer gui = (GuiContainer) event.gui;
        if (slot >= gui.inventorySlots.inventorySlots.size()) {
            return;
        }
        Slot s = (Slot) gui.inventorySlots.inventorySlots.get(slot);
        ItemStack stack = s.getStack();
        if (stack == null) {
            return;
        }
        if (shift) {
            press(true);
        }
        try {
            // GuiScreen.renderToolTip(ItemStack, int, int), obfuscated func_146285_a.
            Method m = ReflectionHelper.findMethod(
                GuiScreen.class,
                gui,
                new String[] { "renderToolTip", "func_146285_a" },
                ItemStack.class,
                int.class,
                int.class);
            int left = ReflectionHelper.findField(GuiContainer.class, "guiLeft", "field_147003_i")
                .getInt(gui);
            int top = ReflectionHelper.findField(GuiContainer.class, "guiTop", "field_147009_r")
                .getInt(gui);
            m.invoke(gui, stack, left + s.xDisplayPosition + 8, top + s.yDisplayPosition + 8);
        } catch (ReflectiveOperationException | RuntimeException e) {
            GregGuns.LOG.error("[gregguns] AUTOTEST could not draw the tooltip", e);
        }
    }
}
