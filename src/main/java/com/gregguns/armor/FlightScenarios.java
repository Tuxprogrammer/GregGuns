package com.gregguns.armor;

import java.util.UUID;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;

import com.gregguns.GregGuns;
import com.gregguns.api.Tier;
import com.gregguns.armor.flight.Flight;
import com.gregguns.armor.flight.FlightItems;
import com.gregguns.armor.flight.ItemAntiGravityDevice;
import com.gregguns.armor.flight.logic.BackKind;
import com.gregguns.armor.flight.logic.FlightPhysics;
import com.gregguns.armor.flight.logic.FlightStatus;
import com.gregguns.armor.slots.BaublesCompat;
import com.gregguns.armor.slots.ElectricGear;
import com.gregguns.armor.slots.SlotEquipment;
import com.gregguns.armor.slots.logic.BaubleSlot;
import com.gregguns.core.config.FlightConfig;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.recipe.FlightTable;
import com.mojang.authlib.GameProfile;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.TierEU;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTRecipe;

/**
 * The {@code armor.flight.*} in-game scenarios: a fake player wears each back device in the Baubles {@code wings} slot,
 * ticks are simulated (the back-device tick, then vanilla gravity), and the motion cap, the EU drain, the fall
 * assistance and flight-ability safety are asserted. Real flight feel needs a client; see implementation-notes.
 * Dev environment only; server side.
 */
// Spec: docs/gregguns-spec/05-armor/powered-flight.md (GGS-05-14, GGS-05-17 to GGS-05-19)
final class FlightScenarios {

    private FlightScenarios() {}

    static void registerAll(ScenarioRegistry registry) {
        registry.register(new Glider());
        registry.register(new JumpPack());
        registry.register(new AntiGravity());
        registry.register(new Recipes());
    }

    private static final class Fake extends FakePlayer {

        Fake(ScenarioContext ctx, String name) {
            super((WorldServer) ctx.world(), new GameProfile(UUID.nameUUIDFromBytes(name.getBytes()), name));
        }

        /** One vanilla tick of vertical motion after the device ran. */
        void gravity() {
            motionY = (motionY - FlightPhysics.GRAVITY) * FlightPhysics.DRAG;
        }

        void clean() {
            for (BaubleSlot s : BaubleSlot.values()) {
                BaublesCompat.clear(this, s);
            }
        }
    }

    private static ItemStack charged(Item item, Tier tier, double eu) {
        ItemStack stack = new ItemStack(item);
        ElectricGear.charge(stack, eu, tier);
        return stack;
    }

    /** The fall distance the rest of the pack leaves for a player with no device, for a fall of {@code blocks}. */
    private static float baselineFall(Fake bare, float blocks) {
        LivingFallEvent fall = new LivingFallEvent(bare, blocks);
        MinecraftForge.EVENT_BUS.post(fall);
        return fall.distance;
    }

    private static float fall(Fake p, float blocks) {
        LivingFallEvent fall = new LivingFallEvent(p, blocks);
        MinecraftForge.EVENT_BUS.post(fall);
        return fall.distance;
    }

    /** Glider: slow descent and forward glide on sneak or key, fall damage negated, nothing without them. */
    private static final class Glider implements Scenario {

        @Override
        public String name() {
            return "armor.flight.glider";
        }

        @Override
        public String description() {
            return "Glider in wings: sneaking or the held key caps fall speed near 0.18 and pushes forward and clears fall distance, with no EU; not sneaking does nothing; removal stops it; the fall is negated.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            Fake p = new Fake(ctx, "[GGFlightGlider]");
            Fake bare = new Fake(ctx, "[GGFlightGliderBare]");
            SlotEquipment slots = new SlotEquipment();
            try {
                ItemStack glider = new ItemStack(FlightItems.glider());
                out.check(BaublesCompat.equip(p, BaubleSlot.WINGS, glider), "glider must fit the wings slot");
                p.rotationYaw = 0.0F;
                p.rotationPitch = 0.0F;
                p.onGround = false;

                // Neither sneaking nor key: no glide.
                p.motionY = -1.5;
                p.fallDistance = 10.0F;
                slots.tick(p);
                out.check(Math.abs(p.motionY + 1.5) < 1e-9, "no sneak and no key: motion untouched, got " + p.motionY);
                out.check(p.fallDistance == 10.0F, "no sneak and no key: fall distance untouched");

                // Sneaking: descent is capped, forward speed is gained, fall distance stays cleared.
                p.setSneaking(true);
                p.motionX = 0.0;
                p.motionZ = 0.0;
                p.motionY = -0.6;
                for (int i = 0; i < 120; i++) {
                    p.fallDistance = 10.0F;
                    slots.tick(p);
                    out.check(p.fallDistance == 0.0F, "tick " + i + ": fall distance must be cleared while gliding");
                    p.gravity();
                }
                out.check(p.motionY > -0.2, "sneak glide must cap the descent near 0.18, got " + p.motionY);
                out.check(p.motionZ > 0.2, "glide must push forward along the look direction, got " + p.motionZ);
                out.check(
                    Flight.server()
                        .lastPublished(p)
                        .equals(new FlightStatus(BackKind.GLIDER, false, true)),
                    "status sent to the client: glider, key up");
                float baseline = baselineFall(bare, 20.0F);
                out.check(fall(p, 20.0F) == 0.0F, "the fall after a glide must be negated");
                out.check(baseline > 10.0F, "a bare player keeps the fall (baseline " + baseline + ")");

                // Key held, not sneaking: the same glide.
                p.setSneaking(false);
                SlotEquipment.backKey(p, true);
                p.motionY = -0.6;
                for (int i = 0; i < 120; i++) {
                    slots.tick(p);
                    p.gravity();
                }
                out.check(p.motionY > -0.2, "key glide must cap the descent, got " + p.motionY);
                out.check(
                    Flight.server()
                        .lastPublished(p)
                        .engaged(),
                    "status sent: key held");
                SlotEquipment.backKey(p, false);

                // On the ground: nothing.
                p.setSneaking(true);
                p.onGround = true;
                p.motionY = -1.5;
                slots.tick(p);
                out.check(Math.abs(p.motionY + 1.5) < 1e-9, "on the ground: no glide");
                p.onGround = false;

                // Another mod's flight ability is never touched, and a flying player is left alone.
                p.capabilities.allowFlying = true;
                p.capabilities.isFlying = true;
                p.motionY = -1.5;
                slots.tick(p);
                out.check(Math.abs(p.motionY + 1.5) < 1e-9, "flying through another mod: the glider defers");
                out.check(p.capabilities.allowFlying && p.capabilities.isFlying, "the glider never writes flight");
                p.capabilities.allowFlying = false;
                p.capabilities.isFlying = false;

                // Removal: no more glide, the client is told.
                BaublesCompat.clear(p, BaubleSlot.WINGS);
                p.motionY = -1.5;
                slots.tick(p);
                out.check(Math.abs(p.motionY + 1.5) < 1e-9, "removed: no glide");
                out.check(
                    Flight.server()
                        .lastPublished(p) == null,
                    "removed: tracker forgotten");
            } finally {
                slots.forget(p);
                p.clean();
                bare.clean();
            }
            return out.build();
        }
    }

    /** Jump Pack: boost per paid jump, no boost empty, fall absorption only when it pays, exact-tier electric item. */
    private static final class JumpPack implements Scenario {

        @Override
        public String name() {
            return "armor.flight.jump_pack";
        }

        @Override
        public String description() {
            return "Jump Pack in wings: a jump adds 0.8 and costs the configured EU; an empty pack adds nothing; a long fall is absorbed for EU only when it can pay; getTier is HV with an HV transfer limit.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            Fake p = new Fake(ctx, "[GGFlightJump]");
            Fake bare = new Fake(ctx, "[GGFlightJumpBare]");
            SlotEquipment slots = new SlotEquipment();
            try {
                ItemStack pack = charged(FlightItems.jumpPack(), Tier.HV, 20000);
                ic2.api.item.IElectricItem electric = FlightItems.jumpPack();
                out.checkEquals("IC2 tier is the HV voltage index", Tier.HV.voltageIndex(), electric.getTier(pack));
                out.check(
                    electric.getTransferLimit(pack) == GTValues.V[Tier.HV.voltageIndex()],
                    "transfer limit must be GTValues.V[HV]");
                out.check(BaublesCompat.equip(p, BaubleSlot.WINGS, pack), "pack must fit the wings slot");
                slots.tick(p);
                out.check(
                    Flight.server()
                        .lastPublished(p)
                        .equals(new FlightStatus(BackKind.JUMP_PACK, false, true)),
                    "charged pack reports powered");

                double before = ElectricGear.charge(pack);
                p.motionY = 0.42;
                MinecraftForge.EVENT_BUS.post(new LivingJumpEvent(p));
                out.check(
                    Math.abs(p.motionY - (0.42 + FlightPhysics.JUMP_BOOST)) < 1e-9,
                    "jump must gain +0.8, got " + p.motionY);
                out.check(
                    Math.abs(before - ElectricGear.charge(pack) - FlightConfig.jumpPackJumpCostEu) < 1e-6,
                    "a jump must cost " + FlightConfig.jumpPackJumpCostEu + " EU");

                // A fall of 12: the pack removes 8 blocks free and 20% of the rest, for one fall cost.
                float baseline = baselineFall(bare, 12.0F);
                before = ElectricGear.charge(pack);
                float absorbed = fall(p, 12.0F);
                float expected = (float) ((baseline - 8.0) * 0.8);
                out.check(
                    Math.abs(absorbed - expected) < 1e-3,
                    "12 block fall: expected " + expected + " (baseline " + baseline + "), got " + absorbed);
                out.check(
                    Math.abs(before - ElectricGear.charge(pack) - FlightConfig.jumpPackFallCostEu) < 1e-6,
                    "absorbing a fall must cost " + FlightConfig.jumpPackFallCostEu + " EU");

                // A fall of 3 is harmless and free.
                before = ElectricGear.charge(pack);
                float small = fall(p, 3.0F);
                out.check(small <= 3.0F, "short fall untouched, got " + small);
                out.check(ElectricGear.charge(pack) == before, "a short fall must cost nothing");

                // Empty: no boost, no absorption (GGS-05-19).
                BaublesCompat.clear(p, BaubleSlot.WINGS);
                ItemStack empty = new ItemStack(FlightItems.jumpPack());
                BaublesCompat.equip(p, BaubleSlot.WINGS, empty);
                slots.tick(p);
                out.check(
                    Flight.server()
                        .lastPublished(p)
                        .equals(new FlightStatus(BackKind.JUMP_PACK, false, false)),
                    "empty pack reports unpowered");
                p.motionY = 0.42;
                MinecraftForge.EVENT_BUS.post(new LivingJumpEvent(p));
                out.check(Math.abs(p.motionY - 0.42) < 1e-9, "an empty pack must not boost, got " + p.motionY);
                float emptyFall = fall(p, 12.0F);
                out.check(
                    Math.abs(emptyFall - baseline) < 1e-3,
                    "an empty pack must not absorb: expected " + baseline + ", got " + emptyFall);

                // Flying through another mod is never altered by the pack.
                p.capabilities.allowFlying = true;
                slots.tick(p);
                out.check(p.capabilities.allowFlying, "the pack never clears another mod's flight");
                p.capabilities.allowFlying = false;
            } finally {
                slots.forget(p);
                p.clean();
                bare.clean();
            }
            return out.build();
        }
    }

    /** Anti Gravity Device: hover, per-tick drain, empty and removal stop it, other flight is never touched. */
    private static final class AntiGravity implements Scenario {

        @Override
        public String name() {
            return "armor.flight.anti_gravity";
        }

        @Override
        public String description() {
            return "Anti Gravity Device in wings: switched on and airborne it holds fall speed near zero and drains exactly the configured EU per tick; empty, removal and foreign flight stop or defer it; it never writes the flight ability; the fall is cancelled only after powered flight.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            Fake p = new Fake(ctx, "[GGFlightAntiGrav]");
            Fake bare = new Fake(ctx, "[GGFlightAntiGravBare]");
            SlotEquipment slots = new SlotEquipment();
            int drive = FlightConfig.antiGravityDriveEuPerTick;
            try {
                ItemStack device = charged(FlightItems.antiGravityDevice(), Tier.IV, drive * 50.0);
                ic2.api.item.IElectricItem electric = FlightItems.antiGravityDevice();
                out.checkEquals("IC2 tier is the IV voltage index", Tier.IV.voltageIndex(), electric.getTier(device));
                out.check(
                    electric.getTransferLimit(device) == GTValues.V[Tier.IV.voltageIndex()],
                    "transfer limit must be GTValues.V[IV]");
                out.check(BaublesCompat.equip(p, BaubleSlot.WINGS, device), "device must fit the wings slot");
                p.onGround = false;

                // Another mod grants flight; ours must leave that alone through the whole run.
                p.capabilities.allowFlying = true;
                p.capabilities.isFlying = false;

                // Switched off: nothing happens and nothing is spent.
                p.motionY = -1.0;
                double start = ElectricGear.charge(device);
                slots.tick(p);
                out.check(Math.abs(p.motionY + 1.0) < 1e-9, "switched off: motion untouched");
                out.check(ElectricGear.charge(device) == start, "switched off: no drain");

                // Switch on with the key, then fly 10 ticks.
                SlotEquipment.backKey(p, true);
                SlotEquipment.backKey(p, false);
                out.check(ItemAntiGravityDevice.engaged(device), "the key must switch the device on");
                p.motionY = -1.0;
                for (int i = 0; i < 10; i++) {
                    p.fallDistance = 15.0F;
                    slots.tick(p);
                    out.check(p.fallDistance == 0.0F, "tick " + i + ": flying clears the fall distance");
                    p.gravity();
                }
                out.check(Math.abs(p.motionY) < 0.1, "hover must hold the fall speed near zero, got " + p.motionY);
                out.check(
                    Math.abs(start - ElectricGear.charge(device) - 10.0 * drive) < 1e-6,
                    "10 flying ticks must drain exactly " + 10 * drive
                        + " EU, drained "
                        + (start - ElectricGear.charge(device)));
                out.check(
                    p.capabilities.allowFlying && !p.capabilities.isFlying,
                    "GGS-05-17: another mod's allowFlying must be untouched, and isFlying never set");
                out.check(
                    Flight.server()
                        .lastPublished(p)
                        .equals(new FlightStatus(BackKind.ANTI_GRAVITY, true, true)),
                    "status sent to the client: engaged and powered");

                // Fall after powered flight is cancelled; a bare player keeps it.
                out.check(fall(p, 20.0F) == 0.0F, "the fall right after powered flight is cancelled");
                out.check(baselineFall(bare, 20.0F) > 10.0F, "a bare player keeps the fall");

                // Foreign flight (isFlying): the device defers and spends nothing.
                p.capabilities.isFlying = true;
                double c = ElectricGear.charge(device);
                p.motionY = -1.0;
                slots.tick(p);
                out.check(Math.abs(p.motionY + 1.0) < 1e-9, "foreign flight: device defers");
                out.check(ElectricGear.charge(device) == c, "foreign flight: no drain");
                out.check(p.capabilities.isFlying, "foreign flight: isFlying untouched");
                p.capabilities.isFlying = false;

                // On the ground: no drain.
                p.onGround = true;
                slots.tick(p);
                out.check(ElectricGear.charge(device) == c, "grounded: no drain");
                p.onGround = false;

                // Run it dry: it stops, switches itself off and reports unpowered.
                for (int i = 0; i < 80; i++) {
                    slots.tick(p);
                }
                out.check(ElectricGear.charge(device) < drive, "the device must run dry");
                out.check(!ItemAntiGravityDevice.engaged(device), "an empty device switches itself off");
                p.motionY = -1.0;
                slots.tick(p);
                out.check(Math.abs(p.motionY + 1.0) < 1e-9, "empty: no hover");
                out.check(
                    Flight.server()
                        .lastPublished(p)
                        .equals(new FlightStatus(BackKind.ANTI_GRAVITY, false, false)),
                    "empty device reports unpowered");

                // Refill, switch on, then take it off: flight ends at once.
                ElectricGear.charge(device, drive * 20.0, Tier.IV);
                SlotEquipment.backKey(p, true);
                slots.tick(p);
                out.check(ElectricGear.charge(device) < drive * 20.0, "refilled device flies again");
                BaublesCompat.clear(p, BaubleSlot.WINGS);
                double afterRemoval = ElectricGear.charge(device);
                p.motionY = -1.0;
                slots.tick(p);
                out.check(Math.abs(p.motionY + 1.0) < 1e-9, "removed: no hover");
                out.check(ElectricGear.charge(device) == afterRemoval, "removed: no drain");
                out.check(
                    p.capabilities.allowFlying && !p.capabilities.isFlying,
                    "removal: another mod's flight still untouched");
                out.check(
                    Flight.server()
                        .lastPublished(p) == null,
                    "removed: tracker forgotten");

                // Camos (CamoChangeable).
                ItemAntiGravityDevice item = FlightItems.antiGravityDevice();
                ItemStack fresh = new ItemStack(item);
                out.checkEquals("camo count", ItemAntiGravityDevice.CAMOS, item.camoCount(fresh));
                item.setCamo(fresh, 3);
                out.checkEquals("camo stored", 3, item.getCamo(fresh));
                item.setCamo(fresh, 99);
                out.checkEquals("out-of-range camo stores 0", 0, item.getCamo(fresh));
            } finally {
                slots.forget(p);
                p.capabilities.allowFlying = false;
                p.clean();
                bare.clean();
            }
            return out.build();
        }
    }

    /** The flight rows have Assembler recipes at their tier voltage. */
    private static final class Recipes implements Scenario {

        @Override
        public String name() {
            return "armor.flight.recipes";
        }

        @Override
        public String description() {
            return "Glider Backpack, Glider Wing, Glider and Jump Pack have Assembler recipes at RECIPE_HV; the Anti Gravity Device has one at RECIPE_IV once part_mechanical_3 is registered.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            for (com.gregguns.recipe.ProductionTable.Row row : FlightTable.ROWS) {
                Item made = GameRegistry.findItem(GregGuns.MODID, row.output());
                boolean deferred = false;
                for (com.gregguns.recipe.ProductionTable.Input in : row.inputs()) {
                    if (in.isGregGuns() && GameRegistry.findItem(GregGuns.MODID, in.itemName()) == null) {
                        deferred = true;
                    }
                }
                boolean found = false;
                for (GTRecipe recipe : RecipeMaps.assemblerRecipes.getAllRecipes()) {
                    ItemStack first = recipe.mOutputs == null || recipe.mOutputs.length == 0 ? null
                        : recipe.mOutputs[0];
                    if (first != null && first.getItem() == made) {
                        found = true;
                        out.checkEquals(
                            row.id() + " EU/t",
                            row.tier() == Tier.IV ? (int) TierEU.RECIPE_IV : (int) TierEU.RECIPE_HV,
                            recipe.mEUt);
                    }
                }
                if (deferred) {
                    out.check(!found, row.id() + ": a deferred recipe must not exist");
                } else {
                    out.check(found, row.id() + ": assembler recipe for " + row.output() + " missing");
                }
            }
            return out.build();
        }
    }
}
