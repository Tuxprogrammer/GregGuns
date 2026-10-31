package com.gregguns.icbm;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

import com.gregguns.GregGuns;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.icbm.control.MTELaunchControl;

import cpw.mods.fml.common.Loader;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/**
 * Dev-only helpers for the I7 play checks on a real-pack server with connected clients (multiplayer, shaders,
 * quests). None is part of {@code test all}; all are registered only behind the test hooks, like every scenario.
 * <ul>
 * <li>{@code testing.icbm_i7_do} runs the operations in {@code i7-ops.txt} in the server's working directory, one per
 * line, then renames the file to {@code i7-ops.done}: {@code npc <entityName> <x> <y> <z> [still]} spawns a mob as a
 * spawn egg would (it rolls its equipment), {@code still} keeps it in place; {@code slay <player> <radius>} kills every
 * living non-player within the radius of the player as that player would, so drops and kill credit are real;
 * {@code lcgun <player> <x> <y> <z>} puts the player's held item in the Launch Control's target slot and presses
 * Load, as the GUI does.</li>
 * <li>{@code testing.icbm_i7_quests} walks the ICBM quests listed in {@code i7-quests.txt} through BetterQuesting
 * for the first connected player (see {@link I7QuestWalk}).</li>
 * </ul>
 * Server side.
 */
// Spec: docs/ROADMAP-ICBM.md (I7: multiplayer, shader and quest checks)
public final class I7PlayChecks {

    private I7PlayChecks() {}

    /** @param registry where to add the helpers */
    static void registerAll(ScenarioRegistry registry) {
        registry.register(new Do());
        registry.register(new Quests());
    }

    /** @return the online player with that name, or null */
    static EntityPlayerMP player(String name) {
        return MinecraftServer.getServer()
            .getConfigurationManager()
            .func_152612_a(name);
    }

    private abstract static class Helper implements Scenario {

        @Override
        public boolean includedInAll() {
            return false;
        }

        @Override
        public int areaRadius() {
            return 1;
        }
    }

    private static final class Do extends Helper {

        private static final File OPS = new File("i7-ops.txt");

        @Override
        public String name() {
            return "testing.icbm_i7_do";
        }

        @Override
        public String description() {
            return "I7 play-check helper (dev only): runs the operations in i7-ops.txt (npc, slay, lcgun). Not part of test all.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            List<String> lines;
            try {
                if (!OPS.isFile()) {
                    return ScenarioResult.fail(name(), "no i7-ops.txt in " + new File(".").getAbsolutePath());
                }
                lines = Files.readAllLines(OPS.toPath(), StandardCharsets.UTF_8);
                Files.move(OPS.toPath(), new File("i7-ops.done").toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                return ScenarioResult.fail(name(), e.toString());
            }
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            for (String line : lines) {
                String l = line.trim();
                if (l.isEmpty() || l.startsWith("#")) {
                    continue;
                }
                try {
                    op(ctx.world(), l.split("\\s+"));
                } catch (RuntimeException e) {
                    GregGuns.LOG.warn("I7 op failed: {}: {}", l, e.toString());
                    out.check(false, "op failed: " + l + ": " + e);
                }
            }
            return out.build();
        }

        private static void op(World w, String[] t) {
            switch (t[0]) {
                case "npc" -> {
                    Entity e = EntityList.createEntityByName(t[1], w);
                    if (!(e instanceof EntityLiving)) {
                        throw new IllegalArgumentException("not a mob: " + t[1]);
                    }
                    EntityLiving mob = (EntityLiving) e;
                    double x = Double.parseDouble(t[2]), y = Double.parseDouble(t[3]), z = Double.parseDouble(t[4]);
                    mob.setLocationAndAngles(x, y, z, 0.0F, 0.0F);
                    mob.onSpawnWithEgg(null);
                    mob.func_110163_bv(); // enablePersistence: no despawn while the session runs
                    if (t.length > 5 && "still".equals(t[5])) {
                        mob.getEntityAttribute(SharedMonsterAttributes.movementSpeed)
                            .setBaseValue(0.0);
                    }
                    w.spawnEntityInWorld(mob);
                    GregGuns.LOG.info(
                        "I7 npc {} #{} at {},{},{} held={} armor={},{},{},{}",
                        t[1],
                        mob.getEntityId(),
                        (int) x,
                        (int) y,
                        (int) z,
                        name(mob.getEquipmentInSlot(0)),
                        name(mob.getEquipmentInSlot(1)),
                        name(mob.getEquipmentInSlot(2)),
                        name(mob.getEquipmentInSlot(3)),
                        name(mob.getEquipmentInSlot(4)));
                }
                case "slay" -> {
                    EntityPlayerMP p = player(t[1]);
                    double r = Double.parseDouble(t[2]);
                    List<EntityLiving> victims = new ArrayList<>();
                    for (Object o : p.worldObj.loadedEntityList) {
                        if (o instanceof EntityLiving && ((Entity) o).getDistanceToEntity(p) <= r) {
                            victims.add((EntityLiving) o);
                        }
                    }
                    for (EntityLiving v : victims) {
                        // Player damage sets recentlyHit, so the kill drops what a player's kill drops.
                        v.attackEntityFrom(DamageSource.causePlayerDamage(p), 10_000.0F);
                        GregGuns.LOG.info(
                            "I7 slay {} #{} at {},{},{} dead={}",
                            EntityList.getEntityString(v),
                            v.getEntityId(),
                            (int) v.posX,
                            (int) v.posY,
                            (int) v.posZ,
                            v.isDead || v.getHealth() <= 0);
                    }
                }
                case "lcgun" -> {
                    EntityPlayerMP p = player(t[1]);
                    int x = Integer.parseInt(t[2]), y = Integer.parseInt(t[3]), z = Integer.parseInt(t[4]);
                    IGregTechTileEntity base = (IGregTechTileEntity) w.getTileEntity(x, y, z);
                    MTELaunchControl lc = (MTELaunchControl) base.getMetaTileEntity();
                    ItemStack held = p.getHeldItem();
                    lc.setInventorySlotContents(MTELaunchControl.SLOT_STICK, held == null ? null : held.copy());
                    p.inventory.setInventorySlotContents(p.inventory.currentItem, null);
                    lc.loadFromStick(p);
                    GregGuns.LOG.info(
                        "I7 lcgun {} at {},{},{} stick={} x=[{}] z=[{}]",
                        t[1],
                        x,
                        y,
                        z,
                        lc.lastStick(),
                        lc.xText(),
                        lc.zText());
                }
                default -> throw new IllegalArgumentException("unknown op " + t[0]);
            }
        }

        private static String name(ItemStack s) {
            return s == null ? "-" : s.getUnlocalizedName();
        }
    }

    private static final class Quests extends Helper {

        @Override
        public String name() {
            return "testing.icbm_i7_quests";
        }

        @Override
        public String description() {
            return "I7 play-check helper (dev only): completes and claims every quest in i7-quests.txt through BetterQuesting for the first player. Not part of test all.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (!Loader.isModLoaded("betterquesting")) {
                return ScenarioResult.skip(name(), "BetterQuesting is not loaded");
            }
            return I7QuestWalk.start(name());
        }
    }
}
