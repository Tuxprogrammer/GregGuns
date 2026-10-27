package com.gregguns.icbm.missile;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S09PacketHeldItemChange;
import net.minecraft.network.play.server.S21PacketChunkData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;

import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.explosion.nuclear.FalloutBlocks;
import com.gregguns.icbm.warhead.WarheadBlocks;
import com.gregguns.icbm.warhead.WarheadCasing;
import com.gregguns.icbm.warhead.WarheadStacks;

import cpw.mods.fml.common.registry.GameRegistry;

/**
 * Client shots for the I3 items: {@code testing.icbm_i3_item_shots} puts the T3 warheads and the three missile bodies
 * in the hotbar and photographs each warhead in the hand, selecting its slot for real, then the hotbar at slot size;
 * {@code testing.icbm_i3_earth_shots} lays Irradiated Earth in a swamp and in plains, next to untouched grass, to show
 * the tint does not follow the biome. Neither is part of {@code test all}. Dev environment only.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/nuclear-and-fallout.md;
// docs/gregguns-icbm-spec/03-missiles/missile-bodies.md
final class I3ItemShotsScenarios {

    private static final int GAP = 60;

    private I3ItemShotsScenarios() {}

    static void registerAll(ScenarioRegistry r) {
        r.register(new ItemShots());
        r.register(new EarthShots());
    }

    private static EntityPlayerMP player() {
        @SuppressWarnings("unchecked")
        List<EntityPlayerMP> ps = new ArrayList<>(
            MinecraftServer.getServer()
                .getConfigurationManager().playerEntityList);
        return ps.isEmpty() ? null : ps.get(0);
    }

    private static void held(EntityPlayerMP p, int slot) {
        p.inventory.currentItem = slot;
        p.playerNetServerHandler.sendPacket(new S09PacketHeldItemChange(slot));
    }

    private static ItemStack gg(String name) {
        Item i = GameRegistry.findItem("gregguns", name);
        return i == null ? null : new ItemStack(i);
    }

    private static ItemStack warhead(WarheadCasing c, String id, int fill) {
        return WarheadStacks.make(Item.getItemFromBlock(WarheadBlocks.of(c)), c, id, fill);
    }

    private abstract static class Base implements Scenario {

        @Override
        public boolean includedInAll() {
            return false;
        }
    }

    private static final class ItemShots extends Base {

        @Override
        public String name() {
            return "testing.icbm_i3_item_shots";
        }

        @Override
        public String description() {
            return "T3 warheads and missile bodies in the hotbar and in the hand, for a connected client. Not part of test all.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            final EntityPlayerMP p = player();
            if (p == null) {
                return ScenarioResult.fail(name(), "needs a connected player");
            }
            if (MissileScenarios.moduleOff()) {
                return ScenarioResult.skip(name(), "the ICBM module is off");
            }
            final ScenarioResult.Builder out = ScenarioResult.builder(name());
            ctx.clearArea(8, 6);
            p.setGameType(WorldSettings.GameType.CREATIVE);
            p.inventory.clearInventory(null, -1);
            ItemStack[] hot = { warhead(WarheadCasing.T3, "nuclear", 4), warhead(WarheadCasing.T3, "exothermic", 4),
                gg("missile_body_t1"), gg("missile_body_t2"), gg("missile_body_t3") };
            for (int i = 0; i < hot.length; i++) {
                out.check(hot[i] != null, "hotbar item " + i + " exists");
                if (hot[i] != null) {
                    p.inventory.setInventorySlotContents(i, hot[i]);
                }
            }
            p.inventoryContainer.detectAndSendChanges();
            final MissileShotsScenario.Plan plan = new MissileShotsScenario.Plan();
            final double ex = ctx.originX() + 0.5;
            final double ez = ctx.originZ() + 0.5;
            final double ey = ctx.originY();
            plan.after(5, () -> MissileShotsScenario.signal(p, "hud", "on"));
            plan.after(10, () -> {
                held(p, 0);
                MissileShotsScenario.cam(p, ex, ey + 1.7, ez, ex, ey + 1.0, ez + 5);
            });
            plan.after(GAP, () -> MissileShotsScenario.signal(p, "shot", "i3fix-hand-nuclear-t3"));
            plan.after(10, () -> held(p, 1));
            plan.after(GAP, () -> MissileShotsScenario.signal(p, "shot", "i3fix-hand-exothermic-t3"));
            plan.after(10, () -> held(p, 4));
            plan.after(GAP, () -> MissileShotsScenario.signal(p, "shot", "i3fix-hand-body-t3"));
            plan.after(10, () -> held(p, 3));
            plan.after(GAP, () -> MissileShotsScenario.signal(p, "shot", "i3fix-hotbar-bodies"));
            plan.after(10, () -> MissileShotsScenario.signal(p, "gui", "inventory"));
            plan.after(GAP, () -> MissileShotsScenario.signal(p, "shot", "i3fix-inventory-bodies"));
            plan.after(10, () -> MissileShotsScenario.signal(p, "close"));
            return ScenarioResult.pending(name(), 3000, t -> {
                plan.run(t);
                return plan.idle() ? out.build() : null;
            });
        }
    }

    private static final class EarthShots extends Base {

        private static final int PLOT = 8;

        @Override
        public String name() {
            return "testing.icbm_i3_earth_shots";
        }

        @Override
        public String description() {
            return "Irradiated Earth beside grass in a swamp and in plains, for a connected client. Not part of test all.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            final EntityPlayerMP p = player();
            if (p == null) {
                return ScenarioResult.fail(name(), "needs a connected player");
            }
            if (FalloutBlocks.irradiated() == null) {
                return ScenarioResult.skip(name(), "fallout blocks are not registered");
            }
            final World w = ctx.world();
            ctx.clearArea(12, 6);
            p.setGameType(WorldSettings.GameType.CREATIVE);
            p.inventory.clearInventory(null, -1);
            w.setWorldTime(6000L);
            // Left half swamp, right half plains; a row of plain grass and a block of Irradiated Earth in each.
            for (int a = -12; a <= 12; a++) {
                for (int b = -2; b <= 14; b++) {
                    boolean swamp = a < 0;
                    boolean earth = Math.abs(a) >= 2 && Math.abs(a) <= 2 + PLOT - 1 && b >= 4 && b <= 4 + PLOT - 1;
                    ctx.setBlock(a, -1, b, earth ? FalloutBlocks.irradiated() : Blocks.grass, 0);
                    setBiome(
                        w,
                        ctx.originX() + a,
                        ctx.originZ() + b,
                        swamp ? BiomeGenBase.swampland : BiomeGenBase.plains);
                }
            }
            for (int cx = (ctx.originX() - 12) >> 4; cx <= (ctx.originX() + 12) >> 4; cx++) {
                for (int cz = (ctx.originZ() - 2) >> 4; cz <= (ctx.originZ() + 14) >> 4; cz++) {
                    p.playerNetServerHandler
                        .sendPacket(new S21PacketChunkData(w.getChunkFromChunkCoords(cx, cz), true, 0xFFFF));
                }
            }
            final MissileShotsScenario.Plan plan = new MissileShotsScenario.Plan();
            final double ex = ctx.originX() + 0.5;
            final double ez = ctx.originZ() + 0.5;
            final double ey = ctx.originY();
            plan.after(5, () -> MissileShotsScenario.signal(p, "hud", "off"));
            plan.after(40, () -> MissileShotsScenario.cam(p, ex, ey + 8, ez - 4, ex, ey - 1, ez + 8));
            plan.after(GAP, () -> MissileShotsScenario.signal(p, "shot", "i3fix-earth-swamp-plains"));
            plan.after(10, () -> MissileShotsScenario.cam(p, ex - 6, ey + 2.5, ez, ex - 6, ey - 1, ez + 7));
            plan.after(GAP, () -> MissileShotsScenario.signal(p, "shot", "i3fix-earth-swamp"));
            plan.after(10, () -> MissileShotsScenario.cam(p, ex + 6, ey + 2.5, ez, ex + 6, ey - 1, ez + 7));
            plan.after(GAP, () -> MissileShotsScenario.signal(p, "shot", "i3fix-earth-plains"));
            plan.after(10, () -> MissileShotsScenario.signal(p, "hud", "on"));
            return ScenarioResult.pending(name(), 3000, t -> {
                plan.run(t);
                return plan.idle() ? ScenarioResult.builder(name())
                    .note("read i3fix-earth-*.png")
                    .build() : null;
            });
        }

        /**
         * Sets a column's biome. The pack's EndlessIDs mod widens the biome array to shorts and refuses the vanilla
         * accessor, so ask for the short array by name (it is not on our compile path) and fall back to the vanilla
         * one where the mod is absent.
         */
        private static void setBiome(World w, int x, int z, BiomeGenBase biome) {
            Chunk chunk = w.getChunkFromBlockCoords(x, z);
            int index = (z & 15) << 4 | (x & 15);
            try {
                short[] wide = (short[]) Chunk.class.getMethod("getBiomeShortArray")
                    .invoke(chunk);
                wide[index] = (short) biome.biomeID;
            } catch (ReflectiveOperationException e) {
                chunk.getBiomeArray()[index] = (byte) biome.biomeID;
            }
            chunk.isModified = true;
        }
    }
}
