package com.gregguns.item.ammo;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.gregguns.api.Tier;

/**
 * The table of ammo and magazine types. Adding a caliber or magazine for a later milestone is one new row here plus
 * its items in {@link AmmoItems}.
 *
 * <p>
 * Ids: ammo {@code "9mm"}; magazine {@code "9mm_pistol"}. A gun takes either a loose round (revolver: {@code "9mm"})
 * or a magazine ({@code "9mm_pistol"}).
 *
 * <p>
 * Spec: docs/techguns-spec/06-items/ammo.md, docs/design/0001-tier-map.md (9mm is LV).
 */
public final class AmmoTypes {

    /** 9mm loose round. LV (0001). Shared by the pistol magazine, revolvers and later the SMG magazine. */
    public static final AmmoType NINE_MM = new AmmoType("9mm", "ammo_9mm_bullet", Tier.LV);

    /** 9mm Pistol Magazine: 3 rounds per refill (ammo.md magazine table). */
    public static final MagazineType NINE_MM_PISTOL = new MagazineType(
        "9mm_pistol",
        NINE_MM,
        "mag_9mm_pistol",
        "mag_9mm_pistol_empty",
        3,
        Tier.LV);

    private static final Map<String, AmmoType> AMMO = new LinkedHashMap<>();
    private static final Map<String, MagazineType> MAGAZINES = new LinkedHashMap<>();

    static {
        AMMO.put(NINE_MM.id(), NINE_MM);
        MAGAZINES.put(NINE_MM_PISTOL.id(), NINE_MM_PISTOL);
    }

    private AmmoTypes() {}

    /** @return the ammo type with this id, or {@code null} */
    public static AmmoType ammo(String id) {
        return AMMO.get(id);
    }

    /** @return the magazine type with this id, or {@code null} */
    public static MagazineType magazine(String id) {
        return MAGAZINES.get(id);
    }

    /** @return all ammo types, in registration order (read-only) */
    public static Map<String, AmmoType> allAmmo() {
        return Collections.unmodifiableMap(AMMO);
    }

    /** @return all magazine types, in registration order (read-only) */
    public static Map<String, MagazineType> allMagazines() {
        return Collections.unmodifiableMap(MAGAZINES);
    }
}
