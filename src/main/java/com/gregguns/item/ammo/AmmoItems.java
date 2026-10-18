package com.gregguns.item.ammo;

import com.gregguns.core.registry.ModuleRegistrar;

/**
 * Registers the ammo and magazine items and holds them for other code (the gun item, recipes).
 *
 * <p>
 * Registered for M1: the 9mm bullet ({@code ammo_9mm_bullet}), the loaded 9mm pistol magazine
 * ({@code mag_9mm_pistol}) and the empty one ({@code mag_9mm_pistol_empty}), plus the Hand Cannon's
 * {@code ammo_stone_bullet} (M2). Names match the texture files.
 *
 * <p>
 * Usage: {@code ItemStack bullets = new ItemStack(AmmoItems.nineMmBullet(), 3);}
 *
 * <p>
 * Spec: docs/gregguns-spec/06-items/ammo.md. Common side, preInit.
 */
public final class AmmoItems {

    private static ItemAmmoRound nineMmBullet;
    private static ItemAmmoRound stoneBullet;
    private static ItemMagazineEmpty nineMmPistolMagazineEmpty;
    private static ItemMagazine nineMmPistolMagazine;

    private AmmoItems() {}

    /**
     * Registers every ammo item. Call once from {@code GunsModule.preInit}.
     *
     * @param registrar the guns module's registrar
     */
    public static void register(ModuleRegistrar registrar) {
        AmmoType nine = AmmoTypes.NINE_MM;
        MagazineType pistol = AmmoTypes.NINE_MM_PISTOL;
        nineMmBullet = registrar.registerItem(nine.roundItemName(), new ItemAmmoRound(nine));
        AmmoType stone = AmmoTypes.STONE;
        stoneBullet = registrar.registerItem(stone.roundItemName(), new ItemAmmoRound(stone));
        nineMmPistolMagazineEmpty = registrar.registerItem(pistol.emptyItemName(), new ItemMagazineEmpty(pistol));
        nineMmPistolMagazine = registrar
            .registerItem(pistol.loadedItemName(), new ItemMagazine(pistol, nineMmPistolMagazineEmpty, nineMmBullet));
    }

    /** @return the 9mm bullet; {@code null} before registration or when the guns module is off */
    public static ItemAmmoRound nineMmBullet() {
        return nineMmBullet;
    }

    /** @return the Hand Cannon's stone bullet; {@code null} before registration or when the guns module is off */
    public static ItemAmmoRound stoneBullet() {
        return stoneBullet;
    }

    /** @return the loaded 9mm pistol magazine; {@code null} before registration */
    public static ItemMagazine nineMmPistolMagazine() {
        return nineMmPistolMagazine;
    }

    /** @return the empty 9mm pistol magazine; {@code null} before registration */
    public static ItemMagazineEmpty nineMmPistolMagazineEmpty() {
        return nineMmPistolMagazineEmpty;
    }
}
