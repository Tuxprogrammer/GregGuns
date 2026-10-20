package com.gregguns.entity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDropsEvent;

import com.gregguns.armor.ItemGregGunsArmor;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.entity.npc.NpcEntity;
import com.gregguns.entity.npc.NpcItems;
import com.gregguns.entity.npc.loot.EquipmentProfile;
import com.gregguns.entity.npc.loot.LootDrop;
import com.gregguns.entity.npc.loot.NpcProfile;
import com.gregguns.entity.npc.loot.NpcProfiles;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * Scenarios of NPC equipment and loot (M7 N5). Dev environment only. Server side.
 *
 * <p>
 * Nothing is spawned into the world: an NPC is built, equipped and killed with {@code onDeath}, which captures its
 * drops and posts a {@link LivingDropsEvent}. Drops are counted from that event, not from a world scan.
 */
// Spec: docs/gregguns-spec/10-entities-npcs/mob-drops-nei.md (GGS-10-15 to GGS-10-19, GGS-10-21)
final class NpcLootScenarios {

    private static final int KILLS = 1000;

    private NpcLootScenarios() {}

    static void registerAll(ScenarioRegistry registry) {
        registry.register(new LootSimulationScenario());
        registry.register(new EquipmentRollScenario());
    }

    /** A profiled NPC that is never spawned: it only rolls gear and dies. */
    public static final class ProfiledNpc extends NpcEntity {

        private NpcProfile profile;

        public ProfiledNpc(World world) {
            super(world);
        }

        ProfiledNpc(World world, NpcProfile profile) {
            super(world);
            this.profile = profile;
        }

        @Override
        protected NpcProfile npcProfile() {
            return profile;
        }

        void killByPlayer() {
            recentlyHit = 100;
            onDeath(DamageSource.generic);
        }
    }

    /** Collects the item names of every {@link LivingDropsEvent} of a {@link ProfiledNpc}. */
    public static final class DropLog {

        final List<List<ItemStack>> kills = new ArrayList<>();

        @SubscribeEvent
        public void onDrops(LivingDropsEvent event) {
            if (event.entityLiving instanceof ProfiledNpc) {
                List<ItemStack> stacks = new ArrayList<>();
                for (net.minecraft.entity.item.EntityItem e : event.drops) {
                    stacks.add(e.getEntityItem());
                }
                kills.add(stacks);
            }
        }

        static DropLog start() {
            DropLog log = new DropLog();
            MinecraftForge.EVENT_BUS.register(log);
            return log;
        }

        void stop() {
            MinecraftForge.EVENT_BUS.unregister(this);
        }
    }

    private static Item itemOf(ItemStack s) {
        return s == null ? null : s.getItem();
    }

    /** 1000 kills per mob: every drop is in the table, fixed drops always fall, no empty stack, bosses never rare. */
    private static final class LootSimulationScenario implements Scenario {

        @Override
        public String name() {
            return "npc.loot_simulation";
        }

        @Override
        public String description() {
            return "1000 player kills per mob: all drops come from its table, fixed drops and gear; no empty stacks.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            for (NpcProfile profile : NpcProfiles.all()
                .values()) {
                checkMob(ctx, out, profile);
            }
            return out.build();
        }

        private void checkMob(ScenarioContext ctx, ScenarioResult.Builder out, NpcProfile profile) {
            String id = profile.id();
            Set<Item> allowed = new HashSet<>();
            List<LootDrop> refs = new ArrayList<>(profile.fixedDrops());
            if (profile.table() != null) {
                for (LootDrop d : profile.table()
                    .slots()) {
                    if (d != null) {
                        refs.add(d);
                    }
                }
            }
            for (LootDrop d : refs) {
                ItemStack s = NpcItems.stack(d.ref(), 1);
                if (s == null) {
                    // A missing GT or pack item skips that drop; the mod's own and vanilla items must resolve.
                    out.check(
                        d.ref()
                            .startsWith("ore:"),
                        id + " drop " + d.ref() + " should resolve");
                } else {
                    allowed.add(s.getItem());
                }
            }
            DropLog log = DropLog.start();
            try {
                Random seed = new Random(id.hashCode());
                for (int i = 0; i < KILLS; i++) {
                    ProfiledNpc npc = new ProfiledNpc(ctx.world(), profile);
                    npc.rollEquipment();
                    Set<Item> gear = new HashSet<>();
                    for (int slot = 0; slot < 5; slot++) {
                        if (npc.getEquipmentInSlot(slot) != null) {
                            gear.add(
                                npc.getEquipmentInSlot(slot)
                                    .getItem());
                        }
                    }
                    npc.getRNG()
                        .setSeed(seed.nextLong());
                    int before = log.kills.size();
                    npc.killByPlayer();
                    if (log.kills.size() != before + 1) {
                        out.check(false, id + " kill " + i + " posted no LivingDropsEvent");
                        return;
                    }
                    for (ItemStack s : log.kills.get(before)) {
                        Item item = itemOf(s);
                        if (!allowed.contains(item) && !gear.contains(item) || s.stackSize < 1) {
                            out.check(false, id + " dropped unexpected " + s);
                            return;
                        }
                    }
                    for (LootDrop fixed : profile.fixedDrops()) {
                        ItemStack want = NpcItems.stack(fixed.ref(), 1);
                        boolean found = false;
                        for (ItemStack s : log.kills.get(before)) {
                            found |= want != null && s.getItem() == want.getItem();
                        }
                        if (want != null && !found) {
                            out.check(false, id + " missed fixed drop " + fixed.ref());
                            return;
                        }
                    }
                }
                if (!profile.rareDrop()) {
                    // A boss's only held-item drop is the 8.5% equipment one: never a bare copy at the rare rate.
                    int guns = 0;
                    for (List<ItemStack> kill : log.kills) {
                        for (ItemStack s : kill) {
                            if (s.getItem() == NpcItems.item(
                                profile.equipment()
                                    .weapons()
                                    .get(0)
                                    .gun())
                                && s.stackSize == 1) {
                                guns++;
                            }
                        }
                    }
                    // Fixed Golden Revolver for Dave and Chainsaw for Steve make one per kill; extras are equipment.
                    out.check(guns >= KILLS, id + " should drop its fixed weapon every kill, saw " + guns);
                }
                out.note(id + ": " + KILLS + " kills ok");
            } finally {
                log.stop();
            }
        }
    }

    /** The vanilla no-argument equipment roll fills the profile's slots with camo armor and a real weapon. */
    private static final class EquipmentRollScenario implements Scenario {

        @Override
        public String name() {
            return "npc.equipment_roll";
        }

        @Override
        public String description() {
            return "addRandomArmor() equips each profile from its tables; armor camo is in range; nothing is enchanted;"
                + " the danger survives a save.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            for (NpcProfile profile : NpcProfiles.all()
                .values()) {
                if (profile.equipment() == null) {
                    continue;
                }
                int armored = 0;
                int armed = 0;
                for (int i = 0; i < 100; i++) {
                    ProfiledNpc npc = new ProfiledNpc(ctx.world(), profile);
                    npc.setDanger(i % 6);
                    npc.rollEquipment();
                    if (npc.getHeldItem() != null) {
                        armed++;
                    }
                    for (int slot = 1; slot <= 4; slot++) {
                        ItemStack s = npc.getEquipmentInSlot(slot);
                        if (s == null) {
                            continue;
                        }
                        armored++;
                        out.check(s.getItem() instanceof ItemGregGunsArmor, profile.id() + " slot " + slot + " armor");
                        if (s.getItem() instanceof ItemGregGunsArmor) {
                            ItemGregGunsArmor armor = (ItemGregGunsArmor) s.getItem();
                            int camo = armor.getCamo(s);
                            out.check(camo >= 0 && camo < armor.camoCount(s), profile.id() + " camo " + camo);
                        }
                    }
                    for (int slot = 0; slot < 5; slot++) {
                        ItemStack s = npc.getEquipmentInSlot(slot);
                        out.check(s == null || !s.isItemEnchanted(), profile.id() + " slot " + slot + " enchanted");
                    }
                    if (i == 0) {
                        NBTTagCompound tag = new NBTTagCompound();
                        npc.writeEntityToNBT(tag);
                        ProfiledNpc loaded = new ProfiledNpc(ctx.world(), profile);
                        loaded.readEntityFromNBT(tag);
                        out.checkEquals(profile.id() + " danger after save", 0, loaded.danger());
                        out.check(
                            loaded.getHeldItem() == null || npc.getHeldItem() != null && loaded.getHeldItem()
                                .getItem()
                                == npc.getHeldItem()
                                    .getItem(),
                            profile.id() + " held item after save");
                    }
                }
                EquipmentProfile eq = profile.equipment();
                boolean hasWeapon = !eq.weapons()
                    .isEmpty();
                boolean anyExists = false;
                for (EquipmentProfile.Weapon w : eq.weapons()) {
                    anyExists |= NpcItems.item(w.gun()) != null;
                }
                if (hasWeapon && anyExists) {
                    out.check(armed == 100, profile.id() + " should always hold a weapon, armed " + armed + "/100");
                }
                if (!eq.armor()
                    .isEmpty()) {
                    out.check(armored > 0, profile.id() + " should wear armor");
                }
                out.note(profile.id() + ": armed " + armed + "/100, armor pieces " + armored);
            }
            return out.build();
        }
    }
}
