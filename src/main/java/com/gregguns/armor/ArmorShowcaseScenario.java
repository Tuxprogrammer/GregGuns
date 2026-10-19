package com.gregguns.armor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.WorldSettings;

import com.gregguns.armor.logic.ArmorCatalog;
import com.gregguns.armor.logic.ArmorSlot;
import com.gregguns.armor.logic.SetDef;
import com.gregguns.armor.logic.ShowcasePlan;
import com.gregguns.armor.model.ArmorModels;
import com.gregguns.core.testing.AutotestSignal;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;

/**
 * {@code armor.showcase}: a visual walkthrough of every armor set for a connected client, driven by the dev autotest
 * signals (see {@code docs/testing/README.md}, "Armor showcase"). It dresses one player in each set and camo in turn,
 * switches the camera to third person from the front (and from behind for the sets with a custom model) and tells the
 * client to take a screenshot, labeled {@code armor-<set>-<camo>-<front|back>}. The evidence is the client's
 * screenshots, which also settle whether the 128 x 64 vanilla-layout sheets map correctly on the vanilla biped.
 *
 * <p>
 * Not part of {@code test all}: it needs a connected client. The player is the first connected one by name, or the one
 * named by the JVM property {@value #PLAYER_PROPERTY} on the server. Dev environment only, server side.
 */
// Spec: docs/gregguns-spec/05-armor/implementation-notes.md ARM-DEV-12, ARM-DEV-13
final class ArmorShowcaseScenario implements Scenario {

    /** Names the player to dress (a server JVM property); without it the first connected player by name is used. */
    static final String PLAYER_PROPERTY = "gregguns.showcase.player";
    /** The scenario name. */
    static final String NAME = "armor.showcase";

    static void register(ScenarioRegistry registry) {
        registry.register(new ArmorShowcaseScenario());
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "Dresses a connected client in every armor set and camo and screenshots it (third person, front and"
            + " back). Needs 1 client; not part of test all.";
    }

    @Override
    public boolean includedInAll() {
        return false;
    }

    @SuppressWarnings("unchecked")
    private static EntityPlayerMP pickPlayer() {
        List<EntityPlayerMP> list = new ArrayList<>(
            MinecraftServer.getServer()
                .getConfigurationManager().playerEntityList);
        list.sort(Comparator.comparing(p -> p.getCommandSenderName()));
        String wanted = System.getProperty(PLAYER_PROPERTY);
        for (EntityPlayerMP p : list) {
            if (wanted != null && wanted.equalsIgnoreCase(p.getCommandSenderName())) {
                return p;
            }
        }
        return wanted != null || list.isEmpty() ? null : list.get(0);
    }

    private static void signal(EntityPlayerMP player, String command, String... args) {
        player.addChatMessage(new ChatComponentText(AutotestSignal.format(command, args)));
    }

    private static SetDef setOf(String id) {
        for (SetDef s : ArmorCatalog.ALL) {
            if (s.id()
                .equals(id)) {
                return s;
            }
        }
        throw new IllegalStateException("no set " + id);
    }

    private static void equip(EntityPlayerMP player, SetDef set, int camo) {
        for (int i = 0; i < 4; i++) {
            player.inventory.armorInventory[i] = null;
        }
        for (ArmorSlot slot : set.pieces()
            .keySet()) {
            ItemGregGunsArmor item = ArmorItems.get(set, slot);
            if (item == null) {
                continue;
            }
            ItemStack stack = new ItemStack(item);
            item.setCamo(stack, camo);
            // armorInventory[0] is the boots, [3] the helmet.
            player.inventory.armorInventory[3 - slot.armorType()] = stack;
        }
        player.inventoryContainer.detectAndSendChanges();
    }

    @Override
    public ScenarioResult run(ScenarioContext ctx) {
        EntityPlayerMP player = pickPlayer();
        if (player == null) {
            return ScenarioResult.fail(NAME, "needs a connected player (" + PLAYER_PROPERTY + " names one)");
        }
        List<ShowcasePlan.SetSpec> specs = new ArrayList<>();
        for (SetDef set : ArmorCatalog.ALL) {
            specs.add(new ShowcasePlan.SetSpec(set.id(), set.camoCount(), ArmorModels.hasModel(set.id())));
        }
        List<ShowcasePlan.Step> steps = ShowcasePlan.build(specs);
        int end = ShowcasePlan.endTick(steps);
        ctx.clearArea(6, 3);
        player.inventory.clearInventory(null, -1);
        player.setGameType(WorldSettings.GameType.CREATIVE);
        player.setHealth(player.getMaxHealth());
        player.playerNetServerHandler
            .setPlayerLocation(ctx.originX() + 0.5, ctx.originY(), ctx.originZ() + 0.5, 0.0f, 0.0f);
        int[] next = { 0 };
        signal(player, "hud", "off");
        return ScenarioResult.pending(NAME, end + 400, t -> {
            while (next[0] < steps.size() && steps.get(next[0])
                .tick() <= t) {
                ShowcasePlan.Step s = steps.get(next[0]++);
                switch (s.kind()) {
                    case EQUIP -> equip(player, setOf(s.setId()), s.camo());
                    case VIEW -> signal(player, "view", s.argument());
                    case SHOT -> signal(player, "shot", s.argument());
                    default -> throw new IllegalStateException("unknown step " + s.kind());
                }
            }
            if (t < end) {
                return null;
            }
            signal(player, "view", "0");
            signal(player, "hud", "on");
            for (int i = 0; i < 4; i++) {
                player.inventory.armorInventory[i] = null;
            }
            player.setGameType(WorldSettings.GameType.SURVIVAL);
            player.inventoryContainer.detectAndSendChanges();
            return ScenarioResult.builder(NAME)
                .note(
                    steps.size() + " steps for "
                        + specs.size()
                        + " sets sent to "
                        + player.getCommandSenderName()
                        + "; read the client screenshots armor-*.png")
                .build();
        });
    }
}
