package com.gregguns.client.armor;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import com.gregguns.armor.slots.ContainerAmmoPouch;

/**
 * The Ammo Pouch GUI: vanilla's one-row chest background with the three unused slot cells painted over, so six slots
 * show. Client only; created by {@code EquipmentGuiHandler.getClientGuiElement}. Visual check owed (no client run in
 * this wave).
 */
// Spec: docs/gregguns-spec/05-armor/baubles-slots.md (GGS-05-03)
public final class GuiAmmoPouch extends GuiContainer {

    // Source: vanilla 1.7.10 assets/minecraft/textures/gui/container/generic_54.png (the chest GUI sheet).
    private static final ResourceLocation CHEST = new ResourceLocation("textures/gui/container/generic_54.png");
    /** Vanilla's panel gray, to hide the three slot cells a six-slot pouch does not use. */
    private static final int PANEL = 0xFFC6C6C6;

    /** @param container the pouch container */
    public GuiAmmoPouch(ContainerAmmoPouch container) {
        super(container);
        this.ySize = 132;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRendererObj.drawString(StatCollector.translateToLocal("gui.gregguns.ammo_pouch"), 8, 6, 0x404040);
        fontRendererObj.drawString(StatCollector.translateToLocal("container.inventory"), 8, ySize - 94, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager()
            .bindTexture(CHEST);
        int left = (width - xSize) / 2;
        int top = (height - ySize) / 2;
        // One chest row (top 17 + 18 high) and the player inventory (114 high), as GuiChest draws a one-row chest.
        drawTexturedModalRect(left, top, 0, 0, xSize, 18 + 17);
        drawTexturedModalRect(left, top + 18 + 17, 0, 126, xSize, 96);
        drawRect(left + 7 + 18 * 6, top + 17, left + 7 + 18 * 9, top + 17 + 18, PANEL);
    }
}
