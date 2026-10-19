package com.gregguns.item.ammo;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.gregguns.item.ammo.def.AmmoDefinition;
import com.gregguns.item.ammo.def.AmmoDefinitions;
import com.gregguns.item.ammo.def.NineMmAmmo;
import com.gregguns.item.ammo.def.RifleAmmo;
import com.gregguns.item.ammo.def.ShotgunShellAmmo;
import com.gregguns.item.ammo.def.StoneAmmo;

/**
 * The index of ammo and magazine types, built from {@link AmmoDefinitions#ALL}. A new caliber is one definition there;
 * nothing is added here. The constants below are aliases for the types that existing code names directly.
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
    public static final AmmoType NINE_MM = NineMmAmmo.TYPE;

    /** Shotgun shell. LV (0001). Loose, loaded into the Sawed Off one shell per round. */
    public static final AmmoType SHOTGUN_SHELL = ShotgunShellAmmo.TYPE;

    /**
     * Stone bullet: the Hand Cannon's round, loaded one at a time. Steam/ULV (0001); made by hand or in the Steam Ammo
     * Press (RC-27, RC-27b).
     */
    public static final AmmoType STONE = StoneAmmo.TYPE;

    /**
     * Rifle bullet. LV (0001, RC-30). The Bolt Action Rifle takes it straight; later rifle magazines are filled with
     * it.
     */
    public static final AmmoType RIFLE = RifleAmmo.TYPE;

    /** 9mm Pistol Magazine: 3 rounds per refill (ammo.md magazine table). */
    public static final MagazineType NINE_MM_PISTOL = NineMmAmmo.PISTOL_MAGAZINE;

    private static final Map<String, AmmoType> AMMO = new LinkedHashMap<>();
    private static final Map<String, MagazineType> MAGAZINES = new LinkedHashMap<>();

    static {
        for (AmmoDefinition def : AmmoDefinitions.ALL) {
            AMMO.put(
                def.type()
                    .id(),
                def.type());
            for (MagazineType magazine : def.magazines()) {
                MAGAZINES.put(magazine.id(), magazine);
            }
        }
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
