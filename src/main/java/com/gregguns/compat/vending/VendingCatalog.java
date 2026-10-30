package com.gregguns.compat.vending;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gregguns.api.Tier;
import com.gregguns.armor.ArmorItemOrder;
import com.gregguns.entity.npc.NpcEggOrder;
import com.gregguns.entity.npc.NpcMob;
import com.gregguns.entity.npc.NpcRegistry;
import com.gregguns.gun.def.GunDefinition;
import com.gregguns.gun.def.GunDefinitions;
import com.gregguns.item.GunsItemOrder;
import com.gregguns.item.coin.CoinDenomination;
import com.gregguns.utility.UtilityItemOrder;

/**
 * Everything GregGuns sells at the pack's Vending Machine, and for how many GregGuns Coins. The list is derived from
 * the
 * item order classes ({@link GunsItemOrder}, {@link ArmorItemOrder}, {@link UtilityItemOrder}) and the spawn egg roster
 * ({@link NpcEggOrder}), so a new gun, armor piece or NPC is on sale without a hand-kept list. A price is a multiple of
 * the tier's unit ({@link #unit}) by item kind, paid as one stack of one coin denomination
 * ({@link CoinDenomination#exact}); every trade waits for its tier's chapter intro quest ({@link QuestGate}).
 *
 * <p>
 * Items a module or a missing mod leaves unregistered are dropped later, when the trades are built against the live
 * registry; this class does not know which modules are on. Pure data, no Minecraft types, both sides.
 */
// Spec: docs/gregguns-spec/06-items/coins-and-vending.md (The shop)
public final class VendingCatalog {

    /** The Vending Machine tab a trade shows under. The machine's tabs are fixed; these are the two GregGuns uses. */
    public enum Category {

        /** Weapons, ammo, armor, gear and spawn eggs: the tab the pack's combat (Survivor coin) trades use. */
        MISC("misc"),
        /** Gun parts, components, armor parts and turret plates: the pack's parts tab. */
        COMPONENTS("components");

        private final String key;

        Category(String key) {
            this.key = key;
        }

        /** @return the Vending Machine's category key ({@code TradeCategory.getKey()}) */
        public String key() {
            return key;
        }
    }

    /** One trade: {@code count} of one GregGuns item stack for one stack of coins. Immutable. */
    public static final class Offer {

        private final String key;
        private final String item;
        private final int meta;
        private final int count;
        private final Tier tier;
        private final Category category;
        private final CoinDenomination.Stack price;

        Offer(String key, String item, int meta, int count, Tier tier, Category category, long value) {
            this.key = key;
            this.item = item;
            this.meta = meta;
            this.count = count;
            this.tier = tier;
            this.category = category;
            this.price = CoinDenomination.exact(value);
            if (price == null) {
                throw new IllegalStateException(key + ": " + value + " coins is not one coin stack");
            }
        }

        /** @return a stable id of the trade, unique in the catalog; the trade group's UUID derives from it */
        public String key() {
            return key;
        }

        /** @return the bare GregGuns registry name sold */
        public String item() {
            return item;
        }

        /** @return the item damage sold */
        public int meta() {
            return meta;
        }

        /** @return how many items one trade hands out */
        public int count() {
            return count;
        }

        /** @return the tier whose intro quest unlocks the trade */
        public Tier tier() {
            return tier;
        }

        /** @return the Vending Machine tab */
        public Category category() {
            return category;
        }

        /** @return the price */
        public CoinDenomination.Stack price() {
            return price;
        }

        /** @return the key of the quest that unlocks the trade */
        public String questKey() {
            return QuestGate.introKey(tier);
        }

        @Override
        public String toString() {
            return key + " x" + count + " for " + price + " (" + tier + ")";
        }
    }

    /** Loose rounds per trade, unless {@link #ROUNDS} says otherwise. */
    static final int ROUNDS_PER_TRADE = 32;

    /** Heavy rounds sell in smaller lots: name to count (the nuke rocket is priced on its own, see {@link #build}). */
    static final Map<String, Integer> ROUNDS;

    /** The nuclear rocket: one per trade, at {@link #NUKE_UNITS} units. */
    static final String NUKE_ROCKET = "ammo_rocket_nuke";
    static final int NUKE_UNITS = 5;

    /** The nuclear power cell: one per trade, a full unit (other components are half a unit). */
    static final String NUCLEAR_CELL = "cell_nuclear";

    static {
        Map<String, Integer> rounds = new HashMap<>();
        rounds.put("ammo_rocket", 8);
        rounds.put("ammo_40mm_grenade", 8);
        rounds.put("ammo_nether_charge", 8);
        ROUNDS = Collections.unmodifiableMap(rounds);
    }

    private static final Map<Tier, Integer> UNIT = new EnumMap<>(Tier.class);

    static {
        UNIT.put(Tier.STEAM, 10);
        UNIT.put(Tier.LV, 20);
        UNIT.put(Tier.MV, 50);
        UNIT.put(Tier.HV, 100);
        UNIT.put(Tier.EV, 200);
        UNIT.put(Tier.IV, 500);
        UNIT.put(Tier.LUV, 1000);
        UNIT.put(Tier.ZPM, 2000);
    }

    /** A spawn egg's place in the shop: the tier that unlocks it and its price in coins. */
    static final class EggPrice {

        final Tier tier;
        final long value;

        EggPrice(Tier tier, long value) {
            this.tier = tier;
            this.value = value;
        }
    }

    /**
     * Spawn egg prices, by threat. Bosses cost ten units of their quest tier (the Attack Helicopter at HV, the
     * Cyberdemon
     * and Alien Bug at EV, Dictator Dave at IV); Psycho Steve, the LV mini-boss, ten LV units. The camp turret has no
     * egg.
     * A unit test asks for every egg mob to be listed here.
     */
    static final Map<NpcMob, EggPrice> EGGS;

    static {
        Map<NpcMob, EggPrice> eggs = new EnumMap<>(NpcMob.class);
        eggs.put(NpcMob.ZOMBIE_SOLDIER, new EggPrice(Tier.STEAM, 5));
        eggs.put(NpcMob.ZOMBIE_FARMER, new EggPrice(Tier.STEAM, 5));
        eggs.put(NpcMob.ZOMBIE_MINER, new EggPrice(Tier.STEAM, 5));
        eggs.put(NpcMob.SKELETON_SOLDIER, new EggPrice(Tier.STEAM, 10));
        eggs.put(NpcMob.ZOMBIE_PIGMAN_SOLDIER, new EggPrice(Tier.LV, 20));
        eggs.put(NpcMob.BANDIT, new EggPrice(Tier.LV, 20));
        eggs.put(NpcMob.OUTCAST, new EggPrice(Tier.MV, 50));
        eggs.put(NpcMob.ARMY_SOLDIER, new EggPrice(Tier.MV, 50));
        eggs.put(NpcMob.SUPER_MUTANT_BASIC, new EggPrice(Tier.MV, 50));
        eggs.put(NpcMob.COMMANDO, new EggPrice(Tier.MV, 100));
        eggs.put(NpcMob.STORMTROOPER, new EggPrice(Tier.HV, 100));
        eggs.put(NpcMob.SUPER_MUTANT_HEAVY, new EggPrice(Tier.HV, 100));
        eggs.put(NpcMob.SUPER_MUTANT_ELITE, new EggPrice(Tier.EV, 200));
        eggs.put(NpcMob.PSYCHO_STEVE, new EggPrice(Tier.LV, 200));
        eggs.put(NpcMob.ATTACK_HELICOPTER, new EggPrice(Tier.HV, 1000));
        eggs.put(NpcMob.CYBER_DEMON, new EggPrice(Tier.EV, 2000));
        eggs.put(NpcMob.ALIEN_BUG, new EggPrice(Tier.EV, 2000));
        eggs.put(NpcMob.DICTATOR_DAVE, new EggPrice(Tier.IV, 5000));
        EGGS = Collections.unmodifiableMap(eggs);
    }

    private VendingCatalog() {}

    /**
     * @param tier a tier
     * @return the tier's price unit in coins: what a side quest of that tier is meant to pay
     */
    public static int unit(Tier tier) {
        return UNIT.get(tier);
    }

    /**
     * @return every trade, guns module first, then armor, utility and the spawn eggs, each in its order class's order
     */
    public static List<Offer> build() {
        List<Offer> out = new ArrayList<>();
        for (GunsItemOrder.Slot slot : GunsItemOrder.build()) {
            addGunsSlot(out, slot);
        }
        for (ArmorItemOrder.Slot slot : ArmorItemOrder.build()) {
            Tier t = slot.tier();
            int u = unit(t);
            switch (slot.kind()) {
                case ARMOR_PIECE:
                case EQUIPMENT:
                    out.add(item(slot.name(), 1, t, Category.MISC, 2L * u));
                    break;
                case FLIGHT:
                    out.add(item(slot.name(), 1, t, Category.MISC, 4L * u));
                    break;
                case PART:
                    out.add(item(slot.name(), 4, t, Category.COMPONENTS, u));
                    break;
                default:
                    break;
            }
        }
        for (UtilityItemOrder.Slot slot : UtilityItemOrder.build()) {
            if (slot.kind() == UtilityItemOrder.Kind.PLATE) {
                out.add(item(slot.name(), 4, slot.tier(), Category.COMPONENTS, unit(slot.tier())));
            }
        }
        for (NpcMob mob : NpcEggOrder.all()) {
            EggPrice price = EGGS.get(mob);
            if (price != null && mob.hasEgg() && !mob.isDevOnly()) {
                out.add(
                    new Offer(
                        "egg:" + mob.registryName(),
                        NpcRegistry.EGG_NAME,
                        mob.entityId(),
                        1,
                        price.tier,
                        Category.MISC,
                        price.value));
            }
        }
        return Collections.unmodifiableList(out);
    }

    private static void addGunsSlot(List<Offer> out, GunsItemOrder.Slot slot) {
        String name = slot.name();
        Tier t = slot.tier();
        int u = unit(t);
        switch (slot.kind()) {
            case GUN:
                out.add(item(name, 1, t, Category.MISC, (isAkimbo(name) ? 5L : 3L) * u));
                break;
            case MELEE:
                out.add(item(name, 1, t, Category.MISC, 2L * u));
                break;
            case MAGAZINE:
                out.add(item(name, 4, t, Category.MISC, u));
                break;
            case AMMO:
                if (NUKE_ROCKET.equals(name)) {
                    out.add(item(name, 1, t, Category.MISC, (long) NUKE_UNITS * u));
                } else {
                    Integer lot = ROUNDS.get(name);
                    out.add(item(name, lot == null ? ROUNDS_PER_TRADE : lot, t, Category.MISC, u));
                }
                break;
            case GRENADE:
                out.add(item(name, 4, t, Category.MISC, u));
                break;
            case PART:
                out.add(item(name, 4, t, Category.COMPONENTS, u));
                break;
            case COMPONENT:
                out.add(item(name, 1, t, Category.COMPONENTS, NUCLEAR_CELL.equals(name) ? u : u / 2));
                break;
            case MAGAZINE_EMPTY:
            default:
                // Empty magazines come from unloading a loaded one; they are not sold.
                break;
        }
    }

    private static boolean isAkimbo(String gunName) {
        for (GunDefinition gun : GunDefinitions.ALL) {
            if (gun.name()
                .equals(gunName)) {
                return gun.singleGun() != null;
            }
        }
        return false;
    }

    private static Offer item(String name, int count, Tier tier, Category category, long value) {
        return new Offer("item:" + name, name, 0, count, tier, category, value);
    }
}
