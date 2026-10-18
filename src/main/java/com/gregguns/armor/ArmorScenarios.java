package com.gregguns.armor;

import java.util.Collections;
import java.util.Locale;
import java.util.UUID;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.BreakSpeed;

import com.gregguns.api.Tier;
import com.gregguns.armor.logic.ArmorBonus;
import com.gregguns.armor.logic.ArmorCatalog;
import com.gregguns.armor.logic.ArmorMaterialDef;
import com.gregguns.armor.logic.ArmorSlot;
import com.gregguns.armor.logic.BonusMap;
import com.gregguns.armor.logic.FallMath;
import com.gregguns.armor.logic.HazardFlag;
import com.gregguns.armor.logic.NightVisionPolicy;
import com.gregguns.armor.logic.PieceDef;
import com.gregguns.armor.logic.SetDef;
import com.gregguns.combat.DamageSources;
import com.gregguns.combat.DamageType;
import com.gregguns.combat.GregTechHazmat;
import com.gregguns.combat.HazardKind;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.mojang.authlib.GameProfile;

import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent;
import gregtech.api.hazards.Hazard;
import gregtech.api.hazards.HazardProtection;

/**
 * The {@code armor.*} in-game scenarios: a fake player wears real armor items and the bonus, protection, lifecycle and
 * compatibility rules are checked against the numbers in the spec tables. Dev environment only. Server side.
 */
// Spec: docs/techguns-spec/05-armor/armor-catalog.md; docs/techguns-spec/05-armor/set-bonuses.md;
// docs/testing/README.md
final class ArmorScenarios {

    private static final float EPS = 0.01F;

    private ArmorScenarios() {}

    static void registerAll(ScenarioRegistry registry) {
        registry.register(new Equip());
        registry.register(new Damage());
        registry.register(new Lifecycle());
        registry.register(new Compat());
        registry.register(new Hazmat());
        registry.register(new Recipes());
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

        void wear(SetDef set, ArmorSlot... slots) {
            for (ArmorSlot slot : slots) {
                // armorInventory[0] is the boots, [3] the helmet.
                inventory.armorInventory[3 - slot.armorType()] = new ItemStack(ArmorItems.get(set, slot));
            }
        }

        void wearAll(SetDef set) {
            wear(
                set,
                set.pieces()
                    .keySet()
                    .toArray(new ArmorSlot[0]));
        }

        void undress() {
            for (int i = 0; i < 4; i++) {
                inventory.armorInventory[i] = null;
            }
        }
    }

    private static TestPlayer player(ScenarioContext ctx, String name) {
        return new TestPlayer((WorldServer) ctx.world(), name);
    }

    private static boolean near(double expected, double actual) {
        return Math.abs(expected - actual) < EPS;
    }

    private static double speedFactor(EntityPlayer p) {
        IAttributeInstance a = p.getEntityAttribute(SharedMonsterAttributes.movementSpeed);
        return a.getAttributeValue() / a.getBaseValue();
    }

    /** Equip and unequip a full set: bonuses and full-set detection follow the worn pieces. */
    private static final class Equip implements Scenario {

        @Override
        public String name() {
            return "armor.equip";
        }

        @Override
        public String description() {
            return "Full Scout set: +50% speed attribute and full-set detected; boots off: +37.5% and not full; all off: no modifier.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            TestPlayer p = player(ctx, "[GGArmorEquip]");
            ArmorEvents events = ArmorModule.events();
            try {
                p.wearAll(ArmorCatalog.T1_SCOUT);
                out.check(
                    Armor.isWearingFullSet(p, ArmorCatalog.T1_SCOUT),
                    "four Scout pieces should be a complete set");
                events.update(p);
                out.check(near(1.5, speedFactor(p)), "full Scout: expected speed x1.5, got " + speedFactor(p));
                out.check(
                    near(0.16, Armor.bonus(p, ArmorBonus.JUMP)),
                    "full Scout jump bonus 0.02*3 + 0.10, got " + Armor.bonus(p, ArmorBonus.JUMP));
                out.checkEquals("vanilla armor bar", 13, p.getTotalArmorValue());

                p.inventory.armorInventory[0] = null;
                events.update(p);
                out.check(!Armor.isWearingFullSet(p, ArmorCatalog.T1_SCOUT), "three pieces are not a full set");
                out.check(near(1.375, speedFactor(p)), "no boots: expected x1.375, got " + speedFactor(p));

                p.undress();
                events.update(p);
                out.check(near(1.0, speedFactor(p)), "naked: expected x1.0, got " + speedFactor(p));
                out.check(
                    events.applied(p)
                        .isEmpty(),
                    "nothing should be recorded as applied");

                // Sprinting doubles the speed bonus (Techguns rule), and walking again takes it back.
                p.wearAll(ArmorCatalog.T1_SCOUT);
                p.setSprinting(true);
                events.update(p);
                out.check(
                    near(2.0 * 1.3, speedFactor(p)),
                    "sprinting: expected (1+2*0.5) x vanilla's 1.3 sprint modifier = 2.6, got " + speedFactor(p));
                p.setSprinting(false);
                events.update(p);
                out.check(near(1.5, speedFactor(p)), "walking again: expected x1.5, got " + speedFactor(p));

                // Knockback resistance from the Combat set.
                p.undress();
                p.wearAll(ArmorCatalog.T1_COMBAT);
                events.update(p);
                out.check(
                    near(
                        0.40,
                        p.getEntityAttribute(SharedMonsterAttributes.knockbackResistance)
                            .getAttributeValue()),
                    "full Combat knockback resistance should be 0.05+0.20+0.10+0.05");
            } finally {
                p.undress();
                events.strip(p);
            }
            return out.build();
        }
    }

    /** Typed damage through the real damage handler and Forge's ApplyArmor. */
    private static final class Damage implements Scenario {

        @Override
        public String name() {
            return "armor.damage";
        }

        @Override
        public String description() {
            return "Full Combat set: 8 projectile damage loses 3.2, 8 energy loses 4.4, poison 8, 15% penetration 3.92; each piece wears once and never breaks.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            TestPlayer p = player(ctx, "[GGArmorDamage]");
            try {
                p.wearAll(ArmorCatalog.T1_COMBAT);
                out.checkEquals("vanilla armor bar 4+5+4+3", 16, p.getTotalArmorValue());
                out.check(
                    near(15.0, Armor.armorValue(p, DamageType.PROJECTILE)),
                    "Armor.armorValue projectile should be 15");
                out.check(
                    near(11.25, Armor.armorValue(p, DamageType.ENERGY)),
                    "Armor.armorValue energy should be 11.25");

                float projectile = p.hurt(DamageSources.direct(DamageType.PROJECTILE, null, 0.0), 8.0F);
                out.check(near(3.2, projectile), "projectile 8 at 15 armor: expected 3.2, got " + projectile);
                for (int i = 0; i < 4; i++) {
                    ItemStack piece = p.inventory.armorInventory[i];
                    out.check(
                        piece != null && piece.getItemDamage() == 1,
                        "slot " + i
                            + " should be worn exactly once, damage="
                            + (piece == null ? "gone" : piece.getItemDamage()));
                }
                float energy = p.hurt(DamageSources.direct(DamageType.ENERGY, null, 0.0), 8.0F);
                out.check(near(4.4, energy), "energy 8 at 11.25 armor: expected 4.4, got " + energy);
                float poison = p.hurt(
                    DamageSources.direct(DamageType.POISON, null, 0.0)
                        .withoutSecondaryEffects(),
                    8.0F);
                out.check(near(8.0, poison), "poison 8 at 0 armor: expected 8.0, got " + poison);
                float pen = p.hurt(DamageSources.direct(DamageType.PROJECTILE, null, 0.15), 8.0F);
                out.check(near(3.92, pen), "projectile 8 at 15% penetration: expected 3.92, got " + pen);
                float unresistable = p.hurt(DamageSources.direct(DamageType.UNRESISTABLE, null, 0.0), 8.0F);
                out.check(near(8.0, unresistable), "unresistable 8: expected 8.0, got " + unresistable);
                float fall = p.hurt(DamageSource.fall, 8.0F);
                out.check(near(8.0, fall), "fall damage bypasses armor: expected 8.0, got " + fall);

                // A plain blockable vanilla source uses the physical value (DamageSource.generic would bypass armor).
                float melee = p.hurt(new DamageSource("gregguns.test_melee"), 8.0F);
                out.check(near(8.0 * 10.0 / 25.0, melee), "generic 8 at 15 physical: expected 3.2, got " + melee);

                // Broken piece: one durability point left gives nothing; wear never takes the last point.
                int max = p.inventory.armorInventory[0].getMaxDamage();
                p.undress();
                p.wearAll(ArmorCatalog.T1_COMBAT);
                for (int i = 0; i < 4; i++) {
                    p.inventory.armorInventory[i].setItemDamage(max - 1);
                }
                out.check(near(0.0, Armor.armorValue(p, DamageType.PROJECTILE)), "all broken: no armor value");
                float broken = p.hurt(DamageSources.direct(DamageType.PROJECTILE, null, 0.0), 8.0F);
                out.check(near(8.0, broken), "broken set: expected 8.0, got " + broken);
                out.checkEquals(
                    "broken pieces stay at one point",
                    max - 1,
                    p.inventory.armorInventory[0].getItemDamage());
                p.inventory.armorInventory[0].setItemDamage(max - 3);
                for (int i = 0; i < 6; i++) {
                    p.hurt(DamageSources.direct(DamageType.PROJECTILE, null, 0.0), 8.0F);
                }
                out.check(
                    p.inventory.armorInventory[0].getItemDamage() <= max - 1,
                    "a worn piece never goes past its last point");
            } finally {
                p.undress();
                ArmorModule.events()
                    .strip(p);
            }
            return out.build();
        }
    }

    /** Bonuses end on logout, death, respawn and dimension change, and a broken piece gives none. */
    private static final class Lifecycle implements Scenario {

        @Override
        public String name() {
            return "armor.lifecycle";
        }

        @Override
        public String description() {
            return "Bonuses applied by Scout armor are removed by logout, death, respawn and dimension change events, and by a broken piece; the next tick re-applies what is still worn.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            TestPlayer p = player(ctx, "[GGArmorLifecycle]");
            ArmorEvents events = ArmorModule.events();
            try {
                p.wearAll(ArmorCatalog.T1_SCOUT);
                events.update(p);
                out.check(near(1.5, speedFactor(p)), "start: expected x1.5");

                events.onLogout(new PlayerLoggedOutEvent(p));
                out.check(near(1.0, speedFactor(p)), "after logout: expected x1.0, got " + speedFactor(p));

                events.update(p);
                out.check(near(1.5, speedFactor(p)), "next tick re-applies what is worn");
                events.onDeath(new LivingDeathEvent(p, DamageSource.generic));
                out.check(near(1.0, speedFactor(p)), "after death: expected x1.0, got " + speedFactor(p));

                events.update(p);
                events.onRespawn(new PlayerRespawnEvent(p));
                out.check(near(1.0, speedFactor(p)), "after respawn: expected x1.0, got " + speedFactor(p));

                events.update(p);
                events.onDimensionChange(new PlayerChangedDimensionEvent(p, 0, 1));
                out.check(near(1.0, speedFactor(p)), "after dimension change: expected x1.0, got " + speedFactor(p));
                events.update(p);
                out.check(near(1.5, speedFactor(p)), "after the dimension change the worn set applies again");

                // Break one piece: its bonus goes, the set is no longer complete.
                ItemStack boots = p.inventory.armorInventory[0];
                boots.setItemDamage(boots.getMaxDamage() - 1);
                events.update(p);
                out.check(near(1.375, speedFactor(p)), "broken boots: expected x1.375, got " + speedFactor(p));
                out.check(!Armor.isWearingFullSet(p, ArmorCatalog.T1_SCOUT), "a broken piece breaks the set");
            } finally {
                p.undress();
                events.strip(p);
            }
            return out.build();
        }
    }

    /** CORE-14, CORE-15 and the event bonuses (jump, fall, break speed) through the real event buses. */
    private static final class Compat implements Scenario {

        @Override
        public String name() {
            return "armor.compat";
        }

        @Override
        public String description() {
            return "Armor leaves step height and potion Night Vision alone; jump, fall and break-speed bonuses fire through the event bus.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            TestPlayer p = player(ctx, "[GGArmorCompat]");
            ArmorEvents events = ArmorModule.events();
            try {
                // CORE-14: another mod's step height survives equipping and unequipping.
                p.stepHeight = 1.25F;
                p.wearAll(ArmorCatalog.T1_SCOUT);
                events.update(p);
                out.check(p.stepHeight == 1.25F, "equip must not touch step height, got " + p.stepHeight);
                p.undress();
                events.update(p);
                out.check(p.stepHeight == 1.25F, "unequip must not reset step height, got " + p.stepHeight);

                // CORE-15: a brewed Night Vision survives armor's upkeep, on a mob (a fake player has no network
                // handler, so it cannot hold potion effects).
                EntityZombie mob = new EntityZombie(ctx.world());
                mob.addPotionEffect(new PotionEffect(Potion.nightVision.id, 6000, 0));
                events.updateNightVision(mob, false);
                events.updateNightVision(mob, true);
                events.updateNightVision(mob, false);
                PotionEffect nv = mob.getActivePotionEffect(Potion.nightVision);
                out.check(nv != null && nv.getDuration() > 5000, "a potion's Night Vision must survive armor upkeep");
                out.check(!events.ownsNightVision(mob), "armor must not claim a potion's Night Vision");
                mob.removePotionEffect(Potion.nightVision.id);
                events.updateNightVision(mob, true);
                nv = mob.getActivePotionEffect(Potion.nightVision);
                out.check(nv != null && nv.getDuration() == NightVisionPolicy.REFRESH_TICKS, "armor grants its own");
                out.check(events.ownsNightVision(mob), "armor owns the effect it granted");
                events.updateNightVision(mob, false);
                out.check(mob.getActivePotionEffect(Potion.nightVision) == null, "armor removes the effect it granted");
                out.check(!events.ownsNightVision(mob), "ownership ends with the effect");

                // Event bonuses.
                p.wearAll(ArmorCatalog.T1_SCOUT);
                p.motionY = 0.0;
                MinecraftForge.EVENT_BUS.post(new LivingJumpEvent(p));
                out.check(near(0.16, p.motionY), "Scout jump: expected +0.16, got " + p.motionY);

                p.undress();
                LivingFallEvent bare = new LivingFallEvent(p, 5.0F);
                MinecraftForge.EVENT_BUS.post(bare);
                // The pack has other fall handlers (an unarmored fall of 5 arrives as 4 here), so the armor result
                // is compared with the same pipeline's baseline instead of a hardcoded 5.
                double baseline = bare.distance;
                p.wearAll(ArmorCatalog.T1_SCOUT);
                LivingFallEvent fall = new LivingFallEvent(p, 5.0F);
                MinecraftForge.EVENT_BUS.post(fall);
                out.check(
                    near(FallMath.adjust(baseline, 0.2, 1.0), fall.distance),
                    "Scout boots: expected (baseline " + baseline + " - 1) * 0.8, got " + fall.distance);
                LivingFallEvent small = new LivingFallEvent(p, 0.8F);
                MinecraftForge.EVENT_BUS.post(small);
                out.check(small.distance == 0.0F, "a fall inside the free height counts as zero");

                p.undress();
                p.wearAll(ArmorCatalog.T1_MINER);
                BreakSpeed dig = new BreakSpeed(p, Blocks.stone, 0, 4.0F, 0, 0, 0);
                MinecraftForge.EVENT_BUS.post(dig);
                out.check(near(4.0 * 1.20, dig.newSpeed), "Miner x4 pieces: expected 4.8, got " + dig.newSpeed);
            } finally {
                p.undress();
                // No removePotionEffect here: a fake player has no network handler. The player is discarded.
                events.strip(p);
            }
            return out.build();
        }
    }

    /** Hazard flags reach GT through the existing hazmat hooks. */
    private static final class Hazmat implements Scenario {

        private SetDef syntheticSet;

        @Override
        public String name() {
            return "armor.hazmat";
        }

        @Override
        public String description() {
            return "Every catalog set is GT hazmat against exactly the hazards its pieces list (T1 none, T2 Commando ELECTRICAL, Riot RADIOLOGICAL, steam HEAT, HEV BIOLOGICAL, GAS and RADIOLOGICAL while uncharged); a test set with flags on every piece is full hazmat for those hazards only, and a single gas-mask piece covers GAS alone; no set claims SPACE.";
        }

        private SetDef synthetic() {
            if (syntheticSet == null) {
                PieceDef.Builder[] b = new PieceDef.Builder[4];
                for (ArmorSlot s : ArmorSlot.values()) {
                    b[s.ordinal()] = PieceDef.builder(s, "Test " + s.id())
                        .partialHazards(HazardFlag.HEAT, HazardFlag.RADIOLOGICAL);
                }
                b[0].fullHazards(HazardFlag.GAS);
                syntheticSet = new SetDef(
                    "test_hazmat",
                    Tier.EV,
                    ArmorMaterialDef.uniform("test_hazmat", 60, 10.0),
                    Collections.singletonList("t1_combat"),
                    BonusMap.EMPTY,
                    b[0].build(),
                    b[1].build(),
                    b[2].build(),
                    b[3].build());
            }
            return syntheticSet;
        }

        private static boolean coversAll(SetDef set, Hazard hazard) {
            for (PieceDef piece : set.pieces()
                .values()) {
                boolean covered = false;
                for (HazardFlag f : HazardFlag.values()) {
                    if (f.name()
                        .equals(hazard.name())
                        && (piece.partialHazards()
                            .contains(f)
                            || piece.fullHazards()
                                .contains(f))) {
                        covered = true;
                    }
                }
                if (!covered) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            TestPlayer p = player(ctx, "[GGArmorHazmat]");
            try {
                for (SetDef set : ArmorCatalog.ALL) {
                    p.undress();
                    p.wearAll(set);
                    for (Hazard h : Hazard.values()) {
                        // Expected: GT needs all four slots to carry the flag. Powered flags are off (nothing is
                        // charged here); armor.t3.hev and armor.steam.power check the charged state.
                        boolean want = set.pieceCount() == 4 && coversAll(set, h);
                        out.check(
                            HazardProtection.isWearingFullHazmatAgainst(p, h) == want,
                            set.id() + (want ? " must be hazmat against " : " must not be hazmat against ") + h);
                    }
                }
                SetDef test = synthetic();
                p.undress();
                for (ArmorSlot slot : ArmorSlot.values()) {
                    ItemGregGunsArmor item = new ItemGregGunsArmor(
                        test,
                        test.pieces()
                            .get(slot));
                    p.inventory.armorInventory[3 - slot.armorType()] = new ItemStack(item);
                }
                out.check(
                    GregTechHazmat.isProtected(p, HazardKind.HEAT),
                    "four pieces with HEAT are full hazmat against heat");
                out.check(
                    GregTechHazmat.isProtected(p, HazardKind.RADIOLOGICAL),
                    "four pieces with RADIOLOGICAL are full hazmat against radiation");
                out.check(
                    !GregTechHazmat.isProtected(p, HazardKind.ELECTRICAL),
                    "no piece lists ELECTRICAL, so no electrical protection (GGS-05-10)");
                out.check(
                    HazardProtection.isWearingFullHazmatAgainst(p, Hazard.GAS),
                    "the helmet covers GAS on its own (protectsAgainstFully)");
                out.check(
                    !HazardProtection.isWearingFullHazmatAgainst(p, Hazard.SPACE),
                    "GregGuns armor never claims SPACE");
                // Remove the helmet: heat now needs all four pieces, so it is no longer hazmat, and GAS is gone.
                p.inventory.armorInventory[3] = null;
                out.check(!GregTechHazmat.isProtected(p, HazardKind.HEAT), "three partial pieces are not hazmat");
                out.check(!HazardProtection.isWearingFullHazmatAgainst(p, Hazard.GAS), "no helmet, no GAS cover");
            } finally {
                p.undress();
            }
            return out.build();
        }
    }

    /** The T1 recipes exist in the crafting manager and on GT's Assembler, at the sheet's voltage and time. */
    private static final class Recipes implements Scenario {

        @Override
        public String name() {
            return "armor.recipes";
        }

        @Override
        public String description() {
            return "The 8 steam-age hand recipes and 4 LV Combat Assembler recipes exist; Heavy Cloth has its recipe.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            for (com.gregguns.recipe.ArmorTable.HandRow row : com.gregguns.recipe.ArmorTable.HAND) {
                out.check(
                    craftingHas(row.output()),
                    row.id() + ": no crafting recipe makes "
                        + row.output()
                            .toUpperCase(Locale.ROOT));
            }
            out.check(craftingHas(com.gregguns.armor.logic.ArmorCatalog.HEAVY_CLOTH), "Heavy Cloth has no recipe");
            for (com.gregguns.recipe.ProductionTable.Row row : com.gregguns.recipe.ArmorTable.COMBAT) {
                boolean found = false;
                for (gregtech.api.util.GTRecipe recipe : gregtech.api.recipe.RecipeMaps.assemblerRecipes
                    .getAllRecipes()) {
                    ItemStack made = recipe.mOutputs == null || recipe.mOutputs.length == 0 ? null : recipe.mOutputs[0];
                    if (made != null && made.getItem() == cpw.mods.fml.common.registry.GameRegistry
                        .findItem(com.gregguns.GregGuns.MODID, row.output())) {
                        found = true;
                        out.checkEquals(row.output() + " EU/t", (int) gregtech.api.enums.TierEU.RECIPE_LV, recipe.mEUt);
                        out.checkEquals(
                            row.output() + " ticks",
                            row.seconds() * gregtech.api.util.GTRecipeBuilder.SECONDS,
                            recipe.mDuration);
                    }
                }
                out.check(found, row.id() + ": assembler recipe for " + row.output() + " missing");
            }
            return out.build();
        }

        @SuppressWarnings("unchecked")
        private static boolean craftingHas(String registryName) {
            net.minecraft.item.Item item = cpw.mods.fml.common.registry.GameRegistry
                .findItem(com.gregguns.GregGuns.MODID, registryName);
            for (Object o : net.minecraft.item.crafting.CraftingManager.getInstance()
                .getRecipeList()) {
                net.minecraft.item.crafting.IRecipe recipe = (net.minecraft.item.crafting.IRecipe) o;
                ItemStack made = recipe.getRecipeOutput();
                if (made != null && made.getItem() == item) {
                    return true;
                }
            }
            return false;
        }
    }
}
