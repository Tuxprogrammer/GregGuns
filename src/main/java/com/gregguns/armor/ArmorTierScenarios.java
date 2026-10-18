package com.gregguns.armor;

import java.util.List;
import java.util.UUID;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.BreakSpeed;

import com.gregguns.GregGuns;
import com.gregguns.api.Tier;
import com.gregguns.armor.logic.ArmorBonus;
import com.gregguns.armor.logic.ArmorCatalog;
import com.gregguns.armor.logic.ArmorSlot;
import com.gregguns.armor.logic.PowerRules;
import com.gregguns.armor.logic.SetDef;
import com.gregguns.armor.slots.ElectricGear;
import com.gregguns.combat.DamageSources;
import com.gregguns.combat.DamageType;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.recipe.ArmorTable;
import com.gregguns.recipe.ProductionTable;
import com.mojang.authlib.GameProfile;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.TierEU;
import gregtech.api.hazards.Hazard;
import gregtech.api.hazards.HazardProtection;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipeBuilder;

/**
 * The {@code armor.t2.*}, {@code armor.t3.*} and {@code armor.steam.*} in-game scenarios (M3 wave B): a fake player
 * wears a full set, the bonus and the typed-damage reduction are checked against the spec tables, and the set comes off
 * again. Dev environment only. Server side.
 */
// Spec: docs/techguns-spec/05-armor/armor-catalog.md sections 2, 5 and 6; docs/gregguns-spec/05-armor/recipes.md;
// docs/gregguns-spec/05-armor/hazard-protection.md; docs/testing/README.md
final class ArmorTierScenarios {

    private static final float EPS = 0.01F;

    private ArmorTierScenarios() {}

    static void registerAll(ScenarioRegistry registry) {
        registry.register(new T2Combat());
        registry.register(new T2Commando());
        registry.register(new T2Riot());
        registry.register(new T2Beret());
        registry.register(new T3Combat());
        registry.register(new T3Hev());
        registry.register(new SteamPower());
        registry
            .register(new Recipes("armor.t2.recipes", "T2 Combat, Commando, Riot and the Beret on the MV Assembler.") {

                @Override
                List<ProductionTable.Row> rows() {
                    List<ProductionTable.Row> all = new java.util.ArrayList<>(ArmorTable.T2_COMBAT);
                    all.addAll(ArmorTable.T2_COMMANDO);
                    all.addAll(ArmorTable.T2_RIOT);
                    all.add(ArmorTable.BERET);
                    return all;
                }
            });
        registry.register(new Recipes("armor.t3.recipes", "T3 Combat and the HEV suit on the EV Assembler.") {

            @Override
            List<ProductionTable.Row> rows() {
                List<ProductionTable.Row> all = new java.util.ArrayList<>(ArmorTable.T3_COMBAT);
                all.addAll(ArmorTable.T3_HEV);
                return all;
            }
        });
        registry.register(
            new Recipes(
                "armor.steam.recipes",
                "Steam Armor Plating and the four steam armor pieces on the EV Assembler.") {

                @Override
                List<ProductionTable.Row> rows() {
                    List<ProductionTable.Row> all = new java.util.ArrayList<>(ArmorTable.STEAM);
                    all.add(ArmorTable.STEAM_PLATING);
                    return all;
                }
            });
    }

    /** A fake player whose {@code damageEntity} can be called directly (a fresh server player is invulnerable). */
    private static final class TestPlayer extends FakePlayer {

        TestPlayer(WorldServer world, String name) {
            super(world, new GameProfile(UUID.nameUUIDFromBytes(name.getBytes()), name));
        }

        @Override
        public boolean isEntityInvulnerable() {
            return false;
        }

        float hurt(DamageSource src, float damage) {
            setHealth(getMaxHealth());
            float before = getHealth();
            damageEntity(src, damage);
            return before - getHealth();
        }

        void wearAll(SetDef set) {
            for (ArmorSlot slot : set.pieces()
                .keySet()) {
                // armorInventory[0] is the boots, [3] the helmet.
                inventory.armorInventory[3 - slot.armorType()] = new ItemStack(ArmorItems.get(set, slot));
            }
        }

        ItemStack worn(ArmorSlot slot) {
            return inventory.armorInventory[3 - slot.armorType()];
        }

        void undress() {
            for (int i = 0; i < 4; i++) {
                inventory.armorInventory[i] = null;
            }
        }

        void setInWater(boolean in) {
            inWater = in;
        }
    }

    private static TestPlayer player(ScenarioContext ctx, String name) {
        return new TestPlayer((WorldServer) ctx.world(), name);
    }

    private static boolean near(double expected, double actual) {
        return Math.abs(expected - actual) < EPS;
    }

    private static double speedFactor(TestPlayer p) {
        IAttributeInstance a = p.getEntityAttribute(SharedMonsterAttributes.movementSpeed);
        return a.getAttributeValue() / a.getBaseValue();
    }

    private static double knockback(TestPlayer p) {
        return p.getEntityAttribute(SharedMonsterAttributes.knockbackResistance)
            .getAttributeValue();
    }

    /**
     * Damage left after a hit of {@code damage} against {@code armor} points, with {@code pen} penetration and
     * {@code resist} penetration resistance (the D4 model: the penetrating share skips armor).
     */
    private static double expected(double damage, double armor, double pen, double resist) {
        double eff = Math.max(0.0, pen - resist);
        return damage * (eff + (1.0 - eff) * (1.0 - armor / 25.0));
    }

    private static float hit(TestPlayer p, DamageType type, double pen) {
        return p.hurt(
            DamageSources.direct(type, null, pen)
                .withoutSecondaryEffects(),
            8.0F);
    }

    private static void checkHit(ScenarioResult.Builder out, TestPlayer p, DamageType type, double armor, double pen,
        double resist) {
        float got = hit(p, type, pen);
        double want = expected(8.0, armor, pen, resist);
        out.check(
            near(want, got),
            type + " 8 at " + armor + " armor, pen " + pen + ": expected " + want + ", got " + got);
    }

    private static void finish(TestPlayer p) {
        p.undress();
        ArmorModule.events()
            .strip(p);
    }

    private static void checkUnequipped(ScenarioResult.Builder out, TestPlayer p, ArmorEvents events) {
        p.undress();
        events.update(p);
        out.check(near(1.0, speedFactor(p)), "unequipped: expected speed x1.0, got " + speedFactor(p));
        out.check(near(0.0, knockback(p)), "unequipped: knockback resistance should be gone");
        out.check(
            events.applied(p)
                .isEmpty(),
            "unequipped: nothing should be recorded as applied");
    }

    // --- T2 -----------------------------------------------------------------------------------------------------

    /** T2 Combat: +40% speed, 0.60 knockback resistance, physical 19, camos. */
    private static final class T2Combat implements Scenario {

        @Override
        public String name() {
            return "armor.t2.combat";
        }

        @Override
        public String description() {
            return "Full T2 Combat: speed x1.4, jump +0.10, knockback 0.60, vanilla bar 20; 8 projectile loses 6.08 and 8 energy 4.56; six camos; unequip clears.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            TestPlayer p = player(ctx, "[GGT2Combat]");
            ArmorEvents events = ArmorModule.events();
            try {
                p.wearAll(ArmorCatalog.T2_COMBAT);
                events.update(p);
                out.check(Armor.isWearingFullSet(p, ArmorCatalog.T2_COMBAT), "four pieces should be a full set");
                out.check(near(1.4, speedFactor(p)), "speed x1.4, got " + speedFactor(p));
                out.check(near(0.60, knockback(p)), "knockback 0.60, got " + knockback(p));
                out.check(near(0.10, Armor.bonus(p, ArmorBonus.JUMP)), "jump +0.10 from the boots");
                out.checkEquals("vanilla armor bar 5+6+5+4", 20, p.getTotalArmorValue());
                checkHit(out, p, DamageType.PROJECTILE, 19.0, 0.0, 0.0);
                checkHit(out, p, DamageType.ENERGY, 14.25, 0.0, 0.0);
                checkHit(out, p, DamageType.POISON, 0.0, 0.0, 0.0);
                ItemGregGunsArmor vest = (ItemGregGunsArmor) p.worn(ArmorSlot.CHESTPLATE)
                    .getItem();
                out.checkEquals("camo count", 6, vest.camoCount(p.worn(ArmorSlot.CHESTPLATE)));
                vest.setCamo(p.worn(ArmorSlot.CHESTPLATE), 4);
                out.check(
                    vest.getArmorTexture(p.worn(ArmorSlot.CHESTPLATE), p, 1, null)
                        .endsWith("t2_combat_swat_1.png"),
                    "camo 4 is SWAT");
                out.check(vest.repairSpec(p.worn(ArmorSlot.CHESTPLATE)) != null, "bench-repairable");
                checkUnequipped(out, p, events);
            } finally {
                finish(p);
            }
            return out.build();
        }
    }

    /** T2 Commando: gun accuracy, underwater mining, oxygen gear, rubber insulation. */
    private static final class T2Commando implements Scenario {

        @Override
        public String name() {
            return "armor.t2.commando";
        }

        @Override
        public String description() {
            return "Full Commando: speed x1.4, accuracy 0.20, underwater mining x6 only in water, oxygen gear; 8 poison loses 4.8, 8 radiation 6.4; full ELECTRICAL hazmat, not RADIOLOGICAL.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            TestPlayer p = player(ctx, "[GGT2Commando]");
            ArmorEvents events = ArmorModule.events();
            try {
                p.wearAll(ArmorCatalog.T2_COMMANDO);
                events.update(p);
                out.check(near(1.4, speedFactor(p)), "speed x1.4, got " + speedFactor(p));
                out.check(near(0.40, knockback(p)), "knockback 0.40, got " + knockback(p));
                out.check(near(0.20, Armor.bonus(p, ArmorBonus.GUN_ACCURACY)), "gun accuracy 0.05 x 4");
                out.check(Armor.bonus(p, ArmorBonus.OXYGEN_GEAR) > 0.0, "the helmet is oxygen gear");
                out.checkEquals("vanilla armor bar 5+5+5+4", 19, p.getTotalArmorValue());
                BreakSpeed dry = new BreakSpeed(p, Blocks.stone, 0, 4.0F, 0, 0, 0);
                MinecraftForge.EVENT_BUS.post(dry);
                out.check(near(4.0, dry.newSpeed), "dry mining is unchanged, got " + dry.newSpeed);
                p.setInWater(true);
                BreakSpeed wet = new BreakSpeed(p, Blocks.stone, 0, 4.0F, 0, 0, 0);
                MinecraftForge.EVENT_BUS.post(wet);
                p.setInWater(false);
                out.check(near(4.0 * 6.0, wet.newSpeed), "underwater mining x(1+5.0)=24, got " + wet.newSpeed);
                checkHit(out, p, DamageType.POISON, 10.0, 0.0, 0.0);
                checkHit(out, p, DamageType.RADIATION, 5.0, 0.0, 0.0);
                checkHit(out, p, DamageType.PROJECTILE, 18.0, 0.0, 0.0);
                out.check(
                    HazardProtection.isWearingFullHazmatAgainst(p, Hazard.ELECTRICAL),
                    "rubber gives full ELECTRICAL cover");
                out.check(
                    !HazardProtection.isWearingFullHazmatAgainst(p, Hazard.RADIOLOGICAL),
                    "no RADIOLOGICAL cover");
                checkUnequipped(out, p, events);
                out.check(Armor.bonus(p, ArmorBonus.GUN_ACCURACY) == 0.0, "accuracy ends with the set");
            } finally {
                finish(p);
            }
            return out.build();
        }
    }

    /** T2 Riot: penetration resistance 10%, lead lining. */
    private static final class T2Riot implements Scenario {

        @Override
        public String name() {
            return "armor.t2.riot";
        }

        @Override
        public String description() {
            return "Full Ranger Veteran: speed x1.5, knockback 0.70; 8 projectile at 15% penetration loses 1.92 (10% resisted); 8 radiation 2.88; full RADIOLOGICAL hazmat.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            TestPlayer p = player(ctx, "[GGT2Riot]");
            ArmorEvents events = ArmorModule.events();
            try {
                p.wearAll(ArmorCatalog.T2_RIOT);
                events.update(p);
                out.check(near(1.5, speedFactor(p)), "speed x1.5, got " + speedFactor(p));
                out.check(near(0.70, knockback(p)), "knockback 0.70, got " + knockback(p));
                out.check(near(0.15, Armor.bonus(p, ArmorBonus.JUMP)), "jump +0.15 from the boots");
                out.check(
                    near(0.10, Armor.penetrationResistance(p, DamageType.PROJECTILE)),
                    "penetration resistance 0.10, got " + Armor.penetrationResistance(p, DamageType.PROJECTILE));
                checkHit(out, p, DamageType.PROJECTILE, 20.0, 0.0, 0.10);
                checkHit(out, p, DamageType.PROJECTILE, 20.0, 0.15, 0.10);
                checkHit(out, p, DamageType.RADIATION, 16.0, 0.0, 0.10);
                checkHit(out, p, DamageType.ICE, 17.0, 0.0, 0.10);
                out.check(
                    HazardProtection.isWearingFullHazmatAgainst(p, Hazard.RADIOLOGICAL),
                    "lead gives full RADIOLOGICAL cover");
                out.check(!HazardProtection.isWearingFullHazmatAgainst(p, Hazard.ELECTRICAL), "no ELECTRICAL cover");
                checkUnequipped(out, p, events);
            } finally {
                finish(p);
            }
            return out.build();
        }
    }

    /** The Beret: one helmet, three colors on single sheets. */
    private static final class T2Beret implements Scenario {

        @Override
        public String name() {
            return "armor.t2.beret";
        }

        @Override
        public String description() {
            return "The Beret alone is a full set: speed x1.1, vanilla bar 2, 8 projectile loses 6.0; three colors map to one sheet each.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            TestPlayer p = player(ctx, "[GGT2Beret]");
            ArmorEvents events = ArmorModule.events();
            try {
                p.wearAll(ArmorCatalog.T2_BERET);
                events.update(p);
                out.check(Armor.isWearingFullSet(p, ArmorCatalog.T2_BERET), "the lone beret is the whole set");
                out.check(near(1.1, speedFactor(p)), "speed x1.1, got " + speedFactor(p));
                out.checkEquals("vanilla armor bar", 2, p.getTotalArmorValue());
                checkHit(out, p, DamageType.PROJECTILE, 2.0, 0.0, 0.0);
                ItemStack hat = p.worn(ArmorSlot.HELMET);
                ItemGregGunsArmor item = (ItemGregGunsArmor) hat.getItem();
                out.check(
                    item.getArmorTexture(hat, p, 0, null)
                        .endsWith("textures/models/armor/t2_beret.png"),
                    "red sheet");
                item.setCamo(hat, 2);
                out.check(
                    item.getArmorTexture(hat, p, 0, null)
                        .endsWith("textures/models/armor/t2_beret_green.png"),
                    "green sheet");
                checkUnequipped(out, p, events);
            } finally {
                finish(p);
            }
            return out.build();
        }
    }

    // --- T3 -----------------------------------------------------------------------------------------------------

    /** T3 Combat: 0.80 knockback resistance and 15% penetration resistance. */
    private static final class T3Combat implements Scenario {

        @Override
        public String name() {
            return "armor.t3.combat";
        }

        @Override
        public String description() {
            return "Full Advanced Combat: speed x1.4, knockback 0.80; 8 projectile loses 0.96, also at 15% penetration (fully resisted); 8 poison 4.8; three camos.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            TestPlayer p = player(ctx, "[GGT3Combat]");
            ArmorEvents events = ArmorModule.events();
            try {
                p.wearAll(ArmorCatalog.T3_COMBAT);
                events.update(p);
                out.check(near(1.4, speedFactor(p)), "speed x1.4, got " + speedFactor(p));
                out.check(near(0.80, knockback(p)), "knockback 0.80, got " + knockback(p));
                out.checkEquals("vanilla armor bar 6+7+6+4", 23, p.getTotalArmorValue());
                checkHit(out, p, DamageType.PROJECTILE, 22.0, 0.0, 0.15);
                checkHit(out, p, DamageType.PROJECTILE, 22.0, 0.15, 0.15);
                checkHit(out, p, DamageType.POISON, 10.0, 0.0, 0.15);
                checkHit(out, p, DamageType.EXPLOSION, 18.0, 0.0, 0.15);
                ItemStack boots = p.worn(ArmorSlot.BOOTS);
                ((ItemGregGunsArmor) boots.getItem()).setCamo(boots, 1);
                out.check(
                    ((ItemGregGunsArmor) boots.getItem()).getArmorTexture(boots, p, 3, null)
                        .endsWith("t3_combat_silver_1.png"),
                    "camo 1 is silver");
                checkUnequipped(out, p, events);
            } finally {
                finish(p);
            }
            return out.build();
        }
    }

    private static double units(TestPlayer p) {
        ItemStack chest = p.worn(ArmorSlot.CHESTPLATE);
        return ((ItemGregGunsArmor) chest.getItem()).powerUnits(chest);
    }

    /** T3 HEV: an electric chestplate switches the bonuses, cooling and the hazard flags. */
    private static final class T3Hev implements Scenario {

        @Override
        public String name() {
            return "armor.t3.hev";
        }

        @Override
        public String description() {
            return "HEV: empty gives no bonus and only the passive GAS cover; charged gives speed x1.4 (x6.4 in water), cooling cancels lava and costs 1 unit, jump and fall cost 5, walking 4 per second; HEAT cover follows the charge.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            TestPlayer p = player(ctx, "[GGT3Hev]");
            ArmorEvents events = ArmorModule.events();
            try {
                SetDef hev = ArmorCatalog.T3_HEV;
                p.wearAll(hev);
                events.update(p);
                out.check(near(1.0, speedFactor(p)), "empty chestplate: no speed bonus, got " + speedFactor(p));
                out.check(Armor.bonus(p, ArmorBonus.COOLING_SYSTEM) == 0.0, "empty: no cooling system");
                out.check(
                    HazardProtection.isWearingFullHazmatAgainst(p, Hazard.GAS),
                    "GAS cover is passive and needs no charge");
                out.check(
                    !HazardProtection.isWearingFullHazmatAgainst(p, Hazard.HEAT),
                    "HEAT cover needs a charged chestplate");
                checkHit(out, p, DamageType.PROJECTILE, 21.0, 0.0, 0.15);
                LivingAttackEvent lavaEmpty = new LivingAttackEvent(p, DamageSource.lava, 4.0F);
                out.check(!MinecraftForge.EVENT_BUS.post(lavaEmpty), "empty cooling system must not cancel lava");

                ItemStack chest = p.worn(ArmorSlot.CHESTPLATE);
                ElectricGear.charge(
                    chest,
                    hev.power()
                        .capacityEu(),
                    Tier.EV);
                out.checkEquals("full charge in units", 10000.0, units(p));
                events.update(p);
                out.check(near(1.4, speedFactor(p)), "charged: speed x1.4, got " + speedFactor(p));
                out.check(near(0.60, knockback(p)), "charged: knockback 0.60, got " + knockback(p));
                out.check(near(0.51, Armor.bonus(p, ArmorBonus.JUMP)), "charged: jump 0.30+0.03+0.03+0.15");
                out.check(Armor.bonus(p, ArmorBonus.STEP_ASSIST) > 0.0, "charged: step assist from the boots");
                out.check(Armor.bonus(p, ArmorBonus.OXYGEN_GEAR) > 0.0, "charged: the helmet is oxygen gear");
                for (Hazard h : new Hazard[] { Hazard.HEAT, Hazard.FROST, Hazard.ELECTRICAL, Hazard.GAS,
                    Hazard.BIOLOGICAL, Hazard.RADIOLOGICAL }) {
                    out.check(HazardProtection.isWearingFullHazmatAgainst(p, h), "charged: full cover against " + h);
                }
                out.check(!HazardProtection.isWearingFullHazmatAgainst(p, Hazard.SPACE), "never SPACE");
                for (DamageType t : new DamageType[] { DamageType.PROJECTILE, DamageType.ENERGY, DamageType.POISON,
                    DamageType.RADIATION, DamageType.EXPLOSION }) {
                    checkHit(out, p, t, 21.0, 0.0, 0.15);
                }

                p.setInWater(true);
                events.update(p);
                out.check(near(6.4, speedFactor(p)), "in water: speed x(1+0.4+5.0)=6.4, got " + speedFactor(p));
                BreakSpeed wet = new BreakSpeed(p, Blocks.stone, 0, 4.0F, 0, 0, 0);
                MinecraftForge.EVENT_BUS.post(wet);
                p.setInWater(false);
                events.update(p);
                out.check(near(1.4, speedFactor(p)), "out of water: speed back to x1.4, got " + speedFactor(p));
                out.check(near(4.0 * 1.4 * 6.0, wet.newSpeed), "mining x1.4 and x6 in water, got " + wet.newSpeed);

                // Cooling: a lava or fire hit is canceled and costs one unit; a burning wearer is put out.
                double before = units(p);
                LivingAttackEvent lava = new LivingAttackEvent(p, DamageSource.lava, 4.0F);
                out.check(MinecraftForge.EVENT_BUS.post(lava), "cooling system cancels a lava hit");
                out.check(units(p) == before - PowerRules.COOLING_COST, "a canceled hit costs one unit");
                LivingAttackEvent fire = new LivingAttackEvent(p, DamageSource.inFire, 1.0F);
                out.check(MinecraftForge.EVENT_BUS.post(fire), "cooling system cancels fire");
                p.setFire(10);
                events.update(p);
                out.check(!p.isBurning(), "cooling system puts the wearer out");

                // Costs: walking, jump and a long fall.
                before = units(p);
                ArmorPower.tick(p, true, 40L);
                out.check(units(p) == before - 4, "walking: one unit per piece per second, 4 pieces");
                ArmorPower.tick(p, true, 41L);
                out.check(units(p) == before - 4, "no drain between whole seconds");
                ArmorPower.tick(p, false, 60L);
                out.check(units(p) == before - 4, "standing still costs nothing");
                before = units(p);
                p.motionY = 0.0;
                MinecraftForge.EVENT_BUS.post(new LivingJumpEvent(p));
                out.check(near(0.51, p.motionY), "jump adds 0.51, got " + p.motionY);
                out.check(units(p) == before - PowerRules.JUMP_COST, "a jump costs 5 units");
                before = units(p);
                LivingFallEvent fall = new LivingFallEvent(p, 10.0F);
                MinecraftForge.EVENT_BUS.post(fall);
                out.check(
                    units(p) == before - PowerRules.FALL_COST,
                    "a long fall costs 5 units, left " + units(p) + " from " + before);
                out.check(fall.distance < 10.0F, "the fall is shortened, got " + fall.distance);
                before = units(p);
                MinecraftForge.EVENT_BUS.post(new LivingFallEvent(p, 2.0F));
                out.check(units(p) == before, "a short fall is free");

                // Discharge: every bonus and the powered covers go.
                ElectricGear.use(chest, ElectricGear.charge(chest), p);
                events.update(p);
                out.check(near(1.0, speedFactor(p)), "discharged: speed back to x1.0, got " + speedFactor(p));
                out.check(
                    !HazardProtection.isWearingFullHazmatAgainst(p, Hazard.HEAT),
                    "discharged: HEAT cover is gone");
                out.check(HazardProtection.isWearingFullHazmatAgainst(p, Hazard.GAS), "discharged: GAS cover stays");
                out.check(
                    !((ItemGregGunsArmor) chest.getItem()).isBroken(chest),
                    "an empty electric chestplate is not broken: IC2 shows charge, there is no wear");
                checkHit(out, p, DamageType.PROJECTILE, 21.0, 0.0, 0.15);
                checkUnequipped(out, p, events);
            } finally {
                finish(p);
            }
            return out.build();
        }
    }

    // --- steam --------------------------------------------------------------------------------------------------

    private static int count(TestPlayer p, Item item) {
        int n = 0;
        for (ItemStack s : p.inventory.mainInventory) {
            if (s != null && s.getItem() == item) {
                n += s.stackSize;
            }
        }
        return n;
    }

    /** Steam armor: bonuses need compressed air; the chestplate burns it and a tank refuels it. */
    private static final class SteamPower implements Scenario {

        @Override
        public String name() {
            return "armor.steam.power";
        }

        @Override
        public String description() {
            return "Steam armor: empty gives only protection (physical 20, 8 projectile loses 6.4) and HEAT cover; charged gives speed x1.2, +5 health, knockback 0.80; walking, jump and fall cost units; a Compressed Air Tank refuels.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            TestPlayer p = player(ctx, "[GGSteam]");
            ArmorEvents events = ArmorModule.events();
            try {
                SetDef steam = ArmorCatalog.STEAM;
                p.wearAll(steam);
                events.update(p);
                out.check(near(1.0, speedFactor(p)), "no air: no speed bonus, got " + speedFactor(p));
                out.check(near(0.0, knockback(p)), "no air: no knockback resistance");
                out.check(near(20.0, p.getMaxHealth()), "no air: no extra health, got " + p.getMaxHealth());
                out.check(units(p) == 0, "a crafted chestplate starts empty");
                out.checkEquals("vanilla armor bar 5+6+5+4", 20, p.getTotalArmorValue());
                checkHit(out, p, DamageType.PROJECTILE, 20.0, 0.0, 0.10);
                out.check(
                    HazardProtection.isWearingFullHazmatAgainst(p, Hazard.HEAT),
                    "heat insulation does not need air");

                ItemGregGunsArmor chestItem = (ItemGregGunsArmor) p.worn(ArmorSlot.CHESTPLATE)
                    .getItem();
                ItemStack chest = p.worn(ArmorSlot.CHESTPLATE);
                chestItem.setPowerUnits(chest, 3600);
                events.update(p);
                out.check(near(1.2, speedFactor(p)), "charged: speed x1.2, got " + speedFactor(p));
                out.check(near(0.80, knockback(p)), "charged: knockback 0.80, got " + knockback(p));
                out.check(near(25.0, p.getMaxHealth()), "charged: +5 health, got " + p.getMaxHealth());
                out.check(near(0.24, Armor.bonus(p, ArmorBonus.JUMP)), "charged: jump 0.03 x3 + 0.15");
                out.check(near(0.20, Armor.bonus(p, ArmorBonus.BREAK_SPEED)), "charged: mining +0.20");
                out.check(Armor.bonus(p, ArmorBonus.STEP_ASSIST) > 0.0, "charged: step assist from the boots");
                BreakSpeed dig = new BreakSpeed(p, Blocks.stone, 0, 4.0F, 0, 0, 0);
                MinecraftForge.EVENT_BUS.post(dig);
                out.check(near(4.0 * 1.2, dig.newSpeed), "charged: mining x1.2, got " + dig.newSpeed);
                checkHit(out, p, DamageType.ENERGY, 16.0, 0.0, 0.10);

                // Costs.
                ArmorPower.tick(p, true, 40L);
                out.check(units(p) == 3596, "walking: 4 pieces cost 4 units, got " + units(p));
                MinecraftForge.EVENT_BUS.post(new LivingJumpEvent(p));
                out.check(units(p) == 3591, "a jump costs 5 units, got " + units(p));
                MinecraftForge.EVENT_BUS.post(new LivingFallEvent(p, 10.0F));
                out.check(units(p) == 3586, "a long fall costs 5 units, got " + units(p));
                MinecraftForge.EVENT_BUS.post(new LivingFallEvent(p, 3.0F));
                out.check(units(p) == 3586, "a fall of 3 blocks is free, got " + units(p));

                // Empty: the bonuses end, the protection stays, a tank refuels.
                chestItem.setPowerUnits(chest, 0);
                events.update(p);
                out.check(near(1.0, speedFactor(p)), "empty again: speed x1.0, got " + speedFactor(p));
                out.check(near(20.0, p.getMaxHealth()), "empty again: health back to 20, got " + p.getMaxHealth());
                Item tank = ArmorParts.tankAir();
                p.inventory.mainInventory[5] = new ItemStack(tank);
                events.update(p);
                out.check(units(p) == 3600, "a tank refuels the chestplate, got " + units(p));
                out.checkEquals("the tank is used up", 0, count(p, tank));
                Item cell = ItemList.Cell_Empty.get(1)
                    .getItem();
                out.check(count(p, cell) >= 1, "an empty cell comes back");
                out.check(near(1.2, speedFactor(p)), "refueled: speed x1.2, got " + speedFactor(p));

                // Broken chestplate: no power.
                chest.setItemDamage(chest.getMaxDamage() - 1);
                events.update(p);
                out.check(near(1.0, speedFactor(p)), "a worn-out chestplate switches the set off");
                chest.setItemDamage(0);
                checkUnequipped(out, p, events);
                out.check(near(20.0, p.getMaxHealth()), "unequipped: health back to 20, got " + p.getMaxHealth());
            } finally {
                finish(p);
            }
            return out.build();
        }
    }

    // --- recipes ------------------------------------------------------------------------------------------------

    private static long eut(Tier tier) {
        switch (tier) {
            case MV:
                return TierEU.RECIPE_MV;
            case EV:
                return TierEU.RECIPE_EV;
            default:
                throw new IllegalArgumentException(tier.toString());
        }
    }

    /** Every row of a group is on GT's Assembler at its tier voltage with the sheet's duration. */
    private abstract static class Recipes implements Scenario {

        private final String name;
        private final String description;

        Recipes(String name, String description) {
            this.name = name;
            this.description = description;
        }

        abstract List<ProductionTable.Row> rows();

        @Override
        public String name() {
            return name;
        }

        @Override
        public String description() {
            return description;
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            for (ProductionTable.Row row : rows()) {
                Item made = GameRegistry.findItem(GregGuns.MODID, row.output());
                boolean found = false;
                for (GTRecipe recipe : RecipeMaps.assemblerRecipes.getAllRecipes()) {
                    ItemStack first = recipe.mOutputs == null || recipe.mOutputs.length == 0 ? null
                        : recipe.mOutputs[0];
                    if (made != null && first != null && first.getItem() == made) {
                        found = true;
                        out.checkEquals(row.output() + " EU/t", (int) eut(row.tier()), recipe.mEUt);
                        out.checkEquals(
                            row.output() + " ticks",
                            row.seconds() * GTRecipeBuilder.SECONDS,
                            recipe.mDuration);
                        for (ProductionTable.Input in : row.inputs()) {
                            if (in.anyDamage()) {
                                Item base = GameRegistry.findItem(GregGuns.MODID, in.itemName());
                                boolean wildcard = false;
                                for (ItemStack s : recipe.mInputs) {
                                    wildcard |= s != null && s.getItem() == base
                                        && s.getItemDamage() == net.minecraftforge.oredict.OreDictionary.WILDCARD_VALUE;
                                }
                                out.check(wildcard, row.output() + ": " + in.itemName() + " must match at any damage");
                            }
                        }
                    }
                }
                out.check(found, row.id() + ": no Assembler recipe makes " + row.output());
            }
            return out.build();
        }
    }
}
