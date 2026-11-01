package com.gregguns.client.render;

/**
 * Decides which holders of a gun get the third-person aimed pre-rotation
 * ({@link com.gregguns.client.render.profile.GunTransforms#applyAimed}). Every gun's third-person rows are written for
 * that pre-rotated frame, so a gun drawn without it points the wrong way: stock up, muzzle back (framework section
 * 6.3). A player holding a gun always has it, and so does every GregGuns NPC, whose biped model carries the gun in the
 * same hand frame and whose arms follow the same {@code aimedBow} pose. The turret draws its gun in a frame of its
 * own and is not in this rule. Pure; no Minecraft types, so a unit test checks the rule the renderer applies.
 */
// Spec: docs/techguns-spec/03-gun-visuals-audio/ (framework section 6.3), docs/gregguns-spec/03-gun-visuals-audio
public final class AimedHold {

    private AimedHold() {}

    /**
     * @param player true when the holder is a player
     * @param npc    true when the holder is a GregGuns biped NPC
     * @return true when the pre-rotation applies
     */
    public static boolean applies(boolean player, boolean npc) {
        return player || npc;
    }
}
