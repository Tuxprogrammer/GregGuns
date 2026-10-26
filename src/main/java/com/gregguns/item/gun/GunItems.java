package com.gregguns.item.gun;

import java.util.LinkedHashMap;
import java.util.Map;

import com.gregguns.core.DevEnvironment;
import com.gregguns.core.registry.ModuleRegistrar;
import com.gregguns.core.registry.Naming;
import com.gregguns.gun.def.BioGunDef;
import com.gregguns.gun.def.BoltActionDef;
import com.gregguns.gun.def.GunDefinition;
import com.gregguns.gun.def.GunDefinitions;
import com.gregguns.gun.def.HandCannonDef;
import com.gregguns.gun.def.PistolDef;
import com.gregguns.gun.def.RevolverDef;
import com.gregguns.gun.def.SawedOffDef;
import com.gregguns.gun.def.ToolProbeDef;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.item.ammo.AmmoTypes;
import com.gregguns.item.ammo.MagazineType;

/**
 * Registers the gun items of {@link GunDefinitions#ALL} and holds them for other code (recipes, the renderer, the
 * HUD). A new gun needs nothing here: look it up with {@link #get(String)}. The named accessors below are
 * conveniences for the first five guns. Registry names are what the pack config lists for Backhand (see
 * {@code docs/gregguns-spec/PACK-CHANGES.md}).
 *
 * <p>
 * Usage: {@code ItemStack pistol = new ItemStack(GunItems.pistol());} (a tag-less stack reads as a full gun).
 *
 * <p>
 * Spec: docs/techguns-spec/02-guns/sheets-handguns-smgs.md. Common side, preInit.
 */
public final class GunItems {

    private static final Map<String, ItemGun> ITEMS = new LinkedHashMap<>();

    private GunItems() {}

    /**
     * Registers every gun in {@link GunDefinitions#ALL}. Call once from {@code GunsModule.preInit}, after the ammo
     * items.
     *
     * @param registrar the guns module's registrar
     */
    public static void register(ModuleRegistrar registrar) {
        for (GunDefinition def : GunDefinitions.ALL) {
            registerOne(registrar, def.name());
        }
        registerProbe(registrar);
    }

    /**
     * Registers one gun by name, so the caller picks the registration order (see {@code GunsItemOrder}).
     *
     * @param registrar the guns module's registrar
     * @param name      a gun's registry name from {@link GunDefinitions#ALL}
     */
    public static void registerOne(ModuleRegistrar registrar, String name) {
        ITEMS.put(name, registrar.registerItem(name, create(GunDefinitions.byName(name))));
    }

    /**
     * Registers the dev-only scenario probe; call after every content item.
     *
     * @param registrar the guns module's registrar
     */
    public static void registerProbe(ModuleRegistrar registrar) {
        if (DevEnvironment.isActive()) {
            // The I11 scenario probe: not a content item, so no icon of its own (it borrows the pistol's).
            ItemGun probe = registrar.registerItem(ToolProbeDef.NAME, new ProbeToolGun());
            probe.setTextureName(Naming.textureName(PistolDef.NAME));
            ITEMS.put(ToolProbeDef.NAME, probe);
        }
    }

    private static ItemGun create(GunDefinition def) {
        if (def.stats()
            .isEnergy()) {
            return new ItemEnergyGun(def);
        }
        if (def.tool() != null) {
            return def.stats()
                .isCharge() ? new ItemPowerHammer(def) : new ItemToolGun(def);
        }
        if (BioGunDef.NAME.equals(def.name())) {
            return new ItemBioGun(def);
        }
        return new ItemGun(def);
    }

    /**
     * @param name a gun's registry name without the mod id, for example {@code "bolt_action_rifle"}
     * @return the registered gun; {@code null} before registration, or for an unknown name
     */
    public static ItemGun get(String name) {
        return ITEMS.get(name);
    }

    /**
     * @return the qualified registry names ({@code gregguns:revolver}) of every registered gun, the dev scenario probe
     *         excluded; what Backhand's item lists need
     */
    public static java.util.List<String> qualifiedNames() {
        java.util.List<String> out = new java.util.ArrayList<>();
        for (Map.Entry<String, ItemGun> e : ITEMS.entrySet()) {
            if (!(e.getValue() instanceof ProbeToolGun)) {
                out.add(Naming.qualifiedName(e.getKey()));
            }
        }
        return out;
    }

    /** @return the 9mm Pistol; {@code null} before registration */
    public static ItemGun pistol() {
        return get(PistolDef.NAME);
    }

    /** @return the Sawed Off; {@code null} before registration */
    public static ItemGun sawedOff() {
        return get(SawedOffDef.NAME);
    }

    /** @return the Revolver; {@code null} before registration */
    public static ItemGun revolver() {
        return get(RevolverDef.NAME);
    }

    /** @return the Hand Cannon; {@code null} before registration */
    public static ItemGun handcannon() {
        return get(HandCannonDef.NAME);
    }

    /** @return the Bolt Action Rifle; {@code null} before registration */
    public static ItemGun boltAction() {
        return get(BoltActionDef.NAME);
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
                return Math.max(1, stats.roundsPerItem() / magazine.roundsPerMagazine());
            }
        }
        return 1;
    }
}
