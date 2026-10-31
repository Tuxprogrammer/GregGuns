package com.gregguns.icbm.complex.gen;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import com.gregguns.core.command.GregGunsSubCommand;
import com.gregguns.worldgen.ComplexWriter;
import com.gregguns.worldgen.Decision;
import com.gregguns.worldgen.PlanKey;
import com.gregguns.worldgen.StructureGenerator;
import com.gregguns.worldgen.StructureGrid;
import com.gregguns.worldgen.StructurePlan;
import com.gregguns.worldgen.StructurePopulator;
import com.gregguns.worldgen.StructureState;

/**
 * {@code /gregguns locate <kind> [radius_cells] [generate]}: lists the structure sites the world seed selects near the
 * sender (kind: {@code complex}, a structure id such as {@code military_base}, or {@code any}), nearest first, with
 * what the placement ledger knows (built, skipped with its reason, or not generated yet), and with {@code generate}
 * forces the nearest undecided ones to decide. {@code locate tp <id> <class> <cellX> <cellZ>} (what a click on a built
 * site's line runs) teleports the player onto the surface at its arrival point. {@code locate rebuild <cellX> <cellZ>}
 * clears a built silo complex's box and writes it again with the running code ({@link ComplexRebuild}). Ops only:
 * the {@code /gregguns} command already requires permission level 2.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/planets-and-placement.md (Locating complexes)
public final class LocateSubCommand implements GregGunsSubCommand {

    /** Rows printed by one listing. */
    static final int MAX_LISTED = 8;

    @Override
    public String name() {
        return "locate";
    }

    @Override
    public void execute(ICommandSender sender, String[] args) {
        LocateArgs parsed;
        try {
            parsed = LocateArgs.parse(args);
        } catch (IllegalArgumentException e) {
            sender.addChatMessage(new ChatComponentTranslation(e.getMessage(), StructureLocator.MAX_RADIUS));
            return;
        }
        World world = sender.getEntityWorld();
        if (!(world instanceof WorldServer)) {
            sender.addChatMessage(new ChatComponentTranslation("gregguns.locate.disabled"));
            return;
        }
        if (parsed.mode == LocateArgs.Mode.TP) {
            teleport(sender, (WorldServer) world, parsed);
            return;
        }
        if (parsed.mode == LocateArgs.Mode.REBUILD) {
            rebuild(sender, (WorldServer) world, parsed.cellX, parsed.cellZ);
            return;
        }
        ComplexWriter writer = StructureGenerator.complexWriter();
        boolean wantComplex = parsed.kind.equals(StructureLocator.COMPLEX) || parsed.kind.equals(StructureLocator.ANY);
        boolean wantOrdinary = !parsed.kind.equals(StructureLocator.COMPLEX);
        boolean complexOn = wantComplex && writer != null && writer.enabledIn(world);
        StructureGrid grid = StructureGenerator.ordinaryGrid();
        boolean ordinaryOn = wantOrdinary && grid != null && StructureGenerator.placementAllowed(world);
        if (!complexOn && !ordinaryOn) {
            sender.addChatMessage(new ChatComponentTranslation("gregguns.locate.disabled"));
            return;
        }
        ChunkCoordinates at = sender.getPlayerCoordinates();
        StructureState state = StructureState.forWorld(world);
        long seed = world.getSeed();
        int dim = world.provider.dimensionId;
        List<StructureLocator.Candidate> found = new ArrayList<StructureLocator.Candidate>();
        int chance = writer == null ? 0 : writer.chancePercent();
        if (complexOn) {
            found.addAll(
                StructureLocator
                    .nearest(seed, dim, chance, at.posX, at.posZ, parsed.radius, p -> state.get(PlanKey.of(p))));
        }
        if (ordinaryOn) {
            // A site a silo complex covers is never drawn (the populator drops it), so it is not listed.
            found.addAll(
                StructureLocator.nearestOrdinary(
                    grid,
                    seed,
                    dim,
                    parsed.kind,
                    at.posX,
                    at.posZ,
                    parsed.radius,
                    p -> state.get(PlanKey.of(p)),
                    writer == null ? null : p -> StructurePopulator.meetsComplex(p, seed, chance, state)));
            StructureLocator.sort(found);
        }
        if (found.isEmpty()) {
            sender.addChatMessage(new ChatComponentTranslation("gregguns.locate.none", parsed.radius));
            return;
        }
        sender.addChatMessage(
            new ChatComponentTranslation(
                "gregguns.locate.header",
                Math.min(found.size(), MAX_LISTED),
                found.size(),
                parsed.radius));
        for (int i = 0; i < found.size() && i < MAX_LISTED; i++) {
            sender.addChatMessage(LocateMessages.row(i + 1, found.get(i)));
        }
        if (parsed.generate) {
            generate((WorldServer) world, sender, found);
        }
    }

    private static void generate(WorldServer world, ICommandSender sender, List<StructureLocator.Candidate> found) {
        List<StructureLocator.Candidate> todo = new ArrayList<StructureLocator.Candidate>();
        for (StructureLocator.Candidate c : found) {
            if (c.status != StructureLocator.Status.UNKNOWN) continue;
            todo.add(c);
            if (todo.size() == LocateGeneration.MAX_CELLS) break;
        }
        if (todo.isEmpty()) {
            sender.addChatMessage(new ChatComponentTranslation("gregguns.locate.generate.none"));
        } else if (!LocateGeneration.start(world, sender, todo)) {
            sender.addChatMessage(new ChatComponentTranslation("gregguns.locate.busy"));
        }
    }

    /** Starts {@link ComplexRebuild} on the built complex of a cell of the sender's dimension. */
    private static void rebuild(ICommandSender sender, WorldServer world, int cellX, int cellZ) {
        ComplexWriter writer = StructureGenerator.complexWriter();
        if (writer == null) {
            sender.addChatMessage(new ChatComponentTranslation("gregguns.locate.disabled"));
            return;
        }
        java.util.Optional<StructurePlan> plan = StructurePopulator
            .complexGridPlanAt(world.getSeed(), world.provider.dimensionId, cellX, cellZ);
        Decision d = plan.isPresent() ? StructureState.forWorld(world)
            .get(PlanKey.of(plan.get())) : null;
        if (d == null || !d.isPlaced()) {
            sender.addChatMessage(new ChatComponentTranslation("gregguns.locate.rebuild.none", cellX, cellZ));
            return;
        }
        ComplexRebuild run = new ComplexRebuild(world, sender, plan.get(), d, writer);
        if (!LocateGeneration.start(run)) {
            sender.addChatMessage(new ChatComponentTranslation("gregguns.locate.busy"));
            return;
        }
        sender.addChatMessage(
            new ChatComponentTranslation(
                "gregguns.locate.rebuild.start",
                cellX,
                cellZ,
                run.chunkCount(),
                run.windowCount()));
    }

    private static void teleport(ICommandSender sender, WorldServer world, LocateArgs a) {
        if (!(sender instanceof EntityPlayerMP)) {
            sender.addChatMessage(new ChatComponentTranslation("gregguns.locate.tp.player"));
            return;
        }
        java.util.Optional<StructurePlan> plan;
        if (a.kind.equals(StructureLocator.COMPLEX)) {
            plan = StructurePopulator.complexGridPlanAt(world.getSeed(), world.provider.dimensionId, a.cellX, a.cellZ);
        } else {
            StructureGrid grid = StructureGenerator.ordinaryGrid();
            plan = grid == null ? java.util.Optional.<StructurePlan>empty()
                : grid.planAt(world.getSeed(), world.provider.dimensionId, a.sizeClass, a.cellX, a.cellZ);
            if (plan.isPresent() && !plan.get()
                .id()
                .equals(a.kind)) plan = java.util.Optional.empty();
        }
        Decision d = plan.isPresent() ? StructureState.forWorld(world)
            .get(PlanKey.of(plan.get())) : null;
        if (d == null || !d.isPlaced()) {
            sender.addChatMessage(new ChatComponentTranslation("gregguns.locate.tp.none", a.cellX, a.cellZ));
            return;
        }
        int[] at = StructureLocator.arrival(plan.get());
        // Loading the chunk makes the height map real; the topmost non-air block is the trapdoor, roof or grass.
        world.theChunkProviderServer.loadChunk(Math.floorDiv(at[0], 16), Math.floorDiv(at[1], 16));
        int y = Math.max(world.getHeightValue(at[0], at[1]), d.groundY() + 1);
        EntityPlayerMP player = (EntityPlayerMP) sender;
        player.setPositionAndUpdate(at[0] + 0.5, y + 0.1, at[1] + 0.5);
        sender.addChatMessage(new ChatComponentTranslation("gregguns.locate.tp.ok", a.cellX, a.cellZ));
    }

    @Override
    public List<String> complete(ICommandSender sender, String[] args) {
        List<String> out = new ArrayList<String>();
        String last = args.length == 0 ? "" : args[args.length - 1].toLowerCase(java.util.Locale.ROOT);
        if (args.length <= 1) {
            for (String k : StructureLocator.kinds()) if (k.startsWith(last)) out.add(k);
            if ("rebuild".startsWith(last)) out.add("rebuild");
        } else if (args[0].equalsIgnoreCase("rebuild")) {
            // Cell coordinates: nothing to complete.
        } else if ("generate".startsWith(last)) {
            out.add("generate");
        }
        return out;
    }
}
