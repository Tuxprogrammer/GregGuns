package com.gregguns.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagFloat;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.DamageSource;
import net.minecraft.world.GameRules;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;

import com.gregguns.armor.ItemGregGunsArmor;
import com.gregguns.core.config.LootConfig;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.core.testing.SkipUnless;
import com.gregguns.entity.NpcLootScenarios.DropLog;
import com.gregguns.entity.NpcLootScenarios.ProfiledNpc;
import com.gregguns.entity.npc.NpcDrops;
import com.gregguns.entity.npc.NpcEntity;
import com.gregguns.entity.npc.NpcGunDropsHandler;
import com.gregguns.entity.npc.NpcItems;
import com.gregguns.entity.npc.loot.EquipmentProfile;
import com.gregguns.entity.npc.loot.NpcProfile;
import com.gregguns.entity.npc.loot.NpcProfiles;
import com.gregguns.item.gun.ItemGun;

import cpw.mods.fml.common.Loader;

/**
 * Scenarios of the drops of an NPC's carried gun (D312): the rare gun drop rate, the ammo, tank or salvage on every
 * loot-dropping death, the loot game rule, and the held-item drop chance after a load. Dev environment only, server
 * side.
 *
 * <p>
 * Each kill is a {@link ProfiledNpc} on a probe profile that has only the real profile's equipment (no table, no fixed
 * drops), so every item a kill drops is either worn armor or the gun's own drop. Drops are read from the
 * {@code LivingDropsEvent} at the lowest priority ({@link DropLog}), never spawned.
 */
// Spec: docs/gregguns-spec/10-entities-npcs/mob-drops-nei.md (GGS-10-38 to GGS-10-40)
final class NpcGunDropScenarios {

    /** Kills per looting level for the rate check. */
    private static final int RATE_KILLS = 2000;
    /** Kills per gun for the every-death check. */
    private static final int GUN_KILLS = 200;
    /** The gun the rate check holds. */
    private static final String RATE_GUN = "ak47";
    /** The profile whose equipment the rate check wears. */
    private static final String RATE_PROFILE = "ArmySoldier";

    private NpcGunDropScenarios() {}

    static void registerAll(ScenarioRegistry registry) {
        registry.register(new Rates());
        registry.register(new EveryGun());
        registry.register(new GameRule());
        registry.register(new SavedChance());
        registry.register(
            SkipUnless.of(
                new MobsInfoPage(),
                () -> Loader.isModLoaded(NpcGunDropsHandler.MOBS_INFO),
                "Mobs Info is not loaded (run with -Pgg.devMobsInfo=true)"));
    }

    /** A profile with only the source profile's equipment and the given rare-drop switch. */
    private static NpcProfile probe(NpcProfile source, boolean rareDrop) {
        NpcProfile.Builder b = NpcProfile.builder("probe_" + source.id(), source.ceiling())
            .rareDrop(rareDrop);
        if (source.equipment() != null) {
            b.equipment(source.equipment());
        }
        return b.build();
    }

    /** Builds an NPC on a probe profile holding a gun, seeded. */
    private static ProfiledNpc armed(ScenarioContext ctx, NpcProfile probe, Item gun, long seed) {
        ProfiledNpc npc = new ProfiledNpc(ctx.world(), probe);
        npc.setCurrentItemOrArmor(0, new ItemStack(gun));
        npc.getRNG()
            .setSeed(seed);
        return npc;
    }

    /** The fake player that kills with a Looting sword. */
    private static FakePlayer killer(ScenarioContext ctx, int looting) {
        FakePlayer p = FakePlayerFactory.getMinecraft((WorldServer) ctx.world());
        ItemStack sword = new ItemStack(Items.diamond_sword);
        if (looting > 0) {
            sword.addEnchantment(Enchantment.looting, looting);
        }
        p.setCurrentItemOrArmor(0, sword);
        return p;
    }

    private static int count(List<ItemStack> drops, Item item) {
        int n = 0;
        for (ItemStack s : drops) {
            if (s.getItem() == item) {
                n += s.stackSize;
            }
        }
        return n;
    }

    /** The rare gun drop: about 2% at Looting 0 and 5% at Looting 3, never through vanilla's equipment roll. */
    private static final class Rates implements Scenario {

        @Override
        public String name() {
            return "npc.gun_drops_rates";
        }

        @Override
        public String description() {
            return "2000 player kills at Looting 0 and 3: the gun drops at about 2% and 5%, and never without the rare"
                + " drop.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            NpcProfile source = NpcProfiles.get(RATE_PROFILE);
            Item gun = NpcItems.item(RATE_GUN);
            if (source == null || gun == null) {
                return out.check(false, "needs profile " + RATE_PROFILE + " and gun " + RATE_GUN)
                    .build();
            }
            FakePlayer player = null;
            DropLog log = DropLog.start();
            try {
                // {looting, rare drop on, lowest, highest}: binomial 3-sigma bands around 2000 * p, fixed seeds.
                int[][] bands = { { 0, 1, 21, 59 }, { 3, 1, 71, 129 }, { 3, 0, 0, 0 } };
                for (int[] band : bands) {
                    player = killer(ctx, band[0]);
                    NpcProfile probe = probe(source, band[1] == 1);
                    Random seeds = new Random(1000L + band[0] * 10 + band[1]);
                    int before = log.kills.size();
                    for (int i = 0; i < RATE_KILLS; i++) {
                        armed(ctx, probe, gun, seeds.nextLong()).kill(DamageSource.causePlayerDamage(player), true);
                    }
                    if (log.kills.size() - before != RATE_KILLS) {
                        return out
                            .check(false, "expected " + RATE_KILLS + " drop events, saw " + (log.kills.size() - before))
                            .build();
                    }
                    int guns = 0;
                    for (int k = before; k < log.kills.size(); k++) {
                        guns += count(log.kills.get(k), gun);
                    }
                    String what = "looting " + band[0] + (band[1] == 1 ? "" : " without the rare drop");
                    out.check(
                        guns >= band[2] && guns <= band[3],
                        what + ": " + guns + " guns in " + RATE_KILLS + " kills, want " + band[2] + " to " + band[3]);
                    out.note(what + ": " + guns + " guns in " + RATE_KILLS + " kills");
                }
            } finally {
                log.stop();
                if (player != null) {
                    player.setCurrentItemOrArmor(0, null);
                }
            }
            return out.build();
        }
    }

    /**
     * Every gun any profile carries: each death drops exactly the gun's own rule, by a player or not, and a non-player
     * kill never drops the gun.
     */
    private static final class EveryGun implements Scenario {

        @Override
        public String name() {
            return "npc.gun_drops_every_gun";
        }

        @Override
        public String description() {
            return "For every NPC-carried gun, 200 kills without a player: ammo, a full tank or recipe salvage on every"
                + " death, in range and within one stack; never the gun, never a battery.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            Map<String, NpcProfile> carriers = new HashMap<>();
            Set<String> guns = new LinkedHashSet<>();
            for (NpcProfile p : NpcProfiles.all()
                .values()) {
                EquipmentProfile eq = p.equipment();
                if (eq == null) {
                    continue;
                }
                for (EquipmentProfile.Weapon w : eq.weapons()) {
                    Item item = NpcItems.item(w.gun());
                    if (item instanceof ItemGun && guns.add(w.gun())) {
                        carriers.put(w.gun(), p);
                    }
                }
            }
            if (guns.isEmpty()) {
                return out.check(false, "no NPC carries a gun")
                    .build();
            }
            // The drop rules' shape, uncapped: a gun or ammo above loot.maxTier stays with the body (D364), which
            // loot.obtain.npc checks; here every gun's own rule must show in full.
            int savedCap = LootConfig.maxTier;
            LootConfig.maxTier = 12;
            DropLog log = DropLog.start();
            try {
                for (String name : guns) {
                    ItemGun gun = (ItemGun) NpcItems.item(name);
                    NpcDrops.GunDrop rule = NpcDrops.gunDrop(name, gun.stats());
                    if (rule == null) {
                        out.check(false, name + " has no drop rule");
                        continue;
                    }
                    Set<Item> allowed = NpcLootScenarios.gunDropItems(new ItemStack(gun));
                    out.check(!allowed.isEmpty(), name + ": its drop items resolve");
                    NpcProfile probe = probe(carriers.get(name), true);
                    Random seeds = new Random(name.hashCode());
                    int before = log.kills.size();
                    for (int i = 0; i < GUN_KILLS; i++) {
                        armed(ctx, probe, gun, seeds.nextLong()).kill(DamageSource.generic, false);
                    }
                    out.checkEquals(name + " drop events", GUN_KILLS, log.kills.size() - before);
                    int bad = 0;
                    int total = 0;
                    for (int k = before; k < log.kills.size(); k++) {
                        int items = 0;
                        int stacks = 0;
                        for (ItemStack s : log.kills.get(k)) {
                            if (s.getItem() instanceof ItemGregGunsArmor) {
                                continue;
                            }
                            if (!allowed.contains(s.getItem())) {
                                bad++;
                                out.check(false, name + " dropped " + s + ", not part of its rule");
                            }
                            if (s.stackSize > s.getMaxStackSize()) {
                                bad++;
                                out.check(false, name + " dropped " + s + ", more than one stack");
                            }
                            items += s.stackSize;
                            stacks++;
                        }
                        total += items;
                        if (rule.kind() == NpcDrops.GunDropKind.SALVAGE) {
                            bad += stacks == com.gregguns.entity.npc.GunSalvage.DRAWS ? 0 : 1;
                        } else {
                            bad += stacks == 1 && items >= rule.minItems() && items <= rule.maxItems() ? 0 : 1;
                        }
                        if (bad > 5) {
                            break;
                        }
                    }
                    out.check(bad == 0, name + ": " + bad + " deaths broke the " + rule.kind() + " rule");
                    out.note(name + " (" + rule.kind() + "): " + total + " items in " + GUN_KILLS + " deaths");
                }
            } finally {
                log.stop();
                LootConfig.maxTier = savedCap;
            }
            return out.build();
        }
    }

    /** With {@code doMobLoot} off nothing drops; the rule is restored on every exit path. */
    private static final class GameRule implements Scenario {

        @Override
        public String name() {
            return "npc.gun_drops_gamerule";
        }

        @Override
        public String description() {
            return "With doMobLoot off an NPC's death drops nothing, gun drops included; on, the same kill drops ammo.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            NpcProfile source = NpcProfiles.get(RATE_PROFILE);
            Item gun = NpcItems.item(RATE_GUN);
            if (source == null || gun == null) {
                return out.check(false, "needs profile " + RATE_PROFILE + " and gun " + RATE_GUN)
                    .build();
            }
            GameRules rules = ctx.world()
                .getGameRules();
            String prior = rules.getGameRuleStringValue("doMobLoot");
            DropLog log = DropLog.start();
            try {
                NpcProfile probe = probe(source, true);
                rules.setOrCreateGameRule("doMobLoot", "false");
                armed(ctx, probe, gun, 7L).killByPlayer();
                rules.setOrCreateGameRule("doMobLoot", "true");
                armed(ctx, probe, gun, 7L).killByPlayer();
                out.checkEquals("drop events", 2, log.kills.size());
                if (log.kills.size() == 2) {
                    out.checkEquals(
                        "drops with doMobLoot off",
                        0,
                        log.kills.get(0)
                            .size());
                    out.check(
                        !log.kills.get(1)
                            .isEmpty(),
                        "the same kill with doMobLoot on drops ammo");
                }
            } finally {
                rules.setOrCreateGameRule("doMobLoot", prior);
                log.stop();
            }
            return out.build();
        }
    }

    /** An NPC read from a save whose drop chances are vanilla's still never drops its held item that way. */
    private static final class SavedChance implements Scenario {

        @Override
        public String name() {
            return "npc.gun_drops_saved_chance";
        }

        @Override
        public String description() {
            return "A new NPC never drops its held item through the equipment roll, also after reading a save that lists"
                + " 0.085.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            NpcProfile source = NpcProfiles.get(RATE_PROFILE);
            if (source == null) {
                return out.check(false, "needs profile " + RATE_PROFILE)
                    .build();
            }
            ProfiledNpc npc = new ProfiledNpc(ctx.world(), source);
            out.checkEquals("new NPC held drop chance", NpcEntity.NO_HELD_DROP, npc.heldDropChance());
            NBTTagCompound tag = new NBTTagCompound();
            npc.writeEntityToNBT(tag);
            NBTTagList chances = new NBTTagList();
            for (int i = 0; i < 5; i++) {
                chances.appendTag(new NBTTagFloat(0.085F));
            }
            // Source: vanilla 1.7.10 EntityLiving#writeEntityToNBT / #readEntityFromNBT, the "DropChances" list.
            tag.setTag("DropChances", chances);
            ProfiledNpc loaded = new ProfiledNpc(ctx.world(), source);
            loaded.readEntityFromNBT(tag);
            out.checkEquals("loaded NPC held drop chance", NpcEntity.NO_HELD_DROP, loaded.heldDropChance());
            return out.build();
        }
    }

    /**
     * Mobs Info's NEI page for three NPCs: no Additional gun row, the rare gun row (none for Psycho Steve), and an
     * ammo or salvage row. Needs Mobs Info ({@code -Pgg.devMobsInfo=true}).
     */
    private static final class MobsInfoPage implements Scenario {

        @Override
        public String name() {
            return "npc.mobsinfo_drops";
        }

        @Override
        public String description() {
            return "Mobs Info's mob page shows the NPC gun drops: rare gun, ammo or salvage, no 8.5% gun row.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            for (String mob : new String[] { "ArmySoldier", "Outcast", "PsychoSteve" }) {
                String roll = MobsInfoRows.rollInDummyWorld("gregguns." + mob);
                out.check(roll == null, String.valueOf(roll));
                List<String[]> rows = MobsInfoRows.of("gregguns." + mob);
                if (rows == null) {
                    out.check(false, mob + " is not on the Mobs Info page");
                    continue;
                }
                int rareGuns = 0;
                int normalFromGun = 0;
                List<String> seen = new ArrayList<>();
                for (String[] r : rows) {
                    // {type, "gun" or "item", item, chance, "info" when the row has an info line}
                    seen.add(r[0] + " " + r[2] + " " + r[3]);
                    boolean isGun = "gun".equals(r[1]);
                    out.check(!("Additional".equals(r[0]) && isGun), mob + ": an Additional gun row is left: " + r[2]);
                    if ("Rare".equals(r[0]) && isGun) {
                        rareGuns++;
                    }
                    // Only our rows carry an info line; Mobs Info's simulated table rows have none.
                    if ("Normal".equals(r[0]) && "info".equals(r[4])) {
                        normalFromGun++;
                    }
                }
                if ("PsychoSteve".equals(mob)) {
                    out.checkEquals(mob + " rare gun rows", 0, rareGuns);
                } else {
                    out.check(rareGuns > 0, mob + " has a rare gun row");
                }
                out.check(normalFromGun > 0, mob + " has an ammo or salvage row");
                out.note(mob + ": " + seen);
            }
            return out.build();
        }
    }
}
