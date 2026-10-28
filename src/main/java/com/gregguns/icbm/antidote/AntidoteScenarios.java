package com.gregguns.icbm.antidote;

import java.util.UUID;

import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingEvent;

import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.mojang.authlib.GameProfile;

/** Dev-only scenario of the antidote (D151). */
// Spec: docs/gregguns-icbm-spec/06-blocks-items/antidote.md
final class AntidoteScenarios {

    private AntidoteScenarios() {}

    static void registerAll(ScenarioRegistry registry) {
        registry.register(new Dose());
    }

    /** A fake player has no network handler, which vanilla's potion callbacks reach for. */
    private static final class Patient extends FakePlayer {

        Patient(WorldServer world, GameProfile profile) {
            super(world, profile);
        }

        @Override
        protected void onNewPotionEffect(PotionEffect effect) {}

        @Override
        protected void onChangedPotionEffect(PotionEffect effect, boolean added) {}

        @Override
        protected void onFinishedPotionEffect(PotionEffect effect) {}
    }

    private static FakePlayer player(ScenarioContext ctx, String name) {
        return new Patient((WorldServer) ctx.world(), new GameProfile(UUID.nameUUIDFromBytes(name.getBytes()), name));
    }

    private static void sicken(FakePlayer p) {
        p.addPotionEffect(new PotionEffect(Potion.poison.id, 600, 1));
        p.addPotionEffect(new PotionEffect(Potion.moveSlowdown.id, 600, 1));
        p.addPotionEffect(new PotionEffect(Potion.confusion.id, 600, 0));
        // GT's own radioactivity (slowness, fatigue, nausea, weakness, hunger and IC2 radiation, which does the
        // damage).
        gregtech.api.util.GTUtility.applyRadioactivity(p, 2, 1);
    }

    private static boolean radiated(FakePlayer p) {
        return ic2.core.IC2Potion.radiation != null && p.isPotionActive(ic2.core.IC2Potion.radiation);
    }

    /** A dose clears the ailments, the guard clears them again for three minutes, and only for the dosed player. */
    private static final class Dose implements Scenario {

        @Override
        public String name() {
            return "icbm.antidote.dose";
        }

        @Override
        public String description() {
            return "Using the antidote cures poison, slowness and nausea at once, keeps clearing them while the protection lasts, spends one item, and leaves an undosed player sick.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            FakePlayer dosed = player(ctx, "gregguns-antidote-a");
            FakePlayer other = player(ctx, "gregguns-antidote-b");
            ItemStack stack = new ItemStack(AntidoteRegistration.item(), 2);
            sicken(dosed);
            sicken(other);
            // A beneficial effect must survive the dose and the guard.
            dosed.addPotionEffect(new PotionEffect(Potion.regeneration.id, 6000, 0));
            out.check(dosed.isPotionActive(Potion.poison), "the test player should start poisoned");
            out.check(radiated(dosed), "GT radioactivity should have given the test player IC2 radiation");
            ItemStack left = AntidoteRegistration.item()
                .onEaten(stack, ctx.world(), dosed);
            out.checkEquals("antidotes left", 1, left.stackSize);
            out.check(
                !dosed.isPotionActive(Potion.poison) && !dosed.isPotionActive(Potion.moveSlowdown)
                    && !dosed.isPotionActive(Potion.confusion),
                "a dose should clear the ailments at once");
            out.check(!radiated(dosed), "a dose should clear IC2 radiation, the radiation damage");
            out.check(dosed.isPotionActive(Potion.regeneration), "a dose must not clear a beneficial effect");
            out.check(
                AntidoteGuard.isProtected(
                    dosed,
                    ctx.world()
                        .getTotalWorldTime()),
                "the dose should protect");
            out.check(
                !AntidoteGuard.isProtected(
                    other,
                    ctx.world()
                        .getTotalWorldTime()),
                "the other player is not protected");
            sicken(dosed);
            return ScenarioResult.pending(name(), 24, ticks -> {
                MinecraftForge.EVENT_BUS.post(new LivingEvent.LivingUpdateEvent(dosed));
                MinecraftForge.EVENT_BUS.post(new LivingEvent.LivingUpdateEvent(other));
                if (ticks < 12) {
                    return null;
                }
                out.check(
                    !dosed.isPotionActive(Potion.poison),
                    "the guard should clear a new poisoning within ten ticks");
                out.check(!radiated(dosed), "the guard should clear new radiation within ten ticks");
                out.check(dosed.isPotionActive(Potion.regeneration), "the guard must not clear a beneficial effect");
                out.check(other.isPotionActive(Potion.poison), "an undosed player keeps the poison");
                out.check(radiated(other), "an undosed player keeps the radiation");
                return out.build();
            });
        }
    }
}
