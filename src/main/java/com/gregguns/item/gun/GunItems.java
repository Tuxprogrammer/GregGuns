package com.gregguns.item.gun;

import com.gregguns.api.Tier;
import com.gregguns.combat.DamageType;
import com.gregguns.core.registry.ModuleRegistrar;
import com.gregguns.gun.logic.GunCatalog;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.sound.GunSoundEvents;
import com.gregguns.item.ammo.AmmoTypes;
import com.gregguns.item.ammo.MagazineType;

/**
 * Registers the gun items and holds them for other code (recipes, the renderer, the HUD).
 *
 * <p>
 * Registered for M1: the 9mm Pistol, {@code gregguns:pistol_9mm}. Its recipe is the Assembler row in
 * {@code GunTable}, registered by {@code GunRecipes}. Registry names are what the pack config lists for Backhand (see
 * {@code docs/gregguns-spec/PACK-CHANGES.md}).
 *
 * <p>
 * Usage: {@code ItemStack pistol = new ItemStack(GunItems.pistol());} (a tag-less stack reads as a full gun).
 *
 * <p>
 * Spec: docs/techguns-spec/02-guns/sheets-handguns-smgs.md (9mm Pistol). Common side, preInit.
 */
public final class GunItems {

    private static ItemGun pistol;

    private GunItems() {}

    /**
     * Registers every gun. Call once from {@code GunsModule.preInit}, after the ammo items.
     *
     * @param registrar the guns module's registrar
     */
    public static void register(ModuleRegistrar registrar) {
        pistol = registrar.registerItem(
            GunSoundEvents.PISTOL_9MM,
            new ItemGun(GunSoundEvents.PISTOL_9MM, GunCatalog.PISTOL, Tier.LV, DamageType.PROJECTILE));
    }

    /** @return the 9mm Pistol; {@code null} before registration or when the guns module is off */
    public static ItemGun pistol() {
        return pistol;
    }

    /**
     * How many rounds one loose round item of the gun's ammo is worth: the magazine size divided by the loose rounds
     * a full magazine is made of (6 for the 9mm bullet: 18 rounds from 3 bullets). A gun whose ammo is not a known
     * magazine counts one round per item.
     *
     * @param stats the gun
     * @return rounds per loose item, at least 1
     */
    public static int roundsPerLooseItem(GunStats stats) {
        for (MagazineType magazine : AmmoTypes.allMagazines()
            .values()) {
            if (magazine.loadedItemName()
                .equals(stats.ammoType())) {
                return Math.max(1, stats.magazineSize() / magazine.roundsPerMagazine());
            }
        }
        return 1;
    }
}
