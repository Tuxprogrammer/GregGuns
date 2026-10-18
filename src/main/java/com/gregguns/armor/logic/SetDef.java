package com.gregguns.armor.logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.gregguns.api.Tier;

/**
 * A set of armor: its material, tier, pieces, camo texture names and full-set bonus. This is the data a new set
 * (T2, T3, powered) adds; the item classes, tooltips, bonus handling and layer textures are all driven from it. Plain
 * Java, both sides.
 *
 * <p>
 * Names: piece {@code p} of set {@code s} is registered as {@code s.id() + "_" + p.slot().id()}, for example
 * {@code t1_combat_helmet}. Camo 0 uses the texture base {@code camos().get(0)}; layers are {@code <base>_1} (helmet,
 * chestplate, boots) and {@code <base>_2} (leggings), as vanilla expects.
 */
// Spec: docs/techguns-spec/05-armor/armor-catalog.md sections 2 and 5; docs/design/0001-tier-map.md
public final class SetDef {

    private final String id;
    private final Tier tier;
    private final ArmorMaterialDef material;
    private final Map<ArmorSlot, PieceDef> pieces;
    private final List<String> camos;
    private final BonusMap fullSetBonus;
    private final PowerSpec power;
    private final TextureLayout layout;

    /** How a set's worn-armor texture files are laid out. */
    public enum TextureLayout {
        /** Vanilla style: {@code <base>_1.png} for helmet, chestplate and boots, {@code <base>_2.png} for leggings. */
        DOUBLE,
        /** One sheet for every slot: {@code <base>.png} (steam armor, beret). */
        SINGLE
    }

    /**
     * @param id           set id, lower snake case, for example {@code "t1_scout"}
     * @param tier         the D1 tier
     * @param material     the material
     * @param camoTextures layer texture bases, camo 0 first; at least one
     * @param fullSetBonus extra bonuses when every piece of the set is worn and intact; {@link BonusMap#EMPTY} for none
     * @param pieces       the pieces; one per slot at most
     */
    public SetDef(String id, Tier tier, ArmorMaterialDef material, List<String> camoTextures, BonusMap fullSetBonus,
        PieceDef... pieces) {
        this(id, tier, material, camoTextures, TextureLayout.DOUBLE, PowerSpec.NONE, fullSetBonus, pieces);
    }

    /**
     * @param id           set id, lower snake case, for example {@code "steam"}
     * @param tier         the D1 tier
     * @param material     the material
     * @param camoTextures texture bases, camo 0 first; at least one
     * @param layout       how the texture files are laid out
     * @param power        how the set is powered; {@link PowerSpec#NONE} for always-on bonuses
     * @param fullSetBonus extra bonuses when every piece is worn and intact
     * @param pieces       the pieces; one per slot at most
     */
    public SetDef(String id, Tier tier, ArmorMaterialDef material, List<String> camoTextures, TextureLayout layout,
        PowerSpec power, BonusMap fullSetBonus, PieceDef... pieces) {
        if (camoTextures.isEmpty()) {
            throw new IllegalArgumentException("a set needs at least one texture base");
        }
        this.id = id;
        this.tier = tier;
        this.material = material;
        this.camos = Collections.unmodifiableList(new ArrayList<>(camoTextures));
        this.fullSetBonus = fullSetBonus;
        this.layout = layout;
        this.power = power;
        EnumMap<ArmorSlot, PieceDef> m = new EnumMap<>(ArmorSlot.class);
        for (PieceDef p : pieces) {
            if (m.put(p.slot(), p) != null) {
                throw new IllegalArgumentException("two pieces for " + p.slot() + " in " + id);
            }
        }
        this.pieces = Collections.unmodifiableMap(m);
    }

    /** @return the set id */
    public String id() {
        return id;
    }

    /** @return the tier */
    public Tier tier() {
        return tier;
    }

    /** @return the material */
    public ArmorMaterialDef material() {
        return material;
    }

    /** @return the pieces by slot, in slot order */
    public Map<ArmorSlot, PieceDef> pieces() {
        return pieces;
    }

    /** @return how many pieces make the set complete */
    public int pieceCount() {
        return pieces.size();
    }

    /** @return texture bases per camo, camo 0 first */
    public List<String> camos() {
        return camos;
    }

    /** @return the number of camos */
    public int camoCount() {
        return camos.size();
    }

    /** @return the extra bonuses of a complete, intact set */
    public BonusMap fullSetBonus() {
        return fullSetBonus;
    }

    /** @return how the set is powered; {@link PowerSpec#NONE} for most sets */
    public PowerSpec power() {
        return power;
    }

    /** @return how the texture files are laid out */
    public TextureLayout layout() {
        return layout;
    }

    /**
     * @param slot a slot the set has
     * @return the registry name of the piece, for example {@code t1_combat_helmet}, or the piece's explicit name
     */
    public String pieceName(ArmorSlot slot) {
        PieceDef p = pieces.get(slot);
        if (p != null && p.registryName() != null) {
            return p.registryName();
        }
        return id + "_" + slot.id();
    }

    /**
     * The armor layer texture path for a piece. A camo index outside the set's range falls back to camo 0 instead of
     * failing (fixes ARM-12, an unchecked index that crashed rendering).
     *
     * @param slot the slot
     * @param camo the stored camo index
     * @return a resource path such as {@code gregguns:textures/models/armor/t1_scout_forest_1.png}
     */
    public String layerTexture(ArmorSlot slot, int camo) {
        String base = camos.get(clampCamo(camo));
        if (layout == TextureLayout.SINGLE) {
            return "gregguns:textures/models/armor/" + base + ".png";
        }
        return "gregguns:textures/models/armor/" + base + (slot == ArmorSlot.LEGGINGS ? "_2" : "_1") + ".png";
    }

    /**
     * @param camo a stored camo index
     * @return {@code camo} when valid, else 0
     */
    public int clampCamo(int camo) {
        return camo >= 0 && camo < camos.size() ? camo : 0;
    }
}
