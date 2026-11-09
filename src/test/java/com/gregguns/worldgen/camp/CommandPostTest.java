package com.gregguns.worldgen.camp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;

import org.junit.jupiter.api.Test;

import com.gregguns.entity.npc.NpcMob;
import com.gregguns.worldgen.ClippingSink;
import com.gregguns.worldgen.FlagPos;
import com.gregguns.worldgen.PopulateWindow;
import com.gregguns.worldgen.WindowedSink;
import com.gregguns.worldgen.layout.PaletteKeys;

/** The red and blue faction and the command pad around the flag (D353). */
class CommandPostTest {

    private final Map<String, Block> blocks = new HashMap<>();
    private final PaletteKeys keys = PaletteKeys
        .resolve(null, name -> blocks.computeIfAbsent(name, n -> new Block(Material.rock) {}), s -> {});

    /** One write with its block and metadata. */
    private static final class Put {

        final int x;
        final int y;
        final int z;
        final Block block;
        final int meta;

        Put(int x, int y, int z, Block block, int meta) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.block = block;
            this.meta = meta;
        }
    }

    private List<Put> run(PlacedCamp camp, CampFaction faction) {
        List<Put> out = new ArrayList<>();
        int c0x = Math.floorDiv(camp.boundsMinX() - 8, 16);
        int c1x = Math.floorDiv(camp.boundsMaxX() - 8, 16);
        int c0z = Math.floorDiv(camp.boundsMinZ() - 8, 16);
        int c1z = Math.floorDiv(camp.boundsMaxZ() - 8, 16);
        final int y = camp.anchorY();
        for (int cx = c0x; cx <= c1x; cx++) {
            for (int cz = c0z; cz <= c1z; cz++) {
                PopulateWindow window = new PopulateWindow(cx, cz);
                WindowedSink.Target target = new WindowedSink.Target() {

                    @Override
                    public void setBlock(int x, int y, int z, Block block, int meta) {
                        out.add(new Put(x, y, z, block, meta));
                    }

                    @Override
                    public void setTile(int x, int y, int z, TileEntity tile) {}

                    @Override
                    public void spawn(Entity entity, double x, double y, double z) {}
                };
                ClippingSink sink = new ClippingSink(window, target);
                CommandPost.write(sink, camp, window, (x, z) -> y, faction, keys);
                assertEquals(0, sink.rejectedWrites(), "every write is asked inside its window");
            }
        }
        return out;
    }

    @Test
    void theFactionIsAPureFunctionOfTheSeedAndTheChance() {
        int red = 0;
        for (long seed = 0; seed < 2000; seed++) {
            assertSame(CampFaction.of(seed, 20), CampFaction.of(seed, 20), "the same seed, the same side");
            assertSame(CampFaction.BLUE, CampFaction.of(seed, 0));
            assertSame(CampFaction.RED, CampFaction.of(seed, 100));
            red += CampFaction.of(seed, 20) == CampFaction.RED ? 1 : 0;
        }
        assertTrue(red > 320 && red < 480, "about one base in five is red: " + red);
        assertSame(CampFaction.RED, CampFaction.byFlagMeta(1));
        assertSame(CampFaction.BLUE, CampFaction.byFlagMeta(0));
        assertTrue(CampFaction.RED.hasCommander() && !CampFaction.BLUE.hasCommander());
        assertEquals(NpcMob.DICTATOR_DAVE.registryName(), CommandPost.COMMANDER);
    }

    @Test
    void theBluePadIsTwentyFiveBlocksSunkAroundTheFlag() {
        PlacedCamp camp = CampPlan.generate(9)
            .place(-40, 70, 25, 0);
        FlagPos flag = camp.flagPos();
        List<Put> w = run(camp, CampFaction.BLUE);
        assertEquals(25, w.size());
        Block wool = keys.get("minecraft:wool").block;
        int ring = 0;
        for (Put x : w) {
            assertEquals(69, x.y, "the pad replaces the ground under the first air level");
            assertTrue(Math.abs(x.x - flag.x) <= 2 && Math.abs(x.z - flag.z) <= 2, "around the flag");
            assertEquals(CampFaction.BLUE.woolMeta(), x.meta);
            ring += x.block == wool ? 1 : 0;
        }
        assertEquals(16, ring, "a wool ring around 9 clay blocks");
    }

    @Test
    void aRedBaseAddsFourStandardsAndThePadTurnsWithTheBase() {
        for (long seed = 0; seed < 6; seed++) {
            CampPlan plan = CampPlan.generate(seed);
            int base = -1;
            for (int rot = 0; rot < 4; rot++) {
                PlacedCamp camp = plan.place(13, 64, -77, rot);
                List<Put> w = run(camp, CampFaction.RED);
                if (base < 0) {
                    base = w.size();
                }
                assertEquals(25 + 4 * 3, w.size(), "pad plus four standards of three: seed " + seed);
                assertEquals(base, w.size(), "rotation only moves it");
                Block fence = keys.get("minecraft:fence").block;
                FlagPos flag = camp.flagPos();
                int poles = 0;
                for (Put x : w) {
                    if (x.block == fence) {
                        poles++;
                        assertEquals(3, Math.abs(x.x - flag.x));
                        assertEquals(3, Math.abs(x.z - flag.z));
                    } else {
                        assertEquals(CampFaction.RED.woolMeta(), x.meta);
                    }
                }
                assertEquals(8, poles);
                int[] post = CommandPost.post(camp);
                assertEquals(2, Math.abs(post[0]) + Math.abs(post[2]), "the post is on the pad, 2 from the flag");
                assertEquals(0, post[1]);
            }
        }
    }
}
