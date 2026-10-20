package com.gregguns.gun.server;

import net.minecraft.block.Block;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fluids.IFluidContainerItem;

import com.gregguns.combat.DamageModel;
import com.gregguns.combat.DamageType;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.core.testing.SkipUnless;
import com.gregguns.gun.def.ToolProbeDef;
import com.gregguns.gun.logic.GunCatalog.AmmoIds;
import com.gregguns.gun.logic.LoopStart;
import com.gregguns.item.gun.GunItems;
import com.gregguns.item.gun.ItemToolGun;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.Materials;

/**
 * The {@code gun.tool.*} scenarios: the tool-gun base (I11) on the test-only probe tool gun. Melee drain and the
 * empty-tank hit, the remainder carried between half-round blocks, a block break refused when {@code BlockBreakGuard}
 * is denied by a protection listener, the throttled swing sound, and the reload that returns the empty tank. Dev
 * environment only, server side.
 */
// Spec: docs/gregguns-spec/02-guns/tool-guns.md
public final class ToolGunScenarios {

    private static final float EPS = 0.05F;
    private static final String DENY_NAME = "[GregGunsToolDeny]";

    private ToolGunScenarios() {}

    /**
     * Registers the scenarios.
     *
     * @param registry where to add them
     */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(guarded(new Melee()));
        registry.register(guarded(new Mining()));
        registry.register(guarded(new BreakDenied()));
        registry.register(guarded(new Swing()));
        registry.register(guarded(new Reload()));
    }

    /** The probe tool gun exists only in the dev workspace; in the pack these scenarios skip. */
    private static Scenario guarded(Scenario s) {
        return SkipUnless.of(
            s,
            () -> GunItems.get(ToolProbeDef.NAME) != null,
            "needs the dev-only probe tool gun (" + ToolProbeDef.NAME + "), which the pack does not register");
    }

    private static ItemToolGun probe() {
        return (ItemToolGun) GunItems.get(ToolProbeDef.NAME);
    }

    private static GunRig rig(ScenarioContext ctx, int loaded) {
        GunRig rig = new GunRig(ctx, probe(), loaded);
        // A real player's tick applies the held item's attribute modifiers; a fake player is not ticked.
        rig.shooter.getAttributeMap()
            .applyAttributeModifiers(rig.gun.getAttributeModifiers());
        return rig;
    }

    private static Item tankItem() {
        return GameRegistry.findItem("gregguns", AmmoIds.FUEL_TANK);
    }

    private static float lost(EntityZombie z) {
        return z.getMaxHealth() - z.getHealth();
    }

    private static float expected(EntityZombie z, double raw, double penetration) {
        double armor = z.getTotalArmorValue();
        return (float) DamageModel
            .resolve(DamageType.PHYSICAL, raw, penetration, share -> share * DamageModel.vanillaArmorFactor(armor));
    }

    /** One hit costs one round; a hit with the tank empty is a bare hand and costs nothing. */
    private static final class Melee implements Scenario {

        @Override
        public String name() {
            return "gun.tool.melee";
        }

        @Override
        public String description() {
            return "A tool gun's left click is a melee hit: 1 + 12 damage with 25% armor penetration and one round spent when loaded; 1 + 2 damage and nothing spent when the tank is empty.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            GunRig rig = rig(ctx, 300);
            EntityZombie loadedTarget = GunRig.zombie(ctx, 2);
            rig.shooter.attackTargetEntityWithCurrentItem(loadedTarget);
            out.check(
                GunRig.near(expected(loadedTarget, 13.0, 0.25), lost(loadedTarget)),
                "a loaded hit should deal " + expected(loadedTarget, 13.0, 0.25) + " but dealt " + lost(loadedTarget));
            out.checkEquals("rounds after one hit", 299, rig.loaded());

            rig.gun.getTagCompound()
                .setShort("ammo", (short) 0);
            EntityZombie emptyTarget = GunRig.zombie(ctx, 3);
            rig.shooter.attackTargetEntityWithCurrentItem(emptyTarget);
            out.check(
                GunRig.near(expected(emptyTarget, 3.0, 0.0), lost(emptyTarget)),
                "an empty hit should deal " + expected(emptyTarget, 3.0, 0.0) + " but dealt " + lost(emptyTarget));
            out.checkEquals("rounds after an empty hit", 0, rig.loaded());
            return out.build();
        }
    }

    /** Mining reports its class, level and speed only while loaded; blocks cost half a round with a remainder. */
    private static final class Mining implements Scenario {

        @Override
        public String name() {
            return "gun.tool.mining";
        }

        @Override
        public String description() {
            return "A loaded tool gun mines as an axe level 3 at speed 14 and harvests logs; empty it mines as a hand. Each block costs half a round: two blocks spend one round and the remainder returns to 0 (breakBlock and the vanilla onBlockDestroyed path).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            ItemToolGun item = probe();
            GunRig rig = rig(ctx, 300);
            ItemStack stack = rig.gun;
            out.check(
                item.getToolClasses(stack)
                    .contains("axe")
                    && !item.getToolClasses(stack)
                        .contains("default"),
                "a loaded gun should list axe and no internal default class");
            // IguanaTweaks renumbers harvest levels in the pack (our axe level 3 reads back 5), so only a floor is
            // checked.
            out.check(item.getHarvestLevel(stack, "axe") >= 3, "axe level should be at least 3");
            out.check(item.getDigSpeed(stack, Blocks.log, 0) == 14.0F, "dig speed on logs should be 14");
            out.check(item.getDigSpeed(stack, Blocks.stone, 0) == 1.0F, "dig speed on stone (a pickaxe block) is 1");
            out.check(ForgeHooks.canToolHarvestBlock(Blocks.log, 0, stack), "a loaded gun should harvest a log");

            ctx.setBlock(3, 0, 0, Blocks.log, 0);
            ctx.setBlock(3, 1, 0, Blocks.log, 0);
            ctx.setBlock(3, 2, 0, Blocks.log, 0);
            int ox = ctx.originX();
            int oy = ctx.originY();
            int oz = ctx.originZ();
            ItemToolGun.BreakResult first = item.breakBlock(rig.shooter, stack, ox + 3, oy, oz);
            out.check(first == ItemToolGun.BreakResult.BROKEN, "the first break should work, was " + first);
            out.checkEquals("rounds after half a round", 300, rig.loaded());
            out.checkEquals("remainder after half a round", 500, item.remainderMilli(stack));
            item.breakBlock(rig.shooter, stack, ox + 3, oy + 1, oz);
            out.checkEquals("rounds after two blocks", 299, rig.loaded());
            out.checkEquals("remainder after two blocks", 0, item.remainderMilli(stack));

            Block log = Blocks.log;
            item.onBlockDestroyed(stack, ctx.world(), log, ox + 3, oy + 2, oz, rig.shooter);
            out.checkEquals("the vanilla path spends half a round too", 500, item.remainderMilli(stack));
            item.onBlockDestroyed(stack, ctx.world(), Blocks.tallgrass, ox + 3, oy + 2, oz, rig.shooter);
            out.checkEquals("a hardness-0 block is free", 500, item.remainderMilli(stack));

            rig.gun.getTagCompound()
                .setShort("ammo", (short) 0);
            out.check(
                item.getToolClasses(stack)
                    .isEmpty(),
                "an empty gun reports no tool class");
            out.checkEquals("empty harvest level", -1, item.getHarvestLevel(stack, "axe"));
            out.check(item.getDigSpeed(stack, Blocks.log, 0) == 1.0F, "an empty gun digs at speed 1");
            ctx.setBlock(4, 0, 0, Blocks.log, 0);
            ItemToolGun.BreakResult empty = item.breakBlock(rig.shooter, stack, ox + 4, oy, oz);
            out.check(empty == ItemToolGun.BreakResult.EMPTY, "an empty gun breaks nothing, was " + empty);
            out.check(
                ctx.world()
                    .getBlock(ox + 4, oy, oz) == Blocks.log,
                "the log should still stand");
            return out.build();
        }
    }

    /** Cancels every break by the named player, like a claim mod. */
    public static final class Denier {

        /** @param event the break */
        @SubscribeEvent
        public void onBreak(BlockEvent.BreakEvent event) {
            if (DENY_NAME.equals(
                event.getPlayer()
                    .getCommandSenderName())) {
                event.setCanceled(true);
            }
        }
    }

    /** A protection listener that cancels the BreakEvent stops the break and spends nothing. */
    private static final class BreakDenied implements Scenario {

        @Override
        public String name() {
            return "gun.tool.break_denied";
        }

        @Override
        public String description() {
            return "ItemToolGun.breakBlock goes through BlockBreakGuard: with a listener cancelling the BreakEvent the block stays, nothing is spent and the result is DENIED; with the listener gone the block breaks and one half round is spent.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            ItemToolGun item = probe();
            GunRig rig = rig(ctx, 300);
            ItemStack stack = rig.gun;
            ctx.setBlock(3, 0, 0, Blocks.log, 0);
            int x = ctx.originX() + 3;
            int y = ctx.originY();
            int z = ctx.originZ();
            // The rig's shooter has its own name; use a second fake player the listener refuses.
            net.minecraftforge.common.util.FakePlayer denied = GunRig.fakePlayer(ctx, DENY_NAME, 0);
            denied.inventory.setInventorySlotContents(0, stack);
            denied.inventory.currentItem = 0;
            Denier denier = new Denier();
            MinecraftForge.EVENT_BUS.register(denier);
            ItemToolGun.BreakResult refused;
            try {
                refused = item.breakBlock(denied, stack, x, y, z);
            } finally {
                MinecraftForge.EVENT_BUS.unregister(denier);
            }
            out.check(refused == ItemToolGun.BreakResult.DENIED, "a cancelled BreakEvent should DENY, was " + refused);
            out.check(
                ctx.world()
                    .getBlock(x, y, z) == Blocks.log,
                "the denied block should still stand");
            out.checkEquals("nothing spent", 300, rig.loaded());
            out.checkEquals("no remainder", 0, item.remainderMilli(stack));

            ItemToolGun.BreakResult allowed = item.breakBlock(denied, stack, x, y, z);
            out.check(
                allowed == ItemToolGun.BreakResult.BROKEN,
                "with no listener the break should work, was " + allowed);
            out.check(
                ctx.world()
                    .getBlock(x, y, z) == Blocks.air,
                "the block should be gone");
            out.checkEquals("half a round spent", 500, item.remainderMilli(stack));
            return out.build();
        }
    }

    /** The swing sound is sent at most once per five ticks. */
    private static final class Swing implements Scenario {

        @Override
        public String name() {
            return "gun.tool.swing_throttle";
        }

        @Override
        public String description() {
            return "The swing of a loaded tool gun plays its sound at most once per 5 ticks per player (the swing itself is never cancelled); an empty gun plays none.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            ItemToolGun item = probe();
            GunRig rig = rig(ctx, 300);
            GunPlayerData data = GunPlayerData.get(rig.shooter);
            long now = ctx.world()
                .getTotalWorldTime();
            data.setLastSwingSoundTick(LoopStart.NEVER);
            out.check(!item.onEntitySwing(rig.shooter, rig.gun), "the swing should not be cancelled");
            out.checkEquals("the first swing plays", now, data.lastSwingSoundTick());
            data.setLastSwingSoundTick(now - 4);
            item.onEntitySwing(rig.shooter, rig.gun);
            out.checkEquals("4 ticks later is throttled", now - 4, data.lastSwingSoundTick());
            item.onEntitySwing(rig.shooter, rig.gun);
            data.setLastSwingSoundTick(now - 5);
            item.onEntitySwing(rig.shooter, rig.gun);
            out.checkEquals("5 ticks later plays", now, data.lastSwingSoundTick());

            rig.gun.getTagCompound()
                .setShort("ammo", (short) 0);
            data.setLastSwingSoundTick(LoopStart.NEVER);
            item.onEntitySwing(rig.shooter, rig.gun);
            out.checkEquals("an empty gun is silent", LoopStart.NEVER, data.lastSwingSoundTick());
            return out.build();
        }
    }

    /** The empty tank comes back through the existing reload path. */
    private static final class Reload implements Scenario {

        @Override
        public String name() {
            return "gun.tool.reload";
        }

        @Override
        public String description() {
            return "An empty tool gun reloads from a full Fuel Tank through the normal path: 300 rounds after 45 ticks, the tank consumed and an empty tank returned.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GunRig rig = rig(ctx, 0);
            ItemStack tank = new ItemStack(tankItem());
            ((IFluidContainerItem) tankItem()).fill(tank, Materials.Diesel.getFluid(1000), true);
            rig.shooter.inventory.addItemStackToInventory(tank);
            int reloadTicks = ToolProbeDef.STATS.reloadTicks();
            rig.press();
            rig.release();
            rig.tick();
            boolean started = rig.reloading();
            return ScenarioResult.pending(name(), reloadTicks + 20, ticks -> {
                rig.tick();
                if (ticks < reloadTicks + 2) {
                    return null;
                }
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.check(started, "the dry trigger should have started a reload");
                out.checkEquals("rounds after the reload", 300, rig.loaded());
                int empty = 0;
                for (ItemStack s : rig.shooter.inventory.mainInventory) {
                    if (s != null && s.getItem() == tankItem()
                        && ((IFluidContainerItem) tankItem()).getFluid(s) == null) {
                        empty += s.stackSize;
                    }
                }
                out.checkEquals("empty tanks returned", 1, empty);
                return out.build();
            });
        }
    }
}
