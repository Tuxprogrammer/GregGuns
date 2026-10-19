package com.gregguns.client.utility;

import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

import com.gregguns.GregGuns;
import com.gregguns.utility.turret.entity.TurretEntity;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Draws the turret entity with {@link TurretModel}, in the sheet for the installed armor plate: 0 none, 5 iron, 10
 * steel, 15 aluminum, 20 carbon. The plate is synced to clients by {@link TurretEntity#displayArmorValue()}.
 * Client only.
 */
// Spec: docs/techguns-spec/09-machines-utility/model-notes/turret_head.md (section 5),
// docs/gregguns-spec/09-machines-utility/turret.md
@SideOnly(Side.CLIENT)
public final class TurretRenderer extends RenderLiving {

    private static final String DIR = "textures/models/machines/turret_sheet";
    private static final ResourceLocation[] TEXTURES = { tex(""), tex("_iron"), tex("_steel"), tex("_aluminum"),
        tex("_carbon") };

    private static ResourceLocation tex(String suffix) {
        return new ResourceLocation(GregGuns.MODID, DIR + suffix + ".png");
    }

    /** Creates the renderer with a small round shadow. */
    public TurretRenderer() {
        super(new TurretModel(), 0.4F);
    }

    /**
     * @param armorValue the plate's armor value
     * @return the sheet for it: one tier per 5 points, clamped to the five tiers
     */
    static ResourceLocation textureFor(int armorValue) {
        return TEXTURES[Math.max(0, Math.min(TEXTURES.length - 1, armorValue / 5))];
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return textureFor(entity instanceof TurretEntity ? ((TurretEntity) entity).displayArmorValue() : 0);
    }
}
