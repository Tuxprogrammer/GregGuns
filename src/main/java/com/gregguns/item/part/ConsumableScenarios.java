package com.gregguns.item.part;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ItemInWorldManager;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.PlaySoundAtEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import com.gregguns.core.testing.QuietConnection;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.mojang.authlib.GameProfile;

import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * In-game checks of Bandages and the Medkit with a real server player: the right click reaches the server the way the
 * click packet does ({@code PlayerInteractEvent} RIGHT_CLICK_AIR, then {@code tryUseItem}), and the player then ticks
 * through {@code onUpdateEntity}, the call the server makes for every connected player. Dev environment only. Server
 * side.
 *
 * <p>
 * The player is never added to the world, so it leaves nothing in the plot.
 */
// Spec: docs/techguns-spec/06-items/special-items.md (Medkit), docs/gregguns-spec/06-items/recipes.md (RC-61, RC-62)
public final class ConsumableScenarios {

    private ConsumableScenarios() {}

    /** @param registry where the scenarios go */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new Use(ComponentItems.BANDAGES));
        registry.register(new Use(ComponentItems.MEDKIT));
        registry.register(new FullHealth());
    }

    /**
     * A player the server ticks like a connected one. Vanilla 1.7.10 {@code NetHandlerPlayServer.processPlayer} calls
     * {@code onUpdateEntity}, which runs {@code EntityPlayer.onUpdate}: the item-in-use countdown, {@code onUsingTick}
     * and {@code onItemUseFinish}. A Forge {@code FakePlayer} skips all of it.
     */
    private static EntityPlayerMP patient(ScenarioContext ctx, String name) {
        WorldServer world = (WorldServer) ctx.world();
        EntityPlayerMP p = new EntityPlayerMP(
            MinecraftServer.getServer(),
            world,
            new GameProfile(UUID.nameUUIDFromBytes(("gregguns-consumable-" + name).getBytes()), "[GG" + name + "]"),
            new ItemInWorldManager(world));
        QuietConnection.attach(p);
        p.setLocationAndAngles(ctx.originX() + 0.5, ctx.originY(), ctx.originZ() + 0.5, 0, 0);
        return p;
    }

    /** One server tick of a connected player. */
    private static void tick(EntityPlayerMP p) {
        // onUpdateEntity opens the player's statistics file every 20th tick of its life; this player has none to open.
        p.ticksExisted = 1;
        p.onUpdateEntity();
    }

    /** Right-clicks the air with {@code stack} in slot 0, as the server handles the click packet. */
    private static void rightClick(EntityPlayerMP p, ItemStack stack) {
        p.inventory.currentItem = 0;
        p.inventory.mainInventory[0] = stack;
        PlayerInteractEvent e = ForgeEventFactory
            .onPlayerInteract(p, PlayerInteractEvent.Action.RIGHT_CLICK_AIR, 0, 0, 0, -1, p.worldObj);
        if (e.useItem != Event.Result.DENY) {
            p.theItemInWorldManager.tryUseItem(p, p.worldObj, stack);
        }
    }

    /** Counts the sounds played at one entity. */
    public static final class SoundTap {

        private final EntityPlayerMP who;
        final List<String> heard = new ArrayList<>();

        SoundTap(EntityPlayerMP who) {
            this.who = who;
        }

        /** @param event a sound at an entity (Forge posts it from {@code World.playSoundAtEntity} on both sides) */
        @SubscribeEvent
        public void onSound(PlaySoundAtEntityEvent event) {
            if (event.entity == who) {
                heard.add(event.name);
            }
        }

        int count(String name) {
            int n = 0;
            for (String s : heard) {
                if (name.equals(s)) n++;
            }
            return n;
        }
    }

    /**
     * A hurt player holds right-click with the item: it is heard while in use, then heals, regenerates and is used up.
     */
    static final class Use implements Scenario {

        private final String item;

        Use(String item) {
            this.item = item;
        }

        @Override
        public String name() {
            return "item.consumable." + item;
        }

        @Override
        public String description() {
            return "A hurt player holding right-click with " + item
                + " hears it in use, then gains the heal and Regeneration, and one is used up";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            Item it = ComponentItems.get(item);
            out.check(it instanceof ItemConsumable, item + " is a consumable");
            if (!(it instanceof ItemConsumable)) return out.build();
            ConsumableSpec spec = ((ItemConsumable) it).spec();
            EntityPlayerMP p = patient(ctx, item);
            p.setHealth(8.0F);
            float before = p.getHealth();
            ItemStack stack = new ItemStack(it, 2);
            SoundTap tap = new SoundTap(p);
            MinecraftForge.EVENT_BUS.register(tap);
            try {
                rightClick(p, stack);
                out.check(p.isUsingItem(), "the right click starts using it");
                out.checkEquals("use time in ticks", spec.useTicks(), p.getItemInUseCount());
                int ticks = 0;
                while (p.isUsingItem() && ticks < spec.useTicks() + 10) {
                    tick(p);
                    ticks++;
                }
                out.check(!p.isUsingItem(), "the use finished on its own");
                out.checkEquals("ticks of holding", spec.useTicks(), ticks);
                out.check(
                    p.getHealth() >= before + spec.healHp() - 0.01F,
                    "health went from " + before + " up by at least " + spec.healHp() + " (now " + p.getHealth() + ")");
                PotionEffect regen = p.getActivePotionEffect(Potion.regeneration);
                out.check(regen != null, "Regeneration is on");
                if (regen != null) {
                    out.checkEquals("Regeneration amplifier", spec.regenAmplifier(), regen.getAmplifier());
                }
                ItemStack left = p.inventory.mainInventory[0];
                out.checkEquals("items left in the hand", 1, left == null ? 0 : left.stackSize);
                out.checkEquals(
                    "use sounds while held (every " + ConsumableSpec.FEEDBACK_PERIOD + " ticks)",
                    spec.useTicks() / ConsumableSpec.FEEDBACK_PERIOD,
                    tap.count(ConsumableSpec.USE_SOUND));
                out.checkEquals("finish sounds", 1, tap.count("random.burp"));
            } finally {
                MinecraftForge.EVENT_BUS.unregister(tap);
                p.inventory.mainInventory[0] = null;
            }
            return out.build();
        }
    }

    /** At full health a right click neither starts a use nor consumes anything. */
    static final class FullHealth implements Scenario {

        @Override
        public String name() {
            return "item.consumable.full_health";
        }

        @Override
        public String description() {
            return "At full health a right click with a medkit does not start a use, heal or consume it";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            Item it = ComponentItems.get(ComponentItems.MEDKIT);
            EntityPlayerMP p = patient(ctx, "full");
            p.setHealth(p.getMaxHealth());
            ItemStack stack = new ItemStack(it, 1);
            try {
                rightClick(p, stack);
                out.check(!p.isUsingItem(), "no use starts at full health");
                for (int i = 0; i < ConsumableSpec.MEDKIT.useTicks() + 5; i++) {
                    tick(p);
                }
                out.check(p.getActivePotionEffect(Potion.regeneration) == null, "no Regeneration");
                ItemStack left = p.inventory.mainInventory[0];
                out.checkEquals("medkits left", 1, left == null ? 0 : left.stackSize);
            } finally {
                p.inventory.mainInventory[0] = null;
            }
            return out.build();
        }
    }
}
