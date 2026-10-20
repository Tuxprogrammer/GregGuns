package com.gregguns.utility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.gregguns.api.Tier;

/**
 * The one registration order of the utility module's items and blocks. Item ids follow registration order in 1.7.10,
 * and NEI's default sort and the creative tab follow the ids, so this is what a player scrolls through: the turret
 * armor plates from the cheapest tier up, then the Explosive Charge, then storage (Reinforced Chest, Weapon Chest),
 * then the Camp Flag and the Bio Blob. The plates come from {@link TurretPlate}, sorted by tier, so a new plate slots
 * itself in. FML remaps ids by registry name, so reordering never breaks an existing world. Pure data.
 */
// Spec: docs/gregguns-spec/09-machines-utility/index.md (registration order); owner request 2026-10-20
public final class UtilityItemOrder {

    /** What a slot registers. */
    public enum Kind {
        /** A turret armor plate item ({@code UtilityParts}). */
        PLATE,
        /** The Explosive Charge block ({@code ChargeRegistration}). */
        CHARGE,
        /** A world block ({@code WorldBlockRegistration}). */
        WORLD_BLOCK
    }

    /** One item or block to register. */
    public static final class Slot {

        private final String name;
        private final Kind kind;
        private final Tier tier;
        private final TurretPlate plate;

        Slot(String name, Kind kind, Tier tier, TurretPlate plate) {
            this.name = name;
            this.kind = kind;
            this.tier = tier;
            this.plate = plate;
        }

        /** @return the bare registry name */
        public String name() {
            return name;
        }

        /** @return what it is */
        public Kind kind() {
            return kind;
        }

        /** @return the tier of a plate; {@code null} for blocks, which have no single tier */
        public Tier tier() {
            return tier;
        }

        /** @return the plate of a {@link Kind#PLATE} slot; otherwise {@code null} */
        public TurretPlate plate() {
            return plate;
        }
    }

    private UtilityItemOrder() {}

    /** @return everything the utility module registers as items or blocks, in registration order */
    public static List<Slot> build() {
        List<Slot> all = new ArrayList<>();
        for (Tier tier : Tier.values()) {
            for (TurretPlate plate : TurretPlate.values()) {
                if (plate.tier() == tier) {
                    all.add(new Slot(plate.registryName(), Kind.PLATE, tier, plate));
                }
            }
        }
        all.add(new Slot(com.gregguns.utility.charge.BlockExplosiveCharge.NAME, Kind.CHARGE, null, null));
        all.add(new Slot(WorldBlockRegistration.REINFORCED_CHEST, Kind.WORLD_BLOCK, null, null));
        all.add(new Slot(WorldBlockRegistration.WEAPON_CHEST, Kind.WORLD_BLOCK, null, null));
        all.add(new Slot(WorldBlockRegistration.CAMP_FLAG, Kind.WORLD_BLOCK, null, null));
        all.add(new Slot(WorldBlockRegistration.BIO_BLOB, Kind.WORLD_BLOCK, null, null));
        return Collections.unmodifiableList(all);
    }

    /**
     * Registers everything in order. Call once from {@code UtilityModule.preInit}.
     *
     * @param registrar the utility module's registrar
     */
    public static void registerAll(com.gregguns.core.registry.ModuleRegistrar registrar) {
        for (Slot slot : build()) {
            switch (slot.kind()) {
                case PLATE:
                    UtilityParts.registerOne(registrar, slot.plate());
                    break;
                case CHARGE:
                    ChargeRegistration.registerBlock(registrar);
                    break;
                case WORLD_BLOCK:
                    WorldBlockRegistration.registerOne(registrar, slot.name());
                    break;
                default:
                    throw new IllegalStateException("unhandled kind " + slot.kind());
            }
        }
        WorldBlockRegistration.registerTileEntities(registrar);
    }
}
