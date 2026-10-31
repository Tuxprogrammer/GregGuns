package com.gregguns.compat.lootbags;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import com.gregguns.core.config.CompatConfig;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.core.testing.SkipUnless;
import com.mojang.authlib.GameProfile;

import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.registry.GameRegistry;

/**
 * In-game checks of the GregGuns loot bags against the real EnhancedLootBags. A bag is opened the way a player's right
 * click reaches the server: {@code PlayerInteractEvent} RIGHT_CLICK_AIR, then {@code tryUseItem} unless the event
 * denied the item use (Forge's NetHandlerPlayServer patch). Skipped when ELB is not loaded or {@code compat.lootBags}
 * is off. Dev environment only. Server side.
 */
// Spec: docs/gregguns-spec/06-items/pack-loot.md (EnhancedLootBags)
public final class LootBagScenarios {

    private static final GameProfile PROFILE = new GameProfile(
        UUID.fromString("5d7e0b1c-2f43-4e8a-9c61-7a0f3b2c1d70"),
        "[GregGunsLootBagTest]");

    private LootBagScenarios() {}

    /** @param registry where the scenarios go */
    public static void registerAll(ScenarioRegistry registry) {
        register(registry, new Groups());
        register(registry, new OpenEvery());
        register(registry, new Reload());
        register(registry, new Guard());
    }

    private static void register(ScenarioRegistry registry, Scenario s) {
        registry.register(
            SkipUnless.of(
                s,
                () -> LootBagInjector.active() && CompatConfig.lootBags,
                "EnhancedLootBags is not loaded, or compat.lootBags is off"));
    }

    private static List<LootBagGroups.Group> shipped() {
        try {
            return LootBagGroups.parse(LootBagGroups.readResource());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static ItemStack stackOf(String itemName) {
        String[] p = LootBagGroups.splitItemName(itemName);
        if (p == null) return null;
        Item item = GameRegistry.findItem(p[0], p[1]);
        if (item == null && GameRegistry.findBlock(p[0], p[1]) != null) {
            item = Item.getItemFromBlock(GameRegistry.findBlock(p[0], p[1]));
        }
        return item == null ? null : new ItemStack(item, 1, p[2] == null ? 0 : Integer.parseInt(p[2]));
    }

    private static ItemStack bag(int id) {
        return new ItemStack(GameRegistry.findItem(LootBagGroups.MOD_ID, "lootbag"), 1, id);
    }

    private static EntityPlayerMP player(ScenarioContext ctx) {
        EntityPlayerMP p = FakePlayerFactory.get((WorldServer) ctx.world(), PROFILE);
        if (p.playerNetServerHandler == null) {
            // tryUseItem resends the inventory after a use; a fake player needs a handler with an unconnected manager.
            com.gregguns.core.testing.QuietConnection.attach(p);
        }
        p.setLocationAndAngles(ctx.originX() + 0.5, ctx.originY(), ctx.originZ() + 0.5, 0, 0);
        return p;
    }

    /** The outcome of one right click with a bag in hand. */
    private static final class Use {

        boolean denied;
        boolean consumed;
        int dropped;
        Throwable error;
    }

    /**
     * Holds {@code stack} in slot 0 and right-clicks the air the way the server handles the click packet. Removes the
     * items the bag dropped.
     */
    private static Use rightClick(ScenarioContext ctx, ItemStack stack, boolean postEvent) {
        EntityPlayerMP p = player(ctx);
        p.inventory.currentItem = 0;
        p.inventory.mainInventory[0] = stack;
        AxisAlignedBB box = AxisAlignedBB
            .getBoundingBox(p.posX - 3, p.posY - 3, p.posZ - 3, p.posX + 3, p.posY + 3, p.posZ + 3);
        clearItems(ctx, box);
        Use use = new Use();
        try {
            if (postEvent) {
                PlayerInteractEvent e = ForgeEventFactory
                    .onPlayerInteract(p, PlayerInteractEvent.Action.RIGHT_CLICK_AIR, 0, 0, 0, -1, ctx.world());
                use.denied = e.useItem == Event.Result.DENY;
            }
            if (!use.denied) {
                p.theItemInWorldManager.tryUseItem(p, ctx.world(), stack);
            }
        } catch (RuntimeException e) {
            use.error = e;
        }
        ItemStack left = p.inventory.mainInventory[0];
        use.consumed = left == null || left.stackSize < 1;
        use.dropped = clearItems(ctx, box);
        p.inventory.mainInventory[0] = null;
        return use;
    }

    private static int clearItems(ScenarioContext ctx, AxisAlignedBB box) {
        List<?> items = ctx.world()
            .getEntitiesWithinAABB(EntityItem.class, box);
        for (Object o : items) {
            ((EntityItem) o).setDead();
        }
        return items.size();
    }

    /** Every shipped group is in ELB, and every drop names an item that exists. */
    static final class Groups implements Scenario {

        @Override
        public String name() {
            return "lootbag.groups";
        }

        @Override
        public String description() {
            return "Every GregGuns loot group is in EnhancedLootBags; every drop is a real item, ICBM ones without NBT";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            List<LootBagGroups.Group> groups = shipped();
            int drops = 0;
            for (LootBagGroups.Group g : groups) {
                out.check(LootBagInjector.hasGroup(g.id), "group " + g.id + " is in EnhancedLootBags");
                drops += g.drops.size();
                for (LootBagGroups.Drop d : g.drops) {
                    ItemStack s = stackOf(d.itemName);
                    out.check(s != null, "group " + g.id + ": " + d.itemName + " exists");
                    if (s == null) continue;
                    try {
                        // A stack straight from a bag has no NBT: its name must still read (chips, schematics,
                        // warheads, electric gear).
                        out.check(
                            !s.getDisplayName()
                                .isEmpty(),
                            d.itemName + " has a name without NBT");
                    } catch (RuntimeException e) {
                        out.check(false, d.itemName + " without NBT throws " + e);
                    }
                }
            }
            out.checkEquals(
                "drops left after the startup prune",
                drops,
                LootBagInjector.ourItemNames()
                    .size());
            return out.build();
        }
    }

    /** Each GregGuns bag opens: it is used up and drops items. */
    static final class OpenEvery implements Scenario {

        @Override
        public String name() {
            return "lootbag.open";
        }

        @Override
        public String description() {
            return "Each GregGuns bag opens on a right click: it is used up and drops its items";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            for (LootBagGroups.Group g : shipped()) {
                Use use = rightClick(ctx, bag(g.id), true);
                out.check(use.error == null, "bag " + g.id + " opens without an error: " + use.error);
                out.check(!use.denied, "bag " + g.id + " is not denied");
                out.check(use.consumed, "bag " + g.id + " is used up");
                out.check(use.dropped >= 1, "bag " + g.id + " drops items (" + use.dropped + ")");
            }
            return out.build();
        }
    }

    /** {@code /lootbags reload} re-reads only the pack file; our groups come straight back. */
    static final class Reload implements Scenario {

        @Override
        public String name() {
            return "lootbag.reload";
        }

        @Override
        public String description() {
            return "After /lootbags reload the GregGuns groups are back at once and a bag still opens";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            MinecraftServer server = MinecraftServer.getServer();
            server.getCommandManager()
                .executeCommand(server, "lootbags reload");
            for (LootBagGroups.Group g : shipped()) {
                out.check(LootBagInjector.hasGroup(g.id), "group " + g.id + " is back right after the reload");
            }
            Use use = rightClick(ctx, bag(1704), true);
            out.check(use.error == null && use.consumed && use.dropped >= 1, "bag 1704 opens after the reload");
            return out.build();
        }
    }

    /** With our groups gone and not put back, a right click is denied instead of crashing ELB. */
    static final class Guard implements Scenario {

        @Override
        public String name() {
            return "lootbag.guard";
        }

        @Override
        public String description() {
            return "A GregGuns bag whose group is gone is denied (no crash); with re-adding on, it is put back and opens";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            try {
                LootBagInjector.suspendForTest(true);
                out.check(LootBagInjector.removeForTest(), "the GregGuns groups are taken out");
                out.check(!LootBagInjector.hasGroup(1702), "group 1702 is gone");
                // Without the guard (no event), ELB itself throws on the missing group: the crash the guard prevents.
                Use raw = rightClick(ctx, bag(1702), false);
                out.note("unguarded use of a bag with no group: " + (raw.error == null ? "no error" : raw.error));
                com.gregguns.GregGuns.LOG
                    .info("lootbag.guard: an unguarded use of a bag with no group gives {}", raw.error);
                Use guarded = rightClick(ctx, bag(1702), true);
                out.check(guarded.denied, "the use is denied");
                out.check(guarded.error == null, "no error: " + guarded.error);
                out.check(!guarded.consumed, "the bag is kept");
                out.check(guarded.dropped == 0, "nothing drops");
                LootBagInjector.suspendForTest(false);
                Use healed = rightClick(ctx, bag(1702), true);
                out.check(LootBagInjector.hasGroup(1702), "the guard put group 1702 back");
                out.check(
                    healed.error == null && !healed.denied && healed.consumed && healed.dropped >= 1,
                    "the bag then opens: " + healed.error);
            } finally {
                LootBagInjector.suspendForTest(false);
                LootBagInjector.ensurePresent();
            }
            return out.build();
        }
    }
}
