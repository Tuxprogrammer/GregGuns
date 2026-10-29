package com.gregguns.icbm.tools;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;

import com.gregguns.api.Tier;
import com.gregguns.core.module.ModuleId;
import com.gregguns.core.module.ModuleState;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.icbm.IcbmAvailability;
import com.gregguns.icbm.missile.EntityMissile;
import com.gregguns.icbm.missile.FlightPhase;
import com.gregguns.icbm.missile.MissileTier;
import com.gregguns.icbm.warhead.TileWarhead;
import com.gregguns.icbm.warhead.WarheadBlocks;
import com.gregguns.icbm.warhead.WarheadCasing;
import com.gregguns.icbm.warhead.WarheadStacks;
import com.mojang.authlib.GameProfile;

/** Dev-only scenarios of the remote detonator and the defuser. */
// Spec: docs/gregguns-icbm-spec/05-detection-defense/handheld-tools.md
final class ToolScenarios {

    private ToolScenarios() {}

    static void registerAll(ScenarioRegistry registry) {
        registry.register(new Remote());
        registry.register(new Defuser());
        registry.register(new ClickRoute());
    }

    private static String gate() {
        if (!ModuleState.isActive(ModuleId.ICBM) || !IcbmAvailability.galacticraftPresent()) {
            return com.gregguns.icbm.IcbmAvailability.offReason();
        }
        return null;
    }

    private static FakePlayer player(ScenarioContext ctx, String name) {
        return new FakePlayer(
            (WorldServer) ctx.world(),
            new GameProfile(UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)), name));
    }

    private static TileWarhead place(ScenarioContext ctx, FakePlayer owner, int dx, int dz) {
        WarheadCasing casing = WarheadCasing.T1;
        ctx.setBlock(dx, 0, dz, WarheadBlocks.of(casing), 0);
        TileEntity t = ctx.world()
            .getTileEntity(ctx.originX() + dx, ctx.originY(), ctx.originZ() + dz);
        if (!(t instanceof TileWarhead)) {
            return null;
        }
        ItemStack stack = WarheadStacks.make(Item.getItemFromBlock(WarheadBlocks.of(casing)), casing, "condensed", 1);
        ((TileWarhead) t).init(casing, stack.getTagCompound(), owner);
        return (TileWarhead) t;
    }

    /** Link, range, dimension, chunk, owner and fuse rules of the remote detonator. */
    private static final class Remote implements Scenario {

        @Override
        public String name() {
            return "icbm.tools.remote_detonator";
        }

        @Override
        public String description() {
            return "A remote detonator links to a placed warhead by sneak-click and lights its fuse (never an instant blast) in range, in the dimension, in a loaded chunk and for the owner; every refusal leaves the fuse alone.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (gate() != null) {
                return ScenarioResult.skip(name(), gate());
            }
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            ctx.clearArea(6, 4);
            FakePlayer owner = player(ctx, "gregguns-remote-owner");
            FakePlayer other = player(ctx, "gregguns-remote-other");
            TileWarhead w = place(ctx, owner, 2, 0);
            if (w == null) {
                return ScenarioResult.fail(name(), "the warhead did not place");
            }
            int x = ctx.originX() + 2;
            int y = ctx.originY();
            int z = ctx.originZ();
            owner.setPosition(ctx.originX() + 0.5, y, z + 0.5);
            other.setPosition(ctx.originX() + 0.5, y, z + 0.5);
            ItemStack hv = ToolRegistration.detonatorStack(Tier.HV);
            ItemRemoteDetonator item = (ItemRemoteDetonator) hv.getItem();
            out.checkEquals("HV range", HandheldRules.RANGE_HV, item.range());
            out.checkEquals(
                "EV range",
                HandheldRules.RANGE_EV,
                ToolRegistration.detonator(Tier.EV)
                    .range());
            out.checkEquals(
                "IV range",
                HandheldRules.RANGE_IV,
                ToolRegistration.detonator(Tier.IV)
                    .range());
            out.checkEquals("an unlinked detonator refuses", RemoteResult.NOT_LINKED, item.fire(owner, hv));

            // Linking: a plain click does not, a sneak-click on a warhead does, on air or stone it does not.
            owner.setSneaking(false);
            out.check(
                !item.onItemUseFirst(hv, owner, ctx.world(), x, y, z, 1, 0.5F, 0.5F, 0.5F)
                    && !ItemRemoteDetonator.linked(hv),
                "a plain click does not link");
            owner.setSneaking(true);
            out.check(
                !item.onItemUseFirst(hv, owner, ctx.world(), x, y + 3, z, 1, 0.5F, 0.5F, 0.5F)
                    && !ItemRemoteDetonator.linked(hv),
                "a sneak-click on air does not link");
            out.check(
                item.onItemUseFirst(hv, owner, ctx.world(), x, y, z, 1, 0.5F, 0.5F, 0.5F)
                    && ItemRemoteDetonator.linked(hv),
                "a sneak-click on the warhead links");
            out.checkEquals(
                "the link keeps the position",
                x,
                hv.getTagCompound()
                    .getInteger(ItemRemoteDetonator.K_X));
            out.checkEquals(
                "the link keeps the dimension",
                ctx.world().provider.dimensionId,
                hv.getTagCompound()
                    .getInteger(ItemRemoteDetonator.K_DIM));
            ItemStack theirs = ToolRegistration.detonatorStack(Tier.HV);
            other.setSneaking(true);
            item.onItemUseFirst(theirs, other, ctx.world(), x, y, z, 1, 0.5F, 0.5F, 0.5F);
            out.check(!ItemRemoteDetonator.linked(theirs), "someone else cannot link to the owner's warhead");

            // Refusals, each leaving the fuse alone.
            out.checkEquals("another player cannot fire it", RemoteResult.NOT_OWNER, item.fire(other, hv));
            owner.setPosition(ctx.originX() + 0.5 + 200, y, z + 0.5);
            out.checkEquals("200 blocks is beyond HV range", RemoteResult.OUT_OF_RANGE, item.fire(owner, hv));
            ItemStack ev = ToolRegistration.detonatorStack(Tier.EV);
            ItemRemoteDetonator.link(ev, x, y, z, ctx.world().provider.dimensionId);
            out.checkEquals(
                "200 blocks is inside EV range",
                RemoteResult.LIT,
                ((ItemRemoteDetonator) ev.getItem()).fire(owner, ev));
            out.check(w.armed(), "the EV detonator lit the fuse from 200 blocks");
            w.defuse();
            owner.setPosition(ctx.originX() + 0.5, y, z + 0.5);
            ItemStack wrongDim = ToolRegistration.detonatorStack(Tier.HV);
            ItemRemoteDetonator.link(wrongDim, x, y, z, ctx.world().provider.dimensionId + 7);
            out.checkEquals("another dimension is refused", RemoteResult.WRONG_DIMENSION, item.fire(owner, wrongDim));
            ItemStack gone = ToolRegistration.detonatorStack(Tier.HV);
            ItemRemoteDetonator.link(gone, x, y + 2, z, ctx.world().provider.dimensionId);
            out.checkEquals("no warhead at the link", RemoteResult.GONE, item.fire(owner, gone));
            // A chunk nobody loaded: 400 blocks out, inside IV range, never loaded for a scenario.
            ItemStack iv = ToolRegistration.detonatorStack(Tier.IV);
            int farX = ctx.originX() + 400;
            if (ctx.world()
                .getChunkProvider()
                .chunkExists(farX >> 4, z >> 4)) {
                out.note("the chunk 400 blocks east is loaded here; the unloaded-chunk check is skipped");
            } else {
                ItemRemoteDetonator.link(iv, farX, y, z, ctx.world().provider.dimensionId);
                out.checkEquals(
                    "an unloaded chunk is refused",
                    RemoteResult.UNLOADED,
                    ((ItemRemoteDetonator) iv.getItem()).fire(owner, iv));
                out.check(
                    !ctx.world()
                        .getChunkProvider()
                        .chunkExists(farX >> 4, z >> 4),
                    "and the refusal did not load the chunk");
            }
            out.check(!w.armed(), "no refusal lit the fuse");

            // The real thing: a right-click lights the standard fuse, which then counts down.
            owner.setSneaking(false);
            hv.getItem()
                .onItemRightClick(hv, ctx.world(), owner);
            out.check(w.armed(), "a right-click lights the fuse");
            out.checkEquals("with the standard hand-lit fuse (D51)", w.litFuseTicks(), w.fuse());
            out.checkEquals("a second press says it is lit", RemoteResult.ALREADY_LIT, item.fire(owner, hv));
            out.check(w.fuse() > 60, "the fuse leaves time to react");
            w.defuse();
            owner.setSneaking(true);
            ItemRemoteDetonator.forgetClicks(owner.getUniqueID());
            hv.getItem()
                .onItemRightClick(hv, ctx.world(), owner);
            out.check(!ItemRemoteDetonator.linked(hv), "a sneak-click in the air clears the link");
            ctx.clearArea(6, 4);
            return out.build();
        }
    }

    /**
     * The two packets one physical click sends on a warhead (block use, then use-item-in-air, same tick): the link
     * survives and the defuse runs once (the I4 client finding).
     */
    private static final class ClickRoute implements Scenario {

        @Override
        public String name() {
            return "icbm.tools.click_route";
        }

        @Override
        public String description() {
            return "A block-use packet followed by an air-use packet in the same tick: the detonator keeps its link and the defuser puts the fuse out once, with no second message and one use.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (gate() != null) {
                return ScenarioResult.skip(name(), gate());
            }
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            ctx.clearArea(6, 4);
            FakePlayer p = player(ctx, "gregguns-click-route");
            TileWarhead w = place(ctx, p, 2, 0);
            if (w == null) {
                return ScenarioResult.fail(name(), "the warhead did not place");
            }
            int x = ctx.originX() + 2;
            int y = ctx.originY();
            int z = ctx.originZ();
            p.setPosition(ctx.originX() + 0.5, y, z + 0.5);
            ItemRemoteDetonator.forgetClicks(p.getUniqueID());
            ItemDefuser.forgetClicks(p.getUniqueID());

            // Detonator: block use then air use while sneaking.
            p.setSneaking(true);
            ItemStack hv = ToolRegistration.detonatorStack(Tier.HV);
            hv.getItem()
                .onItemUseFirst(hv, p, ctx.world(), x, y, z, 1, 0.5F, 0.5F, 0.5F);
            out.check(ItemRemoteDetonator.linked(hv), "the block use links");
            hv.getItem()
                .onItemRightClick(hv, ctx.world(), p);
            out.check(ItemRemoteDetonator.linked(hv), "the air packet of the same click does not clear the link");
            hv.getItem()
                .onItemUseFirst(hv, p, ctx.world(), x, y, z, 1, 0.5F, 0.5F, 0.5F);
            out.check(ItemRemoteDetonator.linked(hv), "a repeat of the block use keeps the link");

            // Defuser: block use twice (repeat or second route) costs one use.
            p.setSneaking(false);
            ItemStack kit = ToolRegistration.defuserStack();
            w.arm(w.litFuseTicks(), p);
            kit.getItem()
                .onItemUseFirst(kit, p, ctx.world(), x, y, z, 1, 0.5F, 0.5F, 0.5F);
            kit.getItem()
                .onItemUseFirst(kit, p, ctx.world(), x, y, z, 1, 0.5F, 0.5F, 0.5F);
            kit.getItem()
                .onItemRightClick(kit, ctx.world(), p);
            out.check(!w.armed(), "the fuse is out");
            out.checkEquals("the defuse ran once and cost one use", 1, kit.getItemDamage());
            ctx.clearArea(6, 4);
            return out.build();
        }
    }

    /** The defuser on a lit warhead and on missiles before and after ignition. */
    private static final class Defuser implements Scenario {

        @Override
        public String name() {
            return "icbm.tools.defuser";
        }

        @Override
        public String description() {
            return "The defuser puts out a lit warhead's fuse and takes an unlaunched missile apart, costs one use each time, and refuses a missile that is counting down or flying.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (gate() != null) {
                return ScenarioResult.skip(name(), gate());
            }
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            ctx.clearArea(6, 6);
            FakePlayer p = player(ctx, "gregguns-defuser");
            TileWarhead w = place(ctx, p, 2, 0);
            if (w == null) {
                return ScenarioResult.fail(name(), "the warhead did not place");
            }
            int x = ctx.originX() + 2;
            int y = ctx.originY();
            int z = ctx.originZ();
            ItemStack kit = ToolRegistration.defuserStack();
            out.checkEquals("a new kit has 32 uses", HandheldRules.DEFUSER_USES, kit.getMaxDamage() + 1);
            out.check(
                !kit.getItem()
                    .onItemUseFirst(kit, p, ctx.world(), x, y, z, 1, 0.5F, 0.5F, 0.5F) || kit.getItemDamage() == 0,
                "an unlit warhead costs nothing");
            out.checkEquals(
                "the unlit warhead is reported",
                DefuseResult.NOT_LIT,
                ItemDefuser.defuseBlock(ctx.world(), x, y, z));
            w.arm(w.litFuseTicks(), p);
            out.check(w.armed(), "the warhead is lit");
            kit.getItem()
                .onItemUseFirst(kit, p, ctx.world(), x, y, z, 1, 0.5F, 0.5F, 0.5F);
            out.check(!w.armed(), "the defuser put the fuse out");
            out.checkEquals("it cost one use", 1, kit.getItemDamage());
            out.check(
                w.data()
                    .explosive()
                    .equals("condensed"),
                "the warhead keeps its explosive");
            out.check(
                ctx.world()
                    .getBlock(x, y, z) == WarheadBlocks.of(WarheadCasing.T1),
                "and stays in place");
            ItemStack worn = ToolRegistration.defuserStack();
            worn.setItemDamage(worn.getMaxDamage());
            w.arm(w.litFuseTicks(), p);
            ItemDefuser.forgetClicks(p.getUniqueID());
            worn.getItem()
                .onItemUseFirst(worn, p, ctx.world(), x, y, z, 1, 0.5F, 0.5F, 0.5F);
            out.check(!w.armed() && worn.stackSize <= 0, "the last use works and wears the kit out");

            // Missiles: standing is disarmed; counting down or flying is refused.
            EntityMissile m = stand(ctx, 0);
            out.checkEquals(
                "a standing missile is taken apart",
                DefuseResult.MISSILE_DISARMED,
                ItemDefuser.defuseMissile(m));
            out.check(m.isDead, "the missile entity is gone");
            EntityMissile flying = stand(ctx, 2);
            setPhase(flying, FlightPhase.CLIMB);
            out.checkEquals("a climbing missile is refused", DefuseResult.IGNITED, ItemDefuser.defuseMissile(flying));
            out.check(!flying.isDead, "and stays whole");
            setPhase(flying, FlightPhase.ARC);
            out.checkEquals("a flying missile is refused", DefuseResult.IGNITED, ItemDefuser.defuseMissile(flying));
            flying.setDead();
            ItemStack kit2 = ToolRegistration.defuserStack();
            EntityMissile left = stand(ctx, 4);
            out.check(
                kit2.getItem()
                    .onLeftClickEntity(kit2, p, left) && left.isDead
                    && kit2.getItemDamage() == 1,
                "a left-click on a standing missile disarms it for one use");
            ctx.clearArea(6, 6);
            return out.build();
        }

        private static EntityMissile stand(ScenarioContext ctx, int dz) {
            EntityMissile m = EntityMissile.create(MissileTier.T1, ctx.world());
            m.setLocationAndAngles(
                ctx.originX() + 0.5,
                ctx.originY() + EntityMissile.PAD_SURFACE,
                ctx.originZ() + 0.5 - 3 - dz,
                0.0F,
                0.0F);
            m.configure(0, "condensed", 1, 1, null);
            ctx.world()
                .spawnEntityInWorld(m);
            return m;
        }

        private static void setPhase(EntityMissile m, FlightPhase phase) {
            try {
                java.lang.reflect.Field f = EntityMissile.class.getDeclaredField("phase");
                f.setAccessible(true);
                f.set(m, phase);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
