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
 * {@code arm <shot|reload> <label> <delayTicks>}, {@code fire <count>}, {@code close}, {@code quit}. Dev autotest only.
 * Client only.
 */
@SideOnly(Side.CLIENT)
final class AutotestCommands {

    private AutotestCommands() {}

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
