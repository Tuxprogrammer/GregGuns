package com.gregguns.icbm.warhead;

import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;

import com.gregguns.core.config.IcbmConfig;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.icbm.explosive.ExplosiveDef;
import com.gregguns.icbm.explosive.Explosives;
import com.gregguns.icbm.explosive.IcbmBlasts;
import com.mojang.authlib.GameProfile;

/**
 * Scenarios of the warhead content: the item NBT, placing and the collision box, the fuse by redstone and by hand,
 * detonation, breaking as one piece, the multi-block drop, and one scenario per T1 and T2 blast type. Dev environment
 * only. Server side.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/warhead-blocks.md (GGS-ICBM-02-10 to -14), blast-types.md
public final class WarheadScenarios {

    private WarheadScenarios() {}

    /** @param registry where to add the scenarios */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new ItemNbt());
        registry.register(new PlaceAndBox());
        registry.register(new FuseRedstone());
        registry.register(new FuseByHand());
        registry.register(new BreakAsOne());
        registry.register(new FuseFullFillBesideRedstone());
        registry.register(new LitStateSyncs());
        registry.register(new InfernalHealth("condensed", 5, 1080.0));
        registry.register(new InfernalHealth("thermobaric", 20, 2700.0));
        // D80: the T3 target is 8,000 at ground zero of a full casing (D34), against a boss-sized pool.
        registry.register(new InfernalHealth("exothermic", 64, 7200.0));
        registry.register(new InfernalHealth("endothermic", 64, 7200.0));
        for (WarheadBlast blast : WarheadBlast.values()) {
            registry.register(new BlastType(blast));
        }
        registry.register(new BlockDamageOff());
        registry.register(new WarheadShotsScenario());
        NuclearScenarios.registerAll(registry);
    }

    private static boolean moduleOff() {
        return WarheadBlocks.of(WarheadCasing.T1) == null;
    }

    private static Block block(WarheadCasing c) {
        return WarheadBlocks.of(c);
    }

    private static TileWarhead place(ScenarioContext ctx, WarheadCasing casing, int dx, int dy, int dz, String id,
        int fill) {
        ctx.setBlock(dx, dy, dz, block(casing), 0);
        TileEntity t = ctx.world()
            .getTileEntity(ctx.originX() + dx, ctx.originY() + dy, ctx.originZ() + dz);
        if (!(t instanceof TileWarhead)) {
            return null;
        }
        ItemStack stack = WarheadStacks.make(Item.getItemFromBlock(block(casing)), casing, id, fill);
        ((TileWarhead) t).init(casing, stack.getTagCompound(), null);
        return (TileWarhead) t;
    }

    private abstract static class Base implements Scenario {

        ScenarioResult needsModule() {
            return moduleOff() ? ScenarioResult.fail(name(), "the ICBM content is not registered") : null;
        }
    }

    /** The NBT clamps to the casing cap, an unknown explosive reads as condensed (GGS-ICBM-02-01, -02). */
    private static final class ItemNbt extends Base {

        @Override
        public String name() {
            return "icbm.warhead.item_nbt";
        }

        @Override
        public String description() {
            return "A warhead stack clamps its fill to the casing cap and reads an unknown explosive as condensed.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (needsModule() != null) {
                return needsModule();
            }
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            Item t1 = Item.getItemFromBlock(block(WarheadCasing.T1));
            ItemStack over = WarheadStacks.make(t1, WarheadCasing.T1, "condensed", 999);
            out.checkEquals(
                "T1 fill clamps to its cap",
                5,
                WarheadStacks.read(WarheadCasing.T1, over)
                    .fill());
            ItemStack bad = WarheadStacks.make(t1, WarheadCasing.T1, "nonsense", 3);
            out.checkEquals(
                "unknown explosive reads as condensed",
                "condensed",
                WarheadStacks.read(WarheadCasing.T1, bad)
                    .explosive());
            ItemStack wrongTier = WarheadStacks.make(t1, WarheadCasing.T1, "thermobaric", 3);
            out.checkEquals(
                "a T2 explosive does not sit in a T1 casing",
                "condensed",
                WarheadStacks.read(WarheadCasing.T1, wrongTier)
                    .explosive());
            Item t2 = Item.getItemFromBlock(block(WarheadCasing.T2));
            out.checkEquals(
                "T2 caps at 20",
                20,
                WarheadStacks.read(WarheadCasing.T2, WarheadStacks.make(t2, WarheadCasing.T2, "thermobaric", 99))
                    .fill());
            return out.build();
        }
    }

    /** Placing fills one block with a non-cube box that matches the column. */
    private static final class PlaceAndBox extends Base {

        @Override
        public String name() {
            return "icbm.warhead.place_and_box";
        }

        @Override
        public String description() {
            return "T1 and T2 warheads place in one block, are not full cubes, and the box is a centered column.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (needsModule() != null) {
                return needsModule();
            }
            ctx.clearArea(3, 4);
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            for (WarheadCasing c : WarheadCasing.values()) {
                int dx = c.ordinal() * 2;
                TileWarhead tile = place(ctx, c, dx, 0, 0, c == WarheadCasing.T1 ? "condensed" : "thermobaric", 99);
                out.check(tile != null, c + " has a warhead tile");
                if (tile == null) {
                    continue;
                }
                World w = ctx.world();
                Block b = block(c);
                out.check(!b.isOpaqueCube(), c + " is not an opaque cube");
                net.minecraft.util.AxisAlignedBB box = b
                    .getCollisionBoundingBoxFromPool(w, ctx.originX() + dx, ctx.originY(), ctx.originZ());
                double width = box.maxX - box.minX;
                out.check(
                    Math.abs(width - (c.boxWidth() + WarheadGeometry.MARGIN)) < 1e-4,
                    c + " collision width " + width);
                out.check(Math.abs(box.maxY - box.minY - 1.0) < 1e-4, c + " box is one block tall");
                out.check(width < 1.0, c + " box is narrower than a cube");
                out.checkEquals(
                    c + " fill clamped",
                    c.fillCap(),
                    tile.data()
                        .fill());
            }
            return out.build();
        }
    }

    private static ScenarioResult awaitGone(ScenarioContext ctx, String name, int dx, int dy, int dz, String what) {
        World world = ctx.world();
        return ScenarioResult.pending(name, 200, ticks -> {
            boolean gone = world.getBlock(ctx.originX() + dx, ctx.originY() + dy, ctx.originZ() + dz)
                != block(WarheadCasing.T1);
            if (!gone && ticks < 190) {
                return null;
            }
            ScenarioResult.Builder out = ScenarioResult.builder(name);
            out.check(gone, what);
            return out.build();
        });
    }

    /** A redstone block next to a warhead lights the fuse, which runs down and detonates. */
    private static final class FuseRedstone extends Base {

        @Override
        public String name() {
            return "icbm.warhead.fuse_redstone";
        }

        @Override
        public String description() {
            return "A warhead next to a redstone block gets a fuse, counts it down and goes off.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (needsModule() != null) {
                return needsModule();
            }
            ctx.clearArea(4, 4);
            TileWarhead tile = place(ctx, WarheadCasing.T1, 0, 0, 0, "smoke", 1);
            ctx.setBlock(1, 0, 0, Blocks.redstone_block, 0);
            // setBlock does not always notify neighbors in scenarios: poke it like a world update would.
            ctx.world()
                .notifyBlocksOfNeighborChange(ctx.originX() + 1, ctx.originY(), ctx.originZ(), Blocks.redstone_block);
            if (tile == null || !tile.armed()) {
                return ScenarioResult.fail(name(), "redstone did not light the fuse");
            }
            return awaitGone(ctx, name(), 0, 0, 0, "the warhead is gone after its fuse");
        }
    }

    /** Flint and steel lights it, a bare hand does not, and a lit fuse is not made longer. */
    private static final class FuseByHand extends Base {

        @Override
        public String name() {
            return "icbm.warhead.fuse_hand";
        }

        @Override
        public String description() {
            return "A bare click does nothing; flint and steel lights the fuse; a second light does not extend it.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (needsModule() != null) {
                return needsModule();
            }
            ctx.clearArea(4, 4);
            TileWarhead tile = place(ctx, WarheadCasing.T1, 0, 0, 0, "smoke", 1);
            FakePlayer p = new FakePlayer(
                (net.minecraft.world.WorldServer) ctx.world(),
                new GameProfile(UUID.nameUUIDFromBytes("[GGWarhead]".getBytes()), "[GGWarhead]"));
            int x = ctx.originX();
            int y = ctx.originY();
            int z = ctx.originZ();
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            p.setCurrentItemOrArmor(0, null);
            boolean bare = block(WarheadCasing.T1).onBlockActivated(ctx.world(), x, y, z, p, 1, 0.5F, 0.5F, 0.5F);
            out.check(!bare && !tile.armed(), "a bare click does not light it");
            p.setCurrentItemOrArmor(0, new ItemStack(Items.flint_and_steel));
            boolean lit = block(WarheadCasing.T1).onBlockActivated(ctx.world(), x, y, z, p, 1, 0.5F, 0.5F, 0.5F);
            out.check(lit && tile.armed(), "flint and steel lights it");
            int lit120 = tile.litFuseTicks();
            out.checkEquals("fuse length follows the radius (D51)", lit120, tile.fuse());
            out.check(lit120 > 60, "the fuse is longer than the 3 s reaction time");
            tile.arm(lit120 + 50, null);
            out.check(tile.fuse() <= lit120, "a lit fuse is never made longer");
            tile.arm(WarheadFuse.CHAIN_TICKS, null);
            out.checkEquals("a chain reaction shortens it", WarheadFuse.CHAIN_TICKS, tile.fuse());
            return out.build();
        }
    }

    private static void killItems(World w, int x, int y, int z) {
        for (Object o : w.getEntitiesWithinAABB(
            net.minecraft.entity.item.EntityItem.class,
            net.minecraft.util.AxisAlignedBB.getBoundingBox(x - 6, y - 2, z - 6, x + 6, y + 6, z + 6))) {
            ((net.minecraft.entity.Entity) o).setDead();
        }
    }

    /** Breaking a tall warhead from any cell removes every cell and drops one item with the NBT. */
    private static final class BreakAsOne extends Base {

        @Override
        public String name() {
            return "icbm.warhead.break_as_one";
        }

        @Override
        public String description() {
            return "A 3-block warhead breaks as one from its core, middle or top cell: one item with the fill, no orphan filler.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (needsModule() != null) {
                return needsModule();
            }
            ctx.clearArea(4, 6);
            World w = ctx.world();
            TileWarhead tile = place(ctx, WarheadCasing.T1, 0, 0, 0, "shrapnel", 4);
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            if (tile == null) {
                return ScenarioResult.fail(name(), "no tile");
            }
            int x = ctx.originX();
            int y = ctx.originY();
            int z = ctx.originZ();
            tile.setHeight(3);
            out.check(WarheadFillers.canPlace(w, x, y, z, 3), "the cells above are free");
            WarheadFillers.place(w, x, y, z, 3);
            out.check(w.getBlock(x, y + 1, z) == WarheadBlocks.filler(), "filler at +1");
            out.check(w.getBlock(x, y + 2, z) == WarheadBlocks.filler(), "filler at +2");
            // A solid block in the way stops placement.
            ctx.setBlock(3, 0, 0, Blocks.stone, 0);
            ctx.setBlock(3, 2, 0, Blocks.stone, 0);
            out.check(!WarheadFillers.canPlace(w, x + 3, y, z, 3), "a blocked cell refuses placement");
            // A solid block in the way stops placement; clear it again for the break runs.
            ctx.setBlock(3, 0, 0, Blocks.air, 0);
            ctx.setBlock(3, 2, 0, Blocks.air, 0);
            // Break each cell in turn (core, middle filler, top filler): the whole warhead goes, one item drops, and
            // no orphan filler is left. The warhead is rebuilt before every run.
            for (int cell = 0; cell < 3; cell++) {
                if (cell > 0) {
                    TileWarhead again = place(ctx, WarheadCasing.T1, 0, 0, 0, "shrapnel", 4);
                    if (again == null) {
                        return ScenarioResult.fail(name(), "no tile for run " + cell);
                    }
                    again.setHeight(3);
                    WarheadFillers.place(w, x, y, z, 3);
                }
                killItems(w, x, y, z);
                String at = "cell " + cell + ": ";
                w.setBlockToAir(x, y + cell, z);
                for (int dy = 0; dy < 3; dy++) {
                    out.check(w.isAirBlock(x, y + dy, z), at + "cell " + dy + " is air (no orphan filler)");
                }
                int items = 0;
                int fill = -1;
                for (Object o : w.getEntitiesWithinAABB(
                    net.minecraft.entity.item.EntityItem.class,
                    net.minecraft.util.AxisAlignedBB.getBoundingBox(x - 6, y - 2, z - 6, x + 6, y + 6, z + 6))) {
                    ItemStack s = ((net.minecraft.entity.item.EntityItem) o).getEntityItem();
                    // Items killed by the previous run stay in the chunk list until the next world tick.
                    if (!((net.minecraft.entity.Entity) o).isDead
                        && s.getItem() == Item.getItemFromBlock(block(WarheadCasing.T1))) {
                        items++;
                        fill = WarheadStacks.read(WarheadCasing.T1, s)
                            .fill();
                    }
                }
                out.checkEquals(at + "exactly one warhead item dropped", 1, items);
                out.checkEquals(at + "the drop keeps its fill", 4, fill);
            }
            killItems(w, x, y, z);
            return out.build();
        }
    }

    /** A full-fill warhead placed beside a powered block gets the full-fill fuse, not the default fill-1 one. */
    private static final class FuseFullFillBesideRedstone extends Base {

        @Override
        public String name() {
            return "icbm.warhead.fuse_full_fill";
        }

        @Override
        public String description() {
            return "A full-fill warhead placed from its item beside a redstone block is lit with the full-fill fuse (D51).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (needsModule() != null) {
                return needsModule();
            }
            ctx.clearArea(4, 4);
            World w = ctx.world();
            int x = ctx.originX();
            int y = ctx.originY();
            int z = ctx.originZ();
            ctx.setBlock(1, 0, 0, Blocks.redstone_block, 0);
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            Item item = Item.getItemFromBlock(block(WarheadCasing.T1));
            ItemStack stack = WarheadStacks.make(item, WarheadCasing.T1, "condensed", 5);
            // The item places the block (lighting it in onBlockAdded, before the NBT is read) and then inits it.
            boolean placed = ((ItemBlockWarhead) item).placeBlockAt(stack, null, w, x, y, z, 1, 0.5F, 0.5F, 0.5F, 0);
            out.check(placed, "the item placed the warhead");
            TileEntity t = w.getTileEntity(x, y, z);
            if (!(t instanceof TileWarhead)) {
                return ScenarioResult.fail(name(), "no warhead tile after placing");
            }
            TileWarhead tile = (TileWarhead) t;
            int fillOneFuse = WarheadFuse.litTicks(
                Math.max(
                    WarheadMath.MIN_RADIUS,
                    Explosives.get("condensed")
                        .radius(1)),
                IcbmConfig.fuseMultiplier);
            out.checkEquals(
                "the stack carried the full fill",
                5,
                tile.data()
                    .fill());
            out.check(tile.armed(), "the powered neighbor lit it");
            out.checkEquals("the fuse follows the full fill", tile.litFuseTicks(), tile.fuse());
            out.check(tile.fuse() > fillOneFuse, "longer than the fill-1 fuse (" + fillOneFuse + ")");
            // Defuse before it burns: the scenario only checks the length.
            w.setBlockToAir(x, y, z);
            killItems(w, x, y, z);
            return out.build();
        }
    }

    /** The lit flag reaches a client tile through the description packet, and the name follows D58. */
    private static final class LitStateSyncs extends Base {

        @Override
        public String name() {
            return "icbm.warhead.lit_sync";
        }

        @Override
        public String description() {
            return "A lit warhead's synced NBT makes a fresh client tile read as armed with the same explosive; an unlit one does not.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (needsModule() != null) {
                return needsModule();
            }
            ctx.clearArea(4, 4);
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            TileWarhead server = place(ctx, WarheadCasing.T1, 0, 0, 0, "smoke", 1);
            if (server == null) {
                return ScenarioResult.fail(name(), "no tile");
            }
            // The description packet carries writeToNBT and the client reads it with readFromNBT; the packet's own
            // reader (func_148857_g) is client-only, so the scenario runs the same NBT round trip on the server.
            out.check(server.getDescriptionPacket() != null, "the tile sends a description packet");
            NBTTagCompound unlit = new NBTTagCompound();
            server.writeToNBT(unlit);
            TileWarhead before = new TileWarhead();
            before.readFromNBT(unlit);
            out.check(!before.armed(), "an unlit warhead reads as unlit on the client");
            server.arm(server.litFuseTicks(), null);
            NBTTagCompound lit = new NBTTagCompound();
            server.writeToNBT(lit);
            TileWarhead client = new TileWarhead();
            client.readFromNBT(lit);
            out.check(client.armed(), "the client tile reads as armed once lit");
            out.checkEquals(
                "the client knows the explosive",
                "smoke",
                client.data()
                    .explosive());
            ctx.world()
                .setBlockToAir(ctx.originX(), ctx.originY(), ctx.originZ());
            killItems(ctx.world(), ctx.originX(), ctx.originY(), ctx.originZ());
            return out.build();
        }
    }

    /**
     * A mob with Infernal-scale health (a full Condensed or Thermobaric warhead against 1,080 or 2,700) dies at ground
     * zero.
     */
    private static final class InfernalHealth extends Base {

        private final String id;
        private final int fill;
        private final double health;

        InfernalHealth(String id, int fill, double health) {
            this.id = id;
            this.fill = fill;
            this.health = health;
        }

        @Override
        public String name() {
            return "icbm.blast.infernal_" + id;
        }

        @Override
        public String description() {
            return "A mob with " + (int) health + " max health at ground zero of a full " + id + " warhead dies.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (needsModule() != null) {
                return needsModule();
            }
            ExplosiveDef def = Explosives.get(id);
            if (def == null) {
                return ScenarioResult.fail(name(), "not registered");
            }
            ctx.clearArea(30, 40);
            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = -4; dz <= 4; dz++) {
                    ctx.setBlock(dx, -1, dz, Blocks.stone, 0);
                }
            }
            World w = ctx.world();
            EntityCow mob = ctx
                .spawnAt(new EntityCow(w), ctx.originX() + 0.5, ctx.originY(), ctx.originZ() + 0.5, 0, 0);
            mob.getEntityAttribute(net.minecraft.entity.SharedMonsterAttributes.maxHealth)
                .setBaseValue(health);
            mob.setHealth((float) health);
            if (mob.getHealth() < health - 1.0) {
                return ScenarioResult.fail(name(), "could not raise the mob's health, got " + mob.getHealth());
            }
            boolean old1 = IcbmConfig.terrainDamageT1;
            boolean old2 = IcbmConfig.terrainDamageT2;
            boolean old3 = IcbmConfig.terrainDamageT3;
            IcbmConfig.terrainDamageT1 = false;
            IcbmConfig.terrainDamageT2 = false;
            IcbmConfig.terrainDamageT3 = false;
            boolean accepted = IcbmBlasts
                .detonate(w, ctx.originX() + 0.5, ctx.originY() + 0.5, ctx.originZ() + 0.5, def, fill, null);
            if (!accepted) {
                IcbmConfig.terrainDamageT1 = old1;
                IcbmConfig.terrainDamageT2 = old2;
                IcbmConfig.terrainDamageT3 = old3;
                return ScenarioResult.fail(name(), "the engine did not accept the blast");
            }
            return ScenarioResult.pending(name(), 400, ticks -> {
                boolean dead = mob.isDead || mob.getHealth() <= 0.0F;
                if (!dead && ticks < 300) {
                    return null;
                }
                IcbmConfig.terrainDamageT1 = old1;
                IcbmConfig.terrainDamageT2 = old2;
                IcbmConfig.terrainDamageT3 = old3;
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.check(
                    dead,
                    "the " + (int) health + "-health mob died at ground zero (health " + mob.getHealth() + ")");
                return out.build();
            });
        }
    }

    /** One scenario per blast type: it detonates through the registry and shows its effect. */
    private static final class BlastType extends Base {

        private final WarheadBlast blast;

        BlastType(WarheadBlast blast) {
            this.blast = blast;
        }

        @Override
        public String name() {
            return "icbm.blast." + blast.id();
        }

        @Override
        public String description() {
            return "A full " + blast.id() + " warhead detonates through the shared pipeline and shows its effect.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (needsModule() != null) {
                return needsModule();
            }
            ExplosiveDef def = Explosives.get(blast.id());
            if (def == null) {
                return ScenarioResult.fail(name(), "not registered");
            }
            ctx.clearArea(30, 40);
            World w = ctx.world();
            int ox = ctx.originX();
            int oy = ctx.originY();
            int oz = ctx.originZ();
            // A stone floor, a glass pane-block and a stone wall for the block effects.
            for (int dx = -26; dx <= 26; dx++) {
                for (int dz = -26; dz <= 26; dz++) {
                    for (int dy = -1; dy >= -30; dy--) {
                        if (dx * dx + dz * dz <= 10 * 10 || dy == -1) {
                            ctx.setBlock(dx, dy, dz, Blocks.stone, 0);
                        }
                    }
                }
            }
            // Off the cow's line (+X), or the pane stops a pulled cow short and the pull check becomes a coin flip.
            ctx.setBlock(0, 1, 3, Blocks.glass, 0);
            EntityCow cow = ctx.spawnAt(new EntityCow(w), ox + 4.5, oy, oz + 0.5, 0, 0);
            boolean oldT1 = IcbmConfig.terrainDamageT1;
            boolean oldT2 = IcbmConfig.terrainDamageT2;
            boolean oldT3 = IcbmConfig.terrainDamageT3;
            IcbmConfig.terrainDamageT1 = true;
            IcbmConfig.terrainDamageT2 = true;
            IcbmConfig.terrainDamageT3 = true;
            // The nuclear crater at full size has its own scenario (icbm.blast.crater_nuclear); here fill 8 (radius 32)
            // is enough to see the kill zone and the crater without a thousand-tick wait.
            int fill = blast == WarheadBlast.NUCLEAR ? 8 : def.maxFill();
            boolean accepted = IcbmBlasts.detonate(w, ox + 0.5, oy + 0.5, oz + 0.5, def, fill, null);
            if (!accepted) {
                IcbmConfig.terrainDamageT1 = oldT1;
                IcbmConfig.terrainDamageT2 = oldT2;
                IcbmConfig.terrainDamageT3 = oldT3;
                return ScenarioResult.fail(name(), "the engine did not accept the blast");
            }
            final double[] extremes = { cow.posX, cow.posX };
            final int[] fireSeen = { 0 };
            return ScenarioResult.pending(name(), 900, ticks -> {
                // A cow wanders for 200 ticks, so push and pull are judged on the farthest it ever got, not where it
                // stands at the end (the cause of the intermittent icbm.blast.attractive failure).
                extremes[0] = Math.min(extremes[0], cow.posX);
                extremes[1] = Math.max(extremes[1], cow.posX);
                if (blast.kind() == WarheadBlast.Kind.FIRE) {
                    fireSeen[0] = Math
                        .max(fireSeen[0], countFire(w, ox, oy, oz, (int) Math.ceil(def.radius(def.maxFill()))));
                }
                boolean done = ticks >= settleTicks();
                if (!done) {
                    return null;
                }
                IcbmConfig.terrainDamageT1 = oldT1;
                IcbmConfig.terrainDamageT2 = oldT2;
                IcbmConfig.terrainDamageT3 = oldT3;
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                check(out, ctx, def, cow, extremes, fireSeen[0]);
                return out.build();
            });
        }

        /** Fire blocks standing on the floor layer within the radius; fire on stone burns out, so this is sampled. */
        private static int countFire(World w, int ox, int oy, int oz, int r) {
            int n = 0;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (w.getBlock(ox + dx, oy, oz + dz) == Blocks.fire) {
                        n++;
                    }
                }
            }
            return n;
        }

        private int settleTicks() {
            switch (blast.kind()) {
                case GAS_DEBILITATION:
                case GAS_CHEMICAL:
                case GAS_CONTAGIOUS:
                case SMOKE:
                    return 30;
                default:
                    return blast == WarheadBlast.NUCLEAR ? 600 : 200;
            }
        }

        private void check(ScenarioResult.Builder out, ScenarioContext ctx, ExplosiveDef def, EntityCow cow,
            double[] extremes, int fireSeen) {
            World w = ctx.world();
            int ox = ctx.originX();
            int oy = ctx.originY();
            int oz = ctx.originZ();
            switch (blast.kind()) {
                case NONE:
                case SHRAPNEL:
                case ANVIL:
                    boolean craters = blast.shape() != com.gregguns.icbm.explosive.BlastShape.NONE;
                    out.check(craters && w.isAirBlock(ox, oy - 1, oz), "the crater removed the floor at the center");
                    out.check(cow.isDead || cow.getHealth() <= 0.0F, "the damage ramp killed the cow in the zone");
                    if (blast.kind() == WarheadBlast.Kind.SHRAPNEL) {
                        out.check(cow.isBurning() || cow.isDead, "shrapnel sets the zone alight");
                    }
                    if (blast.kind() == WarheadBlast.Kind.ANVIL) {
                        out.check(
                            cow.isDead || cow.isPotionActive(Potion.moveSlowdown),
                            "anvil fragments slow what survives");
                    }
                    break;
                case FIRE:
                    out.check(cow.isBurning() || cow.isDead, "the cow is alight");
                    out.check(fireSeen > 0, "the blast placed fire blocks (most seen at once: " + fireSeen + ")");
                    break;
                case GAS_DEBILITATION:
                    out.check(cow.isPotionActive(Potion.moveSlowdown), "debilitation slows");
                    out.check(cow.isPotionActive(Potion.weakness), "debilitation weakens");
                    break;
                case GAS_CHEMICAL:
                    out.check(cow.isPotionActive(Potion.poison), "chemical gas poisons");
                    break;
                case GAS_CONTAGIOUS:
                    out.check(cow.isPotionActive(Potion.poison), "contagious gas poisons");
                    out.check(cow.isPotionActive(Potion.hunger), "contagious gas starves");
                    break;
                case SMOKE:
                    out.check(!cow.isDead && cow.getHealth() > 0.0F, "smoke is harmless");
                    out.check(w.getBlock(ox, oy - 1, oz) == Blocks.stone, "smoke removes no terrain");
                    break;
                case REPEL:
                    out.check(extremes[1] > ox + 5.5, "the cow was thrown outward, max X " + extremes[1]);
                    break;
                case ATTRACT:
                    out.check(extremes[0] < ox + 3.5, "the cow was pulled inward, min X " + extremes[0]);
                    break;
                case SONIC:
                    out.check(cow.isPotionActive(Potion.confusion), "sonic stuns");
                    out.check(w.isAirBlock(ox, oy + 1, oz + 3), "sonic shatters glass");
                    break;
                case BREACH:
                    out.check(w.isAirBlock(ox, oy - 5, oz), "the breaching tunnel runs down");
                    out.check(w.getBlock(ox + 5, oy - 5, oz) == Blocks.stone, "the tunnel is narrow");
                    break;
                default:
                    break;
            }
            out.check(def != null, "defined");
        }
    }

    /** With icbm.terrainDamageT1 off, a crater blast leaves the terrain alone (D11). */
    private static final class BlockDamageOff extends Base {

        @Override
        public String name() {
            return "icbm.blast.block_damage_off";
        }

        @Override
        public String description() {
            return "With icbm.terrainDamageT1 false a condensed warhead removes no blocks.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (needsModule() != null) {
                return needsModule();
            }
            ctx.clearArea(14, 14);
            for (int dx = -10; dx <= 10; dx++) {
                for (int dz = -10; dz <= 10; dz++) {
                    ctx.setBlock(dx, -1, dz, Blocks.stone, 0);
                }
            }
            boolean old = IcbmConfig.terrainDamageT1;
            IcbmConfig.terrainDamageT1 = false;
            ExplosiveDef def = Explosives.get("condensed");
            World w = ctx.world();
            boolean accepted = IcbmBlasts
                .detonate(w, ctx.originX() + 0.5, ctx.originY() + 0.5, ctx.originZ() + 0.5, def, 5, null);
            return ScenarioResult.pending(name(), 300, ticks -> {
                if (ticks < 100) {
                    return null;
                }
                IcbmConfig.terrainDamageT1 = old;
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.check(accepted, "the blast ran");
                out.check(
                    w.getBlock(ctx.originX(), ctx.originY() - 1, ctx.originZ()) == Blocks.stone,
                    "the floor stands");
                return out.build();
            });
        }
    }
}
