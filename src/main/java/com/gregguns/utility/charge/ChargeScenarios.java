package com.gregguns.utility.charge;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.world.BlockEvent;

import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.network.validation.PacketOutcome;
import com.mojang.authlib.GameProfile;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.util.GTOreDictUnificator;

/**
 * Integration scenarios of the explosive charges: boring stone, a GT ore drop, claim cancellation, the offline-owner
 * path and the permission check on the GUI packet. Dev environment only. Server side.
 */
// Spec: docs/gregguns-spec/09-machines-utility/explosive-charges.md (GGS-09-10 to GGS-09-15)
public final class ChargeScenarios {

    private static final int FLOOR_DEPTH = 6;

    private ChargeScenarios() {}

    /**
     * @param registry where to add the scenarios
     */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new BoresStone());
        registry.register(new GtOreDrop());
        registry.register(new RespectsCancel());
        registry.register(new OfflineOwner());
        registry.register(new ForeignButton());
    }

    /** A fake player with a stand-in connection, so the packet pipeline accepts it as a sender. */
    private static FakePlayer player(ScenarioContext ctx, String name) {
        FakePlayer p = new FakePlayer(
            (WorldServer) ctx.world(),
            new GameProfile(UUID.nameUUIDFromBytes(name.getBytes()), name));
        new NetHandlerPlayServer(MinecraftServer.getServer(), new NetworkManager(false), p);
        p.setPosition(ctx.originX() + 0.5, ctx.originY() + 1, ctx.originZ() + 3.5);
        return p;
    }

    /** Stone from one below the origin down, wide enough for the largest tunnel. */
    private static void stoneMass(ScenarioContext ctx) {
        ctx.clearArea(5, 4);
        for (int dy = -1; dy >= -FLOOR_DEPTH; dy--) {
            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = -4; dz <= 4; dz++) {
                    ctx.setBlock(dx, dy, dz, Blocks.stone, 0);
                }
            }
        }
    }

    /** Places a charge on the floor (orientation 1) at the origin. */
    private static TileExplosiveCharge placeCharge(ScenarioContext ctx, ChargeType type, UUID owner, String name) {
        ctx.setBlock(0, 0, 0, BlockExplosiveCharge.instance, type.meta());
        TileExplosiveCharge tile = (TileExplosiveCharge) ctx.world()
            .getTileEntity(ctx.originX(), ctx.originY(), ctx.originZ());
        tile.init(type, 1, null);
        tile.setOwner(owner, name);
        return tile;
    }

    private static int clearItems(World world, ScenarioContext ctx) {
        AxisAlignedBB box = AxisAlignedBB.getBoundingBox(
            ctx.originX() - 8,
            ctx.originY() - 12,
            ctx.originZ() - 8,
            ctx.originX() + 8,
            ctx.originY() + 6,
            ctx.originZ() + 8);
        int count = 0;
        for (Object o : world.getEntitiesWithinAABB(EntityItem.class, box)) {
            ((EntityItem) o).setDead();
            count++;
        }
        return count;
    }

    private static boolean isStone(ScenarioContext ctx, int dx, int dy, int dz) {
        return ctx.world()
            .getBlock(ctx.originX() + dx, ctx.originY() + dy, ctx.originZ() + dz) == Blocks.stone;
    }

    private static boolean isAir(ScenarioContext ctx, int dx, int dy, int dz) {
        return ctx.world()
            .isAirBlock(ctx.originX() + dx, ctx.originY() + dy, ctx.originZ() + dz);
    }

    private abstract static class Base implements Scenario {

        ScenarioResult needsModule() {
            return BlockExplosiveCharge.instance == null
                ? ScenarioResult.fail(name(), "needs the utility module (GG_MODULES=guns,armor,machines,utility)")
                : null;
        }
    }

    /** A real fuse runs down and a 3 x 3 tunnel of the set length is bored below the charge. */
    private static final class BoresStone extends Base {

        @Override
        public String name() {
            return "utility.charge_bores_stone";
        }

        @Override
        public String description() {
            return "An armed Explosive Charge counts its fuse down and bores a 3x3 tunnel of its length into the stone it sits on, dropping items and leaving the rest.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (needsModule() != null) {
                return needsModule();
            }
            stoneMass(ctx);
            FakePlayer owner = player(ctx, "[GGChargeOwner]");
            TileExplosiveCharge tile = placeCharge(ctx, ChargeType.TNT, owner.getUniqueID(), "[GGChargeOwner]");
            // Shortest fuse: two presses of the minus button.
            tile.press(ChargeParams.BTN_FUSE_DOWN);
            tile.press(ChargeParams.BTN_FUSE_DOWN);
            tile.press(ChargeParams.BTN_ARM);
            final World world = ctx.world();
            return ScenarioResult.pending(name(), 140, ticks -> {
                boolean gone = world.getBlock(ctx.originX(), ctx.originY(), ctx.originZ())
                    != BlockExplosiveCharge.instance;
                if (!gone && ticks < 130) {
                    return null;
                }
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.check(gone, "the charge removes itself after its fuse");
                int bored = 0;
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        for (int dy = -1; dy >= -3; dy--) {
                            if (isAir(ctx, dx, dy, dz)) {
                                bored++;
                            }
                        }
                    }
                }
                out.checkEquals("blocks bored in the 3x3x3 tunnel", 27, bored);
                out.check(isStone(ctx, 0, -4, 0), "nothing below the set length is broken");
                out.check(isStone(ctx, 2, -1, 0) && isStone(ctx, 0, -1, 2), "the tunnel is only 3 wide");
                out.check(clearItems(world, ctx) > 0, "the broken stone dropped items");
                return out.build();
            });
        }
    }

    /** The drops of a GT ore block come out as the block gives them at fortune 0, taken before it is removed. */
    private static final class GtOreDrop extends Base {

        @Override
        public String name() {
            return "utility.charge_gt_ore_drop";
        }

        @Override
        public String description() {
            return "A charge that bores a GT ore block yields the same drops getDrops gives at fortune 0 (GGS-09-13).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (needsModule() != null) {
                return needsModule();
            }
            ItemStack ore = GTOreDictUnificator.get(OrePrefixes.ore, Materials.Iron, 1);
            if (ore == null || !(ore.getItem() instanceof ItemBlock)) {
                return ScenarioResult.fail(name(), "GT iron ore is not a block item here");
            }
            stoneMass(ctx);
            Block oreBlock = Block.getBlockFromItem(ore.getItem());
            ctx.setBlock(0, -1, 0, oreBlock, ore.getItemDamage());
            World world = ctx.world();
            List<ItemStack> expected = oreBlock
                .getDrops(world, ctx.originX(), ctx.originY() - 1, ctx.originZ(), ore.getItemDamage(), 0);
            ctx.setBlock(0, 0, 0, BlockExplosiveCharge.instance, 0);
            ChargeParams params = new ChargeParams(ChargeType.TNT);
            // Length 2 is the shortest tunnel and still covers the ore at step 1.
            for (int i = 0; i < 3; i++) {
                params.press(ChargeParams.BTN_LENGTH_DOWN);
            }
            ChargeBlast.Result r = ChargeBlast.detonate(
                (WorldServer) world,
                ctx.originX(),
                ctx.originY(),
                ctx.originZ(),
                1,
                params,
                UUID.nameUUIDFromBytes("[GGOre]".getBytes()),
                new GameProfile(UUID.nameUUIDFromBytes("[GGOre]".getBytes()), "[GGOre]"));
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            out.check(!expected.isEmpty(), "the ore block has drops at fortune 0");
            out.check(isAir(ctx, 0, -1, 0), "the ore block was bored");
            int found = 0;
            AxisAlignedBB box = AxisAlignedBB.getBoundingBox(
                ctx.originX() - 2,
                ctx.originY() - 4,
                ctx.originZ() - 2,
                ctx.originX() + 3,
                ctx.originY() + 2,
                ctx.originZ() + 3);
            List<ItemStack> seen = new ArrayList<>();
            for (Object o : world.getEntitiesWithinAABB(EntityItem.class, box)) {
                seen.add(((EntityItem) o).getEntityItem());
            }
            for (ItemStack want : expected) {
                boolean present = false;
                for (ItemStack got : seen) {
                    if (got.getItem() == want.getItem() && got.getItemDamage() == want.getItemDamage()) {
                        present = true;
                    }
                }
                found += present ? 1 : 0;
            }
            out.checkEquals("expected drop kinds found as items", expected.size(), found);
            out.check(r.events > 0 && r.broken > 0, "break events were posted and blocks removed");
            clearItems(world, ctx);
            return out.build();
        }
    }

    /** Records every break event and cancels two chosen columns. */
    public static final class CancelListener {

        final int ox;
        final int oy;
        final int oz;
        final List<net.minecraft.entity.player.EntityPlayer> players = new ArrayList<>();
        int events;

        CancelListener(int ox, int oy, int oz) {
            this.ox = ox;
            this.oy = oy;
            this.oz = oz;
        }

        /**
         * @param e a break event
         */
        @SubscribeEvent
        public void onBreak(BlockEvent.BreakEvent e) {
            events++;
            players.add(e.getPlayer());
            // Column (+1, 0) is canceled from its second block; column (-1, 0) from its first.
            if (e.x == ox + 1 && e.z == oz && e.y <= oy - 2 || e.x == ox - 1 && e.z == oz) {
                e.setCanceled(true);
            }
        }
    }

    /** A canceled event stops its own column and nothing past it; every event carries a real player. */
    private static final class RespectsCancel extends Base {

        @Override
        public String name() {
            return "utility.charge_respects_cancel";
        }

        @Override
        public String description() {
            return "A listener that cancels break events stops only its own columns; blocks past a canceled one survive, no event has a null player and no Explosion is made.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (needsModule() != null) {
                return needsModule();
            }
            stoneMass(ctx);
            ctx.setBlock(0, 0, 0, BlockExplosiveCharge.instance, 0);
            CancelListener listener = new CancelListener(ctx.originX(), ctx.originY(), ctx.originZ());
            MinecraftForge.EVENT_BUS.register(listener);
            ChargeBlast.Result r;
            try {
                UUID owner = UUID.nameUUIDFromBytes("[GGCancel]".getBytes());
                r = ChargeBlast.detonate(
                    (WorldServer) ctx.world(),
                    ctx.originX(),
                    ctx.originY(),
                    ctx.originZ(),
                    1,
                    new ChargeParams(ChargeType.TNT),
                    owner,
                    new GameProfile(owner, "[GGCancel]"));
            } finally {
                MinecraftForge.EVENT_BUS.unregister(listener);
            }
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            out.checkEquals(
                "blocks broken (27 minus 2 past the first cancel minus 3 in the blocked column)",
                22,
                r.broken);
            out.check(isAir(ctx, 1, -1, 0), "the first block of the (+1,0) column broke");
            out.check(isStone(ctx, 1, -2, 0) && isStone(ctx, 1, -3, 0), "nothing past the canceled block breaks");
            out.check(isStone(ctx, -1, -1, 0) && isStone(ctx, -1, -3, 0), "a column canceled at once stays whole");
            out.check(isAir(ctx, 0, -3, 1) && isAir(ctx, 1, -3, 1), "other columns are unaffected");
            boolean nobodyNull = true;
            for (Object p : listener.players) {
                nobodyNull &= p != null;
            }
            out.check(listener.events > 0 && nobodyNull, "no break event carried a null player (MUTL-30)");
            clearItems(ctx.world(), ctx);
            return out.build();
        }
    }

    /** The owner is not online: the event carries a FakePlayer built from the stored profile. */
    private static final class OfflineOwner extends Base {

        @Override
        public String name() {
            return "utility.charge_offline_owner";
        }

        @Override
        public String description() {
            return "With the owner offline the break events carry a FakePlayer with the owner's UUID, and a charge with no owner still posts non-null events.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (needsModule() != null) {
                return needsModule();
            }
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            UUID owner = UUID.nameUUIDFromBytes("[GGOffline]".getBytes());
            for (int round = 0; round < 2; round++) {
                stoneMass(ctx);
                ctx.setBlock(0, 0, 0, BlockExplosiveCharge.instance, 0);
                CancelListener listener = new CancelListener(ctx.originX(), ctx.originY() + 100, ctx.originZ());
                MinecraftForge.EVENT_BUS.register(listener);
                ChargeBlast.Result r;
                try {
                    r = ChargeBlast.detonate(
                        (WorldServer) ctx.world(),
                        ctx.originX(),
                        ctx.originY(),
                        ctx.originZ(),
                        1,
                        new ChargeParams(ChargeType.TNT),
                        round == 0 ? owner : null,
                        round == 0 ? new GameProfile(owner, "[GGOffline]") : null);
                } finally {
                    MinecraftForge.EVENT_BUS.unregister(listener);
                }
                String label = round == 0 ? "owned" : "ownerless";
                out.check(r.actor instanceof FakePlayer, label + ": the actor is a FakePlayer");
                out.check(r.events > 0 && !listener.players.contains(null), label + ": no event has a null player");
                if (round == 0) {
                    out.checkEquals("the actor carries the owner's UUID", owner, r.actor.getUniqueID());
                }
                clearItems(ctx.world(), ctx);
            }
            return out.build();
        }
    }

    /** A stranger's button packet is rejected; the owner's is accepted. */
    private static final class ForeignButton extends Base {

        @Override
        public String name() {
            return "utility.charge_foreign_button";
        }

        @Override
        public String description() {
            return "A GUI button packet from a player who is neither owner nor team member is rejected (arm included); the owner's packet is applied (MUTL-31).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (needsModule() != null) {
                return needsModule();
            }
            ctx.clearArea(3, 3);
            FakePlayer owner = player(ctx, "[GGBtnOwner]");
            FakePlayer stranger = player(ctx, "[GGBtnStranger]");
            TileExplosiveCharge tile = placeCharge(ctx, ChargeType.ADVANCED, owner.getUniqueID(), "[GGBtnOwner]");
            int dim = ctx.world().provider.dimensionId;
            int x = ctx.originX();
            int y = ctx.originY();
            int z = ctx.originZ();
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            int fuse = tile.params()
                .fuse();
            PacketOutcome denied = new PacketChargeButton(dim, x, y, z, ChargeParams.BTN_FUSE_UP).applyAs(stranger);
            out.check(!denied.isAccepted(), "the stranger's fuse button is rejected: " + denied);
            out.checkEquals(
                "fuse unchanged after the rejected packet",
                fuse,
                tile.params()
                    .fuse());
            PacketOutcome deniedArm = new PacketChargeButton(dim, x, y, z, ChargeParams.BTN_ARM).applyAs(stranger);
            out.check(
                !deniedArm.isAccepted() && !tile.params()
                    .armed(),
                "the stranger cannot arm the charge: " + deniedArm);
            PacketOutcome bad = new PacketChargeButton(dim, x, y, z, 99).applyAs(owner);
            out.check(!bad.isAccepted(), "an unknown button id is rejected even for the owner");
            PacketOutcome ok = new PacketChargeButton(dim, x, y, z, ChargeParams.BTN_FUSE_UP).applyAs(owner);
            out.check(ok.isAccepted(), "the owner's button is accepted: " + ok);
            out.checkEquals(
                "fuse after the owner's plus",
                fuse + ChargeType.FUSE_STEP,
                tile.params()
                    .fuse());
            return out.build();
        }
    }
}
