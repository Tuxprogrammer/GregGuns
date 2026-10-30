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
import com.gregguns.worldgen.StructurePlan;
import com.gregguns.worldgen.StructurePopulator;
import com.gregguns.worldgen.StructureState;

/**
 * {@code /gregguns locate complex [radius_cells] [generate]}: lists the silo complex cells the world seed selects near
 * the sender, nearest first, with what the placement ledger knows (built, skipped with its reason, or not generated
 * yet), and with {@code generate} forces the nearest undecided ones to decide. {@code locate complex tp <cellX>
 * <cellZ>} (what a click on a built cell's line runs) teleports the player onto the surface at the escape ladder. Ops
 * only: the {@code /gregguns} command already requires permission level 2.
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
            sender.addChatMessage(new ChatComponentTranslation(e.getMessage(), ComplexLocator.MAX_RADIUS));
            return;
        }
        World world = sender.getEntityWorld();
        ComplexWriter writer = StructureGenerator.complexWriter();
        if (!(world instanceof WorldServer) || writer == null) {
            sender.addChatMessage(new ChatComponentTranslation("gregguns.locate.disabled"));
            return;
        }
        if (parsed.mode == LocateArgs.Mode.TP) {
            teleport(sender, (WorldServer) world, parsed);
            return;
        }
        if (!writer.enabledIn(world)) {
            sender.addChatMessage(new ChatComponentTranslation("gregguns.locate.disabled"));
            return;
        }
        ChunkCoordinates at = sender.getPlayerCoordinates();
        StructureState state = StructureState.forWorld(world);
        List<ComplexLocator.Candidate> found = ComplexLocator.nearest(
            world.getSeed(),
            world.provider.dimensionId,
            writer.chancePercent(),
            at.posX,
            at.posZ,
            parsed.radius,
            p -> state.get(PlanKey.of(p)));
        if (found.isEmpty()) {
            sender.addChatMessage(new ChatComponentTranslation("gregguns.locate.none", parsed.radius));
            return;
        }
        sender.addChatMessage(
            new ChatComponentTranslation(
                "gregguns.locate.header",
                Math.min(found.size(), MAX_LISTED),
                found.size(),
                parsed.radius,
                writer.chancePercent()));
        for (int i = 0; i < found.size() && i < MAX_LISTED; i++) {
            sender.addChatMessage(LocateMessages.row(i + 1, found.get(i)));
        }
        if (parsed.generate) {
            generate((WorldServer) world, sender, found);
        }
    }

    private static void generate(WorldServer world, ICommandSender sender, List<ComplexLocator.Candidate> found) {
        List<ComplexLocator.Candidate> todo = new ArrayList<ComplexLocator.Candidate>();
        for (ComplexLocator.Candidate c : found) {
            if (c.status != ComplexLocator.Status.UNKNOWN) continue;
            todo.add(c);
            if (todo.size() == LocateGeneration.MAX_CELLS) break;
        }
        if (todo.isEmpty()) {
            sender.addChatMessage(new ChatComponentTranslation("gregguns.locate.generate.none"));
        } else if (!LocateGeneration.start(world, sender, todo)) {
            sender.addChatMessage(new ChatComponentTranslation("gregguns.locate.busy"));
        }
    }

    private static void teleport(ICommandSender sender, WorldServer world, LocateArgs a) {
        if (!(sender instanceof EntityPlayerMP)) {
            sender.addChatMessage(new ChatComponentTranslation("gregguns.locate.tp.player"));
            return;
        }
        java.util.Optional<StructurePlan> plan = StructurePopulator
            .complexGridPlanAt(world.getSeed(), world.provider.dimensionId, a.cellX, a.cellZ);
        Decision d = plan.isPresent() ? StructureState.forWorld(world)
            .get(PlanKey.of(plan.get())) : null;
        if (d == null || !d.isPlaced()) {
            sender.addChatMessage(new ChatComponentTranslation("gregguns.locate.tp.none", a.cellX, a.cellZ));
            return;
        }
        int[] at = ComplexLocator.arrival(plan.get());
        // Loading the chunk makes the height map real; the topmost non-air block is the trapdoor or the grass beside
        // it.
        world.theChunkProviderServer.loadChunk(Math.floorDiv(at[0], 16), Math.floorDiv(at[1], 16));
        int y = Math.max(world.getHeightValue(at[0], at[1]), d.groundY() + 1);
        EntityPlayerMP player = (EntityPlayerMP) sender;
        player.setPositionAndUpdate(at[0] + 0.5, y + 0.1, at[1] + 0.5);
        sender.addChatMessage(new ChatComponentTranslation("gregguns.locate.tp.ok", a.cellX, a.cellZ));
    }

    @Override
    public List<String> complete(ICommandSender sender, String[] args) {
        List<String> out = new ArrayList<String>();
        if (args.length == 1 && "complex".startsWith(args[0].toLowerCase(java.util.Locale.ROOT))) out.add("complex");
        if (args.length >= 2 && args[0].equalsIgnoreCase("complex")
            && "generate".startsWith(args[args.length - 1].toLowerCase(java.util.Locale.ROOT))) out.add("generate");
        return out;
    }
}
