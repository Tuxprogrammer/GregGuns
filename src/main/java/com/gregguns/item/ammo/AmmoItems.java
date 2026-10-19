package com.gregguns.item.ammo;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.item.Item;

import com.gregguns.core.registry.ModuleRegistrar;
import com.gregguns.item.ammo.def.AmmoDefinition;
import com.gregguns.item.ammo.def.AmmoDefinitions;

/**
 * Registers the ammo and magazine items of {@link AmmoDefinitions#ALL} and holds them for other code (the gun item,
 * recipes). A new caliber needs no change here: look its items up with {@link #round(String)} and
 * {@link #magazine(String)}. Names match the texture files.
 *
 * <p>
 * Usage: {@code ItemStack bullets = new ItemStack(AmmoItems.nineMmBullet(), 3);}
 *
 * <p>
 * Spec: docs/gregguns-spec/06-items/ammo.md. Common side, preInit.
 */
public final class AmmoItems {

    private static final Map<String, Item> ITEMS = new LinkedHashMap<>();

    private AmmoItems() {}

    /**
     * Registers every ammo item. Call once from {@code GunsModule.preInit}.
     *
     * @param registrar the guns module's registrar
     */
    public static void register(ModuleRegistrar registrar) {
        for (AmmoDefinition def : AmmoDefinitions.ALL) {
            AmmoType type = def.type();
            ItemAmmoRound round = registrar.registerItem(type.roundItemName(), new ItemAmmoRound(type));
            ITEMS.put(type.roundItemName(), round);
            for (MagazineType magazine : def.magazines()) {
                ItemMagazineEmpty empty = registrar
                    .registerItem(magazine.emptyItemName(), new ItemMagazineEmpty(magazine));
                ITEMS.put(magazine.emptyItemName(), empty);
                ITEMS.put(
                    magazine.loadedItemName(),
                    registrar.registerItem(magazine.loadedItemName(), new ItemMagazine(magazine, empty, round)));
            }
        }
    }

    /**
     * @param name a loose round's registry name, for example {@code "ammo_9mm_bullet"}
     * @return the registered round; {@code null} before registration, when the guns module is off, or for another name
     */
    public static ItemAmmoRound round(String name) {
        Item item = ITEMS.get(name);
        return item instanceof ItemAmmoRound ? (ItemAmmoRound) item : null;
    }

    /**
     * @param name a loaded magazine's registry name
     * @return the registered loaded magazine, or {@code null}
     */
    public static ItemMagazine magazine(String name) {
        Item item = ITEMS.get(name);
        return item instanceof ItemMagazine ? (ItemMagazine) item : null;
    }

    /**
     * @param name an empty magazine's registry name
     * @return the registered empty magazine, or {@code null}
     */
    public static ItemMagazineEmpty emptyMagazine(String name) {
        Item item = ITEMS.get(name);
        return item instanceof ItemMagazineEmpty ? (ItemMagazineEmpty) item : null;
    }

    /** @return the 9mm bullet; {@code null} before registration or when the guns module is off */
    public static ItemAmmoRound nineMmBullet() {
        return round(AmmoTypes.NINE_MM.roundItemName());
    }

    /** @return the shotgun shell ({@code ammo_shotgun_shell}); {@code null} before registration */
    public static ItemAmmoRound shotgunShell() {
        return round(AmmoTypes.SHOTGUN_SHELL.roundItemName());
    }

    /** @return the Hand Cannon's stone bullet; {@code null} before registration or when the guns module is off */
    public static ItemAmmoRound stoneBullet() {
        return round(AmmoTypes.STONE.roundItemName());
    }

    /** @return the rifle bullet; {@code null} before registration or when the guns module is off */
    public static ItemAmmoRound rifleBullet() {
        return round(AmmoTypes.RIFLE.roundItemName());
    }

    /** @return the loaded 9mm pistol magazine; {@code null} before registration */
    public static ItemMagazine nineMmPistolMagazine() {
        return magazine(AmmoTypes.NINE_MM_PISTOL.loadedItemName());
    }

    /** @return the empty 9mm pistol magazine; {@code null} before registration */
    public static ItemMagazineEmpty nineMmPistolMagazineEmpty() {
        return emptyMagazine(AmmoTypes.NINE_MM_PISTOL.emptyItemName());
    }
}
