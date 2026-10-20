package com.gregguns.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.common.util.Constants;

import org.lwjgl.opengl.GL11;

import com.gregguns.client.GunsClientModule;
import com.gregguns.client.anim.AnimWindow;
import com.gregguns.client.anim.CamoIndex;
import com.gregguns.client.anim.GunAnimState;
import com.gregguns.client.render.profile.AkimboLayout;
import com.gregguns.client.render.profile.GunTransforms;
import com.gregguns.client.render.profile.MuzzleFlashSpec;
import com.gregguns.client.render.profile.TransformProgram;
import com.gregguns.gun.logic.GunNbt;
import com.gregguns.item.gun.ItemGun;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The item renderer for one gun, following the per-frame algorithm of the spec: for each part, push a matrix, apply
 * the view's transforms (first person base then reload, firing or idle; third person aimed pre-rotation then base then
 * reload or recoil; ground; icon), bind the texture, draw the part, pop. After the last part the muzzle flash is drawn
 * (first person: the screen effect in the muzzle frame; third person: crossed planes at the muzzle). While the scope
 * overlay is showing in first person nothing is drawn; the overlay itself comes from the HUD layer.
 *
 * <p>
 * Every matrix push is paired with a pop in a {@code finally} (GGS-03-02). Plain {@code GL11} only (GGS-03-01).
 * Client only.
 *
 * <p>
 * Spec: rendering-framework.md sections 4, 6 and 7; angelica-item-rendering.md.
 */
@SideOnly(Side.CLIENT)
final class GunItemRenderer implements IItemRenderer {

    /** Texels at or below this alpha are discarded (vanilla's own cut-out threshold). */
    private static final float ALPHA_CUTOFF = 0.1F;

    private static final TransformProgram.OpSink GL_SINK = new TransformProgram.OpSink() {

        @Override
        public void translate(double x, double y, double z) {
            GL11.glTranslated(x, y, z);
        }

        @Override
        public void rotate(double degrees, double x, double y, double z) {
            GL11.glRotated(degrees, x, y, z);
        }

        @Override
        public void scale(double x, double y, double z) {
            GL11.glScaled(x, y, z);
        }
    };

    private final GunModel model;
    private final GunTransforms transforms;
    private final ResourceLocation[] textures;
    private final ModelState state = new ModelState();
    private final boolean akimbo;

    GunItemRenderer(GunModel model, GunTransforms transforms, boolean akimbo) {
        this.akimbo = akimbo;
        this.model = model;
        this.transforms = transforms;
        this.textures = model.textures();
    }

    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        switch (type) {
            case EQUIPPED:
            case EQUIPPED_FIRST_PERSON:
            case ENTITY:
            case INVENTORY:
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        return true;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack stack, Object... data) {
        GunTransforms t = transforms;
        GunAnimations anims = GunsClientModule.animations();
        boolean firstPerson = type == ItemRenderType.EQUIPPED_FIRST_PERSON;
        boolean held = type == ItemRenderType.EQUIPPED || firstPerson;
        EntityLivingBase holder = held && data.length > 1 && data[1] instanceof EntityLivingBase
            ? (EntityLivingBase) data[1]
            : null;
        if (firstPerson && t.scope() != null
            && anims.zoom()
                .isScoped(stack.getItem())) {
            return; // the scope overlay replaces the gun in first person (spec section 4, step 7)
        }
        double now = anims.now();
        int tag = Item.getIdFromItem(stack.getItem());
        GunAnimState anim = holder == null ? null : anims.peek(holder);
        double reload = anim == null ? AnimWindow.NONE : anim.reloadProgress(now, tag);
        double shotRecoil = anim == null ? AnimWindow.NONE : anim.recoilProgress(now, tag);
        double shotFlash = anim == null ? AnimWindow.NONE : anim.flashProgress(now, tag);
        int attackType = anim == null ? 0 : anim.attackType();

        // An akimbo gun in first person draws the single model once per hand; only the hand that fired recoils.
        // A double-shot akimbo gun (2 rounds per pull) fires both hands at once, so both recoil.
        boolean doubleShot = akimbo && stack.getItem() instanceof ItemGun
            && ((ItemGun) stack.getItem()).stats()
                .ammoPerShot() >= 2;
        int hands = AkimboLayout.handsDrawn(akimbo, firstPerson);
        for (int hand = 0; hand < hands; hand++) {
            boolean fired = AkimboLayout.animates(akimbo, firstPerson, hand, attackType, doubleShot);
            double recoil = fired ? shotRecoil : AnimWindow.NONE;
            double flash = fired ? shotFlash : AnimWindow.NONE;
            boolean mirrored = hands > 1 && hand > 0;
            GL11.glPushMatrix();
            try {
                if (mirrored) {
                    // The second gun: a mirror image about the screen centre. Mirroring flips the winding.
                    double[] shift = AkimboLayout.mirrorTranslate(hand);
                    GL11.glTranslated(shift[0], shift[1], shift[2]);
                    GL11.glRotated(-AkimboLayout.MIRROR_YAW, 0.0, 1.0, 0.0);
                    GL11.glScaled(AkimboLayout.mirrorScaleX(hand), 1.0, 1.0);
                    GL11.glRotated(AkimboLayout.MIRROR_YAW, 0.0, 1.0, 0.0);
                    GL11.glFrontFace(GL11.GL_CW);
                }
                drawGun(stack, type, holder, hand, reload, recoil, flash, firstPerson);
            } finally {
                if (mirrored) {
                    GL11.glFrontFace(GL11.GL_CCW);
                }
                GL11.glPopMatrix();
            }
        }
    }

    private void drawGun(ItemStack stack, ItemRenderType type, EntityLivingBase holder, int hand, double reload,
        double recoil, double flash, boolean firstPerson) {
        GunTransforms t = transforms;
        fillState(stack, type, reload, recoil, hand);
        ResourceLocation texture = textures[CamoIndex.clamp(camoOf(stack), textures.length)];
        int parts = Math.min(t.partCount(), model.parts());
        for (int part = 0; part < parts; part++) {
            GL11.glPushMatrix();
            try {
                applyTransforms(type, holder, part, reload, recoil);
                Minecraft.getMinecraft()
                    .getTextureManager()
                    .bindTexture(texture);
                if (t.partVisible(part, state.ammoLeft, state.reloadProgress, state.fireProgress)) {
                    // Alpha cut-out textures (the scatter laser's sight window, the grim reaper's cage) need the alpha
                    // test: without it their transparent texels draw as solid. Restored right after.
                    boolean alphaWas = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
                    int funcWas = GL11.glGetInteger(GL11.GL_ALPHA_TEST_FUNC);
                    float refWas = GL11.glGetFloat(GL11.GL_ALPHA_TEST_REF);
                    GL11.glEnable(GL11.GL_ALPHA_TEST);
                    GL11.glAlphaFunc(GL11.GL_GREATER, ALPHA_CUTOFF);
                    try {
                        model.render(part, state);
                    } finally {
                        GL11.glAlphaFunc(funcWas, refWas);
                        if (!alphaWas) {
                            GL11.glDisable(GL11.GL_ALPHA_TEST);
                        }
                    }
                }
                if (part == parts - 1 && type == ItemRenderType.EQUIPPED
                    && flash != AnimWindow.NONE
                    && t.muzzleFlash() != null) {
                    GlFx.drawThirdPersonFlash(t.muzzleFlash(), flash, model.muzzle(), model.muzzleAlongX());
                }
            } finally {
                GL11.glPopMatrix();
            }
        }
        MuzzleFlashSpec spec = t.muzzleFlash();
        if (firstPerson && flash != AnimWindow.NONE && spec != null) {
            GlFx.drawFirstPersonFlash(spec, flash);
        }
    }

    private void applyTransforms(ItemRenderType type, EntityLivingBase holder, int part, double reload, double recoil) {
        GunTransforms t = transforms;
        double fire = recoil == AnimWindow.NONE ? 0.0 : recoil;
        // The charge shift is first person only: the local player's eased charge fraction (0 at rest).
        GunAnimations anims = GunsClientModule.animations();
        double chargeShift = type == ItemRenderType.EQUIPPED_FIRST_PERSON ? anims.chargeShift()
            .value(anims.partial()) : 0.0;
        switch (type) {
            case EQUIPPED_FIRST_PERSON:
                t.fpBase()
                    .apply(0.0, part, GL_SINK);
                if (reload != AnimWindow.NONE) {
                    t.fpReload()
                        .apply(reload, part, GL_SINK);
                } else if (recoil != AnimWindow.NONE) {
                    t.fpFire()
                        .apply(recoil, part, GL_SINK);
                } else if (t.hasFpCharge() && chargeShift > 0.0) {
                    t.fpCharge()
                        .apply(chargeShift, part, GL_SINK);
                } else {
                    t.fpIdle()
                        .apply(0.0, part, GL_SINK);
                }
                break;
            case EQUIPPED:
                if (holder instanceof EntityPlayer) {
                    t.applyAimed(GL_SINK);
                }
                t.tpBase()
                    .apply(fire, part, GL_SINK);
                if (reload != AnimWindow.NONE) {
                    t.tpReload()
                        .apply(reload, part, GL_SINK);
                } else {
                    t.tpRecoil()
                        .apply(fire, part, GL_SINK);
                }
                break;
            case ENTITY:
                t.ground()
                    .apply(0.0, part, GL_SINK);
                break;
            default:
                t.icon()
                    .apply(0.0, part, GL_SINK);
                break;
        }
    }

    private void fillState(ItemStack stack, ItemRenderType type, double reload, double recoil, int hand) {
        state.renderType = type;
        state.reloadProgress = reload == AnimWindow.NONE ? ModelState.NOT_RELOADING : (float) reload;
        state.fireProgress = recoil == AnimWindow.NONE ? 0.0f : (float) recoil;
        NBTTagCompound tag = stack.getTagCompound();
        int ammo = tag != null && tag.hasKey(GunNbt.KEY_AMMO, Constants.NBT.TAG_ANY_NUMERIC)
            ? tag.getShort(GunNbt.KEY_AMMO)
            : 100;
        state.ammoLeft = akimbo && ammo != 100 ? AkimboLayout.handAmmo(ammo, hand) : ammo;
    }

    private static int camoOf(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        return tag != null && tag.hasKey(GunNbt.KEY_CAMO, Constants.NBT.TAG_ANY_NUMERIC) ? tag.getByte(GunNbt.KEY_CAMO)
            : 0;
    }
}
