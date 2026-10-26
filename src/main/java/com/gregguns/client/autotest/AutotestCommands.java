package com.gregguns.client.autotest;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import com.gregguns.GregGuns;
import com.gregguns.armor.slots.SlotSyncProbe;
import com.gregguns.armor.slots.logic.BaubleSlot;
import com.gregguns.core.testing.AutotestArgs;
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
 * Other commands (handled by {@link ClientAutotest}): {@code view <0|1|2>}, {@code hud <on|off>} (off hides the GUI
 * layer, as F1 does, for clean third-person shots of worn armor: used by {@code armor.showcase}), {@code shot <label>},
 * {@code arm <shot|reload> <label> <delayTicks>}, {@code fire <count>}, {@code slots <owner>} (reports this client's
 * copy of a player's Baubles slots back to the server as chat lines, see {@code AutotestReport}),
 * {@code look <yaw> <pitch>} (sets the player's rotation, which a server {@code tp} cannot), {@code use} (a real
 * right-click with the held item), {@code usesneak} (the same while sneaking, which StructureLib needs to build),
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
                // "name" is a GregGuns item; "modid:name[:meta]" any other mod's (a Galacticraft rocket, say).
                String[] ref = args[1].split(":");
                boolean other = ref.length >= 2;
                Item item = GameRegistry.findItem(other ? ref[0] : GregGuns.MODID, other ? ref[1] : args[1]);
                if (item == null) {
                    ClientAutotest.logLine("gui nei: no item " + args[1]);
                    return;
                }
                int meta = ref.length >= 3 ? Integer.parseInt(ref[2]) : 0;
                boolean opened = GuiCraftingRecipe.openRecipeGui("item", new ItemStack(item, 1, meta));
                ClientAutotest.logLine("gui nei " + args[1] + " opened=" + opened);
            }
            case "neisearch" -> {
                // Types into NEI's item-panel search field, like a player would; the inventory GUI must be open.
                codechicken.nei.LayoutManager.searchField.setText(args[1]);
                ClientAutotest.logLine("gui neisearch '" + args[1] + "'");
            }
            default -> ClientAutotest.logLine("unknown gui " + args[0]);
        }
    }

    /**
     * {@code look <yaw> <pitch>}: sets the local player's rotation, including the previous-tick values so the camera
     * does not interpolate from the old angle.
     *
     * @param args the arguments after {@code look}
     * @throws IllegalArgumentException on malformed arguments (see {@link AutotestArgs#parseLook})
     */
    static void look(String[] args) {
        float[] angles = AutotestArgs.parseLook(args);
        EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
        player.rotationYaw = angles[0];
        player.prevRotationYaw = angles[0];
        player.rotationYawHead = angles[0];
        player.prevRotationYawHead = angles[0];
        player.renderYawOffset = angles[0];
        player.prevRenderYawOffset = angles[0];
        player.rotationPitch = angles[1];
        player.prevRotationPitch = angles[1];
        ClientAutotest.logLine("look yaw " + angles[0] + " pitch " + angles[1]);
    }

    /**
     * {@code use}: a right-click with the held item, as vanilla's {@code Minecraft.rightClickMouse} does: on the entity
     * under the crosshair through its interact packet, on the block through {@code onPlayerRightClick}, and otherwise
     * (or when nothing took the click) {@code sendUseItem}. The crosshair target is the one the last rendered frame
     * found, so a {@code look} needs a frame before it takes effect.
     */
    // Source: Minecraft 1.7.10 net.minecraft.client.Minecraft.func_147121_ag (MCP rightClickMouse).
    // Decompiled: the vanilla order is entity, then block, then the item.
    static void use() {
        Minecraft mc = Minecraft.getMinecraft();
        EntityClientPlayerMP player = mc.thePlayer;
        ItemStack held = player.getHeldItem();
        MovingObjectPosition target = mc.objectMouseOver;
        boolean useItem = true;
        String what = "air";
        if (target != null && target.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
            what = "entity " + target.entityHit.getCommandSenderName();
            if (mc.playerController.interactWithEntitySendPacket(player, target.entityHit)) {
                useItem = false;
            }
        } else if (target != null && target.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            what = "block " + target.blockX + " " + target.blockY + " " + target.blockZ + " side " + target.sideHit;
            if (mc.playerController.onPlayerRightClick(
                player,
                mc.theWorld,
                held,
                target.blockX,
                target.blockY,
                target.blockZ,
                target.sideHit,
                target.hitVec)) {
                useItem = false;
                player.swingItem();
            }
        }
        if (useItem && held != null && mc.playerController.sendUseItem(player, mc.theWorld, held)) {
            mc.entityRenderer.itemRenderer.resetEquippedProgress2();
        }
        ClientAutotest.logLine(
            "use " + (held == null ? "(empty hand)" : Item.itemRegistry.getNameForObject(held.getItem()))
                + " on "
                + what);
    }

    /**
     * {@code usesneak}: holds the sneak key for {@link #SNEAK_LEAD_TICKS} ticks, so the sneak state reaches the server
     * with the client's movement packets (the place packet carries no sneak flag; the server reads its own copy), then
     * does {@link #use()}, then holds on a few more ticks and lets go. StructureLib builds a multiblock from a
     * sneaking click of a trigger item on the controller (creative players build everything, survival players spend
     * what they carry).
     */
    // Source: StructureLib 1.4.45 ConstructableUtility.handle0 (server side: "if (!aPlayer.isSneaking()) return true;",
    // then construct in creative):
    // https://github.com/GTNewHorizons/StructureLib/blob/1.4.45/src/main/java/com/gtnewhorizon/structurelib/alignment/constructable/ConstructableUtility.java#L43-L66
    static final class SneakUseWait implements Wait {

        private int ticks;

        @Override
        public boolean tick(EntityClientPlayerMP player) {
            KeyBinding sneak = Minecraft.getMinecraft().gameSettings.keyBindSneak;
            if (ticks < SNEAK_LEAD_TICKS + SNEAK_TAIL_TICKS) {
                KeyBinding.setKeyBindState(sneak.getKeyCode(), true);
                player.setSneaking(true);
            }
            ticks++;
            if (ticks == SNEAK_LEAD_TICKS) {
                use();
            }
            if (ticks >= SNEAK_LEAD_TICKS + SNEAK_TAIL_TICKS) {
                KeyBinding.setKeyBindState(sneak.getKeyCode(), false);
                player.setSneaking(false);
                return true;
            }
            return false;
        }
    }

    /** Ticks of sneaking before the click, so the server has seen the sneak start. */
    private static final int SNEAK_LEAD_TICKS = 5;
    /** Ticks of sneaking after the click, so the server handles the click while the player still sneaks. */
    private static final int SNEAK_TAIL_TICKS = 5;

    /** A command that spans several client ticks; the command queue blocks while one is open. */
    interface Wait {

        /**
         * @param player the local player
         * @return true when the wait is over
         */
        boolean tick(EntityClientPlayerMP player);
    }

    /**
     * {@code see <name>}: waits (up to {@link #SEE_WAIT_MS} ms) until a player of that name is loaded in
     * the client world, then tells the server ({@code ack see <name> ok|fail}). A teleport leaves the other client
     * without the entity for a while, and remote shot events need it.
     */
    static final class SeeWait implements Wait {

        private final String name;
        private final long startMs = System.currentTimeMillis();

        SeeWait(String name) {
            this.name = name;
        }

        @Override
        public boolean tick(EntityClientPlayerMP player) {
            boolean seen = Minecraft.getMinecraft().theWorld.getPlayerEntityByName(name) != null;
            if (!seen && System.currentTimeMillis() - startMs <= SEE_WAIT_MS) {
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
    /** Wall-clock wait for another player to load after a teleport (a loaded machine stalls the client). */
    private static final long SEE_WAIT_MS = 60_000L;
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
