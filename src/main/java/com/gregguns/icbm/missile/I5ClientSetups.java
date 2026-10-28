package com.gregguns.icbm.missile;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;

import com.gregguns.GregGuns;
import com.gregguns.core.config.ExplosionsConfig;
import com.gregguns.core.config.IcbmConfig;
import com.gregguns.core.testing.AutotestSignal;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.explosion.nuclear.FalloutBlocks;
import com.gregguns.gun.def.NukeLauncherDef;
import com.gregguns.gun.def.RocketLauncherDef;
import com.gregguns.gun.logic.GunCatalog.AmmoIds;
import com.gregguns.icbm.blast.BlastEngine;
import com.gregguns.icbm.emp.EmpScenarios;
import com.gregguns.icbm.explosive.ExplosiveDef;
import com.gregguns.icbm.explosive.Explosives;
import com.gregguns.icbm.explosive.IcbmBlasts;
import com.gregguns.icbm.launcher.LauncherWarhead;
import com.gregguns.icbm.launcher.LauncherWarheadRegistration;
import com.gregguns.icbm.warhead.TileWarhead;
import com.gregguns.icbm.warhead.WarheadBlocks;
import com.gregguns.icbm.warhead.WarheadCasing;
import com.gregguns.icbm.warhead.WarheadStacks;
import com.gregguns.icbm.warhead.effects.Effects;
import com.gregguns.item.ammo.AmmoItems;
import com.gregguns.item.gun.GunItems;

import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.BaseMetaTileEntity;

/**
 * Dev-only setups for the I5 visual client session (two connected clients). Each builds something at the shared
 * anchor, logs "I5V ..." lines and leaves it standing for a human-driven client; the long ones restore every config
 * switch they change when their blast is over. None is part of {@code test all}.
 */
public final class I5ClientSetups {

    private static final double EYE = 1.62;

    private I5ClientSetups() {}

    /** @param registry where to add the setups */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new OrbRed());
        registry.register(new OrbAnti());
        registry.register(new OrbBoth());
        registry.register(new AntiGrav());
        registry.register(new Ender());
        registry.register(new Warheads());
        registry.register(new Missiles());
        registry.register(new Fallout());
        registry.register(new Launcher());
        registry.register(new BlockDamage(true));
        registry.register(new BlockDamage(false));
        registry.register(new Cam());
        for (int i = 0; i < 9; i++) {
            registry.register(new Aim("w" + i, -8.0 + 2 * i, 1.0, 8.0, 3.2, 0.0));
        }
        for (int i = 0; i < 3; i++) {
            registry.register(new Aim("row" + i, -6.0 + 6 * i, 1.0, 8.0, 7.0, 0.0));
        }
        for (int i = 0; i < 5; i++) {
            registry.register(new Aim("m" + i, -8.0 + 4 * i, 5.0, 26.5, 14.0, 0.0));
        }
        registry.register(new Aim("mrow", 0.0, 5.0, 26.5, 26.0, 0.0));
        registry.register(new FallAim("fall", 150.0, 3.0, 2.0, 5.0));
        registry.register(new FallAim("fall_goo", 150.0, 4.0, 12.0, 5.0));
        registry.register(new FallAim("fall_far", 150.0, 3.0, 6.0, 26.0));
        registry.register(new Aim("lw", 0.0, 1.0, 25.0, 25.0, 0.0));
        registry.register(new Aim("ls", 12.0, 1.0, 25.0, 25.0, 0.0));
        registry.register(new Aim("lb", -11.0, 1.0, 40.0, 40.0, 0.0));
    }

    @SuppressWarnings("unchecked")
    private static List<EntityPlayerMP> players() {
        return new ArrayList<>(
            MinecraftServer.getServer()
                .getConfigurationManager().playerEntityList);
    }

    private static void signal(EntityPlayerMP p, String command, String... args) {
        p.addChatMessage(new ChatComponentText(AutotestSignal.format(command, args)));
    }

    /** Puts the eye at the point and turns it to the target, on the server (a client cannot be aimed reliably). */
    private static void cam(EntityPlayerMP p, double ex, double ey, double ez, double tx, double ty, double tz) {
        MissileShotsScenario.cam(p, ex, ey, ez, tx, ty, tz);
    }

    private static void log(ScenarioContext ctx, String what) {
        GregGuns.LOG.info("I5V {} origin={},{},{}", what, ctx.originX(), ctx.originY(), ctx.originZ());
    }

    private static void floor(ScenarioContext ctx, int half, Block0 b) {
        for (int x = -half; x <= half; x++) {
            for (int z = -half; z <= half; z++) {
                put(ctx.world(), ctx.originX() + x, ctx.originY() - 1, ctx.originZ() + z, b.block, b.meta);
            }
        }
    }

    private static final class Block0 {

        final net.minecraft.block.Block block;
        final int meta;

        Block0(net.minecraft.block.Block block, int meta) {
            this.block = block;
            this.meta = meta;
        }
    }

    private static final Block0 STONE = new Block0(Blocks.stone, 0);

    /** Direct (untracked) world write, so the build stays after the scenario ends. */
    private static void put(World w, int x, int y, int z, net.minecraft.block.Block b, int meta) {
        w.setBlock(x, y, z, b, meta, 2);
    }

    private abstract static class Setup implements Scenario {

        @Override
        public boolean includedInAll() {
            return false;
        }

        @Override
        public int areaRadius() {
            return 4;
        }

        @Override
        public String description() {
            return "I5 visual client-session setup (dev only); leaves its build standing. Not part of test all.";
        }

        @Override
        public final ScenarioResult run(ScenarioContext ctx) {
            return go(I4Anchor.of(ctx, 0, 0), ScenarioResult.builder(name()));
        }

        abstract ScenarioResult go(ScenarioContext ctx, ScenarioResult.Builder out);
    }

    /** Stone cube of half-edge {@code half} centered {@code cy} above the origin, with a hollow of half-edge 2. */
    private static void cube(ScenarioContext ctx, int half, int cy) {
        World w = ctx.world();
        for (int x = -half; x <= half; x++) {
            for (int z = -half; z <= half; z++) {
                for (int y = cy - half; y <= cy + half; y++) {
                    // Lower half only: the body then shows above its bowl instead of hiding inside a shell.
                    boolean hole = y > cy || (Math.abs(x) <= 2 && Math.abs(z) <= 2 && Math.abs(y - cy) <= 2);
                    if (!hole) {
                        put(w, ctx.originX() + x, ctx.originY() + y, ctx.originZ() + z, Blocks.stone, 0);
                    }
                }
            }
        }
    }

    /** Wipes the anchor arena (blocks y-2..y+45 within 48, and every non-player entity) with direct writes. */
    @SuppressWarnings("unchecked")
    private static void wipe(ScenarioContext ctx, int r, int h) {
        World w = ctx.world();
        int n = 0;
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                w.getChunkProvider()
                    .loadChunk((ctx.originX() + x) >> 4, (ctx.originZ() + z) >> 4);
                for (int y = -2; y <= h; y++) {
                    if (w.getBlock(ctx.originX() + x, ctx.originY() + y, ctx.originZ() + z) != Blocks.air) {
                        w.setBlock(ctx.originX() + x, ctx.originY() + y, ctx.originZ() + z, Blocks.air, 0, 2);
                        n++;
                    }
                }
            }
        }
        int dead = 0;
        for (Object o : new ArrayList<>(w.loadedEntityList)) {
            Entity e = (Entity) o;
            if (!(e instanceof EntityPlayerMP) && Math.abs(e.posX - ctx.originX()) < 120
                && Math.abs(e.posZ - ctx.originZ()) < 120) {
                e.setDead();
                dead++;
            }
        }
        log(ctx, "wipe cleared " + n + " blocks, " + dead + " entities");
    }

    private static void survivalPlayers(boolean creative) {
        for (EntityPlayerMP p : players()) {
            p.setGameType(creative ? WorldSettings.GameType.CREATIVE : WorldSettings.GameType.SURVIVAL);
            p.capabilities.allowFlying = true;
            p.capabilities.isFlying = true;
            p.sendPlayerAbilities();
        }
    }

    /**
     * Places players at (dx, dy, dz) from the origin looking at a point; the second player stands 4 blocks to the side.
     */
    private static void stand(ScenarioContext ctx, double dx, double dy, double dz, double tx, double ty, double tz) {
        int i = 0;
        for (EntityPlayerMP p : players()) {
            double side = i++ == 0 ? -2.0 : 2.0;
            cam(
                p,
                ctx.originX() + dx + side,
                ctx.originY() + dy + EYE,
                ctx.originZ() + dz,
                ctx.originX() + tx,
                ctx.originY() + ty,
                ctx.originZ() + tz);
        }
    }

    // ---- 1. orbs ----

    /** A red matter body (fill 2) in a stone cube; players 80 blocks away. Ends and restores config. */
    private static final class OrbRed extends Setup {

        @Override
        public String name() {
            return "testing.icbm_i5v_orb_red";
        }

        @Override
        ScenarioResult go(ScenarioContext ctx, ScenarioResult.Builder out) {
            wipe(ctx, 16, 28);
            floor(ctx, 40, STONE);
            cube(ctx, 10, 10);
            survivalPlayers(true);
            stand(ctx, 0, 16, -70, 0, 10, 0);
            return red(ctx, out, name(), 45, 2, 0.5, 10.5, 0.5, null);
        }
    }

    private static ScenarioResult red(ScenarioContext ctx, ScenarioResult.Builder out, String name, int lifeSeconds,
        int fill, double dx, double dy, double dz, Runnable atTick200) {
        final boolean savedT4 = IcbmConfig.terrainDamageT4;
        final int savedLife = IcbmConfig.redMatterLifetimeSeconds;
        IcbmConfig.terrainDamageT4 = true;
        IcbmConfig.redMatterLifetimeSeconds = lifeSeconds;
        BlastEngine engine = BlastEngine.instance();
        engine.clear();
        World w = ctx.world();
        if (!IcbmBlasts.detonate(
            w,
            ctx.originX() + dx,
            ctx.originY() + dy,
            ctx.originZ() + dz,
            Explosives.get("redmatter"),
            fill,
            null)) {
            IcbmConfig.terrainDamageT4 = savedT4;
            IcbmConfig.redMatterLifetimeSeconds = savedLife;
            return ScenarioResult.fail(name, "red matter not accepted");
        }
        log(ctx, "red matter started life=" + lifeSeconds + "s");
        return ScenarioResult.pending(name, 20 * (lifeSeconds + 40), t -> {
            if (t == 200 && atTick200 != null) {
                atTick200.run();
            }
            if (t % 40 == 0) {
                net.minecraft.nbt.NBTTagCompound s = engine.lastEffectState();
                log(
                    ctx,
                    "red t=" + t
                        + " active="
                        + engine.activeBlasts()
                        + " rmRadius="
                        + s.getInteger("rmRadius")
                        + " rmMode="
                        + s.getInteger("rmMode"));
            }
            if (engine.activeBlasts() > 0 && t < 20 * (lifeSeconds + 30)) {
                return null;
            }
            IcbmConfig.terrainDamageT4 = savedT4;
            IcbmConfig.redMatterLifetimeSeconds = savedLife;
            log(ctx, "red matter over at t=" + t);
            return out.build();
        });
    }

    /** A full antimatter flash (radius 80) at the center; players 150 blocks away. */
    private static final class OrbAnti extends Setup {

        @Override
        public String name() {
            return "testing.icbm_i5v_orb_anti";
        }

        @Override
        ScenarioResult go(ScenarioContext ctx, ScenarioResult.Builder out) {
            wipe(ctx, 8, 10);
            floor(ctx, 40, STONE);
            survivalPlayers(true);
            stand(ctx, 0, 20, -160, 0, 10, 0);
            final boolean savedT4 = IcbmConfig.terrainDamageT4;
            IcbmConfig.terrainDamageT4 = false;
            BlastEngine engine = BlastEngine.instance();
            engine.clear();
            ExplosiveDef def = Explosives.get("antimatter");
            // Let the clients settle on the camera first (the shot is taken by the driver after the blast starts).
            if (!IcbmBlasts.detonate(
                ctx.world(),
                ctx.originX() + 0.5,
                ctx.originY() + 5.5,
                ctx.originZ() + 0.5,
                def,
                def.maxFill(),
                null)) {
                IcbmConfig.terrainDamageT4 = savedT4;
                return ScenarioResult.fail(name(), "antimatter not accepted");
            }
            log(ctx, "antimatter started radius=" + def.radius(def.maxFill()));
            return ScenarioResult.pending(name(), 20 * 120, t -> {
                if (t % 40 == 0) {
                    log(ctx, "anti t=" + t + " active=" + engine.activeBlasts());
                }
                if (engine.activeBlasts() > 0 && t < 20 * 110) {
                    return null;
                }
                IcbmConfig.terrainDamageT4 = savedT4;
                log(ctx, "antimatter over at t=" + t);
                return out.build();
            });
        }
    }

    /** A red matter body, then at tick 200 an antimatter flash (fill 1) 60 blocks east: it must end the body. */
    private static final class OrbBoth extends Setup {

        @Override
        public String name() {
            return "testing.icbm_i5v_orb_both";
        }

        @Override
        ScenarioResult go(ScenarioContext ctx, ScenarioResult.Builder out) {
            wipe(ctx, 16, 28);
            floor(ctx, 40, STONE);
            cube(ctx, 10, 10);
            survivalPlayers(true);
            stand(ctx, 30, 16, -75, 30, 10, 0);
            World w = ctx.world();
            return red(ctx, out, name(), 120, 2, 0.5, 10.5, 0.5, () -> {
                IcbmBlasts.detonate(
                    w,
                    ctx.originX() + 60.5,
                    ctx.originY() + 10.5,
                    ctx.originZ() + 0.5,
                    Explosives.get("antimatter"),
                    1,
                    null);
                log(ctx, "antimatter fired at red matter");
            });
        }
    }

    // ---- 2. anti-grav ----

    /** A striped block plate with an anti-gravitational warhead over it; players 70 blocks off on two sides. */
    private static final class AntiGrav extends Setup {

        @Override
        public String name() {
            return "testing.icbm_i5v_antigrav";
        }

        @Override
        ScenarioResult go(ScenarioContext ctx, ScenarioResult.Builder out) {
            wipe(ctx, 26, 40);
            floor(ctx, 40, STONE);
            World w = ctx.world();
            for (int x = -22; x <= 22; x++) {
                for (int z = -22; z <= 22; z++) {
                    put(
                        w,
                        ctx.originX() + x,
                        ctx.originY(),
                        ctx.originZ() + z,
                        Blocks.stained_hardened_clay,
                        (x + 22) / 6 % 16);
                    put(w, ctx.originX() + x, ctx.originY() + 1, ctx.originZ() + z, Blocks.quartz_block, 0);
                }
            }
            survivalPlayers(true);
            List<EntityPlayerMP> ps = players();
            double[][] spots = { { -75, 12, 0 }, { 0, 12, 75 } };
            for (int i = 0; i < ps.size() && i < 2; i++) {
                cam(
                    ps.get(i),
                    ctx.originX() + spots[i][0],
                    ctx.originY() + spots[i][1] + EYE,
                    ctx.originZ() + spots[i][2],
                    ctx.originX(),
                    ctx.originY() + 6,
                    ctx.originZ());
            }
            final boolean saved = IcbmConfig.terrainDamageT3;
            IcbmConfig.terrainDamageT3 = true;
            BlastEngine engine = BlastEngine.instance();
            engine.clear();
            ExplosiveDef def = Explosives.get("antigrav");
            if (!IcbmBlasts
                .detonate(w, ctx.originX() + 0.5, ctx.originY() + 2.5, ctx.originZ() + 0.5, def, def.maxFill(), null)) {
                IcbmConfig.terrainDamageT3 = saved;
                return ScenarioResult.fail(name(), "antigrav not accepted");
            }
            log(ctx, "antigrav started radius=" + def.radius(def.maxFill()));
            return ScenarioResult.pending(name(), 20 * 200, t -> {
                int flying = 0;
                for (Object o : w.loadedEntityList) {
                    if (o instanceof com.gregguns.icbm.warhead.EntityFlyingBlock) {
                        flying++;
                    }
                }
                if (t % 40 == 0) {
                    log(ctx, "antigrav t=" + t + " active=" + engine.activeBlasts() + " flyingBlocks=" + flying);
                }
                if ((engine.activeBlasts() > 0 || flying > 0 || t < 100) && t < 20 * 190) {
                    return null;
                }
                IcbmConfig.terrainDamageT3 = saved;
                log(ctx, "antigrav over at t=" + t + " flyingBlocks=" + flying);
                return out.build();
            });
        }
    }

    // ---- 3. ender ----

    /** Cows around an ender warhead, Bob 4 blocks from it; the landing spot is forced 28 blocks away. */
    private static final class Ender extends Setup {

        @Override
        public String name() {
            return "testing.icbm_i5v_ender";
        }

        @Override
        ScenarioResult go(ScenarioContext ctx, ScenarioResult.Builder out) {
            wipe(ctx, 26, 12);
            floor(ctx, 40, STONE);
            World w = ctx.world();
            List<EntityPlayerMP> ps = players();
            survivalPlayers(false);
            for (int i = 0; i < 6; i++) {
                double a = i * Math.PI / 3.0;
                EntityCow c = new EntityCow(w);
                c.setLocationAndAngles(
                    ctx.originX() + 0.5 + 7 * Math.cos(a),
                    ctx.originY(),
                    ctx.originZ() + 0.5 + 7 * Math.sin(a),
                    0,
                    0);
                w.spawnEntityInWorld(c);
            }
            // Alice watches from far away; Bob (second player) stands near the blast to be sent away.
            if (!ps.isEmpty()) {
                ps.get(0)
                    .setGameType(WorldSettings.GameType.CREATIVE);
                cam(
                    ps.get(0),
                    ctx.originX() - 10,
                    ctx.originY() + 14 + EYE,
                    ctx.originZ() - 62,
                    ctx.originX() - 10,
                    ctx.originY() + 1,
                    ctx.originZ() - 10);
            }
            final EntityPlayerMP bob = ps.size() > 1 ? ps.get(1) : null;
            if (bob != null) {
                bob.capabilities.isFlying = false;
                bob.sendPlayerAbilities();
                bob.playerNetServerHandler
                    .setPlayerLocation(ctx.originX() + 4.5, ctx.originY(), ctx.originZ() + 0.5, 90.0F, 0.0F);
            }
            final int tx = ctx.originX() - 20;
            final int tz = ctx.originZ() - 20;
            Effects.forceEnderTarget(tx, ctx.originY(), tz);
            ExplosiveDef def = Explosives.get("ender");
            BlastEngine engine = BlastEngine.instance();
            engine.clear();
            if (!IcbmBlasts
                .detonate(w, ctx.originX() + 0.5, ctx.originY() + 0.5, ctx.originZ() + 0.5, def, def.maxFill(), null)) {
                Effects.clearEnderTarget();
                return ScenarioResult.fail(name(), "ender not accepted");
            }
            log(ctx, "ender started, landing at " + tx + "," + tz);
            return ScenarioResult.pending(name(), 20 * 40, t -> {
                if (bob != null && t % 10 == 0 && t <= 400) {
                    log(
                        ctx,
                        "ender t=" + t
                            + " bob="
                            + (int) bob.posX
                            + ","
                            + (int) bob.posY
                            + ","
                            + (int) bob.posZ
                            + " hp="
                            + bob.getHealth());
                }
                if (t < 400) {
                    return null;
                }
                Effects.clearEnderTarget();
                return out.build();
            });
        }
    }

    // ---- 4. warhead looks ----

    private static final Object[][] LOOKS = { { WarheadCasing.T3, "exothermic" }, { WarheadCasing.T3, "endothermic" },
        { WarheadCasing.T3, "nuclear" }, { WarheadCasing.T3, "emp" }, { WarheadCasing.T3, "ender" },
        { WarheadCasing.T3, "antigrav" }, { WarheadCasing.T3, "" }, { WarheadCasing.T4, "antimatter" },
        { WarheadCasing.T5, "redmatter" } };

    private static ItemStack lookStack(Object[] look) {
        WarheadCasing c = (WarheadCasing) look[0];
        Item item = Item.getItemFromBlock(WarheadBlocks.of(c));
        String id = (String) look[1];
        return id.isEmpty() ? new ItemStack(item) : WarheadStacks.make(item, c, id, 1);
    }

    /** Nine placed warheads in a row at z=+8 (T3 skins, the empty T3, T4, T5), and the same nine as hotbar items. */
    private static final class Warheads extends Setup {

        @Override
        public String name() {
            return "testing.icbm_i5v_warheads";
        }

        @Override
        ScenarioResult go(ScenarioContext ctx, ScenarioResult.Builder out) {
            wipe(ctx, 14, 6);
            floor(ctx, 40, STONE);
            World w = ctx.world();
            List<EntityPlayerMP> ps = players();
            if (ps.isEmpty()) {
                return ScenarioResult.fail(name(), "needs a player");
            }
            survivalPlayers(true);
            for (int i = 0; i < LOOKS.length; i++) {
                int dx = -8 + 2 * i;
                WarheadCasing c = (WarheadCasing) LOOKS[i][0];
                put(w, ctx.originX() + dx, ctx.originY(), ctx.originZ() + 8, WarheadBlocks.of(c), 0);
                TileEntity t = w.getTileEntity(ctx.originX() + dx, ctx.originY(), ctx.originZ() + 8);
                ItemStack s = lookStack(LOOKS[i]);
                if (t instanceof TileWarhead) {
                    ((TileWarhead) t).init(c, s.getTagCompound(), ps.get(0));
                }
            }
            for (EntityPlayerMP p : ps) {
                p.inventory.clearInventory(null, -1);
                for (int i = 0; i < LOOKS.length; i++) {
                    p.inventory.setInventorySlotContents(i, lookStack(LOOKS[i]));
                }
                p.inventoryContainer.detectAndSendChanges();
            }
            stand(ctx, 0, 1, 1, 0, 1, 8);
            log(
                ctx,
                "warheads placed x=-8..8 step 2 (exo, endo, nuc, emp, ender, antigrav, empty T3, T4 antimatter, T5 red)");
            return out.build();
        }
    }

    /** Five missiles on the pad plane at z=+26: three T3 skins, T4 and T5 casings. */
    private static final class Missiles extends Setup {

        @Override
        public String name() {
            return "testing.icbm_i5v_missiles";
        }

        @Override
        ScenarioResult go(ScenarioContext ctx, ScenarioResult.Builder out) {
            wipe(ctx, 18, 14);
            floor(ctx, 40, STONE);
            String[] ids = { "nuclear", "ender", "antigrav", "antimatter", "redmatter" };
            int[] casing = { 3, 3, 3, 4, 5 };
            for (int i = 0; i < 5; i++) {
                EntityMissile m = EntityMissile.create(MissileTier.T3, ctx.world());
                m.setLocationAndAngles(
                    ctx.originX() + -8 + 4 * i + 0.5,
                    ctx.originY() + EntityMissile.PAD_SURFACE,
                    ctx.originZ() + 26.5,
                    0.0F,
                    0.0F);
                m.configure(0, ids[i], 1, casing[i], null);
                ctx.world()
                    .spawnEntityInWorld(m);
            }
            survivalPlayers(true);
            stand(ctx, 0, 6, 8, 0, 5, 26.5);
            log(ctx, "5 T3 missiles x=-8..8 step 4 at z=26 (nuclear, ender, antigrav, T4 antimatter, T5 red)");
            return out.build();
        }
    }

    // ---- 6. fallout ----

    /** A patch of every fallout block variant on a grass/dirt/sand ground, read in daylight and at a distance. */
    private static final class Fallout extends Setup {

        @Override
        public String name() {
            return "testing.icbm_i5v_fallout";
        }

        @Override
        ScenarioResult go(ScenarioContext ctx, ScenarioResult.Builder out) {
            World w = ctx.world();
            // The natural surface of the flat world (grass at y=3), 150 blocks east of the arena so the floating
            // floors of the other setups do not shade it: real daylight, colors as a player sees them.
            final int fx = ctx.originX() + 150;
            final int fz = ctx.originZ();
            final int gy = 3;
            final int oy = gy + 1;
            for (int x = -14; x <= 14; x++) {
                for (int z = -6; z <= 18; z++) {
                    w.getChunkProvider()
                        .loadChunk((fx + x) >> 4, (fz + z) >> 4);
                    put(w, fx + x, gy, fz + z, Blocks.grass, 0);
                    put(w, fx + x, oy, fz + z, Blocks.air, 0);
                }
            }
            net.minecraft.block.Block earth = FalloutBlocks.irradiated();
            // 5 variants, cool then hot (two rows of five, 2 apart); then the natural ground for comparison
            for (int v = 0; v < FalloutBlocks.VARIANTS; v++) {
                put(w, fx - 4 + 2 * v, gy, fz - 2, earth, v);
                put(w, fx - 4 + 2 * v, gy, fz + 2, earth, v | FalloutBlocks.HOT_BIT);
            }
            put(w, fx - 4, gy, fz + 6, Blocks.dirt, 0);
            put(w, fx - 2, gy, fz + 6, Blocks.stone, 0);
            put(w, fx, gy, fz + 6, Blocks.sand, 0);
            put(w, fx + 2, gy, fz + 6, Blocks.gravel, 0);
            put(w, fx + 4, gy, fz + 6, Blocks.cobblestone, 0);
            for (int i = 0; i < 4; i++) {
                put(w, fx - 3 + 2 * i, oy, fz + 10, FalloutBlocks.trinitite(), 0);
                put(w, fx - 3 + 2 * i, oy, fz + 13, FalloutBlocks.goo(), 0);
            }
            put(w, fx - 3, oy, fz + 13, FalloutBlocks.goo(), 1);
            survivalPlayers(true);
            for (EntityPlayerMP p : players()) {
                cam(p, fx + 0.5, oy + 7, fz - 12, fx + 0.5, gy, fz + 5);
            }
            GregGuns.LOG.info(
                "I5V fallout patch at {},{},{}: cool row z-2, hot row z+2, natural z+6, trinitite z+10, goo z+13",
                fx,
                gy,
                fz);
            return out.build();
        }
    }

    // ---- 5. launcher ----

    /** Gives Alice both launchers, ammo and warheads; stands targets 25 to 45 blocks away. */
    private static final class Launcher extends Setup {

        @Override
        public String name() {
            return "testing.icbm_i5v_launcher";
        }

        @Override
        ScenarioResult go(ScenarioContext ctx, ScenarioResult.Builder out) {
            wipe(ctx, 46, 6);
            floor(ctx, 46, STONE);
            World w = ctx.world();
            List<EntityPlayerMP> ps = players();
            if (ps.isEmpty()) {
                return ScenarioResult.fail(name(), "needs a player");
            }
            // Targets: an oak plank wall with a grass strip (fire), a stone wall (terrain), an EMP battery buffer.
            for (int x = -4; x <= 4; x++) {
                for (int y = 0; y <= 2; y++) {
                    put(w, ctx.originX() + x, ctx.originY() + y, ctx.originZ() + 25, Blocks.planks, 0);
                    put(w, ctx.originX() + 12 + x, ctx.originY() + y, ctx.originZ() + 25, Blocks.stone, 0);
                }
            }
            int buffer = ItemList.Battery_Buffer_1by1_MV.get(1)
                .getItemDamage();
            EmpScenarios.place(ctx, -12, 0, 40, buffer);
            EmpScenarios.place(ctx, -10, 0, 40, buffer);
            EntityPlayerMP p = ps.get(0);
            p.setGameType(WorldSettings.GameType.SURVIVAL);
            p.capabilities.isFlying = false;
            p.capabilities.allowFlying = false;
            p.sendPlayerAbilities();
            p.inventory.clearInventory(null, -1);
            p.inventory.setInventorySlotContents(0, new ItemStack(GunItems.get(RocketLauncherDef.NAME)));
            p.inventory.setInventorySlotContents(1, new ItemStack(GunItems.get(NukeLauncherDef.NAME)));
            p.inventory.setInventorySlotContents(2, new ItemStack(AmmoItems.round(AmmoIds.ROCKET), 16));
            p.inventory.setInventorySlotContents(3, new ItemStack(AmmoItems.round(AmmoIds.ROCKET_NUKE), 8));
            p.inventory.setInventorySlotContents(4, LauncherWarheadRegistration.stack(LauncherWarhead.INCENDIARY));
            p.inventory.setInventorySlotContents(5, LauncherWarheadRegistration.stack(LauncherWarhead.EMP));
            p.inventory.setInventorySlotContents(6, LauncherWarheadRegistration.stack(LauncherWarhead.HE));
            p.inventory.setInventorySlotContents(7, LauncherWarheadRegistration.stack(LauncherWarhead.INCENDIARY));
            p.inventoryContainer.detectAndSendChanges();
            p.playerNetServerHandler
                .setPlayerLocation(ctx.originX() + 0.5, ctx.originY(), ctx.originZ() + 0.5, 0.0F, 0.0F);
            cam(
                p,
                ctx.originX() + 0.5,
                ctx.originY() + EYE,
                ctx.originZ() + 0.5,
                ctx.originX() + 0.5,
                ctx.originY() + 2,
                ctx.originZ() + 25.5);
            log(ctx, "launcher: planks wall z=25 x=-4..4, stone wall x=8..16 z=25, buffers at -12/-10,z=40");
            // Power the buffers once their tiles exist.
            return ScenarioResult.pending(name(), 100, t -> {
                if (t < 60) {
                    return null;
                }
                for (int x : new int[] { -12, -10 }) {
                    IGregTechTileEntity b = EmpScenarios.live(ctx, x, 0, 40);
                    if (b != null) {
                        ((BaseMetaTileEntity) b).setStoredEU(1_000_000L);
                    }
                }
                log(ctx, "launcher: buffers charged");
                return out.build();
            });
        }
    }

    /** Logs the buffers' stored EU and the cows' positions; run it before and after a shot. */
    private static final class Cam extends Setup {

        @Override
        public String name() {
            return "testing.icbm_i5v_report";
        }

        @Override
        ScenarioResult go(ScenarioContext ctx, ScenarioResult.Builder out) {
            World w = ctx.world();
            for (int x : new int[] { -12, -10 }) {
                IGregTechTileEntity b = EmpScenarios.live(ctx, x, 0, 40);
                log(ctx, "report buffer x=" + x + " eu=" + (b == null ? "none" : String.valueOf(b.getStoredEU())));
            }
            log(
                ctx,
                "report planks=" + w.getBlock(ctx.originX(), ctx.originY() + 1, ctx.originZ() + 25)
                    .getUnlocalizedName()
                    + " stone="
                    + w.getBlock(ctx.originX() + 12, ctx.originY() + 1, ctx.originZ() + 25)
                        .getUnlocalizedName()
                    + " fireAbovePlanks="
                    + w.getBlock(ctx.originX(), ctx.originY() + 3, ctx.originZ() + 25)
                        .getUnlocalizedName()
                    + " fireInFront="
                    + w.getBlock(ctx.originX(), ctx.originY() + 1, ctx.originZ() + 24)
                        .getUnlocalizedName());
            for (EntityPlayerMP p : players()) {
                log(
                    ctx,
                    "report " + p.getCommandSenderName()
                        + " at "
                        + (int) p.posX
                        + ","
                        + (int) p.posY
                        + ","
                        + (int) p.posZ
                        + " hp="
                        + p.getHealth());
            }
            return out.build();
        }
    }

    /** Switches explosions.blockDamage; the driver turns it off again. */
    private static final class BlockDamage extends Setup {

        private final boolean on;

        BlockDamage(boolean on) {
            this.on = on;
        }

        @Override
        public String name() {
            return "testing.icbm_i5v_blockdamage_" + (on ? "on" : "off");
        }

        @Override
        ScenarioResult go(ScenarioContext ctx, ScenarioResult.Builder out) {
            ExplosionsConfig.blockDamage = on;
            log(ctx, "explosions.blockDamage=" + on);
            return out.build();
        }
    }

    /** Puts every player {@code dist} blocks south of a fallout point (x relative to the origin, y absolute). */
    private static final class FallAim extends Setup {

        private final String id;
        private final double x;
        private final double y;
        private final double z;
        private final double dist;

        FallAim(String id, double x, double y, double z, double dist) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.z = z;
            this.dist = dist;
        }

        @Override
        public String name() {
            return "testing.icbm_i5v_aim_" + id;
        }

        @Override
        ScenarioResult go(ScenarioContext ctx, ScenarioResult.Builder out) {
            for (EntityPlayerMP p : players()) {
                cam(
                    p,
                    ctx.originX() + x + 0.5,
                    y + 3.5,
                    ctx.originZ() + z - dist,
                    ctx.originX() + x + 0.5,
                    y,
                    ctx.originZ() + z);
            }
            return out.build();
        }
    }

    /** Puts every player {@code dist} blocks south of a point (relative to the origin), looking at it. */
    private static final class Aim extends Setup {

        private final String id;
        private final double x;
        private final double y;
        private final double z;
        private final double dist;

        Aim(String id, double x, double y, double z, double dist, double unused) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.z = z;
            this.dist = dist;
        }

        @Override
        public String name() {
            return "testing.icbm_i5v_aim_" + id;
        }

        @Override
        ScenarioResult go(ScenarioContext ctx, ScenarioResult.Builder out) {
            int i = 0;
            for (EntityPlayerMP p : players()) {
                double side = id.equals("launcher") ? 0 : (i++ == 0 ? -0.6 : 0.6);
                cam(
                    p,
                    ctx.originX() + x + side,
                    ctx.originY() + y + (id.equals("launcher") ? EYE : 1.2),
                    ctx.originZ() + z - dist,
                    ctx.originX() + x,
                    ctx.originY() + y,
                    ctx.originZ() + z);
            }
            return out.build();
        }
    }
}
