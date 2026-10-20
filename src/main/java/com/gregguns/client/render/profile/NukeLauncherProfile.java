package com.gregguns.client.render.profile;

/**
 * The Nuke Launcher's model: the Rocket Launcher's geometry and transforms with a different skin.
 *
 * <p>
 * Originality: built clean-room from {@code docs/techguns-spec/03-gun-visuals-audio/model-notes/nuke_launcher.md},
 * which asks for the existing launcher layout with a new palette only: olive and brown 45 degree hazard stripes on
 * the tube, warm brown-black hardware, and a yellow rocket with orange-red bands and dark-gray seams. The sheet is
 * painted by {@code tools/asset-gen/gun_paint.py paint nuke_launcher} from {@code gun-paint/nuke_launcher.json}, on the
 * UV layout copied from {@code rocket_launcher.layout.json}. It is listed in {@link ClientGunDefinitions}, and shares
 * the Rocket Launcher's
 * fitted in-hand transforms (the notes give both guns the same targets, which {@link InHandTargets} holds).
 *
 * <p>
 * Spec: model-notes/nuke_launcher.md sections 1 and 3.
 */
public final class NukeLauncherProfile {

    /** Texture resource paths of the model sheet. The Nuke Launcher has no camo skins. */
    public static final String[] TEXTURES = { "gregguns:textures/models/guns/nuke_launcher.png" };
    /** The default texture, which the tests read. */
    public static final String TEXTURE = TEXTURES[0];

    /** The Rocket Launcher's boxes, muzzle and transforms, with the nuke skin. */
    public static final ClientGunDefinition DEFINITION = ClientGunDefinition
        .variantOf("nuke_launcher", RocketLauncherProfile.DEFINITION, TEXTURES);

    private NukeLauncherProfile() {}
}
