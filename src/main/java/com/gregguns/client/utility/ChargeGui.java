package com.gregguns.client.utility;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import com.gregguns.network.GregGunsNetwork;
import com.gregguns.utility.charge.ChargeContainer;
import com.gregguns.utility.charge.ChargeParams;
import com.gregguns.utility.charge.ChargeType;
import com.gregguns.utility.charge.PacketChargeButton;
import com.gregguns.utility.charge.TileExplosiveCharge;

/**
 * The charge settings screen: radius, length and fuse with plus and minus buttons, and Detonate. The radius buttons are
 * not shown for the TNT charge, whose radius is fixed (MUTL-33). Every press is a {@link PacketChargeButton}; the
 * server decides. All text is translated (MUTL-32). Client only; created by {@code UtilityGuis.clientElement}. Visual
 * check owed (no client run in this wave).
 */
// Spec: docs/techguns-spec/09-machines-utility/explosive-charges.md (Container and GUI)
public final class ChargeGui extends GuiContainer {

    private static final ResourceLocation TNT_BG = new ResourceLocation(
        "gregguns",
        "textures/gui/explosive_charge.png");
    private static final ResourceLocation ADV_BG = new ResourceLocation(
        "gregguns",
        "textures/gui/advanced_explosive_charge.png");

    private final ChargeContainer container;

    /**
     * @param container the slotless container that knows where the charge is
     */
    public ChargeGui(ChargeContainer container) {
        super(container);
        this.container = container;
        this.xSize = 176;
        this.ySize = 166;
    }

    private ChargeType type() {
        TileExplosiveCharge tile = container.charge();
        return tile == null ? ChargeType.TNT
            : tile.params()
                .type();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void initGui() {
        super.initGui();
        int left = (width - xSize) / 2;
        int top = (height - ySize) / 2;
        buttonList.add(
            new GuiButton(
                ChargeParams.BTN_ARM,
                left + 57,
                top + 73,
                60,
                20,
                StatCollector.translateToLocal("gui.gregguns.charge.detonate")));
        if (type().radiusAdjustable()) {
            addStep(ChargeParams.BTN_RADIUS_UP, left, top, 40);
        }
        addStep(ChargeParams.BTN_LENGTH_UP, left, top, 50);
        addStep(ChargeParams.BTN_FUSE_UP, left, top, 60);
    }

    @SuppressWarnings("unchecked")
    private void addStep(int plusId, int left, int top, int y) {
        buttonList.add(new GuiButton(plusId, left + 120, top + y, 10, 10, "+"));
        buttonList.add(new GuiButton(plusId + 1, left + 130, top + y, 10, 10, "-"));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        GregGunsNetwork.channel()
            .sendToServer(
                new PacketChargeButton(
                    mc.thePlayer.worldObj.provider.dimensionId,
                    container.x(),
                    container.y(),
                    container.z(),
                    button.id));
        if (button.id == ChargeParams.BTN_ARM) {
            mc.thePlayer.closeScreen();
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        TileExplosiveCharge tile = container.charge();
        String title = StatCollector.translateToLocal("tile.gregguns." + type().itemName() + ".name");
        fontRendererObj.drawString(title, (xSize - fontRendererObj.getStringWidth(title)) / 2, 28, 0x404040);
        if (tile == null) {
            return;
        }
        ChargeParams p = tile.params();
        fontRendererObj.drawString(
            StatCollector.translateToLocalFormatted("gui.gregguns.charge.radius", p.radius()),
            37,
            40,
            0x404040);
        fontRendererObj.drawString(
            StatCollector.translateToLocalFormatted("gui.gregguns.charge.length", p.length()),
            37,
            50,
            0x404040);
        fontRendererObj.drawString(
            StatCollector.translateToLocalFormatted("gui.gregguns.charge.delay", p.fuse() / 20),
            37,
            60,
            0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager()
            .bindTexture(type() == ChargeType.ADVANCED ? ADV_BG : TNT_BG);
        drawTexturedModalRect((width - xSize) / 2, (height - ySize) / 2, 0, 0, xSize, ySize);
    }
}
