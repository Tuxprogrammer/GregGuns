package com.gregguns.icbm.complex.gen;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;

import com.gregguns.worldgen.Decision;
import com.gregguns.worldgen.PlanKey;
import com.gregguns.worldgen.StructureState;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * The forced generation behind {@code /gregguns locate complex generate}. A complex is decided by the first populate of
 * a window that overlaps it, so the job loads the few chunks around each chosen cell's center, one chunk per server
 * tick, and reads the ledger's record. The chunks it loaded are queued for unloading afterwards; the ledger and the
 * blocks the populate wrote are saved with the world, and the rest of the complex builds as its chunks generate.
 * Server thread only; one job at a time.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/planets-and-placement.md (Locating complexes)
public final class LocateGeneration {

    /** Chunk loads per server tick: a populate runs the whole worldgen stack, so one keeps the tick short. */
    static final int CHUNKS_PER_TICK = 1;
    /** The most cells one run generates. */
    public static final int MAX_CELLS = 5;

    private static Job active;
    private static boolean registered;

    private LocateGeneration() {}

    /** Hooks the tick and unload events, once. */
    public static void ensureRegistered() {
        if (registered) return;
        registered = true;
        Handler h = new Handler();
        FMLCommonHandler.instance()
            .bus()
            .register(h);
        MinecraftForge.EVENT_BUS.register(h);
    }

    /** @return whether a run is in progress */
    public static boolean busy() {
        return active != null;
    }

    /**
     * Starts a run over the given cells (at most {@link #MAX_CELLS}).
     *
     * @return false when a run is already going
     */
    static boolean start(WorldServer world, ICommandSender sender, List<ComplexLocator.Candidate> cells) {
        if (active != null) return false;
        ensureRegistered();
        active = new Job(world, sender, new ArrayList<ComplexLocator.Candidate>(cells));
        return true;
    }

    /** Runs the active job to its end without waiting for ticks (scenarios; the server thread is theirs). */
    public static void drain() {
        while (active != null) active.step();
    }

    /** Public because Forge's generated event handlers call it from outside the package. */
    public static final class Handler {

        @SubscribeEvent
        public void onTick(TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.END || active == null) return;
            for (int i = 0; i < CHUNKS_PER_TICK && active != null; i++) active.step();
        }

        @SubscribeEvent
        public void onUnload(WorldEvent.Unload event) {
            if (active != null && active.world == event.world) active = null;
        }
    }

    private static final class Job {

        final WorldServer world;
        private final ICommandSender sender;
        private final List<ComplexLocator.Candidate> cells;
        private int cell;
        private List<int[]> chunks;
        private int next;
        private final List<int[]> loadedByUs = new ArrayList<int[]>();
        private int built, skipped, undecided;

        Job(WorldServer world, ICommandSender sender, List<ComplexLocator.Candidate> cells) {
            this.world = world;
            this.sender = sender;
            this.cells = cells;
            sender.addChatMessage(new ChatComponentTranslation("gregguns.locate.generate.start", cells.size()));
        }

        private Decision record(ComplexLocator.Candidate c) {
            return StructureState.forWorld(world)
                .get(PlanKey.of(c.plan));
        }

        /** Loads one chunk of the current cell, or finishes the cell, or the job. */
        void step() {
            if (cell >= cells.size()) {
                sender.addChatMessage(
                    new ChatComponentTranslation("gregguns.locate.generate.done", built, skipped, undecided));
                active = null;
                return;
            }
            ComplexLocator.Candidate c = cells.get(cell);
            if (chunks == null) {
                chunks = ComplexLocator.chunksFor(c.plan);
                next = 0;
            }
            // The populate fires with the last of the inner 2 x 2; the ring after them only loads when a window was
            // already half generated and nothing decided yet.
            boolean decided = record(c) != null;
            if (decided || next >= chunks.size()) {
                finishCell(c);
                return;
            }
            int[] ch = chunks.get(next++);
            ChunkProviderServer cps = world.theChunkProviderServer;
            if (!cps.chunkExists(ch[0], ch[1])) loadedByUs.add(ch);
            cps.loadChunk(ch[0], ch[1]);
        }

        private void finishCell(ComplexLocator.Candidate c) {
            ChunkProviderServer cps = world.theChunkProviderServer;
            for (int[] ch : loadedByUs) cps.unloadChunksIfNotNearSpawn(ch[0], ch[1]);
            loadedByUs.clear();
            Decision d = record(c);
            ComplexLocator.Candidate now = new ComplexLocator.Candidate(c.plan, d, c.distance);
            if (d == null) {
                undecided++;
                sender.addChatMessage(progress(now, new ChatComponentTranslation("gregguns.locate.status.undecided")));
            } else if (d.isPlaced()) {
                built++;
                sender.addChatMessage(
                    LocateMessages
                        .clickable(progress(now, LocateMessages.status(now, ComplexLocator.arrival(c.plan))), now));
            } else {
                skipped++;
                sender.addChatMessage(progress(now, LocateMessages.status(now, null)));
            }
            cell++;
            chunks = null;
        }

        private ChatComponentTranslation progress(ComplexLocator.Candidate c,
            net.minecraft.util.IChatComponent outcome) {
            return new ChatComponentTranslation(
                "gregguns.locate.progress",
                cell + 1,
                cells.size(),
                c.cellX(),
                c.cellZ(),
                outcome);
        }
    }
}
