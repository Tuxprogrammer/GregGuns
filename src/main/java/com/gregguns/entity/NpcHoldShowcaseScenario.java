package com.gregguns.entity;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.WorldSettings;

import com.gregguns.armor.ArmorItems;
import com.gregguns.armor.ItemGregGunsArmor;
import com.gregguns.armor.logic.ArmorCatalog;
import com.gregguns.armor.logic.ArmorSlot;
import com.gregguns.armor.logic.SetDef;
import com.gregguns.armor.slots.ItemGasMask;
import com.gregguns.core.testing.AutotestSignal;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.entity.npc.NpcMob;
import com.gregguns.item.gun.GunItems;

import cpw.mods.fml.common.registry.GameRegistry;

/**
 * {@code npc.hold_showcase}: a visual check of how NPCs hold guns, for a connected client (see
 * {@code docs/testing/README.md}). Each step stands one NPC on a fixed spot holding one gun, puts the (creative,
 * empty-handed) player beside it or in front of it and asks the client for a screenshot labeled
 * {@code npc-<mob>-<gun>-<idle|aim>-<side|front>}, with the NPC idle (no target, arms down) or aiming (arms up). Then
 * the player holds each gun and is shot from the front and the back
 * ({@code player-<gun>-<front|back>}) as the reference for what a held gun looks like. Not part of {@code test all}:
 * it needs a connected client. Dev environment only, server side.
 *
 * <p>
 * Every NPC wears a full set of its profile's first armor (camo 0), so the shots also check that the armor follows the
 * posed arms (D310): the sleeve covers the arm in the hip carry and the aim. An armor sweep then puts the Army Soldier
 * with an AK-47 in each custom-model set, one vanilla-layer set and a mask helmet:
 * {@code armor-<set>-<idle|aim>-<side|front>}.
 */
// Spec: docs/techguns-spec/03-gun-visuals-audio/ (framework section 6.3, the aimed pre-rotation)
final class NpcHoldShowcaseScenario implements Scenario {

    private static final String NAME = "npc.hold_showcase";
    /** Ticks between two steps: the client needs the teleport and the entity update before the frame is shot. */
    private static final int STEP = 30;
    /** Every kind drawn with the biped or skeleton model, with the first armor set of its profile. */
    private static final NpcMob[] MOBS = { NpcMob.ZOMBIE_SOLDIER, NpcMob.ZOMBIE_FARMER, NpcMob.ZOMBIE_MINER,
        NpcMob.ZOMBIE_PIGMAN_SOLDIER, NpcMob.SKELETON_SOLDIER, NpcMob.BANDIT, NpcMob.ARMY_SOLDIER, NpcMob.COMMANDO,
        NpcMob.OUTCAST, NpcMob.STORMTROOPER, NpcMob.PSYCHO_STEVE, NpcMob.DICTATOR_DAVE };
    /** The armor of each kind in {@link #MOBS}, from NpcProfiles; t1_combat for the kinds with none. */
    private static final String[] MOB_ARMOR = { "t1_combat", "t1_combat", "t1_combat", "t2_combat", "t1_combat",
        "t1_scout", "t2_combat", "t2_commando", "t3_power", "t3_hev", "t1_miner", "t2_beret" };
    /** The armor sweep: every custom-model set, one vanilla-layer set, and a mask helmet over it. */
    private static final String[] SWEEP = { "t3_exo", "t3_power", "steam", "t2_riot", "t2_beret", "t2_combat",
        "t2_combat+" + ItemGasMask.NAME };
    private static final String SWEEP_GUN = "ak47";
    private static final String[] GUNS = { "pistol_9mm", "ak47", "minigun", "rocket_launcher" };

    static void register(ScenarioRegistry registry) {
        registry.register(new NpcHoldShowcaseScenario());
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "Stands NPCs holding each gun type in front of a connected client and screenshots them from the side"
            + " and the front, then the player holding the same guns. Needs 1 client; not part of test all.";
    }

    @Override
    public boolean includedInAll() {
        return false;
    }

    /** One screenshot: who stands there, with which gun, seen from where. */
    private static final class Shot {

        private final NpcMob mob;
        private final String gun;
        private final boolean front;
        private final boolean aim;
        private final String label;
        private final String armor;

        Shot(NpcMob mob, String gun, boolean front, boolean aim, String label, String armor) {
            this.mob = mob;
            this.gun = gun;
            this.front = front;
            this.aim = aim;
            this.label = label;
            this.armor = armor;
        }

        /** @return the armor set id, with {@code +<mask>} for a mask helmet in place of the set's helmet; or null */
        String armor() {
            return armor;
        }

        NpcMob mob() {
            return mob;
        }

        String gun() {
            return gun;
        }

        boolean front() {
            return front;
        }

        boolean aim() {
            return aim;
        }

        String label() {
            return label;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public ScenarioResult run(ScenarioContext ctx) {
        List<EntityPlayerMP> players = MinecraftServer.getServer()
            .getConfigurationManager().playerEntityList;
        if (players.isEmpty()) {
            return ScenarioResult.fail(NAME, "needs a connected player");
        }
        EntityPlayerMP player = players.get(0);
        for (String gun : GUNS) {
            if (GunItems.get(gun) == null) {
                return ScenarioResult.fail(NAME, "no gun named " + gun);
            }
        }
        List<Shot> shots = new ArrayList<>();
        for (int m = 0; m < MOBS.length; m++) {
            NpcMob mob = MOBS[m];
            for (String gun : GUNS) {
                for (boolean aim : new boolean[] { false, true }) {
                    String stem = "npc-" + mob.name() + "-" + gun + (aim ? "-aim" : "-idle");
                    shots.add(new Shot(mob, gun, false, aim, stem + "-side", MOB_ARMOR[m]));
                    shots.add(new Shot(mob, gun, true, aim, stem + "-front", MOB_ARMOR[m]));
                }
            }
        }
        for (String armor : SWEEP) {
            for (boolean aim : new boolean[] { false, true }) {
                String stem = "armor-" + armor.replace('+', '-') + (aim ? "-aim" : "-idle");
                shots.add(new Shot(NpcMob.ARMY_SOLDIER, SWEEP_GUN, false, aim, stem + "-side", armor));
                shots.add(new Shot(NpcMob.ARMY_SOLDIER, SWEEP_GUN, true, aim, stem + "-front", armor));
            }
        }
        for (String gun : GUNS) {
            shots.add(new Shot(null, gun, true, true, "player-" + gun + "-front", null));
            shots.add(new Shot(null, gun, false, true, "player-" + gun + "-back", null));
        }
        for (Shot s : shots) {
            if (s.armor() != null && wear(null, s.armor()) != null) {
                return ScenarioResult.fail(NAME, wear(null, s.armor()));
            }
        }
        ctx.clearArea(8, 4);
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                ctx.setBlock(dx, -1, dz, Blocks.stone, 0);
            }
        }
        player.inventory.clearInventory(null, -1);
        player.setGameType(WorldSettings.GameType.CREATIVE);
        double cx = ctx.originX() + 0.5;
        double cy = ctx.originY();
        double cz = ctx.originZ() + 0.5;
        // Hostile mobs despawn at once in a peaceful world; the scenario puts it back when it ends.
        EnumDifficulty prior = player.worldObj.difficultySetting;
        player.worldObj.difficultySetting = EnumDifficulty.NORMAL;
        // Noon for every shot, so the before and after runs of a change compare in the same light.
        long priorTime = player.worldObj.getWorldTime();
        signal(player, "hud", "off");
        EntityLiving[] current = { null };
        EntityLiving[] decoy = { null };
        return ScenarioResult.pending(NAME, shots.size() * STEP + 200, t -> {
            int index = t / STEP;
            int phase = t % STEP;
            if (index >= shots.size()) {
                if (current[0] != null) {
                    current[0].setDead();
                }
                if (decoy[0] != null) {
                    decoy[0].setDead();
                }
                player.worldObj.difficultySetting = prior;
                player.worldObj.setWorldTime(priorTime);
                signal(player, "view", "0");
                signal(player, "hud", "on");
                player.inventory.clearInventory(null, -1);
                player.setGameType(WorldSettings.GameType.SURVIVAL);
                return ScenarioResult.builder(NAME)
                    .note(shots.size() + " shots sent; read the client screenshots npc-*.png and player-*.png")
                    .build();
            }
            Shot s = shots.get(index);
            player.worldObj.setWorldTime(6000L);
            if (phase == 0) {
                if (current[0] != null) {
                    current[0].setDead();
                    current[0] = null;
                }
                if (decoy[0] != null) {
                    decoy[0].setDead();
                    decoy[0] = null;
                }
                ItemStack gun = new ItemStack(GunItems.get(s.gun()));
                if (s.mob() == null) {
                    player.inventory.setInventorySlotContents(0, gun);
                    player.inventory.currentItem = 0;
                    player.inventoryContainer.detectAndSendChanges();
                    player.playerNetServerHandler.setPlayerLocation(cx, cy, cz, 0.0F, 0.0F);
                    // The camera is in the player; third person view 2 looks back at the face, view 1 at the back.
                    signal(player, "view", s.front() ? "2" : "1");
                } else {
                    player.inventory.setInventorySlotContents(0, null);
                    player.inventoryContainer.detectAndSendChanges();
                    EntityLiving npc = (EntityLiving) EntityList.createEntityByName(
                        s.mob()
                            .fullName(),
                        player.worldObj);
                    npc.setLocationAndAngles(cx, cy, cz, 0.0F, 0.0F);
                    npc.setCurrentItemOrArmor(0, gun);
                    wear(npc, s.armor());
                    player.worldObj.spawnEntityInWorld(npc);
                    current[0] = npc;
                    if (s.aim()) {
                        // The target it aims at, 6 blocks ahead of it (+z), so it faces +z whichever way the camera is.
                        EntityLiving pig = new EntityPig(player.worldObj);
                        pig.setLocationAndAngles(cx, cy, cz + 6.0, 0.0F, 0.0F);
                        pig.getEntityAttribute(SharedMonsterAttributes.maxHealth)
                            .setBaseValue(1.0E6);
                        pig.setHealth(1.0E6F);
                        player.worldObj.spawnEntityInWorld(pig);
                        decoy[0] = pig;
                    }
                    signal(player, "view", "0");
                    // Side: 3 blocks to the NPC's left (+x), looking toward -x (yaw 90). Front: 3 blocks ahead (+z)
                    // looking toward -z (yaw 180).
                    if (s.front()) {
                        player.playerNetServerHandler.setPlayerLocation(cx, cy, cz + 3.0, 180.0F, 5.0F);
                    } else {
                        player.playerNetServerHandler.setPlayerLocation(cx + 3.0, cy, cz, 90.0F, 5.0F);
                    }
                }
            } else if (current[0] != null) {
                // Keep the NPC on its spot, facing +z, whatever its AI does.
                EntityLiving npc = current[0];
                npc.setLocationAndAngles(cx, cy, cz, 0.0F, 0.0F);
                npc.rotationYawHead = 0.0F;
                npc.prevRotationYawHead = 0.0F;
                npc.renderYawOffset = 0.0F;
                npc.prevRenderYawOffset = 0.0F;
                npc.prevRotationYaw = 0.0F;
                npc.motionX = 0.0;
                npc.motionZ = 0.0;
                // An NPC aims (arms up) while it has a target.
                if (decoy[0] != null) {
                    decoy[0].setLocationAndAngles(cx, cy, cz + 6.0, 0.0F, 0.0F);
                    decoy[0].setHealth(1.0E6F);
                }
                npc.setAttackTarget(decoy[0]);
            }
            if (phase == STEP - 8) {
                signal(player, "shot", s.label());
            }
            return null;
        });
    }

    /**
     * Puts every piece of an armor set on an NPC, or only checks that they are registered.
     *
     * @param npc   the NPC, or null to check only
     * @param armor a set id, optionally {@code <set>+<mask item>} for a mask in the helmet slot
     * @return null when every piece exists, else what is missing
     */
    private static String wear(EntityLiving npc, String armor) {
        int plus = armor.indexOf('+');
        String setId = plus < 0 ? armor : armor.substring(0, plus);
        SetDef set = ArmorCatalog.find(setId);
        if (set == null) {
            return "no armor set " + setId;
        }
        for (int type = 0; type < 4; type++) {
            ItemStack stack;
            if (type == 0 && plus >= 0) {
                Item mask = GameRegistry.findItem("gregguns", armor.substring(plus + 1));
                if (mask == null) {
                    return "no mask " + armor.substring(plus + 1);
                }
                stack = new ItemStack(mask);
            } else {
                ArmorSlot slot = ArmorSlot.fromArmorType(type);
                if (set.pieces()
                    .get(slot) == null) {
                    // A set without this piece (the Beret is a helmet only): the slot stays empty.
                    continue;
                }
                ItemGregGunsArmor piece = ArmorItems.get(set, slot);
                if (piece == null) {
                    return "armor set " + setId + " has no piece " + type;
                }
                stack = new ItemStack(piece);
                piece.setCamo(stack, 0);
            }
            if (npc != null) {
                // Vanilla equipment slot: 1 boots to 4 helmet.
                npc.setCurrentItemOrArmor(4 - type, stack);
            }
        }
        return null;
    }

    private static void signal(EntityPlayerMP player, String command, String... args) {
        player.addChatMessage(new ChatComponentText(AutotestSignal.format(command, args)));
    }
}
