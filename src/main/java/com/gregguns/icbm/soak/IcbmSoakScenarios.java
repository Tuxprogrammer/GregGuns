package com.gregguns.icbm.soak;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;

import com.gregguns.core.config.IcbmConfig;
import com.gregguns.core.testing.ScenarioChunks;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.explosion.nuclear.FalloutEngine;
import com.gregguns.icbm.blast.BlastEngine;
import com.gregguns.icbm.emp.EmpMode;
import com.gregguns.icbm.emp.EmpPulse;
import com.gregguns.icbm.emp.EmpScenarios;
import com.gregguns.icbm.explosive.ExplosiveDef;
import com.gregguns.icbm.explosive.Explosives;
import com.gregguns.icbm.explosive.IcbmBlasts;
import com.gregguns.icbm.missile.EntityMissile;
import com.gregguns.icbm.missile.LaunchResult;
import com.gregguns.icbm.missile.MissileTier;
import com.mojang.authlib.GameProfile;

import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.util.GTOreDictUnificator;

/**
 * The I7 soak runs, each a scripted, repeatable burst on whatever server runs it (made for a real GTNH dedicated
 * server with every module on): many missiles in flight across the three tiers, sixteen large blasts at once with
 * nuclear and antimatter among them, three red matter bodies, and EMP pulses over a dense GT base. A small missile
 * salvo is the repeated load of the long play soak. All opt-in ({@code gregguns test icbm.soak.<name>}); the bar is
 * {@link SoakVerdict}. Each burst works in its own region, kilometers apart, so craters from one never sit under
 * another. Dev environment only. Server side.
 */
// Spec: docs/gregguns-icbm-spec/01-core/soak.md
public final class IcbmSoakScenarios {

    /** How far each soak's region lies from the scenario origin, blocks. */
    static final int REGION = 1536;
    /** The highest cap the config allows on missiles in flight ({@code icbm.maxMissilesInFlight}). */
    static final int CONFIG_MAX_MISSILES = 100;

    private IcbmSoakScenarios() {}

    /** @param registry where to add the soaks */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new Missiles("icbm.soak.missiles", -1, false, true));
        registry.register(new Missiles("icbm.soak.missiles_max", CONFIG_MAX_MISSILES, true, false));
        registry.register(new Missiles("icbm.soak.salvo", 5, false, false));
        registry.register(new Blasts());
        registry.register(new RedMatter());
        registry.register(new Emp());
    }

    /** The name and id the soak acts and owns machines under. */
    static final String OWNER_NAME = "[GG Soak]";
    static final UUID OWNER_UUID = UUID.nameUUIDFromBytes("gregguns_soak".getBytes(StandardCharsets.UTF_8));

    /** @return the player a soak acts as: a fake player that never joins the world, so its blasts break terrain */
    static FakePlayer owner(World world) {
        return FakePlayerFactory.get((WorldServer) world, new GameProfile(OWNER_UUID, OWNER_NAME));
    }

    /**
     * @param world the world
     * @param box   where to look
     * @param top   how many item kinds to name
     * @return the item entities in the box, counted by item, most first: "name xN, ..."
     */
    static String itemCensus(World world, net.minecraft.util.AxisAlignedBB box, int top) {
        Map<String, Integer> byName = new java.util.HashMap<>();
        int total = 0;
        for (Object o : world.getEntitiesWithinAABB(EntityItem.class, box)) {
            ItemStack st = ((EntityItem) o).getEntityItem();
            String n = st == null || st.getItem() == null ? "?"
                : net.minecraft.item.Item.itemRegistry.getNameForObject(st.getItem()) + ":" + st.getItemDamage();
            byName.merge(n, 1, Integer::sum);
            total++;
        }
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(byName.entrySet());
        sorted.sort((a, b) -> b.getValue() - a.getValue());
        StringBuilder sb = new StringBuilder(total + " item entities");
        for (int i = 0; i < Math.min(top, sorted.size()); i++) {
            sb.append(i == 0 ? ": " : ", ")
                .append(
                    sorted.get(i)
                        .getKey())
                .append(" x")
                .append(
                    sorted.get(i)
                        .getValue());
        }
        return sb.toString();
    }

    /** Holds chunks a few at a time; what every burst with a far region shares. */
    static final class Holds {

        final List<ScenarioChunks.Hold> holds = new ArrayList<>();

        void add(World world, int x, int z, int radius) {
            holds.add(ScenarioChunks.hold(world, x, z, radius));
        }

        void release() {
            for (ScenarioChunks.Hold h : holds) {
                h.release();
            }
            holds.clear();
        }
    }

    // ---- missiles ------------------------------------------------------------------------------------------------

    /** Missiles of all three tiers launched in one tick, to targets all around, warheads at full fill. */
    static final class Missiles extends SoakScenario {

        private final String name;
        private final int count;
        private final boolean raiseCap;
        private final boolean overCapCheck;

        /**
         * @param name         the scenario name
         * @param count        missiles to launch, or -1 for the configured cap
         * @param raiseCap     whether to lift {@code icbm.maxMissilesInFlight} to {@code count} for the run
         * @param overCapCheck whether to check that one more past the cap is refused
         */
        Missiles(String name, int count, boolean raiseCap, boolean overCapCheck) {
            this.name = name;
            this.count = count;
            this.raiseCap = raiseCap;
            this.overCapCheck = overCapCheck;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public String description() {
            return "Soak (opt-in): " + (count < 0 ? "the configured cap of" : Integer.toString(count))
                + " missiles of all three tiers launched in one tick to targets 300 to 4,000 blocks away; every"
                + " flight and blast ends, the server recovers, and no chunk ticket is left.";
        }

        @Override
        public int areaRadius() {
            return 48;
        }

        @Override
        int burstLimit() {
            return 20 * 60 * 12;
        }

        @Override
        Burst plan(ScenarioContext ctx) {
            return new MissileBurst(ctx, count, raiseCap, overCapCheck, count > 0 && count <= 5);
        }
    }

    private static final class MissileBurst implements SoakScenario.Burst {

        private static final int SPACING = 4;

        final ScenarioContext ctx;
        final World world;
        final int wanted;
        final boolean raiseCap;
        final boolean overCapCheck;
        final boolean shortFlights;
        final int savedCap;
        final List<EntityMissile> missiles = new ArrayList<>();
        final Map<LaunchResult, Integer> results = new EnumMap<>(LaunchResult.class);
        final Map<MissileTier, Integer> tiers = new EnumMap<>(MissileTier.class);
        LaunchResult overCap;
        int n;

        MissileBurst(ScenarioContext ctx, int count, boolean raiseCap, boolean overCapCheck, boolean shortFlights) {
            this.ctx = ctx;
            this.world = ctx.world();
            this.wanted = count;
            this.raiseCap = raiseCap;
            this.overCapCheck = overCapCheck;
            this.shortFlights = shortFlights;
            this.savedCap = IcbmConfig.maxMissilesInFlight;
        }

        @Override
        public boolean prepare(int tick) {
            if (tick == 1) {
                if (raiseCap) {
                    IcbmConfig.maxMissilesInFlight = Math.max(savedCap, wanted);
                }
                n = wanted < 0 ? IcbmConfig.maxMissilesInFlight : wanted;
                // The climb needs the column clear 12 blocks plus the body's height over the pads.
                ctx.clearArea(Math.min(46, gridHalf() + 2), 26);
                return false;
            }
            if (tick == 2) {
                spawnMissiles();
            }
            return tick >= 3;
        }

        private int gridHalf() {
            int cols = (int) Math.ceil(Math.sqrt(Math.max(1, n)));
            return (cols - 1) * SPACING / 2;
        }

        private void spawnMissiles() {
            int cols = (int) Math.ceil(Math.sqrt(n));
            int half = gridHalf();
            for (int i = 0; i < n; i++) {
                MissileTier tier = tierOf(i);
                EntityMissile m = arm(tier, -half + (i % cols) * SPACING, -half + (i / cols) * SPACING);
                missiles.add(m);
                tiers.merge(tier, 1, Integer::sum);
            }
        }

        private MissileTier tierOf(int i) {
            if (shortFlights) {
                return i % 2 == 0 ? MissileTier.T1 : MissileTier.T2;
            }
            int k = i % 10;
            return k < 4 ? MissileTier.T1 : k < 7 ? MissileTier.T2 : MissileTier.T3;
        }

        private EntityMissile arm(MissileTier tier, int dx, int dz) {
            EntityMissile m = EntityMissile.create(tier, world);
            m.setLocationAndAngles(ctx.originX() + dx + 0.5, ctx.originY(), ctx.originZ() + dz + 0.5, 0.0F, 0.0F);
            String explosive = shortFlights ? "condensed"
                : tier == MissileTier.T1 ? "condensed" : tier == MissileTier.T2 ? "thermobaric" : "exothermic";
            ExplosiveDef def = Explosives.get(explosive);
            int fill = shortFlights || def == null ? 1 : def.maxFill();
            m.configure(0, explosive, fill, tier.ordinal() + 1, null);
            m.fill(m.capacityMb(), true);
            ctx.spawn(m);
            return m;
        }

        /** @return {minimum, maximum} target distance for a tier, inside its range */
        private int[] band(EntityMissile m) {
            int max = m.maxRange();
            if (shortFlights) {
                return new int[] { 300, Math.min(900, max - 50) };
            }
            switch (m.tier()) {
                case T1:
                    return new int[] { 300, Math.min(950, max - 50) };
                case T2:
                    return new int[] { 1000, Math.min(2500, max - 50) };
                default:
                    return new int[] { 2000, Math.min(4000, max - 50) };
            }
        }

        @Override
        public void fire(ScenarioResult.Builder out) {
            FakePlayer owner = owner(world);
            Random rnd = new Random(0x50AC + n);
            for (int i = 0; i < missiles.size(); i++) {
                EntityMissile m = missiles.get(i);
                int[] b = band(m);
                double angle = 2.0 * Math.PI * i / missiles.size() + rnd.nextDouble() * 0.2;
                double dist = b[0] + rnd.nextDouble() * Math.max(1, b[1] - b[0]);
                // The site-factor launch also takes a silo-only T3 body; scatter and range factors of a plain pad.
                LaunchResult r = m
                    .launch(m.posX + Math.cos(angle) * dist, m.posZ + Math.sin(angle) * dist, owner, 1.0, 1.0);
                results.merge(r, 1, Integer::sum);
            }
            if (overCapCheck) {
                EntityMissile extra = arm(MissileTier.T1, 0, gridHalf() + SPACING);
                overCap = extra.launch(extra.posX + 500, extra.posZ, owner, 1.0, 1.0);
                // Refused, so still on its pad: take it away without dropping its parts.
                extra.setDead();
                ctx.despawn(extra);
            }
        }

        private int alive() {
            int a = 0;
            for (EntityMissile m : missiles) {
                if (!m.isDead) {
                    a++;
                }
            }
            return a;
        }

        @Override
        public boolean done() {
            return alive() == 0 && BlastEngine.instance()
                .activeBlasts() == 0;
        }

        @Override
        public String status() {
            return "missiles alive " + alive()
                + ", in flight "
                + EntityMissile.flying()
                + ", blasts "
                + BlastEngine.instance()
                    .activeBlasts();
        }

        @Override
        public void stop() {
            for (EntityMissile m : missiles) {
                m.tickets()
                    .release();
                m.setDead();
            }
            BlastEngine.instance()
                .clear();
        }

        @Override
        public void check(ScenarioResult.Builder out, SoakProbe before, SoakProbe after, SoakProbe peak) {
            int ok = results.getOrDefault(LaunchResult.OK, 0);
            out.note("launched " + missiles.size() + " " + tiers + ": " + results);
            out.checkEquals("every missile launched", missiles.size(), ok);
            if (overCapCheck) {
                out.checkEquals("one past the cap is refused", LaunchResult.OVER_CAP, overCap);
            }
            out.check(
                peak.missilesFlying <= IcbmConfig.maxMissilesInFlight,
                "never more in flight than the cap: peak " + peak.missilesFlying);
            out.check(
                peak.forcedChunks - before.forcedChunks <= 2 * missiles.size(),
                "missiles force at most two chunks each: peak " + (peak.forcedChunks - before.forcedChunks)
                    + " for "
                    + missiles.size());
            int accepted = 0;
            int refused = 0;
            for (EntityMissile m : missiles) {
                String r = m.impactReport();
                if (r.endsWith("accepted")) {
                    accepted++;
                } else if (r.endsWith("REFUSED")) {
                    refused++;
                }
            }
            out.note("impacts: " + accepted + " blasts accepted, " + refused + " refused by the blast queue");
            out.checkEquals("every missile ended", 0, alive());
        }

        @Override
        public void cleanup() {
            for (EntityMissile m : missiles) {
                if (!m.isDead) {
                    m.tickets()
                        .release();
                    m.setDead();
                }
            }
            IcbmConfig.maxMissilesInFlight = savedCap;
        }
    }

    // ---- large blasts --------------------------------------------------------------------------------------------

    /** Sixteen large blasts in one tick: antimatter, three nuclear, and twelve LuV and EV craters. */
    static final class Blasts extends SoakScenario {

        @Override
        public String name() {
            return "icbm.soak.blasts";
        }

        @Override
        public String description() {
            return "Soak (opt-in): the blast queue's worth of large blasts at once on real terrain (one antimatter,"
                + " three nuclear, four exothermic, four endothermic, four thermobaric) and one more refused; every"
                + " crater and its fallout ends, the server recovers, and no chunk ticket is left.";
        }

        @Override
        int burstLimit() {
            return 20 * 60 * 20;
        }

        @Override
        Burst plan(ScenarioContext ctx) {
            return new SiteBurst(
                ctx,
                REGION,
                0,
                new String[] { "antimatter", "nuclear", "nuclear", "nuclear", "exothermic", "exothermic", "exothermic",
                    "exothermic", "endothermic", "endothermic", "endothermic", "endothermic", "thermobaric",
                    "thermobaric", "thermobaric", "thermobaric" },
                224,
                true);
        }
    }

    /** Blasts at sites on a square grid of a far region, on the real ground. */
    private static class SiteBurst implements SoakScenario.Burst {

        final ScenarioContext ctx;
        final World world;
        final int regionX;
        final int regionZ;
        final String[] explosives;
        final int spacing;
        final boolean overflowCheck;
        final Holds holds = new Holds();
        final int[][] sites;
        int accepted;
        boolean overflowRefused;

        SiteBurst(ScenarioContext ctx, int dx, int dz, String[] explosives, int spacing, boolean overflowCheck) {
            this.ctx = ctx;
            this.world = ctx.world();
            this.regionX = ctx.originX() + dx;
            this.regionZ = ctx.originZ() + dz;
            this.explosives = explosives;
            this.spacing = spacing;
            this.overflowCheck = overflowCheck;
            this.sites = new int[explosives.length][3];
            int cols = (int) Math.ceil(Math.sqrt(explosives.length));
            for (int i = 0; i < explosives.length; i++) {
                sites[i][0] = regionX + (i % cols) * spacing;
                sites[i][2] = regionZ + (i / cols) * spacing;
            }
        }

        double radius(int i) {
            ExplosiveDef def = Explosives.get(explosives[i]);
            return def == null ? 16.0 : Math.min(IcbmConfig.maxBlastRadius, def.radius(def.maxFill()));
        }

        @Override
        public boolean prepare(int tick) {
            // One site a tick: each hold loads (and in a fresh world generates) a square of chunks.
            int i = tick - 1;
            if (i < sites.length) {
                holds.add(world, sites[i][0], sites[i][2], (int) Math.ceil(radius(i)) + 16);
                sites[i][1] = world.getHeightValue(sites[i][0], sites[i][2]);
                return false;
            }
            return extraPrepare(i - sites.length);
        }

        /** @return true when the burst's own extra setup is done; none by default */
        boolean extraPrepare(int step) {
            return true;
        }

        @Override
        public void fire(ScenarioResult.Builder out) {
            FakePlayer owner = owner(world);
            for (int i = 0; i < sites.length; i++) {
                ExplosiveDef def = Explosives.get(explosives[i]);
                if (def != null && IcbmBlasts
                    .detonate(world, sites[i][0] + 0.5, sites[i][1], sites[i][2] + 0.5, def, def.maxFill(), owner)) {
                    accepted++;
                }
            }
            if (overflowCheck) {
                ExplosiveDef small = Explosives.get("condensed");
                overflowRefused = !IcbmBlasts
                    .detonate(world, regionX - spacing + 0.5, sites[0][1], regionZ + 0.5, small, 1, owner);
            }
        }

        @Override
        public boolean done() {
            return BlastEngine.instance()
                .activeBlasts() == 0
                && !FalloutEngine.instance()
                    .busy(world);
        }

        @Override
        public String status() {
            return "blasts " + BlastEngine.instance()
                .activeBlasts()
                + " (undecided "
                + BlastEngine.instance()
                    .undecidedBlasts()
                + "), fallout converting "
                + FalloutEngine.instance()
                    .busy(world);
        }

        @Override
        public void stop() {
            BlastEngine.instance()
                .clear();
        }

        @Override
        public void check(ScenarioResult.Builder out, SoakProbe before, SoakProbe after, SoakProbe peak) {
            out.checkEquals("every blast accepted", sites.length, accepted);
            if (overflowCheck) {
                out.check(
                    overflowRefused,
                    "one blast past icbm.maxActiveBlasts (" + IcbmConfig.maxActiveBlasts + ") is refused");
            }
            out.checkEquals("the ray-cast cubes are all freed", 0L, after.gridBytes);
            out.checkEquals("no flying block is left", 0, after.flyingBlocks);
            StringBuilder s = new StringBuilder();
            for (int i = 0; i < sites.length; i++) {
                s.append(
                    String.format(Locale.ROOT, " %s@%d,%d,%d", explosives[i], sites[i][0], sites[i][1], sites[i][2]));
            }
            out.note("sites:" + s);
            int last = sites.length - 1;
            out.note(
                "items around the sites after the burst: " + itemCensus(
                    world,
                    net.minecraft.util.AxisAlignedBB.getBoundingBox(
                        regionX - 128,
                        0,
                        regionZ - 128,
                        sites[last][0] + 128,
                        256,
                        sites[last][2] + 128),
                    6));
        }

        @Override
        public void cleanup() {
            holds.release();
        }
    }

    // ---- red matter ----------------------------------------------------------------------------------------------

    /** Three red matter bodies at full fill, one of them among a hundred mobs and two hundred items. */
    static final class RedMatter extends SoakScenario {

        /** Mobs and item stacks around the crowd body. */
        static final int CROWD_MOBS = 100;
        static final int CROWD_ITEMS = 200;
        /** Where the underground body burns. */
        static final int UNDERGROUND_Y = 24;

        @Override
        public String name() {
            return "icbm.soak.redmatter";
        }

        @Override
        public String description() {
            return "Soak (opt-in): three red matter bodies at full fill, one in a crowd of 100 mobs and 200 items;"
                + " each ends inside its lifetime, the server recovers, and no chunk ticket is left.";
        }

        @Override
        int burstLimit() {
            return IcbmConfig.redMatterLifetimeSeconds * 20 + 1200;
        }

        @Override
        Burst plan(ScenarioContext ctx) {
            return new SiteBurst(ctx, 0, REGION, new String[] { "redmatter", "redmatter", "redmatter" }, 160, false) {

                final List<Entity> crowd = new ArrayList<>();

                @Override
                boolean extraPrepare(int step) {
                    // Body 2 burns underground, under its column; body 3 sits in a crowd it pulls in.
                    sites[1][1] = UNDERGROUND_Y;
                    spawnCrowd(world, sites[2][0], sites[2][2], crowd);
                    return true;
                }

                @Override
                public boolean done() {
                    return BlastEngine.instance()
                        .activeBlasts("redmatter") == 0;
                }

                @Override
                public void check(ScenarioResult.Builder out, SoakProbe before, SoakProbe after, SoakProbe peak) {
                    super.check(out, before, after, peak);
                    int dead = 0;
                    for (Entity e : crowd) {
                        if (e.isDead) {
                            dead++;
                        }
                    }
                    out.note("the crowd body took " + dead + " of " + crowd.size() + " mobs and items");
                }

                @Override
                public void cleanup() {
                    for (Entity e : crowd) {
                        if (!e.isDead) {
                            world.removeEntity(e);
                        }
                    }
                    super.cleanup();
                }
            };
        }

        /** Spawns the crowd around a body: persistent zombies and long-lived iron ingots on the ground. */
        static void spawnCrowd(World world, int x, int z, List<Entity> crowd) {
            Random rnd = new Random(0x4ED);
            for (int i = 0; i < CROWD_MOBS + CROWD_ITEMS; i++) {
                double a = rnd.nextDouble() * Math.PI * 2;
                double d = 6 + rnd.nextDouble() * 30;
                int px = (int) Math.floor(x + Math.cos(a) * d);
                int pz = (int) Math.floor(z + Math.sin(a) * d);
                int py = world.getHeightValue(px, pz);
                Entity e;
                if (i < CROWD_MOBS) {
                    EntityZombie zombie = new EntityZombie(world);
                    zombie.func_110163_bv(); // persistent: never despawns mid-run
                    e = zombie;
                } else {
                    EntityItem item = new EntityItem(world, 0, 0, 0, new ItemStack(Items.iron_ingot, 4));
                    item.lifespan = 20 * 600;
                    e = item;
                }
                e.setLocationAndAngles(px + 0.5, py + 0.5, pz + 0.5, 0.0F, 0.0F);
                world.spawnEntityInWorld(e);
                crowd.add(e);
            }
        }
    }

    // ---- EMP over a dense base -----------------------------------------------------------------------------------

    /** A dense GT base (1,024 charged battery buffers, 7,000 cables, 8 multiblock controllers) and six EMPs at once. */
    static final class Emp extends SoakScenario {

        @Override
        public String name() {
            return "icbm.soak.emp";
        }

        @Override
        public String description() {
            return "Soak (opt-in): over a dense GT base of about 8,000 tile entities, four EMP pulses at the largest"
                + " tower radius and two EMP warheads in one tick; each pulse stays inside the device cap, the server"
                + " recovers, and no chunk ticket is left.";
        }

        @Override
        int burstLimit() {
            return 20 * 60 * 3;
        }

        @Override
        Burst plan(ScenarioContext ctx) {
            return new EmpBurst(ctx);
        }
    }

    private static final class EmpBurst implements SoakScenario.Burst {

        /** Side of the base, blocks. */
        static final int SIDE = 64;
        /** The base's floor height: in the open sky, so the terrain under it does not matter. */
        static final int Y = 200;
        /** Tile entities placed per setup tick. */
        static final int PER_TICK = 512;
        /** Ticks GT needs before a placed machine is a settled tile entity. */
        static final int WARMUP = 60;
        /** EU set into each battery buffer. */
        static final long CHARGE = 10_000L;

        final ScenarioContext ctx;
        final World world;
        final int x0;
        final int z0;
        final Holds holds = new Holds();
        final List<int[]> todo = new ArrayList<>();
        final List<int[]> placed = new ArrayList<>();
        final List<EmpPulse.Result> pulses = new ArrayList<>();
        int bufferId;
        int cableId;
        int multiId;
        int warm;
        int empBlasts;

        EmpBurst(ScenarioContext ctx) {
            this.ctx = ctx;
            this.world = ctx.world();
            this.x0 = ctx.originX() - REGION;
            this.z0 = ctx.originZ();
        }

        @Override
        public boolean prepare(int tick) {
            if (tick == 1) {
                holds.add(world, x0 + SIDE / 2, z0 + SIDE / 2, IcbmConfig.empMaxRadius + 16);
                bufferId = ItemList.Battery_Buffer_1by1_MV.get(1)
                    .getItemDamage();
                multiId = ItemList.Machine_Multi_BlastFurnace.get(1)
                    .getItemDamage();
                ItemStack cable = GTOreDictUnificator.get(OrePrefixes.cableGt01, Materials.Tin, 1L);
                cableId = cable == null ? -1 : cable.getItemDamage();
                for (int x = 0; x < SIDE; x++) {
                    for (int z = 0; z < SIDE; z++) {
                        boolean buffer = x % 2 == 0 && z % 2 == 0;
                        todo.add(new int[] { x, 0, z, buffer ? bufferId : cableId });
                        todo.add(new int[] { x, 1, z, cableId });
                    }
                }
                for (int k = 0; k < 8; k++) {
                    todo.add(new int[] { k * 8, 3, 0, multiId });
                }
                return false;
            }
            if (!todo.isEmpty()) {
                for (int k = 0; k < PER_TICK && !todo.isEmpty(); k++) {
                    int[] t = todo.remove(todo.size() - 1);
                    if (t[3] < 0) {
                        continue;
                    }
                    IGregTechTileEntity base = EmpScenarios.placeAbs(world, x0 + t[0], Y + t[1], z0 + t[2], t[3]);
                    // A multiblock with no owner throws in GT's power-fail tracker when it is removed (D239).
                    base.setOwnerName(OWNER_NAME);
                    base.setOwnerUuid(OWNER_UUID);
                    placed.add(t);
                }
                return false;
            }
            if (++warm < WARMUP) {
                return false;
            }
            if (warm == WARMUP) {
                for (int[] t : placed) {
                    if (t[3] == bufferId) {
                        TileEntity te = world.getTileEntity(x0 + t[0], Y + t[1], z0 + t[2]);
                        if (te instanceof BaseMetaTileEntity) {
                            ((BaseMetaTileEntity) te).setStoredEU(CHARGE);
                        }
                    }
                }
            }
            return true;
        }

        @Override
        public void fire(ScenarioResult.Builder out) {
            int r = IcbmConfig.empMaxRadius;
            for (int[] c : new int[][] { { 16, 16 }, { 48, 16 }, { 16, 48 }, { 48, 48 } }) {
                pulses.add(EmpPulse.fire(world, x0 + c[0] + 0.5, Y + 1.5, z0 + c[1] + 0.5, r, EmpMode.ALL));
            }
            ExplosiveDef emp = Explosives.get("emp");
            FakePlayer owner = owner(world);
            for (int[] c : new int[][] { { 32, -20 }, { 32, SIDE + 20 } }) {
                if (emp != null && IcbmBlasts
                    .detonate(world, x0 + c[0] + 0.5, Y + 1.5, z0 + c[1] + 0.5, emp, emp.maxFill(), owner)) {
                    empBlasts++;
                }
            }
        }

        @Override
        public boolean done() {
            return BlastEngine.instance()
                .activeBlasts() == 0;
        }

        @Override
        public String status() {
            return "blasts " + BlastEngine.instance()
                .activeBlasts();
        }

        @Override
        public void stop() {
            BlastEngine.instance()
                .clear();
        }

        @Override
        public void check(ScenarioResult.Builder out, SoakProbe before, SoakProbe after, SoakProbe peak) {
            out.check(cableId >= 0, "the GT tin cable resolves to a machine id");
            int buffers = 0;
            int drained = 0;
            for (int[] t : placed) {
                if (t[3] == bufferId) {
                    buffers++;
                    TileEntity te = world.getTileEntity(x0 + t[0], Y + t[1], z0 + t[2]);
                    if (te instanceof IGregTechTileEntity && ((IGregTechTileEntity) te).getStoredEU() == 0L) {
                        drained++;
                    }
                }
            }
            out.note(
                "base: " + placed.size() + " tile entities, " + buffers + " battery buffers, " + drained + " drained");
            for (int i = 0; i < pulses.size(); i++) {
                EmpPulse.Result p = pulses.get(i);
                out.note(
                    String.format(
                        Locale.ROOT,
                        "pulse %d: %d devices, %d skipped by the cap, %d tiles scanned, %.2f ms",
                        i + 1,
                        p.devices,
                        p.skippedByCap,
                        p.tilesScanned,
                        p.nanos / 1.0e6));
                out.check(
                    p.devices <= IcbmConfig.empMaxDevices,
                    "pulse " + (i + 1) + " stays inside the device cap: " + p.devices);
                out.check(p.devices > 0, "pulse " + (i + 1) + " reached the base");
            }
            out.checkEquals("both EMP warheads accepted", 2, empBlasts);
        }

        @Override
        public void cleanup() {
            for (int[] t : placed) {
                world.setBlockToAir(x0 + t[0], Y + t[1], z0 + t[2]);
            }
            placed.clear();
            holds.release();
        }
    }
}
