package com.gregguns.gun.server;

import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.BlockEvent;

import com.gregguns.combat.DamageModel;
import com.gregguns.combat.DamageType;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.gun.def.PowerHammerAdvDef;
import com.gregguns.gun.def.PowerHammerDef;
import com.gregguns.gun.logic.Shockwave;
import com.gregguns.item.gun.GunItems;
import com.gregguns.item.gun.ItemGun;
import com.gregguns.recipe.GunRecipeScenarios;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * The {@code gun.power_hammer.*} and {@code gun.power_hammer_adv.*} scenarios: the charged shockwave breaks the blocks
 * of its box and hits what stands in it, a block break refused by {@code BlockBreakGuard} leaves the block and spends
 * no air for it, and the recipes (the Advanced one is the upgrade row that matches any used hammer). Dev environment
 * only, server side.
 */
// Spec: docs/techguns-spec/02-guns/sheets-launchers-tools-grenades.md (Power Hammer);
// docs/gregguns-spec/02-guns/tool-guns.md
public final class PowerHammerScenarios {

    private static final String SHOOTER_NAME = "[GregGunsGunTest]";
    private static final int WALL_X0 = 3;
    private static final int WALL_X1 = 6;

    private PowerHammerScenarios() {}

    /**
     * Registers the Power Hammer scenarios.
     *
     * @param registry where to add them
     */
    public static void registerHammer(ScenarioRegistry registry) {
        registry.register(new Wave("gun.power_hammer.shockwave", PowerHammerDef.NAME));
        registry.register(new Refused("gun.power_hammer.shockwave_denied", PowerHammerDef.NAME));
        registry.register(new Hit("gun.power_hammer.shockwave_hit", PowerHammerDef.NAME));
        registry.register(
            GunRecipeScenarios.assembly(
                "gun.power_hammer.recipe",
                "The Power Hammer (GR-91) is in GT's assembler map at HV, 35 s, matching its sheet inputs, and not below HV.",
                PowerHammerDef.DEFINITION.assembly()));
    }

    /**
     * Registers the Power Hammer (Advanced) scenarios.
     *
     * @param registry where to add them
     */
    public static void registerAdv(ScenarioRegistry registry) {
        registry.register(new Wave("gun.power_hammer_adv.shockwave", PowerHammerAdvDef.NAME));
        registry.register(
            GunRecipeScenarios.upgrade(
                "gun.power_hammer_adv.upgrade",
                "The GR-92 upgrade is found with a used, camouflaged Power Hammer (any NBT) in slot 0 at EV and outputs a Power Hammer (Advanced).",
                PowerHammerAdvDef.DEFINITION.assembly()));
    }

    private static GunRig rig(ScenarioContext ctx, String gun, int loaded) {
        return new GunRig(ctx, GunItems.get(gun), loaded);
    }

    /** A stone wall four wide across the corridor, in front of the shooter. */
    private static void wall(ScenarioContext ctx) {
        for (int dx = WALL_X0; dx <= WALL_X1; dx++) {
            for (int dy = 0; dy <= 3; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    ctx.setBlock(dx, dy, dz, Blocks.stone, 0);
                }
            }
        }
    }

    private static int standing(ScenarioContext ctx) {
        int n = 0;
        for (int dx = WALL_X0; dx <= WALL_X1; dx++) {
            for (int dy = 0; dy <= 3; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (ctx.world()
                        .getBlock(ctx.originX() + dx, ctx.originY() + dy, ctx.originZ() + dz) == Blocks.stone) {
                        n++;
                    }
                }
            }
        }
        return n;
    }

    private static final int WALL_BLOCKS = (WALL_X1 - WALL_X0 + 1) * 4 * 5;

    /** Holds the charge for {@code ticks} server ticks and releases it. */
    private static void chargeAndRelease(GunRig rig, int ticks) {
        ServerGunHandler.onCharge(rig.shooter, true);
        for (int i = 0; i < ticks; i++) {
            rig.tick();
        }
        ServerGunHandler.onCharge(rig.shooter, false);
        rig.tick();
    }

    /** A full charge against a wall breaks the blocks of the box and the air pays for each. */
    private static final class Wave implements Scenario {

        private final String name;
        private final String gun;

        Wave(String name, String gun) {
            this.name = name;
            this.gun = gun;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public String description() {
            return "A full 20 tick charge spends 10 rounds of air, and the shockwave breaks the 4 x 4 box of stone in front of the shooter (nearest first, one round per block) and leaves the stone beyond its reach.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            GunRig rig = rig(ctx, gun, 300);
            wall(ctx);
            int before = standing(ctx);
            chargeAndRelease(rig, 20);
            int broken = before - standing(ctx);
            out.checkEquals("wall blocks before", WALL_BLOCKS, before);
            out.check(
                broken >= 16 && broken <= Shockwave.SIZE * Shockwave.SIZE * Shockwave.SIZE,
                "the shockwave should break between 16 and 64 blocks, broke " + broken);
            out.checkEquals("rounds: 10 for the charge and one per block", 300 - 10 - broken, rig.loaded());
            out.check(
                ctx.world()
                    .getBlock(ctx.originX() + WALL_X0, ctx.originY() + 1, ctx.originZ()) == Blocks.air,
                "the wall face in front of the shooter should be broken");
            out.check(
                ctx.world()
                    .getBlock(ctx.originX() + WALL_X1, ctx.originY() + 1, ctx.originZ()) == Blocks.stone,
                "the back of the wall is beyond the box and should stand");
            return out.build();
        }
    }

    /** Cancels every break by the rig's shooter, like a claim mod, while registered. */
    public static final class Denier {

        /** @param event the break */
        @SubscribeEvent
        public void onBreak(BlockEvent.BreakEvent event) {
            if (SHOOTER_NAME.equals(
                event.getPlayer()
                    .getCommandSenderName())) {
                event.setCanceled(true);
            }
        }
    }

    /** A refused break leaves the block and costs no air; once allowed the same release breaks. */
    private static final class Refused implements Scenario {

        private final String name;
        private final String gun;

        Refused(String name, String gun) {
            this.name = name;
            this.gun = gun;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public String description() {
            return "With a protection listener cancelling the BreakEvent, a full charge breaks no block of the wall and spends only the 10 rounds of the charge (BlockBreakGuard refuses each shockwave block); with the listener gone the next charge breaks blocks.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            GunRig rig = rig(ctx, gun, 300);
            wall(ctx);
            Denier denier = new Denier();
            MinecraftForge.EVENT_BUS.register(denier);
            try {
                chargeAndRelease(rig, 20);
            } finally {
                MinecraftForge.EVENT_BUS.unregister(denier);
            }
            out.checkEquals("wall blocks standing after a refused wave", WALL_BLOCKS, standing(ctx));
            out.checkEquals("only the charge was spent", 290, rig.loaded());
            for (int i = 0; i < 6; i++) {
                rig.tick(); // the 4 tick cooldown after the release
            }
            chargeAndRelease(rig, 20);
            out.check(standing(ctx) < WALL_BLOCKS, "with the listener gone the shockwave should break blocks");
            return out.build();
        }
    }

    /** Half a charge hurts a zombie in the box for 5 x 2.5. */
    private static final class Hit implements Scenario {

        private final String name;
        private final String gun;

        Hit(String name, String gun) {
            this.name = name;
            this.gun = gun;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public String description() {
            return "A half charge (10 ticks) spends 5 rounds and hits a zombie 2 blocks ahead for 5 x 2.5 = 12.5 raw PHYSICAL damage; it breaks no block in an open corridor.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            GunRig rig = rig(ctx, gun, 300);
            EntityZombie target = GunRig.zombie(ctx, 2);
            chargeAndRelease(rig, 10);
            double armor = target.getTotalArmorValue();
            float expected = (float) DamageModel
                .resolve(DamageType.PHYSICAL, 12.5, 0.0, share -> share * DamageModel.vanillaArmorFactor(armor));
            float lost = target.getMaxHealth() - target.getHealth();
            out.check(GunRig.near(expected, lost), "the zombie should lose " + expected + " but lost " + lost);
            out.checkEquals("rounds after a half charge in an open corridor", 295, rig.loaded());
            ItemStack held = rig.shooter.getHeldItem();
            out.check(held != null && held.getItem() instanceof ItemGun, "the hammer is still in hand");
            return out.build();
        }
    }
}
