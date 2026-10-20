package com.gregguns.entity.npc.loot;

import com.gregguns.api.Tier;

/**
 * The Infernal Mobs drop bands for a GregGuns NPC, as pure functions. The band follows the modifier count the way
 * Infernal Mobs picks its own list: up to 5 elite, up to 10 ultra, more infernal. One roll per 5 modifiers. The tier
 * ceiling per band is design 0007: elite LV, ultra MV, infernal MV with a rare HV roll. The soft-dependency handler
 * (N9) calls these; nothing here touches Minecraft or Infernal Mobs.
 */
// Spec: docs/design/0007-pack-loot-and-infernal-mobs.md (decision 2),
// docs/gregguns-spec/10-entities-npcs/infernal-mobs.md
// (GGS-10-10 to GGS-10-12)
public enum InfernalBands {

    /** Up to 5 modifiers. */
    ELITE(Tier.LV, Tier.LV),
    /** 6 to 10 modifiers. */
    ULTRA(Tier.MV, Tier.MV),
    /** More than 10 modifiers. */
    INFERNAL(Tier.MV, Tier.HV);

    /** Modifiers per roll. */
    public static final int MODIFIERS_PER_ROLL = 5;

    private final Tier common;
    private final Tier rare;

    InfernalBands(Tier common, Tier rare) {
        this.common = common;
        this.rare = rare;
    }

    /**
     * @param modifierSize the mob's modifier count
     * @return its band
     */
    public static InfernalBands forModifiers(int modifierSize) {
        if (modifierSize <= 5) {
            return ELITE;
        }
        return modifierSize <= 10 ? ULTRA : INFERNAL;
    }

    /**
     * @param modifierSize the mob's modifier count
     * @return how many loot rolls it earns, at least 1 for any infernal mob
     */
    public static int rolls(int modifierSize) {
        return Math.max(1, modifierSize / MODIFIERS_PER_ROLL);
    }

    /** @return the tier ceiling of an ordinary roll in this band */
    public Tier commonCeiling() {
        return common;
    }

    /** @return the tier ceiling of this band's rare roll (HV only for infernal) */
    public Tier rareCeiling() {
        return rare;
    }

    /**
     * @param npcCeiling the NPC's own tier ceiling
     * @param rareRoll   true for the band's rare roll
     * @return the lower of the NPC's ceiling and the band's, so a Steam-tier zombie never drops MV loot
     */
    public Tier ceilingFor(Tier npcCeiling, boolean rareRoll) {
        Tier band = rareRoll ? rare : common;
        return npcCeiling.ordinal() < band.ordinal() ? npcCeiling : band;
    }
}
