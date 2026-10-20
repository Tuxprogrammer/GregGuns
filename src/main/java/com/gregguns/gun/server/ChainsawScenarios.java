package com.gregguns.gun.server;

import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.init.Blocks;
import net.minecraftforge.common.ForgeHooks;

import com.gregguns.combat.DamageModel;
import com.gregguns.combat.DamageType;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.gun.def.ChainsawDef;
import com.gregguns.item.gun.GunItems;
import com.gregguns.item.gun.ItemToolGun;
import com.gregguns.projectile.InvisibleProjectile;
import com.gregguns.recipe.GunRecipeScenarios;

/**
 * The {@code gun.chainsaw.*} scenarios: the right-click cut (an invisible projectile that hurts the target and costs a
 * round), and the left button (melee, and mining a log as an axe). Dev environment only, server side.
 */
// Spec: docs/techguns-spec/02-guns/sheets-launchers-tools-grenades.md (Chainsaw);
// docs/gregguns-spec/02-guns/tool-guns.md
public final class ChainsawScenarios {

    private ChainsawScenarios() {}

    /**
     * Registers the Chainsaw scenarios.
     *
     * @param registry where to add them
     */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new Cut());
        registry.register(new LeftClick());
        registry.register(
            GunRecipeScenarios.assembly(
                "gun.chainsaw.recipe",
                "The Chainsaw (GR-90) is in GT's assembler map at HV, 35 s, matching its sheet inputs, and not below HV.",
                ChainsawDef.DEFINITION.assembly()));
    }

    private static float expected(EntityZombie z, double raw, double penetration) {
        double armor = z.getTotalArmorValue();
        return (float) DamageModel
            .resolve(DamageType.PHYSICAL, raw, penetration, share -> share * DamageModel.vanillaArmorFactor(armor));
    }

    private static boolean sawInvisibleCut(ScenarioContext ctx) {
        for (Object o : ctx.world().loadedEntityList) {
            if (o instanceof InvisibleProjectile) {
                return true;
            }
        }
        return false;
    }

    /** Right click: one invisible projectile, 10 damage with 25 percent penetration, one round. */
    private static final class Cut implements Scenario {

        private static final int END_TICK = 6;

        @Override
        public String name() {
            return "gun.chainsaw.cut";
        }

        @Override
        public String description() {
            return "Holding the trigger on the Chainsaw fires an InvisibleProjectile that costs a zombie 2 blocks away 10 PHYSICAL damage with 25% armor penetration and spends one round.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GunRig rig = new GunRig(ctx, GunItems.get(ChainsawDef.NAME), 300);
            EntityZombie target = GunRig.zombie(ctx, 2);
            rig.press();
            rig.tick();
            boolean[] seen = { sawInvisibleCut(ctx) };
            return ScenarioResult.pending(name(), END_TICK + 10, ticks -> {
                rig.release();
                rig.tick();
                seen[0] |= sawInvisibleCut(ctx);
                if (ticks < END_TICK) {
                    return null;
                }
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                float want = expected(target, 10.0, 0.25);
                float lost = target.getMaxHealth() - target.getHealth();
                out.check(seen[0] || rig.bullets() > 0, "the cut should be an InvisibleProjectile entity");
                out.check(GunRig.near(want, lost), "the zombie should lose " + want + " but lost " + lost);
                out.checkEquals("rounds after one cut", 299, rig.loaded());
                return out.build();
            });
        }
    }

    /** Left click: melee 12 (+1 hand) and mining a log as an axe level 3 at speed 14. */
    private static final class LeftClick implements Scenario {

        @Override
        public String name() {
            return "gun.chainsaw.left_click";
        }

        @Override
        public String description() {
            return "A loaded Chainsaw mines a log (axe 3, dig speed 14, harvests it, one round) and its melee hit costs a zombie 1 + 12 damage with 25% penetration and one round.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            ItemToolGun item = (ItemToolGun) GunItems.get(ChainsawDef.NAME);
            GunRig rig = new GunRig(ctx, item, 300);
            // A real player's tick applies the held item's attribute modifiers; a fake player is not ticked.
            rig.shooter.getAttributeMap()
                .applyAttributeModifiers(rig.gun.getAttributeModifiers());

            // IguanaTweaks renumbers harvest levels in the pack (our axe level 3 reads back 5), so only a floor is
            // checked.
            out.check(item.getHarvestLevel(rig.gun, "axe") >= 3, "axe level should be at least 3");
            out.check(item.getDigSpeed(rig.gun, Blocks.log, 0) == 14.0F, "dig speed on a log should be 14");
            out.check(ForgeHooks.canToolHarvestBlock(Blocks.log, 0, rig.gun), "a loaded Chainsaw harvests a log");
            ctx.setBlock(3, 0, 0, Blocks.log, 0);
            ItemToolGun.BreakResult mined = item
                .breakBlock(rig.shooter, rig.gun, ctx.originX() + 3, ctx.originY(), ctx.originZ());
            out.check(mined == ItemToolGun.BreakResult.BROKEN, "the log should break, was " + mined);
            out.checkEquals("rounds after one block", 299, rig.loaded());

            EntityZombie target = GunRig.zombie(ctx, 2);
            rig.shooter.attackTargetEntityWithCurrentItem(target);
            float want = expected(target, 13.0, 0.25);
            float lost = target.getMaxHealth() - target.getHealth();
            out.check(GunRig.near(want, lost), "a melee hit should deal " + want + " but dealt " + lost);
            out.checkEquals("rounds after the hit", 298, rig.loaded());
            return out.build();
        }
    }
}
