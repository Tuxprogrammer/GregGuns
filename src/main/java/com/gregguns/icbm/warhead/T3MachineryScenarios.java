package com.gregguns.icbm.warhead;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.world.World;

import com.gregguns.core.config.IcbmConfig;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.icbm.blast.BlastEngine;
import com.gregguns.icbm.explosive.ExplosiveDef;
import com.gregguns.icbm.explosive.Explosives;
import com.gregguns.icbm.explosive.IcbmBlasts;
import com.gregguns.icbm.warhead.effects.Effects;

/**
 * Scenarios of the machinery the T3 ender and anti-gravitational warheads add (D80): the teleport with a forced
 * landing spot, and the flying blocks (lift, cap, tile entities kept, conservation, save and load). Server side.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/blast-types.md (Ender, Anti-gravitational)
final class T3MachineryScenarios {

    private T3MachineryScenarios() {}

    static void registerAll(ScenarioRegistry registry) {
        registry.register(new EnderTeleport());
        registry.register(new AntiGravFlight());
    }

    private static boolean off() {
        return WarheadBlocks.of(WarheadCasing.T1) == null;
    }

    /** A cow at the center is pulled in and sent to the forced landing spot; terrain and a far cow are untouched. */
    private static final class EnderTeleport implements Scenario {

        @Override
        public String name() {
            return "icbm.blast.ender_teleport";
        }

        @Override
        public String description() {
            return "A full ender warhead sends a cow near the center to the forced landing spot 28 blocks away, in the"
                + " same dimension, with no fall damage and no crater; a cow outside the radius stays.";
        }

        @Override
        public int areaRadius() {
            return 30;
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (off()) {
                return ScenarioResult.fail(name(), "the ICBM content is not registered");
            }
            ctx.clearArea(30, 12);
            World w = ctx.world();
            final int ox = ctx.originX();
            final int oy = ctx.originY();
            final int oz = ctx.originZ();
            for (int dx = -26; dx <= 26; dx++) {
                for (int dz = -26; dz <= 26; dz++) {
                    ctx.setBlock(dx, -1, dz, Blocks.stone, 0);
                }
            }
            // The landing spot is inside the loaded plot, outside the pull (28 blocks from the center): a chunk
            // nothing keeps loaded would unload under the blast, which then stops, by design.
            final int tx = ox - 20;
            final int tz = oz - 20;
            EntityCow near = ctx.spawnAt(new EntityCow(w), ox + 4.5, oy, oz + 0.5, 0, 0);
            EntityCow far = ctx.spawnAt(new EntityCow(w), ox + 24.5, oy, oz - 24.5, 0, 0);
            final double farX = far.posX;
            ExplosiveDef def = Explosives.get("ender");
            Effects.forceEnderTarget(tx, oy, tz);
            near.fallDistance = 10.0F;
            if (!IcbmBlasts.detonate(w, ox + 0.5, oy + 0.5, oz + 0.5, def, def.maxFill(), null)) {
                Effects.clearEnderTarget();
                return ScenarioResult.fail(name(), "the engine did not accept the blast");
            }
            return ScenarioResult.pending(name(), 400, ticks -> {
                if (ticks < 200) {
                    return null;
                }
                Effects.clearEnderTarget();
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                double d = Math.sqrt(Math.pow(near.posX - (tx + 0.5), 2) + Math.pow(near.posZ - (tz + 0.5), 2));
                out.check(!near.isDead && near.getHealth() > 0.0F, "the teleported cow is alive");
                out.check(
                    d < 4.0,
                    "the cow stands at the landing spot, " + d
                        + " blocks off (at "
                        + near.posX
                        + ","
                        + near.posY
                        + ","
                        + near.posZ
                        + "; landing chunk loaded: "
                        + w.getChunkProvider()
                            .chunkExists(tx >> 4, tz >> 4)
                        + ")");
                out.check(near.dimension == w.provider.dimensionId, "the cow stayed in its dimension");
                out.check(w.getBlock(ox, oy - 1, oz) == Blocks.stone, "an ender warhead digs no crater");
                out.check(!far.isDead && Math.abs(far.posX - farX) < 12.0, "a cow outside the radius stays put");
                out.check(
                    BlastEngine.instance()
                        .activeBlasts() == 0,
                    "the blast finished");
                return out.build();
            });
        }
    }

    /** The flying blocks: bounded lift, tile entities stay, blocks are conserved, and an entity survives NBT. */
    private static final class AntiGravFlight implements Scenario {

        private static final int REGION = 48;

        @Override
        public String name() {
            return "icbm.blast.antigrav_flight";
        }

        @Override
        public String description() {
            return "A full anti-gravitational warhead lifts at most the configured number of plain blocks, never a"
                + " chest; they fly, fall back and every block is placed or dropped (none lost); a flying block"
                + " survives a save to NBT.";
        }

        @Override
        public int areaRadius() {
            return 30;
        }

        private static int count(World w, int ox, int oy, int oz) {
            int n = 0;
            for (int dx = -REGION; dx <= REGION; dx++) {
                for (int dz = -REGION; dz <= REGION; dz++) {
                    for (int dy = -64; dy <= 100; dy++) {
                        if (w.getBlock(ox + dx, oy + dy, oz + dz) == Blocks.quartz_block) {
                            n++;
                        }
                    }
                }
            }
            return n;
        }

        @SuppressWarnings("unchecked")
        private static int quartzItems(World w, java.util.Set<Integer> seen) {
            int n = 0;
            for (Object o : w.loadedEntityList) {
                if (o instanceof EntityItem && !seen.contains(((Entity) o).getEntityId())) {
                    ItemStack s = ((EntityItem) o).getEntityItem();
                    if (s != null && s.getItem() == net.minecraft.item.Item.getItemFromBlock(Blocks.quartz_block)) {
                        n += s.stackSize;
                    }
                }
            }
            return n;
        }

        @SuppressWarnings("unchecked")
        private static int flying(World w) {
            int n = 0;
            for (Object o : w.loadedEntityList) {
                if (o instanceof EntityFlyingBlock) {
                    n++;
                }
            }
            return n;
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (off()) {
                return ScenarioResult.fail(name(), "the ICBM content is not registered");
            }
            ctx.clearArea(30, 12);
            World w = ctx.world();
            final int ox = ctx.originX();
            final int oy = ctx.originY();
            final int oz = ctx.originZ();
            for (int dx = -26; dx <= 26; dx++) {
                for (int dz = -26; dz <= 26; dz++) {
                    ctx.setBlock(dx, -1, dz, Blocks.quartz_block, 0);
                }
            }
            ctx.setBlock(3, 0, 3, Blocks.chest, 0);
            TileEntityChest chest = (TileEntityChest) w.getTileEntity(ox + 3, oy, oz + 3);
            if (chest == null) {
                return ScenarioResult.fail(name(), "no chest tile entity");
            }
            chest.setInventorySlotContents(0, new ItemStack(Blocks.quartz_block, 5));
            for (int dy = 0; dy < 4; dy++) {
                ctx.setBlock(0, dy, 0, Blocks.quartz_block, 0);
            }
            final int quartzBefore = count(w, ox, oy, oz);
            // Items already lying about (an earlier run's leftovers) are not ours: only items new since the blast
            // count.
            final java.util.Set<Integer> seen = new java.util.HashSet<>();
            for (Object o : w.loadedEntityList) {
                seen.add(((Entity) o).getEntityId());
            }
            final int itemsBefore = 0;
            // The save and load of one flying block, before the blast: NBT round trip keeps block, meta and phase.
            ScenarioResult.Builder early = ScenarioResult.builder(name());
            EntityFlyingBlock probe = EntityFlyingBlock
                .lift(w, ox + 0.5, oy + 20.0, oz + 0.5, Blocks.cobblestone, 0, oy + 30.0, 100, 0.0, 0.0);
            NBTTagCompound tag = new NBTTagCompound();
            probe.writeToNBTOptional(tag);
            Entity back = EntityList.createEntityFromNBT(tag, w);
            early.check(
                back instanceof EntityFlyingBlock && ((EntityFlyingBlock) back).block() == Blocks.cobblestone,
                "a flying block reloads from NBT as the same block");

            boolean oldT3 = IcbmConfig.terrainDamageT3;
            int oldCap = IcbmConfig.antigravMaxBlocks;
            IcbmConfig.terrainDamageT3 = true;
            IcbmConfig.antigravMaxBlocks = 512;
            ExplosiveDef def = Explosives.get("antigrav");
            if (!IcbmBlasts.detonate(w, ox + 0.5, oy + 0.5, oz + 0.5, def, def.maxFill(), null)) {
                IcbmConfig.terrainDamageT3 = oldT3;
                return ScenarioResult.fail(name(), "the engine did not accept the blast");
            }
            final int[] peak = { 0 };
            final boolean[] midChecked = { false };
            return ScenarioResult.pending(name(), 1500, ticks -> {
                peak[0] = Math.max(peak[0], flying(w));
                if (ticks == 150 && !midChecked[0]) {
                    midChecked[0] = true;
                    early.check(peak[0] > 0, "blocks were lifted into flying blocks");
                    early.check(
                        peak[0] <= IcbmConfig.antigravMaxBlocks,
                        "at most " + IcbmConfig.antigravMaxBlocks + " flying blocks at once, saw " + peak[0]);
                    early.check(w.getBlock(ox, oy, oz) != Blocks.quartz_block, "the quartz column was lifted first");
                    early.check(
                        w.getBlock(ox + 3, oy, oz + 3) == Blocks.chest && chest == w.getTileEntity(ox + 3, oy, oz + 3)
                            && ((IInventory) chest).getStackInSlot(0) != null
                            && ((IInventory) chest).getStackInSlot(0).stackSize == 5,
                        "the chest and its contents were not lifted");
                }
                if (ticks < 1200 || flying(w) > 0 && ticks < 1450) {
                    return null;
                }
                IcbmConfig.terrainDamageT3 = oldT3;
                IcbmConfig.antigravMaxBlocks = oldCap;
                early.check(flying(w) == 0, "every flying block has landed, " + flying(w) + " left");
                int quartzAfter = count(w, ox, oy, oz);
                int itemsAfter = quartzItems(w, seen);
                early.check(
                    quartzAfter + itemsAfter >= quartzBefore + itemsBefore,
                    "no block is lost: " + quartzBefore
                        + " quartz before, "
                        + quartzAfter
                        + " placed and "
                        + (itemsAfter - itemsBefore)
                        + " dropped after");
                early.check(
                    quartzAfter + itemsAfter <= quartzBefore + itemsBefore,
                    "no block is duplicated: " + (quartzAfter + itemsAfter)
                        + " against "
                        + (quartzBefore + itemsBefore));
                early.note("peak flying blocks " + peak[0] + " origin " + ox + "," + oy + "," + oz);
                return early.build();
            });
        }
    }
}
