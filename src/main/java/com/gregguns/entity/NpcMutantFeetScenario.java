package com.gregguns.entity;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;

import com.gregguns.GregGuns;
import com.gregguns.core.testing.AutotestSignal;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.entity.npc.ItemNpcEgg;
import com.gregguns.entity.npc.SuperMutantBasic;
import com.gregguns.entity.npc.SuperMutantTier;

/**
 * {@code npc.mutant_feet}: a visual check that the three Super Mutants stand on the floor, for a connected client (see
 * {@code docs/testing/README.md}). The Basic, Heavy and Elite stand side by side on a stone floor, 4 blocks apart along
 * X at Z = 0, with their AI cleared. The server places the player and asks the client for screenshots:
 * <ul>
 * <li>{@code mutant-feet-stand-line}: all three standing, seen side on from Z = -6 with the eye 0.12 blocks above the
 * floor (the player stands on a slab in a pit), so the floor line runs through the middle of the frame;</li>
 * <li>{@code mutant-feet-stand-elite}: the Elite's feet from 2 blocks, at the same eye height;</li>
 * <li>{@code mutant-feet-stand-overview}: all three from a normal standing eye, looking down a little;</li>
 * <li>{@code mutant-feet-walk-line-1..4} and {@code mutant-feet-walk-elite-1..3}: the same views while each mutant
 * walks back and forth along X over 3 blocks, so some frames catch the legs mid-stride.</li>
 * </ul>
 * The entity side is checked as well: each mutant's {@code posY} and bounding box bottom must sit on the floor top
 * while standing and while walking, and the log records both with the floor Y ({@code [mutant-feet]} lines), so a sunk
 * render can be told from a sunk entity. Not part of {@code test all}: it needs a connected client. Dev environment
 * only, server side. The mutants are removed and the difficulty restored on every exit path.
 */
// Spec: docs/techguns-spec/10-entities-npcs/per-mob.md (14. Super Mutants); rendering-and-models.md section 2
final class NpcMutantFeetScenario implements Scenario {

    private static final String NAME = "npc.mutant_feet";
    private static final SuperMutantTier[] TIERS = { SuperMutantTier.BASIC, SuperMutantTier.HEAVY,
        SuperMutantTier.ELITE };
    /** Lane centers along X, relative to the origin: Basic, Heavy, Elite. */
    private static final int[] LANES = { -4, 0, 4 };
    /** Half the walk along X, in blocks. */
    private static final double WALK = 1.5;
    /** Standing mutants face +X (yaw -90), side on to a camera on the -Z side looking +Z (yaw 0). */
    private static final float FACE_X = -90.0F;
    private static final int WALK_START = 230;
    private static final int END = 400;
    private static final double EPS = 1.0e-3;

    static void register(ScenarioRegistry registry) {
        registry.register(new NpcMutantFeetScenario());
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "Stands the three Super Mutants on a stone floor, then walks them, and screenshots their feet side on at"
            + " floor level; checks posY and the bounding box bottom against the floor. Needs 1 client; not part of"
            + " test all.";
    }

    @Override
    public boolean includedInAll() {
        return false;
    }

    @Override
    public int areaRadius() {
        return 12;
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
        World world = ctx.world();
        ctx.clearArea(12, 8);
        for (int dx = -10; dx <= 10; dx++) {
            for (int dz = -10; dz <= 6; dz++) {
                ctx.setBlock(dx, -1, dz, Blocks.stone, 0);
                ctx.setBlock(dx, -2, dz, Blocks.stone, 0);
            }
        }
        // Two camera pits: the player stands on a bottom slab one block down, so the eye is 0.12 above the floor top.
        pit(ctx, 0, -6);
        pit(ctx, 4, -2);
        int ox = ctx.originX();
        int oy = ctx.originY();
        int oz = ctx.originZ();
        // Hostile mobs despawn at once in a peaceful world; the scenario puts it back when it ends.
        EnumDifficulty prior = world.difficultySetting;
        world.difficultySetting = EnumDifficulty.NORMAL;
        long priorTime = world.getWorldTime();
        SuperMutantBasic[] mutants = new SuperMutantBasic[TIERS.length];
        ScenarioResult.Builder out = ScenarioResult.builder(NAME);
        for (int i = 0; i < TIERS.length; i++) {
            Entity e = ItemNpcEgg.spawn(world, TIERS[i].mob(), ox + LANES[i] + 0.5, oy, oz + 0.5, null);
            if (!(e instanceof SuperMutantBasic)) {
                out.check(false, "the egg path should spawn a " + TIERS[i] + " mutant, got " + e);
                if (e != null) {
                    world.removeEntity(e);
                }
                continue;
            }
            SuperMutantBasic m = (SuperMutantBasic) e;
            m.tasks.taskEntries.clear();
            m.targetTasks.taskEntries.clear();
            m.setAttackTarget(null);
            mutants[i] = m;
        }
        boolean[] heading = new boolean[TIERS.length];
        return ScenarioResult.pending(NAME, END + 20, t -> {
            world.setWorldTime(6000L);
            boolean walking = t >= WALK_START;
            for (int i = 0; i < mutants.length; i++) {
                SuperMutantBasic m = mutants[i];
                if (m == null) {
                    continue;
                }
                m.setAttackTarget(null);
                double lane = ox + LANES[i] + 0.5;
                if (!walking) {
                    hold(m, lane, m.posY, oz + 0.5);
                } else {
                    // Back and forth along X: the move helper turns the body to the heading, so the legs swing side on.
                    double target = heading[i] ? lane + WALK : lane - WALK;
                    if (Math.abs(m.posX - target) < 0.3) {
                        heading[i] = !heading[i];
                        target = heading[i] ? lane + WALK : lane - WALK;
                    }
                    m.posZ = oz + 0.5;
                    m.motionZ = 0.0;
                    m.getMoveHelper()
                        .setMoveTo(target, oy, oz + 0.5, 1.0);
                }
            }
            switch (t) {
                case 2 -> signal(player, "hud", "off");
                case 40 -> {
                    footing(out, mutants, oy, "standing");
                    player.playerNetServerHandler.setPlayerLocation(ox + 0.5, oy - 1.5, oz - 5.5, 0.0F, 0.0F);
                }
                case 100 -> signal(player, "shot", "mutant-feet-stand-line");
                case 105 -> player.playerNetServerHandler.setPlayerLocation(ox + 4.5, oy - 1.5, oz - 1.5, 0.0F, 0.0F);
                case 160 -> signal(player, "shot", "mutant-feet-stand-elite");
                case 165 -> player.playerNetServerHandler.setPlayerLocation(ox + 0.5, oy, oz - 7.5, 0.0F, 12.0F);
                case 220 -> signal(player, "shot", "mutant-feet-stand-overview");
                case WALK_START -> player.playerNetServerHandler
                    .setPlayerLocation(ox + 0.5, oy - 1.5, oz - 5.5, 0.0F, 0.0F);
                case 290, 297, 304, 311 -> {
                    log(mutants, oy, "walk-line-" + ((t - 290) / 7 + 1));
                    signal(player, "shot", "mutant-feet-walk-line-" + ((t - 290) / 7 + 1));
                }
                case 315 -> player.playerNetServerHandler.setPlayerLocation(ox + 4.5, oy - 1.5, oz - 1.5, 0.0F, 0.0F);
                case 370, 376, 382 -> {
                    log(mutants, oy, "walk-elite-" + ((t - 370) / 6 + 1));
                    signal(player, "shot", "mutant-feet-walk-elite-" + ((t - 370) / 6 + 1));
                }
                case 390 -> footing(out, mutants, oy, "walking");
                default -> {}
            }
            if (t < END) {
                return null;
            }
            for (SuperMutantBasic m : mutants) {
                if (m != null) {
                    world.removeEntity(m);
                }
            }
            world.difficultySetting = prior;
            world.setWorldTime(priorTime);
            signal(player, "hud", "on");
            player.playerNetServerHandler.setPlayerLocation(ox + 0.5, oy, oz - 5.5, 0.0F, 0.0F);
            return out.note("read the client screenshots mutant-feet-*.png")
                .build();
        });
    }

    /** A 1x1 pit with a bottom stone slab in it, one block below the floor, at (dx, dz) relative to the origin. */
    private static void pit(ScenarioContext ctx, int dx, int dz) {
        ctx.setBlock(dx, -1, dz, Blocks.air, 0);
        ctx.setBlock(dx, -2, dz, Blocks.stone_slab, 0);
    }

    /** Keeps a standing mutant on its spot, side on, with no motion. */
    private static void hold(SuperMutantBasic m, double x, double y, double z) {
        m.setLocationAndAngles(x, y, z, FACE_X, 0.0F);
        m.prevRotationYaw = FACE_X;
        m.rotationYawHead = FACE_X;
        m.prevRotationYawHead = FACE_X;
        m.renderYawOffset = FACE_X;
        m.prevRenderYawOffset = FACE_X;
        m.motionX = 0.0;
        m.motionZ = 0.0;
    }

    /** Checks that each mutant stands on the floor top: posY and the box bottom at {@code floorTop}, on the ground. */
    private static void footing(ScenarioResult.Builder out, SuperMutantBasic[] mutants, int floorTop, String when) {
        log(mutants, floorTop, when);
        for (int i = 0; i < mutants.length; i++) {
            SuperMutantBasic m = mutants[i];
            if (m == null) {
                continue;
            }
            out.check(
                Math.abs(m.posY - floorTop) < EPS,
                TIERS[i] + " " + when + ": posY " + m.posY + " should be the floor top " + floorTop);
            out.check(
                Math.abs(m.boundingBox.minY - floorTop) < EPS,
                TIERS[i] + " " + when + ": box bottom " + m.boundingBox.minY + " should be the floor top " + floorTop);
            out.check(m.onGround, TIERS[i] + " " + when + ": should be on the ground");
        }
    }

    private static void log(SuperMutantBasic[] mutants, int floorTop, String when) {
        for (int i = 0; i < mutants.length; i++) {
            SuperMutantBasic m = mutants[i];
            if (m == null) {
                continue;
            }
            GregGuns.LOG.info(
                "[mutant-feet] {} {} posY={} boxMinY={} floorTop={} onGround={} limbSwingAmount={} dead={} age={} inWorld={}"
                    + " difficulty={}",
                when,
                TIERS[i],
                String.format("%.4f", m.posY),
                String.format("%.4f", m.boundingBox.minY),
                floorTop,
                m.onGround,
                String.format("%.3f", m.limbSwingAmount),
                m.isDead,
                m.ticksExisted,
                m.worldObj.loadedEntityList.contains(m),
                m.worldObj.difficultySetting);
        }
    }

    private static void signal(EntityPlayerMP player, String command, String... args) {
        player.addChatMessage(new ChatComponentText(AutotestSignal.format(command, args)));
    }
}
