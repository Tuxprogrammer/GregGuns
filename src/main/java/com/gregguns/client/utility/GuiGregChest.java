package com.gregguns.client.utility;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.inventory.Slot;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import com.gregguns.utility.storage.ContainerGregChest;
import com.gregguns.utility.storage.TileGregChest;

/**
 * The chest GUI: a gray panel with 3 rows of 12 slot wells above the player's inventory, drawn with flat rectangles
 * (the chest sheet in the assets is a widget sheet, not a background). Client only. Visual check owed.
 */
// Spec: docs/techguns-spec/09-machines-utility/misc-tile-entities.md section 1 (GUI); placeholder look
public final class GuiGregChest extends GuiContainer {

    private static final int PANEL = 0xFFC6C6C6;
    private static final int LIGHT = 0xFFFFFFFF;
    private static final int SHADOW = 0xFF555555;
    private static final int WELL = 0xFF8B8B8B;

    private final ContainerGregChest chestContainer;

    /** @param container the chest container */
    public GuiGregChest(ContainerGregChest container) {
        super(container);
        this.chestContainer = container;
        this.xSize = ContainerGregChest.WIDTH;
        this.ySize = ContainerGregChest.HEIGHT;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        TileGregChest chest = chestContainer.chest();
        String title = chest.hasCustomInventoryName() ? chest.getInventoryName()
            : StatCollector.translateToLocal(chest.getInventoryName());
        fontRendererObj.drawString(title, 8, 6, 0x404040);
        fontRendererObj.drawString(StatCollector.translateToLocal("container.inventory"), 35, ySize - 94, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        int left = (width - xSize) / 2;
        int top = (height - ySize) / 2;
        drawRect(left, top, left + xSize, top + ySize, SHADOW);
        drawRect(left, top, left + xSize - 1, top + ySize - 1, LIGHT);
        drawRect(left + 1, top + 1, left + xSize - 1, top + ySize - 1, PANEL);
        for (Object o : inventorySlots.inventorySlots) {
            Slot s = (Slot) o;
            int sx = left + s.xDisplayPosition;
            int sy = top + s.yDisplayPosition;
            drawRect(sx - 1, sy - 1, sx + 17, sy + 17, SHADOW);
            drawRect(sx, sy, sx + 18, sy + 18, LIGHT);
            drawRect(sx, sy, sx + 17, sy + 17, WELL);
        }
        RenderHelper.enableGUIStandardItemLighting();
    }
}
