package com.gregguns.icbm.production.gc;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.util.StatCollector;

import com.gregguns.icbm.production.ContainerMissileBench;
import com.gregguns.icbm.production.MissileBuildSpec;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import micdoodle8.mods.galacticraft.api.recipe.ISchematicResultPage;
import micdoodle8.mods.galacticraft.api.recipe.SchematicRegistry;

/**
 * The client screen of a missile page: a panel with a rocket silhouette and a framed outline for every slot, laid out
 * in the shape of the missile (see MissileBenchLayout), and
 * Galacticraft's Back and Next buttons to flip between the unlocked pages. Draws with vanilla rectangles, so it needs
 * no texture. Client only.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/build-and-recipes.md (NASA Workbench page)
@SideOnly(Side.CLIENT)
public final class GuiMissileBench extends GuiContainer implements ISchematicResultPage {

    private static final int BG = 0xFFC6C6C6;
    private static final int EDGE = 0xFF373737;
    private static final int SLOT = 0xFF8B8B8B;
    private final MissileBuildSpec spec;
    private int pageIndex;

    public GuiMissileBench(InventoryPlayer inventory, MissileBuildSpec spec) {
        super(new ContainerMissileBench(inventory, spec));
        this.spec = spec;
        this.xSize = 176;
        this.ySize = ContainerMissileBench.PLAYER_Y + 82;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void initGui() {
        super.initGui();
        buttonList.add(
            new GuiButton(
                0,
                guiLeft - 44,
                guiTop + 20,
                40,
                20,
                StatCollector.translateToLocal("gui.button.back.name")));
        buttonList.add(
            new GuiButton(
                1,
                guiLeft - 44,
                guiTop + 44,
                40,
                20,
                StatCollector.translateToLocal("gui.button.next.name")));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (!button.enabled) {
            return;
        }
        if (button.id == 0) {
            SchematicRegistry.flipToLastPage(pageIndex);
        } else if (button.id == 1) {
            SchematicRegistry.flipToNextPage(pageIndex);
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        Gui.drawRect(guiLeft, guiTop, guiLeft + xSize, guiTop + ySize, EDGE);
        Gui.drawRect(guiLeft + 1, guiTop + 1, guiLeft + xSize - 1, guiTop + ySize - 1, BG);
        MissileBenchArt.draw(spec, guiLeft + ContainerMissileBench.X0, guiTop, true);
        for (Object o : inventorySlots.inventorySlots) {
            Slot s = (Slot) o;
            if (s.yDisplayPosition >= ContainerMissileBench.PLAYER_Y) {
                int x = guiLeft + s.xDisplayPosition;
                int y = guiTop + s.yDisplayPosition;
                Gui.drawRect(x - 1, y - 1, x + 17, y + 17, EDGE);
                Gui.drawRect(x, y, x + 16, y + 16, SLOT);
            }
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRendererObj.drawString(
            StatCollector.translateToLocalFormatted("gregguns.icbm.bench.title", spec.number()),
            8,
            6,
            0x404040);
        MissileBenchArt.drawLabels(fontRendererObj, spec, ContainerMissileBench.X0, 0);
        fontRendererObj.drawString(
            StatCollector.translateToLocal("container.inventory"),
            8,
            ContainerMissileBench.PLAYER_Y - 10,
            0x404040);
    }

    @Override
    public void setPageIndex(int index) {
        this.pageIndex = index;
    }
}
