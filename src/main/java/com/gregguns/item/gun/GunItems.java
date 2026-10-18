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
 * Registered so far: the 9mm Pistol, {@code gregguns:pistol_9mm}, the Revolver, {@code gregguns:revolver}, the
 * Sawed Off, {@code gregguns:sawed_off}, and the Hand Cannon, {@code gregguns:handcannon}. Its
 * recipe is the Assembler row in
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
    private static ItemGun revolver;
    private static ItemGun sawedOff;
    private static ItemGun handcannon;

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
        revolver = registrar.registerItem(
            GunSoundEvents.REVOLVER,
            new ItemGun(GunSoundEvents.REVOLVER, GunCatalog.REVOLVER, Tier.LV, DamageType.PROJECTILE));
        sawedOff = registrar.registerItem(
            GunSoundEvents.SAWED_OFF,
            new ItemGun(GunSoundEvents.SAWED_OFF, GunCatalog.SAWED_OFF, Tier.LV, DamageType.PROJECTILE));
        handcannon = registrar.registerItem(
            GunSoundEvents.HANDCANNON,
            new ItemGun(GunSoundEvents.HANDCANNON, GunCatalog.HANDCANNON, Tier.STEAM, DamageType.PROJECTILE));
    }

    /** @return the 9mm Pistol; {@code null} before registration or when the guns module is off */
    public static ItemGun pistol() {
        return pistol;
    }

    /** @return the Sawed Off; {@code null} before registration or when the guns module is off */
    public static ItemGun sawedOff() {
        return sawedOff;
    }

    /** @return the Revolver; {@code null} before registration or when the guns module is off */
    public static ItemGun revolver() {
        return revolver;
    }

    /** @return the Hand Cannon; {@code null} before registration or when the guns module is off */
    public static ItemGun handcannon() {
        return handcannon;
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
        if (stats.ammoType()
            .equals(stats.looseAmmoType())) {
            // The gun loads straight from its loose round item (the Revolver): one item fills the gun, so a refund
            // is worth that many rounds per item, not one.
            return Math.max(1, stats.roundsPerItem());
        }
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
