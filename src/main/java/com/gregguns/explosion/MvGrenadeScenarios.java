package com.gregguns.explosion;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S09PacketHeldItemChange;
import net.minecraft.network.play.server.S39PacketPlayerAbilities;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;

import com.gregguns.core.config.ExplosionsConfig;
import com.gregguns.core.testing.AutotestSignal;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.item.grenade.GrenadeItems;
import com.gregguns.item.grenade.ItemThrownGrenade;
import com.gregguns.projectile.FlashbangProjectile;
import com.gregguns.projectile.FragGrenadeProjectile;
import com.gregguns.projectile.GrenadeProjectile;
import com.mojang.authlib.GameProfile;

/**
 * The scenarios of the MV grenades: {@code grenade.frag}, {@code grenade.flash}, {@code grenade.flash.noblocks}, and
 * the client walkthrough {@code mp.flashbang}. Dev environment only. Server side.
 */
// Spec: docs/gregguns-spec/04-projectiles-fx/grenades.md (Frag Grenade, Flashbang); docs/testing/README.md
public final class MvGrenadeScenarios {

    private static final int FRAG_FUSE = 6;
    private static final int FLASH_FUSE = 4;

    private MvGrenadeScenarios() {}

    /**
     * Registers the scenarios.
     *
     * @param registry the registry to add to
     */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new Frag());
        registry.register(new Flash());
        registry.register(new FlashNoBlocks());
        registry.register(new FlashbangVisual());
    }

    private static EntityCow cow(ScenarioContext ctx, double dx, double dz) {
        EntityCow cow = new EntityCow(ctx.world());
        cow.setLocationAndAngles(ctx.originX() + 0.5 + dx, ctx.originY(), ctx.originZ() + 0.5 + dz, 0.0F, 0.0F);
        return ctx.spawn(cow);
    }

    /**
     * A player-like entity that is not in the world: no network handler, so it can be built where a real one cannot.
     */
    private static FakePlayer fake(ScenarioContext ctx, String name, double dx, double yaw) {
        FakePlayer p = new FakePlayer(
            (WorldServer) ctx.world(),
            new GameProfile(UUID.nameUUIDFromBytes(name.getBytes()), name));
        p.setLocationAndAngles(ctx.originX() + 0.5 + dx, ctx.originY(), ctx.originZ() + 0.5, (float) yaw, 0.0F);
        return p;
    }

    private static PotionEffect effect(EntityLivingBase e, Potion potion) {
        return e.getActivePotionEffect(potion);
    }

    /** The Frag Grenade blasts with falloff and fragments, and leaves what is out of range alone. */
    private static final class Frag implements Scenario {

        @Override
        public String name() {
            return "grenade.frag";
        }

        @Override
        public String description() {
            return "A Frag Grenade at rest explodes: zombies at 1.5 and 5 blocks lose at least the 25 damage / radius 6 blast figure through the damage model, the near one more than the far one, the near one also takes fragment damage on top of the blast, and the floor is intact.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GrenadeScenarios.chamber(ctx);
            EntityZombie near = GrenadeScenarios.dummy(ctx, 1.5);
            EntityZombie mid = GrenadeScenarios.dummy(ctx, 5.0);
            EntityZombie thrower = GrenadeScenarios.dummy(ctx, -2.0);
            FragGrenadeProjectile grenade = new FragGrenadeProjectile(ctx.world());
            grenade.throwFrom(thrower, 1.0, 25.0F, 6.0F, 2, "frag_grenade");
            grenade.setLocationAndAngles(ctx.originX() + 0.5, ctx.originY() + 0.05, ctx.originZ() + 0.5, 0.0F, 0.0F);
            grenade.motionX = 0.0;
            grenade.motionY = 0.0;
            grenade.motionZ = 0.0;
            grenade.setFuse(FRAG_FUSE);
            ctx.spawn(grenade);
            return ScenarioResult.pending(name(), 60, ticks -> {
                if (!grenade.isDead) {
                    return null;
                }
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                double dNear = distance(near, grenade);
                double dMid = distance(mid, grenade);
                float blastNear = GrenadeScenarios.expectedLoss(near, BlastFalloff.damage(25.0, 3.0, 9.0, dNear));
                float blastMid = GrenadeScenarios.expectedLoss(mid, BlastFalloff.damage(25.0, 3.0, 9.0, dMid));
                out.check(
                    GrenadeScenarios.lost(near) >= blastNear - GrenadeScenarios.EPS,
                    "near (" + dNear
                        + ") should lose at least the blast "
                        + blastNear
                        + ", lost "
                        + GrenadeScenarios.lost(near));
                out.check(
                    GrenadeScenarios.lost(near) > blastNear + 0.4F,
                    "near should also take fragment damage on top of the blast " + blastNear
                        + ", lost "
                        + GrenadeScenarios.lost(near));
                out.check(
                    GrenadeScenarios.lost(mid) >= blastMid - GrenadeScenarios.EPS,
                    "mid (" + dMid
                        + ") should lose at least the blast "
                        + blastMid
                        + ", lost "
                        + GrenadeScenarios.lost(mid));
                out.check(
                    GrenadeScenarios.lost(near) > GrenadeScenarios.lost(mid) && GrenadeScenarios.lost(mid) > 0.0F,
                    "damage should fall with distance: near " + GrenadeScenarios.lost(near)
                        + ", mid "
                        + GrenadeScenarios.lost(mid));
                out.check(
                    ctx.world()
                        .getBlock(ctx.originX(), ctx.originY() - 1, ctx.originZ()) == Blocks.stone,
                    "the Frag Grenade should not break blocks");
                return out.build();
            });
        }

        private static double distance(EntityLivingBase e, GrenadeProjectile g) {
            double dx = e.posX - g.posX;
            double dy = e.posY - g.posY;
            double dz = e.posZ - g.posZ;
            return Math.sqrt(dx * dx + dy * dy + dz * dz);
        }
    }

    /**
     * The Flashbang blinds a player who faces it, spares one who turns away, slows mobs in view, and respects walls.
     */
    private static final class Flash implements Scenario {

        @Override
        public String name() {
            return "grenade.flash";
        }

        @Override
        public String description() {
            return "A Flashbang at rest goes off: a player facing it 4 blocks away would get about 80 ticks of Blindness and 40 of Nausea, one facing away none (measured, not applied: a fake player has no connection), a cow at 2 blocks gets heavy Slowness and Weakness, one at 6.5 light Slowness, one behind a stone wall and one past 8 blocks get nothing, and nobody loses health.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GrenadeScenarios.chamber(ctx);
            // A stone wall across the +z side, 3 blocks out and 4 high.
            for (int dx = -3; dx <= 3; dx++) {
                for (int dy = 0; dy <= 4; dy++) {
                    ctx.setBlock(dx, dy, 3, Blocks.stone, 0);
                }
            }
            EntityCow close = cow(ctx, 2.0, 0.0);
            EntityCow edge = cow(ctx, -6.5, 0.0);
            EntityCow walled = cow(ctx, 0.0, 5.5);
            EntityCow out = cow(ctx, 0.0, -8.5);
            FlashbangProjectile grenade = new FlashbangProjectile(ctx.world());
            grenade.setLocationAndAngles(ctx.originX() + 0.5, ctx.originY() + 1.0, ctx.originZ() + 0.5, 0.0F, 0.0F);
            grenade.setFuse(FLASH_FUSE);
            ctx.spawn(grenade);
            return ScenarioResult.pending(name(), 60, ticks -> {
                if (!grenade.isDead) {
                    return null;
                }
                ScenarioResult.Builder result = ScenarioResult.builder(name());
                // Looking along -x (yaw 90): the player at +4 faces the blast, the one at -4 has it behind.
                FakePlayer facing = fake(ctx, "[GregGunsFlashFront]", 4.0, 90.0);
                FakePlayer away = fake(ctx, "[GregGunsFlashBack]", -4.0, 90.0);
                double bx = grenade.posX;
                double by = grenade.posY;
                double bz = grenade.posZ;
                int frontTicks = FlashMath.effectTicks(FlashBlast.intensityAt(ctx.world(), facing, bx, by, bz));
                int backTicks = FlashMath.effectTicks(FlashBlast.intensityAt(ctx.world(), away, bx, by, bz));
                // A player outside the world has no connection for the potion packet, so the player part checks the
                // durations the blast would apply; the potions themselves are vanilla's and the mobs below show them.
                result.check(
                    frontTicks >= 70 && frontTicks <= 90,
                    "a player facing the flash at 4 blocks should get about 80 ticks, got " + frontTicks);
                result.check(
                    FlashMath.nauseaTicks(frontTicks) >= 35 && FlashMath.nauseaTicks(frontTicks) <= 45,
                    "nausea should be about 40 ticks");
                result.check(
                    backTicks == 0,
                    "a player with their back to the flash at 4 blocks should get none, got " + backTicks);
                PotionEffect slow = effect(close, Potion.moveSlowdown);
                result.check(
                    slow != null && slow.getAmplifier() == 3 && effect(close, Potion.weakness) != null,
                    "a cow at 2 blocks should have heavy Slowness and Weakness");
                PotionEffect slowEdge = effect(edge, Potion.moveSlowdown);
                result.check(
                    slowEdge != null && slowEdge.getAmplifier() == 1,
                    "a cow at 6.5 blocks should have light Slowness, was "
                        + (slowEdge == null ? "none" : slowEdge.getAmplifier()));
                result.check(
                    effect(walled, Potion.moveSlowdown) == null,
                    "a cow behind the stone wall should be unaffected");
                result.check(effect(out, Potion.moveSlowdown) == null, "a cow past 8 blocks should be unaffected");
                for (EntityCow c : new EntityCow[] { close, edge, walled, out }) {
                    result.check(
                        c.getHealth() == c.getMaxHealth(),
                        "a flash must not hurt a cow, health " + c.getHealth());
                }
                return result.build();
            });
        }
    }

    /** The Flashbang breaks nothing even with block damage on. */
    private static final class FlashNoBlocks implements Scenario {

        @Override
        public String name() {
            return "grenade.flash.noblocks";
        }

        @Override
        public String description() {
            return "With explosions.blockDamage on, a Flashbang goes off beside a stone pillar and a glass pane: every block in a 11 by 7 by 11 box is unchanged afterwards.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GrenadeScenarios.chamber(ctx);
            ctx.setBlock(1, 0, 0, Blocks.stone, 0);
            ctx.setBlock(1, 1, 0, Blocks.stone, 0);
            ctx.setBlock(-1, 0, 1, Blocks.glass_pane, 0);
            ctx.setBlock(0, 0, -1, Blocks.dirt, 0);
            EntityCow near = cow(ctx, 2.0, 2.0);
            String before = snapshot(ctx);
            boolean saved = ExplosionsConfig.blockDamage;
            ExplosionsConfig.blockDamage = true;
            FlashbangProjectile grenade = new FlashbangProjectile(ctx.world());
            grenade.setLocationAndAngles(ctx.originX() + 0.5, ctx.originY() + 0.4, ctx.originZ() + 0.5, 0.0F, 0.0F);
            grenade.setFuse(FLASH_FUSE);
            ctx.spawn(grenade);
            return ScenarioResult.pending(name(), 60, ticks -> {
                if (!grenade.isDead) {
                    return null;
                }
                ExplosionsConfig.blockDamage = saved;
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.check(before.equals(snapshot(ctx)), "no block within the flash should have changed");
                out.check(near.getHealth() == near.getMaxHealth(), "the cow should be unhurt");
                out.check(effect(near, Potion.moveSlowdown) != null, "the flash should still have gone off");
                return out.build();
            });
        }

        private static String snapshot(ScenarioContext ctx) {
            StringBuilder sb = new StringBuilder();
            for (int dx = -5; dx <= 5; dx++) {
                for (int dy = -2; dy <= 4; dy++) {
                    for (int dz = -5; dz <= 5; dz++) {
                        Block b = ctx.world()
                            .getBlock(ctx.originX() + dx, ctx.originY() + dy, ctx.originZ() + dz);
                        sb.append(Block.getIdFromBlock(b))
                            .append(',');
                    }
                }
            }
            return sb.toString();
        }
    }

    private static void signal(EntityPlayerMP player, String command, String... args) {
        player.addChatMessage(new ChatComponentText(AutotestSignal.format(command, args)));
    }

    private static void select(EntityPlayerMP player, int slot) {
        player.inventory.currentItem = slot;
        player.playerNetServerHandler.sendPacket(new S09PacketHeldItemChange(slot));
    }

    /** Screenshots of the held grenades, a thrown flashbang going off in view, and a frag. Needs one client. */
    private static final class FlashbangVisual implements Scenario {

        private static final int THROW = 45;
        private static final int FUSE = 40;
        private static final int NEAR = THROW + FUSE + 30;
        private static final int NEAR_FUSE = 20;
        private static final int FRAG_THROW = 330;
        private static final int FRAG_FUSE_TICKS = 30;

        @Override
        public boolean includedInAll() {
            return false; // needs a connected client: run by name from the visual check
        }

        @Override
        public String name() {
            return "mp.flashbang";
        }

        @Override
        public String description() {
            return "Gives the first connected player a Frag Grenade and a Flashbang stack, signals screenshots of each held item, throws a flashbang through the item's release path (one screenshot of it in flight), sets one off two blocks in front of the player and signals screenshots of the white-out, two stages of the fade and the blind aftermath, then throws a frag. Needs 1 client.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            List<EntityPlayerMP> players = new ArrayList<>(
                MinecraftServer.getServer()
                    .getConfigurationManager().playerEntityList);
            if (players.isEmpty()) {
                return ScenarioResult.fail(name(), "needs a connected player");
            }
            EntityPlayerMP a = players.get(0);
            GrenadeScenarios.chamber(ctx);
            a.inventory.clearInventory(null, -1);
            a.setHealth(a.getMaxHealth());
            a.inventory.mainInventory[0] = new ItemStack(GrenadeItems.fragGrenade(), 16);
            a.inventory.mainInventory[1] = new ItemStack(GrenadeItems.flashbang(), 16);
            a.playerNetServerHandler
                .setPlayerLocation(ctx.originX() + 0.5, ctx.originY(), ctx.originZ() + 0.5, -90.0F, -5.0F);
            a.inventoryContainer.detectAndSendChanges();
            boolean savedDisable = a.capabilities.disableDamage;
            // The frag lands a few blocks away and would hurt; the screenshot is of the blast, not of a respawn.
            a.capabilities.disableDamage = true;
            a.playerNetServerHandler.sendPacket(new S39PacketPlayerAbilities(a.capabilities));
            return ScenarioResult.pending(name(), 500, t -> {
                switch (t) {
                    case 4:
                        select(a, 0);
                        break;
                    case 15:
                        signal(a, "shot", "f-held-frag");
                        break;
                    case 18:
                        select(a, 1);
                        break;
                    case 29:
                        signal(a, "shot", "f-held-flashbang");
                        break;
                    case THROW:
                        throwHeld(ctx, a, FUSE);
                        break;
                    case THROW + 8:
                        signal(a, "shot", "f-thrown");
                        break;
                    case NEAR:
                        placeNear(ctx, a, NEAR_FUSE);
                        break;
                    case NEAR + NEAR_FUSE + 2:
                        signal(a, "shot", "f-flash");
                        break;
                    case NEAR + NEAR_FUSE + 22:
                        signal(a, "shot", "f-fade-1");
                        break;
                    case NEAR + NEAR_FUSE + 60:
                        signal(a, "shot", "f-fade-2");
                        break;
                    case NEAR + NEAR_FUSE + 130:
                        signal(a, "shot", "f-aftermath");
                        break;
                    case FRAG_THROW - 2:
                        select(a, 0);
                        break;
                    case FRAG_THROW:
                        throwHeld(ctx, a, FRAG_FUSE_TICKS);
                        break;
                    case FRAG_THROW + 12:
                        signal(a, "shot", "f-frag-landed");
                        break;
                    case FRAG_THROW + FRAG_FUSE_TICKS + 1:
                        signal(a, "shot", "f-frag-boom");
                        break;
                    default:
                        break;
                }
                if (t < FRAG_THROW + FRAG_FUSE_TICKS + 20) {
                    return null;
                }
                a.capabilities.disableDamage = savedDisable;
                a.playerNetServerHandler.sendPacket(new S39PacketPlayerAbilities(a.capabilities));
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                ItemStack frag = a.inventory.mainInventory[0];
                ItemStack flash = a.inventory.mainInventory[1];
                out.check(frag != null && frag.stackSize == 15, "one frag grenade should have been used");
                out.check(flash != null && flash.stackSize == 15, "one flashbang should have been used");
                return out.build();
            });
        }

        /** Puts a flashbang on the floor two blocks in front of the player, so the white-out is at full strength. */
        private static void placeNear(ScenarioContext ctx, EntityPlayerMP a, int fuse) {
            FlashbangProjectile near = new FlashbangProjectile(ctx.world());
            near.setLocationAndAngles(a.posX + 2.0, a.posY + 0.3, a.posZ, 0.0F, 0.0F);
            near.setFuse(fuse);
            ctx.spawn(near);
        }

        /** Releases the held grenade after a short wind-up (a gentle lob) and sets its fuse. */
        private static void throwHeld(ScenarioContext ctx, EntityPlayerMP a, int fuse) {
            ItemStack held = a.getHeldItem();
            ((ItemThrownGrenade) held.getItem())
                .onPlayerStoppedUsing(held, ctx.world(), a, ItemThrownGrenade.USE_DURATION - 6);
            for (Object o : ctx.world().loadedEntityList) {
                if (o instanceof GrenadeProjectile && ((GrenadeProjectile) o).getFuseLeft() > fuse) {
                    ((GrenadeProjectile) o).setFuse(fuse);
                }
            }
        }
    }
}
