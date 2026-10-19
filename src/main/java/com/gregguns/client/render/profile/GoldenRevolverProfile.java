package com.gregguns.client.render.profile;

/**
 * The Golden Revolver's client definition: the Revolver's clean-room model, boxes and transforms, wearing the
 * {@code golden_revolver} texture. That texture is painted by the same pipeline as the Revolver's
 * ({@code tools/asset-gen/gun_paint.py paint} writes {@code revolver.png} and {@code golden_revolver.png} from one
 * layout), so the two always agree on the UV layout. The Akimbo Golden Revolvers draw this definition twice.
 *
 * <p>
 * Deviation: Techguns gives the golden gun its own 24-box model (the Revolver's 20 plus gold fittings). Until a
 * clean-room variant model with extra fittings is built, the gold is carried by the texture alone; the variant model is
 * a polish item, not a gameplay gate.
 */
// Spec: docs/techguns-spec/03-gun-visuals-audio/gun-sheets.md "Revolver and Golden Revolver"
public final class GoldenRevolverProfile {

    /** Texture resource path of the model sheet (64x32, the Revolver's layout). */
    public static final String TEXTURE = "gregguns:textures/models/guns/golden_revolver.png";

    /** The client half of the Golden Revolver's definition, listed in {@link ClientGunDefinitions}. */
    public static final ClientGunDefinition DEFINITION = ClientGunDefinition
        .variantOf("golden_revolver", RevolverProfile.DEFINITION, TEXTURE);

    private GoldenRevolverProfile() {}
}
