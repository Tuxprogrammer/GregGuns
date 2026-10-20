package com.gregguns.item;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gregguns.api.Tier;
import com.gregguns.gun.def.GunDefinition;
import com.gregguns.gun.def.GunDefinitions;
import com.gregguns.item.ammo.AmmoType;
import com.gregguns.item.ammo.AmmoTypes;
import com.gregguns.item.ammo.MagazineType;
import com.gregguns.item.ammo.def.AmmoDefinition;
import com.gregguns.item.ammo.def.AmmoDefinitions;
import com.gregguns.item.part.PartCatalog;

/**
 * The one registration order of the guns module's items, derived by sorting the definitions. Item ids follow
 * registration order in 1.7.10, and NEI's default sort and the creative tab follow the ids, so this order is what a
 * player scrolls through: tier by tier (Steam to ZPM), and inside a tier each gun, then its akimbo variant, then the
 * magazines it uses (empty, then full), then the loose ammo it uses; then the tier's melee tools, grenades and gun
 * components. A magazine or round shared by several guns appears once, at the first (lowest-tier, earliest) gun that
 * uses it. Ammo no gun uses lands at the end of its own tier's gun block.
 *
 * <p>
 * Nothing here is a hand-kept list of guns: a new gun, ammo or component definition slots itself in. FML remaps ids
 * by registry name, so reordering never breaks an existing world. Pure data, no Minecraft types, both sides.
 */
// Spec: docs/gregguns-spec/06-items/index.md (registration order); owner request 2026-10-19 (NEI id order)
public final class GunsItemOrder {

    /** What a slot registers, which decides the class that builds the item. */
    public enum Kind {
        /** A gun item ({@code GunItems}). */
        GUN,
        /** An empty magazine ({@code AmmoItems}). */
        MAGAZINE_EMPTY,
        /** A loaded magazine ({@code AmmoItems}). */
        MAGAZINE,
        /** A loose round ({@code AmmoItems}). */
        AMMO,
        /** A melee tool ({@code MeleeItems}). */
        MELEE,
        /** A grenade ({@code GrenadeItems}). */
        GRENADE,
        /** A part from {@code PartItems}. */
        PART,
        /** A component from {@code ComponentItems}. */
        COMPONENT
    }

    /** One item to register. */
    public static final class Slot {

        private final String name;
        private final Tier tier;
        private final Kind kind;
        private final String gun;

        Slot(String name, Tier tier, Kind kind, String gun) {
            this.name = name;
            this.tier = tier;
            this.kind = kind;
            this.gun = gun;
        }

        /** @return the bare registry name */
        public String name() {
            return name;
        }

        /** @return the tier group the item sits in */
        public Tier tier() {
            return tier;
        }

        /** @return what kind of item it is */
        public Kind kind() {
            return kind;
        }

        /** @return for a magazine or round, the gun that first uses it; otherwise {@code null} */
        public String gun() {
            return gun;
        }
    }

    private static final List<String> MELEE_LV = Collections.unmodifiableList(Arrays.asList("combat_knife", "crowbar"));

    private GunsItemOrder() {}

    /**
     * @return every item the guns module registers except the dev probe, in registration order; the tier never
     *         decreases down the list
     */
    public static List<Slot> build() {
        Map<Tier, List<Slot>> byTier = new EnumMap<>(Tier.class);
        for (Tier tier : Tier.values()) {
            byTier.put(tier, new ArrayList<Slot>());
        }
        Set<String> emitted = new HashSet<>();

        List<GunDefinition> guns = new ArrayList<>(GunDefinitions.ALL);
        Set<GunDefinition> done = new HashSet<>();
        for (GunDefinition gun : guns) {
            if (gun.singleGun() != null || !done.add(gun)) {
                continue;
            }
            List<GunDefinition> family = new ArrayList<>();
            family.add(gun);
            for (GunDefinition other : guns) {
                if (gun.name()
                    .equals(other.singleGun()) && other.tier() == gun.tier()
                    && done.add(other)) {
                    family.add(other);
                }
            }
            addFamily(byTier, emitted, family);
        }
        // Akimbo variants whose single gun sits in another tier stand on their own.
        for (GunDefinition gun : guns) {
            if (done.add(gun)) {
                addFamily(byTier, emitted, Collections.singletonList(gun));
            }
        }
        // Ammo no gun uses.
        for (AmmoDefinition def : AmmoDefinitions.ALL) {
            for (MagazineType mag : def.magazines()) {
                addMagazine(byTier, emitted, mag, null, mag.tier());
            }
            addRound(
                byTier,
                emitted,
                def.type(),
                null,
                def.type()
                    .tier());
        }
        for (String name : MELEE_LV) {
            byTier.get(Tier.LV)
                .add(new Slot(name, Tier.LV, Kind.MELEE, null));
        }
        byTier.get(Tier.LV)
            .add(new Slot("stielgranate", Tier.LV, Kind.GRENADE, null));
        byTier.get(Tier.MV)
            .add(new Slot("frag_grenade", Tier.MV, Kind.GRENADE, null));
        byTier.get(Tier.MV)
            .add(new Slot("flashbang", Tier.MV, Kind.GRENADE, null));
        for (PartCatalog.Entry entry : PartCatalog.entries()) {
            if (entry.owner() == PartCatalog.Owner.PART_ITEMS) {
                byTier.get(entry.tier())
                    .add(new Slot(entry.name(), entry.tier(), Kind.PART, null));
            }
        }
        for (PartCatalog.Entry entry : PartCatalog.entries()) {
            if (entry.owner() == PartCatalog.Owner.COMPONENT_ITEMS) {
                byTier.get(entry.tier())
                    .add(new Slot(entry.name(), entry.tier(), Kind.COMPONENT, null));
            }
        }
        List<Slot> all = new ArrayList<>();
        for (Tier tier : Tier.values()) {
            all.addAll(byTier.get(tier));
        }
        return Collections.unmodifiableList(all);
    }

    private static void addFamily(Map<Tier, List<Slot>> byTier, Set<String> emitted, List<GunDefinition> family) {
        Tier tier = family.get(0)
            .tier();
        List<Slot> slots = byTier.get(tier);
        for (GunDefinition gun : family) {
            slots.add(new Slot(gun.name(), tier, Kind.GUN, null));
        }
        String first = family.get(0)
            .name();
        List<String> ids = new ArrayList<>();
        for (GunDefinition gun : family) {
            ids.add(
                gun.stats()
                    .ammoType());
            ids.add(
                gun.stats()
                    .looseAmmoType());
        }
        for (String id : ids) {
            MagazineType mag = AmmoTypes.magazine(id);
            if (mag != null) {
                addMagazine(byTier, emitted, mag, first, tier);
            }
        }
        for (String id : ids) {
            AmmoType ammo = AmmoTypes.ammo(id);
            if (ammo != null) {
                addRound(byTier, emitted, ammo, first, tier);
            }
        }
        for (String id : ids) {
            MagazineType mag = AmmoTypes.magazine(id);
            if (mag != null) {
                addRound(byTier, emitted, mag.ammo(), first, tier);
            }
        }
    }

    private static void addMagazine(Map<Tier, List<Slot>> byTier, Set<String> emitted, MagazineType mag, String gun,
        Tier tier) {
        if (emitted.add(mag.emptyItemName())) {
            byTier.get(tier)
                .add(new Slot(mag.emptyItemName(), tier, Kind.MAGAZINE_EMPTY, gun));
        }
        if (emitted.add(mag.loadedItemName())) {
            byTier.get(tier)
                .add(new Slot(mag.loadedItemName(), tier, Kind.MAGAZINE, gun));
        }
    }

    private static void addRound(Map<Tier, List<Slot>> byTier, Set<String> emitted, AmmoType ammo, String gun,
        Tier tier) {
        if (emitted.add(ammo.roundItemName())) {
            byTier.get(tier)
                .add(new Slot(ammo.roundItemName(), tier, Kind.AMMO, gun));
        }
    }
}
