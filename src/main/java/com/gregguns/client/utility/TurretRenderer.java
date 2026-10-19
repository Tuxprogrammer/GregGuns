package com.gregguns.client.utility;

import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

import com.gregguns.GregGuns;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Draws the turret entity with {@link TurretModel}. Placeholder art: the Steel Bastion Plating texture stands in for
 * the turret sheet until the asset agent draws one. Client only.
 */
// Spec: docs/gregguns-spec/09-machines-utility/turret.md (placeholder rendering)
@SideOnly(Side.CLIENT)
public final class TurretRenderer extends RenderLiving {

    private static final ResourceLocation TEXTURE = new ResourceLocation(
        GregGuns.MODID,
        "textures/blocks/casings/bastion_plating_steel.png");

    /** Creates the renderer with a small round shadow. */
    public TurretRenderer() {
        super(new TurretModel(), 0.4F);
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return TEXTURE;
    }
}
