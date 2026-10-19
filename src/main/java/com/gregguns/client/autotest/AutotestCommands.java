package com.gregguns.client.autotest;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Vec3;

import com.gregguns.GregGuns;
import com.gregguns.armor.slots.SlotSyncProbe;
import com.gregguns.armor.slots.logic.BaubleSlot;
import com.gregguns.core.testing.AutotestReport;
import com.gregguns.core.testing.AutotestSignal;

import codechicken.nei.recipe.GuiCraftingRecipe;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.ReflectionHelper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The GUI commands of the dev autotest, sent by the server as {@code gui <what> ...} signals:
 * <ul>
 * <li>{@code gui inventory}: the player inventory;</li>
 * <li>{@code gui creative <tabLabel>}: the creative inventory on the tab with that label (the player must already be
 * in creative mode);</li>
 * <li>{@code gui press <x> <y> <z>}: a real right-click on the block's west face, which opens a machine GUI;</li>
 * <li>{@code gui nei <item>}: NEI's recipe view for the output {@code gregguns:<item>}.</li>
 * </ul>
 * Other commands (handled by {@link ClientAutotest}): {@code view <0|1|2>}, {@code shot <label>},
 * {@code arm <shot|reload> <label> <delayTicks>}, {@code fire <count>}, {@code slots <owner>} (reports this client's
 * copy of a player's Baubles slots back to the server as chat lines, see {@code AutotestReport}),
 * {@code scope <texturePath|off>} (draws a scope
 * overlay on the HUD, loaded at the bounded size), {@code close}, {@code quit}. Dev autotest only.
 * Client only.
 */
@SideOnly(Side.CLIENT)
final class AutotestCommands {

    private AutotestCommands() {}

    /**
     * {@code slots <owner>}: describes this client's copy of the named player's Baubles slots and sends one
     * {@link AutotestReport} chat line per slot to the server, which files them for the {@code mp.slots} scenarios.
     * The owner is the local player or another player in the client's world; an unknown name is logged and reports
     * nothing, so the server scenario times out with a clear message.
     */
    static void reportSlots(String owner) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityClientPlayerMP me = mc.thePlayer;
        net.minecraft.entity.player.EntityPlayer target = mc.theWorld.getPlayerEntityByName(owner);
        if (target == null) {
            ClientAutotest.logLine("slots: no player " + owner + " in this client's world");
            return;
        }
        for (BaubleSlot slot : BaubleSlot.values()) {
            String payload = SlotSyncProbe.describe(target, slot);
            String line;
            try {
                line = AutotestReport.format(me.getCommandSenderName(), owner, slot.typeName(), payload);
            } catch (IllegalArgumentException e) {
                ClientAutotest.logLine("slots: cannot report " + slot + ": " + e.getMessage());
                continue;
            }
            ClientAutotest.logLine("slots " + owner + " " + slot.typeName() + " " + payload);
            me.sendChatMessage(line);
        }
    }

    /** @param args the arguments after {@code gui} */
    static void openGui(String[] args) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityClientPlayerMP player = mc.thePlayer;
        switch (args[0]) {
            case "inventory" -> mc.displayGuiScreen(new GuiInventory(player));
            case "creative" -> openCreative(mc, player, args[1]);
            case "press" -> {
                int x = Integer.parseInt(args[1]);
                int y = Integer.parseInt(args[2]);
                int z = Integer.parseInt(args[3]);
                mc.playerController.onPlayerRightClick(
                    player,
                    mc.theWorld,
                    player.getHeldItem(),
                    x,
                    y,
                    z,
                    4,
                    Vec3.createVectorHelper(x, y + 0.5, z + 0.5));
            }
            case "nei" -> {
                Item item = GameRegistry.findItem(GregGuns.MODID, args[1]);
                if (item == null) {
                    ClientAutotest.logLine("gui nei: no item " + args[1]);
                    return;
                }
                boolean opened = GuiCraftingRecipe.openRecipeGui("item", new ItemStack(item));
                ClientAutotest.logLine("gui nei " + args[1] + " opened=" + opened);
            }
            default -> ClientAutotest.logLine("unknown gui " + args[0]);
        }
    }

    /** A command that spans several client ticks; the command queue blocks while one is open. */
    interface Wait {

        /**
         * @param player the local player
         * @return true when the wait is over
         */
        boolean tick(EntityClientPlayerMP player);
    }

    /**
     * {@code see <name>}: waits (up to {@link #HOLD_TIMEOUT_TICKS} ticks) until a player of that name is loaded in
     * the client world, then tells the server ({@code ack see <name> ok|fail}). A teleport leaves the other client
     * without the entity for a while, and remote shot events need it.
     */
    static final class SeeWait implements Wait {

        private final String name;
        private int waited;

        SeeWait(String name) {
            this.name = name;
        }

        @Override
        public boolean tick(EntityClientPlayerMP player) {
            boolean seen = Minecraft.getMinecraft().theWorld.getPlayerEntityByName(name) != null;
            if (!seen && ++waited <= HOLD_TIMEOUT_TICKS) {
                return false;
            }
            ClientAutotest.logLine("see " + name + " -> " + (seen ? "loaded" : "NOT LOADED"));
            player.sendChatMessage(AutotestSignal.format("ack", "see", name, seen ? "ok" : "fail"));
            return true;
        }
    }

    /**
     * {@code hold <name>}: waits (up to {@link #HOLD_TIMEOUT_TICKS} client ticks) until the selected hotbar slot holds
     * the item, keeps it in hand for {@link #HOLD_SETTLE_TICKS} ticks so the renderer and HUD draw it, then logs what
     * is held with its tooltip (plain and advanced, which runs the item's tooltip code) and tells the server
     * ({@code ack hold <name> ok|fail}) so it can move on. The client command queue blocks while a wait is open.
     */
    static final class HoldWait implements Wait {

        private final String expected;
        private int waited;
        private int settled = -1;

        HoldWait(String expected) {
            this.expected = expected;
        }

        /**
         * @param player the local player
         * @return true when the wait is over (reported either way)
         */
        @Override
        public boolean tick(EntityClientPlayerMP player) {
            if (settled < 0) {
                if (matches(player.getHeldItem(), expected)) {
                    settled = 0;
                } else if (++waited > HOLD_TIMEOUT_TICKS) {
                    report(player, false);
                    return true;
                }
                return false;
            }
            if (++settled < HOLD_SETTLE_TICKS) {
                return false;
            }
            report(player, matches(player.getHeldItem(), expected));
            return true;
        }

        @SuppressWarnings("unchecked")
        private void report(EntityClientPlayerMP player, boolean ok) {
            ItemStack held = player.getHeldItem();
            if (held == null) {
                StringBuilder bar = new StringBuilder();
                for (int i = 0; i < 9; i++) {
                    ItemStack stack = player.inventory.mainInventory[i];
                    bar.append(i)
                        .append('=')
                        .append(stack == null ? "-" : Item.itemRegistry.getNameForObject(stack.getItem()))
                        .append(' ');
                }
                ClientAutotest.logLine(
                    "hold " + expected
                        + " -> NOTHING HELD (selected slot "
                        + player.inventory.currentItem
                        + ", hotbar "
                        + bar
                        + ")");
            } else if (!ok) {
                ClientAutotest.logLine(
                    "hold " + expected
                        + " -> NOTHING HELD (wrong item "
                        + Item.itemRegistry.getNameForObject(held.getItem())
                        + ")");
            } else {
                java.util.List<String> tip = held.getTooltip(player, true);
                ClientAutotest.logLine(
                    "hold " + expected
                        + " -> "
                        + Item.itemRegistry.getNameForObject(held.getItem())
                        + " x"
                        + held.stackSize
                        + " tooltip "
                        + tip.size()
                        + " lines, name '"
                        + held.getDisplayName()
                        + "'");
            }
            player
                .sendChatMessage(AutotestSignal.format("ack", "hold", expected.replace('_', '-'), ok ? "ok" : "fail"));
        }
    }

    /** {@code sync}: runs in command order, so answering it proves every earlier queued command has finished. */
    static void syncAck() {
        Minecraft.getMinecraft().thePlayer.sendChatMessage(AutotestSignal.format("ack", "sync", "ready", "ok"));
    }

    /** Client ticks to wait for the item to arrive in the hand. */
    private static final int HOLD_TIMEOUT_TICKS = 200;
    /** Client ticks the item stays in hand once it arrived, so it is drawn. */
    private static final int HOLD_SETTLE_TICKS = 5;

    /** @return whether {@code held} is the item named {@code expected} (registry name, optionally {@code :meta}) */
    private static boolean matches(ItemStack held, String expected) {
        if (held == null) {
            return false;
        }
        String name = Item.itemRegistry.getNameForObject(held.getItem());
        if (expected.equals(name)) {
            return true;
        }
        return expected.startsWith(name + ":") && expected.equals(name + ":" + held.getItemDamage());
    }

    /**
     * {@code creativegive <name>}: the client-side creative path, putting the stack into hotbar slot 0 with the
     * creative slot packet. {@code modid:name:meta} gives a metadata item.
     */
    static void creativeGive(String spec) {
        Minecraft mc = Minecraft.getMinecraft();
        String[] parts = spec.split(":");
        Item item = GameRegistry.findItem(parts[0], parts[1]);
        if (item == null) {
            ClientAutotest.logLine("creativegive: no item " + spec);
            return;
        }
        ItemStack stack = new ItemStack(item, 1, parts.length > 2 ? Integer.parseInt(parts[2]) : 0);
        mc.thePlayer.inventory.setInventorySlotContents(0, stack);
        mc.playerController.sendSlotPacket(stack, 36);
    }

    private static void openCreative(Minecraft mc, EntityClientPlayerMP player, String label) {
        CreativeTabs tab = null;
        for (CreativeTabs candidate : CreativeTabs.creativeTabArray) {
            if (candidate != null && label.equals(candidate.getTabLabel())) {
                tab = candidate;
            }
        }
        if (tab == null) {
            ClientAutotest.logLine("gui creative: no tab " + label);
            return;
        }
        GuiContainerCreative gui = new GuiContainerCreative(player);
        mc.displayGuiScreen(gui);
        try {
            // The tab page is a private static in Forge's GuiContainerCreative; the selector is a private method.
            Field page = ReflectionHelper.findField(GuiContainerCreative.class, "tabPage");
            page.setInt(null, tab.getTabPage());
            Method select = ReflectionHelper.findMethod(
                GuiContainerCreative.class,
                gui,
                new String[] { "setCurrentCreativeTab", "func_147050_b" },
                CreativeTabs.class);
            select.invoke(gui, tab);
            ClientAutotest.logLine("gui creative tab " + label + " index " + tab.getTabIndex());
        } catch (ReflectiveOperationException | RuntimeException e) {
            GregGuns.LOG.error("[gregguns] AUTOTEST could not select creative tab {}", label, e);
        }
    }
}
