package com.gregguns.gun.server;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraftforge.common.util.FakePlayer;

import com.gregguns.api.Tier;
import com.gregguns.armor.slots.ElectricGear;
import com.gregguns.combat.DamageModel;
import com.gregguns.combat.DamageType;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.gun.def.GunDefinition;
import com.gregguns.gun.def.GunDefinitions;
import com.gregguns.gun.logic.AmmoInventory;
import com.gregguns.gun.logic.ChargeSource;
import com.gregguns.gun.logic.FireControl;
import com.gregguns.gun.logic.GunController;
import com.gregguns.gun.logic.GunState;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.ShotContext;
import com.gregguns.gun.logic.ShotOffset;
import com.gregguns.gun.logic.ShotPattern;
import com.gregguns.gun.logic.SpreadModel;
import com.gregguns.item.gun.GunItems;
import com.gregguns.item.gun.ItemEnergyGun;
import com.gregguns.item.gun.StackCharge;
import com.gregguns.projectile.BeamVisual;
import com.gregguns.projectile.EnergyBlastProjectile;
import com.gregguns.projectile.GenericProjectile;
import com.gregguns.projectile.LaserProjectile;
import com.gregguns.projectile.Projectiles;
import com.gregguns.recipe.GateRule;
import com.gregguns.recipe.GunRecipeScenarios;
import com.gregguns.recipe.ProductionTable;

import cpw.mods.fml.common.FMLCommonHandler;

/**
 * The {@code gun.*} scenarios of the IV energy batch (M5 B4): the Blaster Rifle, the De-Atomizer Pistol with its
 * Akimbo, and the Scatter Laser Rifle. Each gun's fire scenario drives the real {@link ServerGunHandler} through a
 * {@link GunRig} on a charged stack and checks what one shot costs in EU and what it launches; the hit scenarios
 * launch the gun's own stats at a zombie; the akimbo scenarios check the alternating hands and the charge through the
 * real crafting manager; one tier-gate scenario covers all three. Dev environment only, server side.
 */
// Spec: docs/techguns-spec/02-guns/sheets-energy-special.md (Blaster Rifle, De-Atomizer Pistol, Akimbo, Scatter Laser
// Rifle); docs/gregguns-spec/02-guns/energy-electric-items.md (GGS-02-09 to GGS-02-16); docs/testing/README.md
public final class EnergyBatchScenarios {

    private static final int HELD_TICKS = 13;
    private static final double TOLERANCE = 0.25;

    private EnergyBatchScenarios() {}

    /**
     * Registers the Blaster Rifle's scenarios.
     *
     * @param registry where to add them
     * @param gunName  {@code "blaster_rifle"}
     */
    public static void registerBlaster(ScenarioRegistry registry, String gunName) {
        GunDefinition def = GunDefinitions.byName(gunName);
        registry.register(new AutoFire(def, EnergyBlastProjectile.class));
        registry.register(new BoltHit(def, 8));
        registry.register(recipe(def));
        registry.register(new TierGate());
    }

    /**
     * Registers the De-Atomizer Pistol's scenarios.
     *
     * @param registry where to add them
     * @param gunName  {@code "deatomizer_pistol"}
     */
    public static void registerDeatomizer(ScenarioRegistry registry, String gunName) {
        GunDefinition def = GunDefinitions.byName(gunName);
        registry.register(new SemiFire(def));
        registry.register(new BoltHit(def, 8));
        registry.register(recipe(def));
    }

    /**
     * Registers the Akimbo De-Atomizer Pistols' scenarios.
     *
     * @param registry where to add them
     * @param gunName  {@code "akimbo_deatomizer_pistols"}
     */
    public static void registerAkimbo(ScenarioRegistry registry, String gunName) {
        GunDefinition def = GunDefinitions.byName(gunName);
        registry.register(new AkimboFire(def));
        registry.register(new AkimboCombine(def));
    }

    /**
     * Registers the Scatter Laser Rifle's scenarios.
     *
     * @param registry where to add them
     * @param gunName  {@code "scatter_laser_rifle"}
     */
    public static void registerScatter(ScenarioRegistry registry, String gunName) {
        GunDefinition def = GunDefinitions.byName(gunName);
        registry.register(new ScatterFire(def));
        registry.register(new ScatterHit(def));
        registry.register(recipe(def));
    }

    private static Scenario recipe(GunDefinition def) {
        return GunRecipeScenarios.assembly(
            "gun." + def.name() + ".recipe",
            "The " + def.name()
                + " is in GT's assembler map (so NEI lists it) at "
                + def.tier()
                + " with its sheet inputs, and not below it.",
            def.assembly());
    }

    // ----- shared helpers ----------------------------------------------------------------------------------------

    /** A rig holding the gun charged with {@code shots} shots' worth of EU. */
    private static GunRig chargedRig(ScenarioContext ctx, GunDefinition def, int shots, int corridor) {
        GunRig rig = new GunRig(ctx, GunItems.get(def.name()), 0, corridor);
        ElectricGear.charge(
            rig.gun,
            (double) def.stats()
                .euPerShot() * shots,
            def.tier());
        return rig;
    }

    private static long stored(ItemStack gun) {
        return (long) Math.floor(ElectricGear.charge(gun));
    }

    private static boolean allOfType(List<GenericProjectile> seen, Class<?> type) {
        for (GenericProjectile p : seen) {
            if (!type.isInstance(p)) {
                return false;
            }
        }
        return true;
    }

    private static int beams(ScenarioContext ctx) {
        int n = 0;
        for (Object o : ctx.world().loadedEntityList) {
            if (o instanceof BeamVisual && !((Entity) o).isDead) {
                n++;
            }
        }
        return n;
    }

    private static float lost(EntityZombie z) {
        return z.getMaxHealth() - z.getHealth();
    }

    private static float expectedEnergyLoss(EntityZombie z, double damage, double penetration) {
        double armor = z.getTotalArmorValue();
        return (float) DamageModel
            .resolve(DamageType.ENERGY, damage, penetration, share -> share * DamageModel.vanillaArmorFactor(armor));
    }

    private static EntityZombie tankyZombie(ScenarioContext ctx, int dx) {
        EntityZombie z = GunRig.zombie(ctx, dx);
        z.getEntityAttribute(SharedMonsterAttributes.maxHealth)
            .setBaseValue(400.0);
        z.setHealth(400.0F);
        z.hurtResistantTime = 0;
        return z;
    }

    // ----- Blaster Rifle -----------------------------------------------------------------------------------------

    /** Holding the trigger fires an energy bolt every interval, each spending exactly one shot's EU. */
    private static final class AutoFire implements Scenario {

        private final GunDefinition def;
        private final Class<?> projectile;

        AutoFire(GunDefinition def, Class<?> projectile) {
            this.def = def;
            this.projectile = projectile;
        }

        @Override
        public String name() {
            return "gun." + def.name() + ".fire";
        }

        @Override
        public String description() {
            return "Holding the trigger on a charged " + def.name()
                + " fires every "
                + def.stats()
                    .fireRateTicks()
                + " ticks: each shot launches one energy bolt and spends exactly "
                + def.stats()
                    .euPerShot()
                + " EU from the stack, with no reload and no round change.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GunStats stats = def.stats();
            GunRig rig = chargedRig(ctx, def, 6, 12);
            long start = stored(rig.gun);
            rig.press();
            return ScenarioResult.pending(name(), HELD_TICKS + 10, ticks -> {
                rig.tick();
                if (ticks < HELD_TICKS) {
                    return null;
                }
                rig.release();
                int expected = 1 + (HELD_TICKS - 1) / stats.fireRateTicks();
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.checkEquals("bolts launched", expected, rig.bullets());
                out.check(allOfType(rig.seenProjectiles(), projectile), "every projectile should be an energy blast");
                out.checkEquals("EU spent", (long) expected * stats.euPerShot(), start - stored(rig.gun));
                out.check(stats.isEnergy(), "an energy gun");
                out.check(stats.hasScope(), "the gun has a scope");
                return out.build();
            });
        }
    }

    /** A bolt launched from the gun's stats hurts a zombie by the ENERGY figure with the gun's armor penetration. */
    private static final class BoltHit implements Scenario {

        private final GunDefinition def;
        private final int range;

        BoltHit(GunDefinition def, int range) {
            this.def = def;
            this.range = range;
        }

        @Override
        public String name() {
            return "gun." + def.name() + ".hit";
        }

        @Override
        public String description() {
            return "One " + def.name()
                + " bolt (straight, no spread) hits a zombie "
                + range
                + " blocks away for its ENERGY damage figure after the damage model with the gun's armor penetration, inside the full-damage range.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GunStats stats = def.stats();
            GunRig rig = new GunRig(ctx, GunItems.get(def.name()), 0, range + 4);
            EntityZombie z = tankyZombie(ctx, range);
            ShotOffset straight = SpreadModel.roll(ctx.random(), stats, ShotContext.npc(0.0), 0)[0];
            Projectiles.launcher()
                .launch(ctx.world(), rig.shooter, stats, straight, 1.0F);
            int flight = (int) Math.ceil(range / stats.projectileSpeed()) + 4;
            return ScenarioResult.pending(name(), flight + 10, ticks -> {
                if (ticks < flight) {
                    return null;
                }
                float expected = expectedEnergyLoss(z, stats.damage(), stats.penetration());
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.check(
                    Math.abs(expected - lost(z)) < TOLERANCE,
                    "zombie should lose " + expected + " but lost " + lost(z));
                return out.build();
            });
        }
    }

    // ----- De-Atomizer Pistol ------------------------------------------------------------------------------------

    /** Semi-auto: a held trigger fires once; a second pull after the interval fires again. */
    private static final class SemiFire implements Scenario {

        private final GunDefinition def;

        SemiFire(GunDefinition def) {
            this.def = def;
        }

        @Override
        public String name() {
            return "gun." + def.name() + ".fire";
        }

        @Override
        public String description() {
            return "A charged " + def.name()
                + " is semi-auto: holding the trigger for 20 ticks fires one blue blast, a second pull fires another, each spending exactly "
                + def.stats()
                    .euPerShot()
                + " EU; the blast has no area explosion (blast radius 0).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GunStats stats = def.stats();
            GunRig rig = chargedRig(ctx, def, 4, 12);
            long start = stored(rig.gun);
            rig.press();
            int[] shotsAfterHold = new int[1];
            return ScenarioResult.pending(name(), 60, ticks -> {
                rig.tick();
                if (ticks == 20) {
                    shotsAfterHold[0] = rig.bullets();
                    rig.release();
                    return null;
                }
                if (ticks == 21) {
                    rig.press();
                    return null;
                }
                if (ticks < 30) {
                    return null;
                }
                rig.release();
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.checkEquals("blasts after holding the trigger for 20 ticks", 1, shotsAfterHold[0]);
                out.checkEquals("blasts after the second pull", 2, rig.bullets());
                out.check(allOfType(rig.seenProjectiles(), EnergyBlastProjectile.class), "every projectile is a blast");
                out.checkEquals("EU spent", 2L * stats.euPerShot(), start - stored(rig.gun));
                out.checkEquals("no area explosion", 0.0, stats.blastRadius());
                return out.build();
            });
        }
    }

    // ----- Akimbo De-Atomizer Pistols ----------------------------------------------------------------------------

    /** Alternating hands: one shot per pull, the hand by the parity of the shots of charge left. */
    private static final class AkimboFire implements Scenario {

        private final GunDefinition def;

        AkimboFire(GunDefinition def) {
            this.def = def;
        }

        @Override
        public String name() {
            return "gun." + def.name() + ".fire";
        }

        @Override
        public String description() {
            return "The Akimbo De-Atomizer Pistols spend one shot's EU per pull and alternate hands: four pulls give two of each hand and no two in a row from the same hand.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GunStats stats = def.stats();
            ItemStack stack = new ItemStack(GunItems.get(def.name()));
            ElectricGear.charge(stack, (double) stats.euPerShot() * 5, def.tier());
            GunController controller = new GunController(new FireControl(0, 0));
            GunState state = GunState.full(stats);
            AmmoInventory ammo = new AmmoInventory() {

                @Override
                public int count(String ammoType) {
                    return 0;
                }

                @Override
                public int withdraw(String ammoType, int maxItems) {
                    return 0;
                }

                @Override
                public void depositItems(String ammoType, int items) {}

                @Override
                public void depositRounds(String ammoType, int rounds) {}

                @Override
                public ChargeSource charge() {
                    return StackCharge.spending(stack, null);
                }
            };
            long start = stored(stack);
            int[] hands = new int[4];
            int fired = 0;
            for (int pull = 0; pull < 4; pull++) {
                controller.triggerPressed();
                for (int t = 0; t < stats.fireRateTicks() + 2; t++) {
                    var r = controller.tick(stats, state, ammo, true, false);
                    if (r.shot()
                        .wasFired()) {
                        hands[fired++] = r.shot()
                            .attackType();
                    }
                }
                controller.triggerReleased();
                controller.tick(stats, state, ammo, true, false);
            }
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            out.checkEquals("shots from four pulls", 4, fired);
            out.checkEquals("EU spent", 4L * stats.euPerShot(), start - stored(stack));
            int left = 0;
            for (int i = 0; i < 4; i++) {
                left += hands[i];
                if (i > 0) {
                    out.check(hands[i] != hands[i - 1], "hand " + i + " should differ from the one before");
                }
            }
            out.checkEquals("left-hand shots", 2, left);
            out.check(stats.akimbo() && !stats.doubleShot(), "alternating akimbo, not a double shot");
            return out.build();
        }
    }

    /** The crafting-table combine and split carry the charge: the sum in, the halves out (odd EU to the second). */
    private static final class AkimboCombine implements Scenario {

        private final GunDefinition def;

        AkimboCombine(GunDefinition def) {
            this.def = def;
        }

        @Override
        public String name() {
            return "gun." + def.name() + ".combine";
        }

        @Override
        public String description() {
            return "Two De-Atomizer Pistols (5,000,001 and 10,000,000 EU) craft into the Akimbo with 15,000,001 EU and no machine row; crafting that back gives one gun with half (7,500,000) and, once, a second with the remainder (7,500,001).";
        }

        private InventoryCrafting grid(ItemStack... stacks) {
            InventoryCrafting grid = new InventoryCrafting(new Container() {

                @Override
                public boolean canInteractWith(EntityPlayer player) {
                    return true;
                }
            }, 3, 3);
            for (int i = 0; i < stacks.length; i++) {
                grid.setInventorySlotContents(i, stacks[i]);
            }
            return grid;
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            out.check(def.assembly() == null, "the akimbo gun has no machine row (crafting table only)");
            GunDefinition singleDef = GunDefinitions.byName(def.singleGun());
            ItemEnergyGun singleItem = (ItemEnergyGun) GunItems.get(singleDef.name());
            ItemStack a = new ItemStack(singleItem);
            ItemStack b = new ItemStack(singleItem);
            ElectricGear.charge(a, 5_000_001, singleDef.tier());
            ElectricGear.charge(b, 10_000_000, singleDef.tier());

            ItemStack pair = CraftingManager.getInstance()
                .findMatchingRecipe(grid(a, null, b), ctx.world());
            out.check(
                pair != null && pair.getItem() == GunItems.get(def.name()),
                "two guns should craft the akimbo gun");
            if (pair == null || pair.getItem() != GunItems.get(def.name())) {
                return out.build();
            }
            out.checkEquals("combined charge", 15_000_001L, stored(pair));

            InventoryCrafting splitGrid = grid(pair);
            ItemStack first = CraftingManager.getInstance()
                .findMatchingRecipe(splitGrid, ctx.world());
            out.check(first != null && first.getItem() == singleItem, "the akimbo gun should split");
            if (first == null || first.getItem() != singleItem) {
                return out.build();
            }
            out.checkEquals("first gun charge (half)", 7_500_000L, stored(first));
            FakePlayer player = GunRig.fakePlayer(ctx, "[GregGunsAkimboEnergyTest]", 0);
            FMLCommonHandler.instance()
                .firePlayerCraftingEvent(player, first, splitGrid);
            ItemStack second = null;
            for (ItemStack s : player.inventory.mainInventory) {
                if (s != null && s.getItem() == singleItem) {
                    second = s;
                }
            }
            out.check(second != null, "the second gun should be in the player's inventory");
            if (second != null) {
                out.checkEquals("second gun charge (the remainder)", 7_500_001L, stored(second));
            }
            return out.build();
        }
    }

    // ----- Scatter Laser Rifle -----------------------------------------------------------------------------------

    /** One pull fires five beams and spends one shot's EU. */
    private static final class ScatterFire implements Scenario {

        private final GunDefinition def;

        ScatterFire(GunDefinition def) {
            this.def = def;
        }

        @Override
        public String name() {
            return "gun." + def.name() + ".fire";
        }

        @Override
        public String description() {
            return "One pull on a charged Scatter Laser Rifle launches five laser carriers and shows five beams, and spends exactly one shot's EU ("
                + def.stats()
                    .euPerShot()
                + ").";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GunStats stats = def.stats();
            GunRig rig = chargedRig(ctx, def, 4, 12);
            long start = stored(rig.gun);
            int beamsBefore = beams(ctx);
            rig.press();
            return ScenarioResult.pending(name(), 20, ticks -> {
                rig.tick();
                if (ticks < 2) {
                    return null;
                }
                rig.release();
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.checkEquals("laser carriers launched", 5, rig.bullets());
                out.check(allOfType(rig.seenProjectiles(), LaserProjectile.class), "every projectile is a laser");
                out.checkEquals("beams shown", beamsBefore + 5, beams(ctx));
                out.checkEquals("EU spent", stats.euPerShot(), start - stored(rig.gun));
                out.checkEquals(
                    "pattern projectiles",
                    5,
                    stats.pattern()
                        .projectileCount());
                out.check(
                    stats.pattern()
                        .shape() == ShotPattern.Shape.FAN,
                    "a fan");
                return out.build();
            });
        }
    }

    /** All five beams of a fan hit a zombie two blocks away. */
    private static final class ScatterHit implements Scenario {

        private final GunDefinition def;

        ScatterHit(GunDefinition def) {
            this.def = def;
        }

        @Override
        public String name() {
            return "gun." + def.name() + ".hit";
        }

        @Override
        public String description() {
            return "The five beams of one fan, rolled from the gun's pattern, all hit a zombie two blocks away: it loses five times the ENERGY figure of one beam.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GunStats stats = def.stats();
            GunRig rig = new GunRig(ctx, GunItems.get(def.name()), 0, 8);
            EntityZombie z = tankyZombie(ctx, 2);
            ShotOffset[] offsets = stats.pattern()
                .roll(ctx.random(), stats, ShotContext.npc(0.0), 0);
            for (ShotOffset offset : offsets) {
                z.hurtResistantTime = 0;
                Projectiles.launcher()
                    .launch(ctx.world(), rig.shooter, stats, offset, 1.0F);
            }
            float expected = offsets.length * expectedEnergyLoss(z, stats.damage(), stats.penetration());
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            out.checkEquals("beams in the fan", 5, offsets.length);
            out.check(
                Math.abs(expected - lost(z)) < TOLERANCE,
                "zombie should lose " + expected + " but lost " + lost(z));
            return out.build();
        }
    }

    // ----- tier gate ---------------------------------------------------------------------------------------------

    /** The IV batch: IC2 tier 5, an 8192 EU/t transfer limit, tungsten steel gate, nothing below IV. */
    private static final class TierGate implements Scenario {

        @Override
        public String name() {
            return "gun.energy_iv.tier_gate";
        }

        @Override
        public String description() {
            return "The IV energy guns (Blaster Rifle, De-Atomizer Pistol, Akimbo, Scatter Laser Rifle) are IC2 tier 5 electric items with an 8192 EU/t transfer limit that never give energy back; the three machine rows are IV Assembler rows gated by a tungsten steel plate, and the Akimbo has no row.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            String[] guns = { "blaster_rifle", "deatomizer_pistol", "akimbo_deatomizer_pistols",
                "scatter_laser_rifle" };
            for (String g : guns) {
                GunDefinition def = GunDefinitions.byName(g);
                ItemStack stack = new ItemStack(GunItems.get(g));
                ItemEnergyGun item = (ItemEnergyGun) GunItems.get(g);
                out.checkEquals(g + " tier", Tier.IV, def.tier());
                out.checkEquals(g + " IC2 tier", 5, item.getTier(stack));
                out.checkEquals(g + " transfer limit", 8192.0, item.getTransferLimit(stack));
                out.check(!item.canProvideEnergy(stack), g + " gives no energy back");
                ProductionTable.Row row = def.assembly();
                if (def.singleGun() == null) {
                    out.check(row != null, g + " has a machine row");
                    if (row != null) {
                        out.checkEquals(g + " row tier", Tier.IV, row.tier());
                        out.check(
                            GateRule.satisfied(row, ProductionTable.byOutput(), ProductionTable.GATE_EXEMPT),
                            g + " row has the tungsten steel gate");
                    }
                } else {
                    out.check(row == null, g + " is crafting-table only");
                }
            }
            return out.build();
        }
    }
}
