package com.gregguns.gun.server;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.util.FakePlayer;

import com.gregguns.combat.DamageFactors;
import com.gregguns.combat.DamageModel;
import com.gregguns.combat.DamageSources;
import com.gregguns.combat.DamageType;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.gun.logic.GunCatalogue;
import com.gregguns.gun.logic.GunNbt;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.item.ammo.AmmoTypes;
import com.gregguns.item.gun.GunItems;
import com.gregguns.item.gun.PlayerAmmoStore;
import com.gregguns.projectile.GenericProjectile;
import com.mojang.authlib.GameProfile;

import cpw.mods.fml.common.registry.GameRegistry;

/**
 * The {@code gun.*} in-game scenarios: a {@link FakePlayer} holding a real {@code pistol_9mm} stack is driven through
 * the same {@link ServerGunHandler} entry points the trigger, zoom and reload packets reach ({@code onTrigger},
 * {@code onZoomToggle}, {@code onReloadKey}) and the same per-tick method the player tick event calls
 * ({@code onPlayerTick}). Nothing about firing is re-implemented: the scenarios only stand in for the network and for
 * the world's player tick, which a fake player does not get. No seam was needed in the handler, because its entry
 * points already take an {@code EntityPlayerMP}, which {@code FakePlayer} is. Dev environment only, server side.
 */
// Spec: docs/gregguns-spec/02-guns/index.md, docs/techguns-spec/02-guns/gun-lifecycle.md; docs/testing/README.md
public final class GunScenarios {

    private static final float EPS = 0.05F;
    private static final float EAST_YAW = -90.0F;
    private static final String SHOOTER_NAME = "[GregGunsGunTest]";
    private static final String TARGET_NAME = "[GregGunsGunTarget]";

    private GunScenarios() {}

    /**
     * Registers the gun scenarios.
     *
     * @param registry where to add them
     */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new Fire());
        registry.register(new Reload());
        registry.register(new ForcedReload());
        registry.register(new DryFire());
        registry.register(new HitsPlayer());
        registry.register(new ZoomNoOp());
    }

    // ----- rig -------------------------------------------------------------------------------------------------

    /** A shooter holding the pistol in hotbar slot 0, east-facing, with a corridor in front of it. */
    private static final class Rig {

        final ScenarioContext ctx;
        final FakePlayer shooter;
        final ItemStack gun;
        private final Map<GenericProjectile, Boolean> seen = new IdentityHashMap<>();
        int shots;
        private int lastLoaded;

        Rig(ScenarioContext ctx, int loaded) {
            this.ctx = ctx;
            for (int dx = -1; dx <= 12; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    ctx.setBlock(dx, -1, dz, Blocks.stone, 0);
                    for (int dy = 0; dy <= 3; dy++) {
                        ctx.setBlock(dx, dy, dz, Blocks.air, 0);
                    }
                }
            }
            shooter = fakePlayer(ctx, SHOOTER_NAME, 0);
            gun = new ItemStack(GunItems.pistol());
            NBTTagCompound tag = new NBTTagCompound();
            tag.setShort(GunNbt.KEY_AMMO, (short) loaded);
            tag.setByte(GunNbt.KEY_CAMO, (byte) 0);
            gun.setTagCompound(tag);
            shooter.inventory.currentItem = 0;
            shooter.inventory.setInventorySlotContents(0, gun);
            lastLoaded = loaded;
        }

        /** Puts items into the shooter's inventory after the gun. */
        void give(String registryName, int count) {
            Item item = GameRegistry.findItem("gregguns", registryName);
            shooter.inventory.addItemStackToInventory(new ItemStack(item, count));
        }

        int count(String registryName) {
            return new PlayerAmmoStore(shooter).count(registryName);
        }

        int loaded() {
            return GunNbt.read(gun.getTagCompound(), GunCatalogue.PISTOL)
                .loaded();
        }

        boolean reloading() {
            return GunPlayerData.get(shooter)
                .controller()
                .reload()
                .isReloading();
        }

        void press() {
            ServerGunHandler.onTrigger(shooter, true);
        }

        void release() {
            ServerGunHandler.onTrigger(shooter, false);
        }

        /** One server tick of the gun logic (what the player tick event does), then a note of new bullets. */
        void tick() {
            ServerGunHandler.onPlayerTick(shooter);
            int now = loaded();
            if (now < lastLoaded) {
                shots += lastLoaded - now;
            }
            lastLoaded = now;
            for (Object o : ctx.world().loadedEntityList) {
                if (o instanceof GenericProjectile) {
                    seen.putIfAbsent((GenericProjectile) o, Boolean.TRUE);
                }
            }
        }

        int bullets() {
            return seen.size();
        }

        boolean bulletsGone() {
            for (GenericProjectile p : seen.keySet()) {
                if (!p.isDead) {
                    return false;
                }
            }
            return true;
        }
    }

    /** A fake player that can be hurt: a fresh server player is invulnerable for its first ticks. */
    private static final class Mortal extends FakePlayer {

        Mortal(WorldServer world, GameProfile profile) {
            super(world, profile);
            // The spawn-protection counter (ticks of immunity after joining); a fake player is never ticked down.
            clearSpawnProtection();
        }

        @Override
        public boolean isEntityInvulnerable() {
            return false;
        }

        @Override
        public boolean canAttackPlayer(EntityPlayer other) {
            // Forge's FakePlayer refuses all player-versus-player damage; the scenario is exactly that case.
            return true;
        }

        private void clearSpawnProtection() {
            try {
                // field_147101_bU is private in EntityPlayerMP and has no accessor; the dev environment keeps SRG
                // names.
                java.lang.reflect.Field field = EntityPlayerMP.class.getDeclaredField("field_147101_bU");
                field.setAccessible(true);
                field.setInt(this, 0);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("cannot clear the fake player's spawn protection", e);
            }
        }
    }

    private static FakePlayer fakePlayer(ScenarioContext ctx, String name, int dx) {
        FakePlayer fake = new Mortal(
            (WorldServer) ctx.world(),
            new GameProfile(UUID.nameUUIDFromBytes(name.getBytes()), name));
        fake.setLocationAndAngles(ctx.originX() + 0.5 + dx, ctx.originY(), ctx.originZ() + 0.5, EAST_YAW, 0.0F);
        fake.rotationYawHead = EAST_YAW;
        return fake;
    }

    private static EntityZombie zombie(ScenarioContext ctx, int dx) {
        EntityZombie z = new EntityZombie(ctx.world());
        z.setLocationAndAngles(ctx.originX() + 0.5 + dx, ctx.originY(), ctx.originZ() + 0.5, 90.0F, 0.0F);
        // A pumpkin is not armour, but it stops the zombie burning in daylight.
        z.setCurrentItemOrArmor(4, new ItemStack(Blocks.pumpkin));
        return ctx.spawn(z);
    }

    private static float expectedLoss(EntityLivingBase target, double rawDamage) {
        double armour = target.getTotalArmorValue();
        return (float) DamageModel
            .resolve(DamageType.PROJECTILE, rawDamage, 0.0, share -> share * DamageModel.vanillaArmourFactor(armour));
    }

    private static boolean near(float expected, float actual) {
        return Math.abs(expected - actual) < EPS;
    }

    // ----- scenarios -------------------------------------------------------------------------------------------

    /** Fire at a zombie: bullet launched, damage per the model, ammo down by one, cooldown holds, buffered click. */
    private static final class Fire implements Scenario {

        private static final int RANGE = 6;
        private static final int END_TICK = 30;

        @Override
        public String name() {
            return "gun.fire";
        }

        @Override
        public String description() {
            return "A loaded pistol fired through the server handler launches a bullet, costs a zombie the DamageModel figure and one round, and holds the 4 tick cooldown (GUN-29 buffer).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            Rig rig = new Rig(ctx, GunCatalogue.PISTOL.magazineSize());
            EntityZombie target = zombie(ctx, RANGE);
            float[] firstHit = { Float.NaN };
            int[] shotsAt = new int[END_TICK + 1];

            rig.press();
            rig.release();
            rig.tick(); // tick 0: the shot
            int afterFirst = rig.loaded();
            int bulletsFirst = rig.bullets();

            return ScenarioResult.pending(name(), END_TICK + 10, ticks -> {
                // 'ticks' counts the ticks after the shot.
                if (ticks == 1) {
                    rig.press(); // a click inside the cooldown, too early to be remembered until it clears
                    rig.release();
                }
                if (ticks == 3) {
                    rig.press(); // remembered: fires the tick the cooldown clears (tick 4)
                    rig.release();
                }
                rig.tick();
                shotsAt[Math.min(ticks, END_TICK)] = rig.shots;
                if (Float.isNaN(firstHit[0]) && target.getHealth() < target.getMaxHealth()) {
                    firstHit[0] = target.getMaxHealth() - target.getHealth();
                }
                if (ticks < END_TICK) {
                    return null;
                }
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                float expected = expectedLoss(target, GunCatalogue.PISTOL.damage());
                out.checkEquals("rounds after the first shot", 17, afterFirst);
                out.checkEquals("bullets launched by the first shot", 1, bulletsFirst);
                out.check(
                    !Float.isNaN(firstHit[0]) && near(expected, firstHit[0]),
                    "zombie should lose " + expected + " from the first bullet but lost " + firstHit[0]);
                out.checkEquals("shots fired 1 tick later (click inside the cooldown)", 1, shotsAt[1]);
                out.checkEquals("shots fired 3 ticks later (still cooling down)", 1, shotsAt[3]);
                out.checkEquals("shots fired 4 ticks later (the remembered click)", 2, shotsAt[4]);
                out.checkEquals("rounds at the end", 16, rig.loaded());
                out.checkEquals("bullets launched in all", 2, rig.bullets());
                return out.build();
            });
        }
    }

    /** Empty pistol with a magazine: the dry trigger starts a 35 tick reload, then 18 rounds and an empty magazine. */
    private static final class Reload implements Scenario {

        @Override
        public String name() {
            return "gun.reload";
        }

        @Override
        public String description() {
            return "An empty pistol reloads 35 ticks after the trigger: 18 rounds, the magazine consumed, an empty magazine returned.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            Rig rig = new Rig(ctx, 0);
            String loadedMag = AmmoTypes.NINE_MM_PISTOL.loadedItemName();
            rig.give(loadedMag, 1);
            int reloadTicks = GunCatalogue.PISTOL.reloadTicks();
            rig.press();
            rig.release();
            rig.tick(); // tick 0: empty, so the reload starts
            boolean started = rig.reloading();
            int[] loadedAt = new int[reloadTicks + 3];
            int[] magsAt = new int[reloadTicks + 3];
            return ScenarioResult.pending(name(), reloadTicks + 20, ticks -> {
                rig.tick();
                if (ticks < loadedAt.length) {
                    loadedAt[ticks] = rig.loaded();
                    magsAt[ticks] = rig.count(loadedMag);
                }
                if (ticks < reloadTicks + 2) {
                    return null;
                }
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.check(started, "the dry trigger should have started a reload");
                out.checkEquals("rounds one tick before the end", 0, loadedAt[reloadTicks - 1]);
                out.checkEquals("magazine kept until the swap", 1, magsAt[reloadTicks - 1]);
                out.checkEquals("rounds at the 35th tick", 18, loadedAt[reloadTicks]);
                out.checkEquals("loaded magazines left", 0, rig.count(loadedMag));
                out.checkEquals("empty magazines returned", 1, rig.count(AmmoTypes.NINE_MM_PISTOL.emptyItemName()));
                out.checkEquals("bullets launched", 0, rig.bullets());
                return out.build();
            });
        }
    }

    /** GUN-13: a forced reload with rounds loaded keeps them, to whole-bullet rounding. */
    private static final class ForcedReload implements Scenario {

        @Override
        public String name() {
            return "gun.reload.forced";
        }

        @Override
        public String description() {
            return "The reload key on a pistol with 7 rounds swaps the magazine after 35 ticks and refunds floor(7/6)=1 bullet (GUN-13); the leftover round is the documented rounding.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            Rig rig = new Rig(ctx, 7);
            rig.give(AmmoTypes.NINE_MM_PISTOL.loadedItemName(), 1);
            int reloadTicks = GunCatalogue.PISTOL.reloadTicks();
            rig.tick(); // registers the held gun with the handler
            ServerGunHandler.onReloadKey(rig.shooter);
            boolean started = rig.reloading();
            return ScenarioResult.pending(name(), reloadTicks + 20, ticks -> {
                rig.tick();
                if (ticks < reloadTicks + 1) {
                    return null;
                }
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.check(started, "the reload key should start a forced reload");
                out.checkEquals("rounds after the swap", 18, rig.loaded());
                out.checkEquals("loose bullets refunded", 1, rig.count(AmmoTypes.NINE_MM.roundItemName()));
                out.checkEquals("empty magazines returned", 1, rig.count(AmmoTypes.NINE_MM_PISTOL.emptyItemName()));
                out.checkEquals("loaded magazines left", 0, rig.count(AmmoTypes.NINE_MM_PISTOL.loadedItemName()));
                return out.build();
            });
        }
    }

    /** No rounds and no magazines: the trigger does nothing and nothing throws. */
    private static final class DryFire implements Scenario {

        @Override
        public String name() {
            return "gun.dryfire";
        }

        @Override
        public String description() {
            return "An empty pistol with no magazines fires nothing, starts no reload, loses no state and does not crash.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            Rig rig = new Rig(ctx, 0);
            rig.press();
            rig.release();
            rig.tick();
            return ScenarioResult.pending(name(), 20, ticks -> {
                if (ticks == 2) {
                    rig.press();
                    rig.release();
                }
                rig.tick();
                if (ticks < 10) {
                    return null;
                }
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.checkEquals("rounds", 0, rig.loaded());
                out.checkEquals("bullets launched", 0, rig.bullets());
                out.check(!rig.reloading(), "no reload should start without a magazine");
                return out.build();
            });
        }
    }

    /** A shot at a second fake player damages them. */
    private static final class HitsPlayer implements Scenario {

        @Override
        public String name() {
            return "gun.player";
        }

        @Override
        public String description() {
            return "A shot from one fake player hits another fake player 6 blocks away and costs them the DamageModel figure.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            Rig rig = new Rig(ctx, GunCatalogue.PISTOL.magazineSize());
            // A fake player has no network handler, so the entity tracker would crash on it if it were spawned through
            // the world. Put it into its chunk only: hit tests find it there, and it is never ticked or tracked.
            FakePlayer victim = fakePlayer(ctx, TARGET_NAME, 6);
            Chunk chunk = ctx.world()
                .getChunkFromBlockCoords((int) victim.posX, (int) victim.posZ);
            chunk.addEntity(victim);
            float before = victim.getHealth();
            rig.press();
            rig.release();
            rig.tick();
            return ScenarioResult.pending(name(), 40, ticks -> {
                rig.tick();
                if (!rig.bulletsGone() && ticks < 35) {
                    return null;
                }
                chunk.removeEntity(victim);
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                float lost = before - victim.getHealth();
                // Player-on-player hits carry the configured PvP damage factor (DamageFactorsConfig, synced).
                double pvpFactor = DamageFactors.damageFactor(
                    DamageSources.settingsFromConfig(),
                    DamageFactors.Attacker.PLAYER,
                    true,
                    MinecraftServer.getServer()
                        .isPVPEnabled());
                float expected = expectedLoss(victim, (float) (GunCatalogue.PISTOL.damage() * pvpFactor));
                out.check(near(expected, lost), "the player should lose " + expected + " but lost " + lost);
                out.checkEquals("bullets launched", 1, rig.bullets());
                return out.build();
            });
        }
    }

    /** The unscoped pistol accepts a zoom intent and nothing changes. */
    private static final class ZoomNoOp implements Scenario {

        @Override
        public String name() {
            return "gun.zoom";
        }

        @Override
        public String description() {
            return "A zoom intent on the unscoped pistol is accepted and leaves the zoom state off and the FOV factor at 1.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            Rig rig = new Rig(ctx, GunCatalogue.PISTOL.magazineSize());
            rig.tick(); // registers the held gun with the handler
            ServerGunHandler.onZoomToggle(rig.shooter);
            rig.tick();
            GunStats stats = GunCatalogue.PISTOL;
            GunPlayerData data = GunPlayerData.get(rig.shooter);
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            out.check(
                !data.controller()
                    .zoom()
                    .isZoomed(stats),
                "an unscoped gun must not zoom");
            out.check(
                data.controller()
                    .zoom()
                    .fovMultiplier(stats) == 1.0,
                "the FOV factor should stay 1");
            out.checkEquals("rounds untouched", stats.magazineSize(), rig.loaded());
            return out.build();
        }
    }
}
