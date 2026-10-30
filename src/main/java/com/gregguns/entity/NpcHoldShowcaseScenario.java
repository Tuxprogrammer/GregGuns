package com.gregguns.entity;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.WorldSettings;

import com.gregguns.core.testing.AutotestSignal;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.entity.npc.NpcMob;
import com.gregguns.item.gun.GunItems;

/**
 * {@code npc.hold_showcase}: a visual check of how NPCs hold guns, for a connected client (see
 * {@code docs/testing/README.md}). Each step stands one NPC on a fixed spot holding one gun, puts the (creative,
 * empty-handed) player beside it or in front of it and asks the client for a screenshot labeled
 * {@code npc-<mob>-<gun>-<side|front>}. Then the player holds each gun and is shot from the front and the back
 * ({@code player-<gun>-<front|back>}) as the reference for what a held gun looks like. Not part of {@code test all}:
 * it needs a connected client. Dev environment only, server side.
 */
// Spec: docs/techguns-spec/03-gun-visuals-audio/ (framework section 6.3, the aimed pre-rotation)
final class NpcHoldShowcaseScenario implements Scenario {

    private static final String NAME = "npc.hold_showcase";
    /** Ticks between two steps: the client needs the teleport and the entity update before the frame is shot. */
    private static final int STEP = 30;
    private static final NpcMob[] MOBS = { NpcMob.ZOMBIE_SOLDIER, NpcMob.SKELETON_SOLDIER, NpcMob.BANDIT,
        NpcMob.ARMY_SOLDIER, NpcMob.COMMANDO, NpcMob.OUTCAST };
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
        private final String label;

        Shot(NpcMob mob, String gun, boolean front, String label) {
            this.mob = mob;
            this.gun = gun;
            this.front = front;
            this.label = label;
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
        for (NpcMob mob : MOBS) {
            for (String gun : GUNS) {
                shots.add(new Shot(mob, gun, false, "npc-" + mob.name() + "-" + gun + "-side"));
                shots.add(new Shot(mob, gun, true, "npc-" + mob.name() + "-" + gun + "-front"));
            }
        }
        for (String gun : GUNS) {
            shots.add(new Shot(null, gun, true, "player-" + gun + "-front"));
            shots.add(new Shot(null, gun, false, "player-" + gun + "-back"));
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
        signal(player, "hud", "off");
        EntityLiving[] current = { null };
        return ScenarioResult.pending(NAME, shots.size() * STEP + 200, t -> {
            int index = t / STEP;
            int phase = t % STEP;
            if (index >= shots.size()) {
                if (current[0] != null) {
                    current[0].setDead();
                }
                player.worldObj.difficultySetting = prior;
                signal(player, "view", "0");
                signal(player, "hud", "on");
                player.inventory.clearInventory(null, -1);
                player.setGameType(WorldSettings.GameType.SURVIVAL);
                return ScenarioResult.builder(NAME)
                    .note(shots.size() + " shots sent; read the client screenshots npc-*.png and player-*.png")
                    .build();
            }
            Shot s = shots.get(index);
            if (phase == 0) {
                if (current[0] != null) {
                    current[0].setDead();
                    current[0] = null;
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
                    player.worldObj.spawnEntityInWorld(npc);
                    current[0] = npc;
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
                npc.renderYawOffset = 0.0F;
                npc.motionX = 0.0;
                npc.motionZ = 0.0;
            }
            if (phase == STEP - 8) {
                signal(player, "shot", s.label());
            }
            return null;
        });
    }

    private static void signal(EntityPlayerMP player, String command, String... args) {
        player.addChatMessage(new ChatComponentText(AutotestSignal.format(command, args)));
    }
}
