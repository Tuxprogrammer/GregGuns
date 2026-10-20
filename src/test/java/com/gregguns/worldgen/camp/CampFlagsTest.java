package com.gregguns.worldgen.camp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;

import org.junit.jupiter.api.Test;

import com.gregguns.utility.camp.TileCampFlag;
import com.gregguns.worldgen.ClippingSink;
import com.gregguns.worldgen.Decision;
import com.gregguns.worldgen.FlagPos;
import com.gregguns.worldgen.PlanKey;
import com.gregguns.worldgen.PlanLedger;
import com.gregguns.worldgen.PopulateWindow;
import com.gregguns.worldgen.SizeClass;
import com.gregguns.worldgen.WindowedSink;

/** The flag post-step: one flag, initialized with the saved cap and names, recorded, never an NPC (Q12). */
class CampFlagsTest {

    private static final Block FLAG = new Block(Material.rock) {};

    /** Remembers what was written; counts spawns so a test can prove there were none. */
    private static final class Capture implements WindowedSink.Target {

        final List<int[]> blocks = new ArrayList<>();
        final List<TileEntity> tiles = new ArrayList<>();
        int spawns;

        @Override
        public void setBlock(int x, int y, int z, Block block, int meta) {
            blocks.add(new int[] { x, y, z });
        }

        @Override
        public void setTile(int x, int y, int z, TileEntity tile) {
            tiles.add(tile);
        }

        @Override
        public void spawn(Entity entity, double x, double y, double z) {
            spawns++;
        }
    }

    private static PopulateWindow windowOf(PlacedCamp camp) {
        int cx = Math.floorDiv(camp.flagPos().x - 8, 16);
        int cz = Math.floorDiv(camp.flagPos().z - 8, 16);
        return new PopulateWindow(cx, cz);
    }

    @Test
    void placesInitializesAndRecordsExactlyOneFlag() {
        for (int rot = 0; rot < 4; rot++) {
            PlacedCamp camp = CampPlan.generate(21, 48, 36)
                .place(300, 70, -200, rot);
            PopulateWindow w = windowOf(camp);
            Capture cap = new Capture();
            PlanLedger ledger = new PlanLedger();
            String key = PlanKey.of(SizeClass.BIG, 1, 2);
            ledger.decideOnce(key, () -> Decision.placed(70));

            boolean placed = CampFlags.place(
                new ClippingSink(w, cap),
                w,
                FLAG,
                camp.flagSpec(0.75F),
                TileCampFlag::new,
                CampFlags.into(ledger, key));

            assertTrue(placed);
            assertEquals(1, cap.blocks.size());
            assertEquals(camp.flagPos().x, cap.blocks.get(0)[0]);
            assertEquals(camp.flagPos().z, cap.blocks.get(0)[2]);
            assertEquals(1, cap.tiles.size());
            TileCampFlag tile = (TileCampFlag) cap.tiles.get(0);
            assertTrue(tile.isInitialized());
            assertEquals(CampCap.cap(camp.footprintX(), camp.footprintZ(), 0.75F), tile.maxNpcs());
            assertEquals(Arrays.asList("ArmySoldier"), tile.npcNames());
            assertEquals(
                camp.plan()
                    .spawnPoints().length / 3,
                tile.spawnPointCount());
            assertEquals(
                Arrays.asList(new FlagPos(camp.flagPos().x, 70, camp.flagPos().z)),
                ledger.get(key)
                    .flags());
            assertEquals(0, cap.spawns, "worldgen never spawns an NPC (Q12)");
        }
    }

    @Test
    void doesNothingOutsideTheFlagsWindow() {
        PlacedCamp camp = CampPlan.generate(21, 48, 36)
            .place(300, 70, -200, 0);
        PopulateWindow elsewhere = new PopulateWindow(windowOf(camp).chunkX() + 3, windowOf(camp).chunkZ());
        Capture cap = new Capture();
        boolean placed = CampFlags
            .place(new ClippingSink(elsewhere, cap), elsewhere, FLAG, camp.flagSpec(0.75F), TileCampFlag::new, p -> {
                throw new AssertionError("must not record");
            });
        assertFalse(placed);
        assertTrue(cap.blocks.isEmpty() && cap.tiles.isEmpty());
    }

    @Test
    void aMissingBlockOrTileIsSkippedNotDereferenced() {
        PlacedCamp camp = CampPlan.generate(21, 48, 36)
            .place(300, 70, -200, 0);
        PopulateWindow w = windowOf(camp);
        Capture cap = new Capture();
        CampFlags.Recorder never = p -> {
            throw new AssertionError("must not record");
        };
        assertFalse(CampFlags.place(new ClippingSink(w, cap), w, null, camp.flagSpec(0.75F), TileCampFlag::new, never));
        assertFalse(CampFlags.place(new ClippingSink(w, cap), w, FLAG, camp.flagSpec(0.75F), () -> null, never));
        assertTrue(cap.tiles.isEmpty(), "WGEN-12: no tile setup without a tile");
    }
}
