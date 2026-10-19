package com.gregguns.client.accessory;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.world.WorldEvent;

import org.lwjgl.opengl.GL11;

import com.gregguns.armor.flight.Flight;
import com.gregguns.armor.flight.logic.BackKind;
import com.gregguns.armor.flight.logic.FlightStatus;
import com.gregguns.armor.slots.BaublesCompat;
import com.gregguns.armor.slots.logic.BaubleSlot;
import com.gregguns.client.accessory.AccessoryLooks.Look;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Draws the worn models of the accessories that live in Baubles slots: the back devices (the {@code wings} slot) on
 * the torso and the head items (the {@code head} slot: Night Vision Goggles and the Oxygen Mask) on the head. The
 * helmet-slot masks do not come through here; they are armor models ({@link HelmetModels}).
 *
 * <p>
 * Hooked to {@code RenderPlayerEvent.Specials.Post}, inside the player's own model transform, and anchored with
 * {@code ModelRenderer.postRender} of the body part, so the model follows the body's turn and lean and the sneak pose
 * exactly like the armor does. The Baubles inventories of other players are synced by Baubles Expanded, so their
 * accessories show too. Client only.
 */
// Spec: docs/gregguns-spec/05-armor/baubles-slots.md (worn models); chunk H of the model review
// Deviation: Baubles Expanded's own render hook (IRenderBauble) is not used. It would need every accessory to
// implement a Baubles interface and would not cover Galacticraft's slots; this one reads the typed slots through
// BaublesCompat.
// Source: RenderPlayer.renderSpecials fires the event inside the player model transform (Forge 1.7.10):
// https://github.com/MinecraftForge/MinecraftForge/blob/1.7.10/patches/minecraft/net/minecraft/client/renderer/entity/RenderPlayer.java.patch
@SideOnly(Side.CLIENT)
public final class AccessoryRenderEvents {

    private static final float PX = 1.0f / 16.0f;
    // Chest armor expands the torso by one texel, so a back device sits one texel further out when it is worn.
    private static final float BACK_GAP = 2.0f;
    private static final float BACK_GAP_ARMORED = 3.0f;
    // A helmet expands the head box by one texel on every side; head items grow by a tenth to clear it.
    private static final float HELMET_GROW = 1.12f;
    private static final float HEAD_CENTER_Y = -4.0f;
    private static final float FOLD_PER_TICK = 0.2f;
    private static final double FALLING_DY = -0.05;

    private final Map<String, AccessoryModel> models = new HashMap<>();
    private final Map<Integer, Fold> folds = new HashMap<>();

    /** The fold state of one glider wearer (0 deployed, 1 stowed). */
    private static final class Fold {

        float value = 1.0f;
        int tick = Integer.MIN_VALUE;
    }

    @SubscribeEvent
    public void onSpecialsPost(RenderPlayerEvent.Specials.Post event) {
        EntityPlayer player = event.entityPlayer;
        if (player.isInvisible() || !BaublesCompat.isLoaded()) {
            return;
        }
        ModelBiped biped = event.renderer.modelBipedMain;
        Look back = AccessoryLooks.of(BaublesCompat.first(player, BaubleSlot.WINGS));
        if (back != null && back.def()
            .mount() == AccessoryDef.Mount.BACK) {
            drawBack(player, biped, back);
        }
        Look head = AccessoryLooks.of(BaublesCompat.first(player, BaubleSlot.HEAD));
        if (head != null && head.def()
            .mount() == AccessoryDef.Mount.HEAD) {
            drawHead(player, biped, head);
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.world.isRemote) {
            folds.clear();
        }
    }

    private AccessoryModel model(AccessoryDef def) {
        return models.computeIfAbsent(def.id(), id -> new AccessoryModel(def));
    }

    private void drawBack(EntityPlayer player, ModelBiped biped, Look look) {
        AccessoryModel model = model(look.def());
        model.setFold(look.def() == AccessoryDefs.GLIDER ? fold(player) : 0.0f);
        GL11.glPushMatrix();
        biped.bipedBody.postRender(PX);
        float gap = player.getCurrentArmor(2) != null ? BACK_GAP_ARMORED : BACK_GAP;
        GL11.glTranslatef(0.0f, 0.0f, gap * PX);
        draw(model, look);
        GL11.glPopMatrix();
    }

    private void drawHead(EntityPlayer player, ModelBiped biped, Look look) {
        AccessoryModel model = model(look.def());
        GL11.glPushMatrix();
        biped.bipedHead.postRender(PX);
        if (player.getCurrentArmor(3) != null) {
            GL11.glTranslatef(0.0f, HEAD_CENTER_Y * PX, 0.0f);
            GL11.glScalef(HELMET_GROW, HELMET_GROW, HELMET_GROW);
            GL11.glTranslatef(0.0f, -HEAD_CENTER_Y * PX, 0.0f);
        }
        draw(model, look);
        GL11.glPopMatrix();
    }

    private static void draw(AccessoryModel model, Look look) {
        Minecraft.getMinecraft().renderEngine.bindTexture(model.texture(look.texture()));
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        model.render();
    }

    /**
     * Eases the glider's wings toward deployed while the wearer glides and toward stowed otherwise.
     *
     * @return 0 deployed, 1 stowed
     */
    private float fold(EntityPlayer player) {
        Fold fold = folds.computeIfAbsent(player.getEntityId(), id -> new Fold());
        if (fold.tick != player.ticksExisted) {
            fold.tick = player.ticksExisted;
            fold.value += deployed(player) ? -FOLD_PER_TICK : FOLD_PER_TICK;
            fold.value = Math.max(0.0f, Math.min(1.0f, fold.value));
        }
        return fold.value;
    }

    /**
     * Whether the glider's wings should be out: the local player's own status says so while the key is held, and every
     * wearer's wings open while they fall through the air (other clients only see the motion, never the key).
     */
    private static boolean deployed(EntityPlayer player) {
        FlightStatus status = Flight.mirror()
            .status(player);
        if (status.kind() == BackKind.GLIDER && status.engaged()) {
            return true;
        }
        return !player.onGround && !player.isInWater()
            && !player.capabilities.isFlying
            && player.posY - player.prevPosY < FALLING_DY;
    }
}
