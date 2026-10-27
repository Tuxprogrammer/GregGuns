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

    private static FakePlayer player(ScenarioContext ctx, String name) {
        return new FakePlayer(
            (WorldServer) ctx.world(),
            new GameProfile(UUID.nameUUIDFromBytes(name.getBytes()), name));
    }

    private static void sicken(FakePlayer p) {
        p.addPotionEffect(new PotionEffect(Potion.poison.id, 600, 1));
        p.addPotionEffect(new PotionEffect(Potion.moveSlowdown.id, 600, 1));
        p.addPotionEffect(new PotionEffect(Potion.confusion.id, 600, 0));
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
            out.check(dosed.isPotionActive(Potion.poison), "the test player should start poisoned");
            ItemStack left = AntidoteRegistration.item()
                .onEaten(stack, ctx.world(), dosed);
            out.checkEquals("antidotes left", 1, left.stackSize);
            out.check(
                !dosed.isPotionActive(Potion.poison) && !dosed.isPotionActive(Potion.moveSlowdown)
                    && !dosed.isPotionActive(Potion.confusion),
                "a dose should clear the ailments at once");
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
                out.check(other.isPotionActive(Potion.poison), "an undosed player keeps the poison");
                return out.build();
            });
        }
    }
}
