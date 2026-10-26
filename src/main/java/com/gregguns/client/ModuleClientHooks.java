package com.gregguns.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.StatCollector;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

import com.gregguns.GregGuns;
import com.gregguns.core.module.ModuleContent;
import com.gregguns.core.module.ModuleId;
import com.gregguns.core.module.ModuleState;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;

/**
 * Client side of the module flags: the tooltip line on items of a module that is off, dropping the server's flags when
 * the connection ends, and telling NEI to re-filter when the flags change. Client only.
 */
// Spec: docs/gregguns-spec/01-core/config-gtnhlib.md (module switches)
public final class ModuleClientHooks {

    /** NEI's mod id, for the soft guard. */
    private static final String NEI = "NotEnoughItems";

    /** @param event a tooltip; gets a red line when the stack's module is off */
    @SubscribeEvent
    public void onTooltip(ItemTooltipEvent event) {
        ModuleId off = ModuleContent.disabledModuleOf(event.itemStack);
        if (off != null) {
            event.toolTip.add(
                "\u00a7c"
                    + StatCollector.translateToLocalFormatted(ModuleState.DISABLED_KEY, ModuleState.disabledFlag(off)));
        }
    }

    /** @param event leaving a server: its module flags no longer apply */
    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        if (ModuleState.usingServerValues()) {
            ModuleState.clearServerMask();
            GregGuns.LOG.info("Left the server: this client's own GregGuns module flags apply again");
            refreshNei();
        }
    }

    /** Re-runs NEI's item filter so hidden and shown GregGuns items follow the module flags now in force. */
    public static void refreshNei() {
        if (!Loader.isModLoaded(NEI)) {
            return;
        }
        Minecraft.getMinecraft()
            .func_152344_a(NeiRefresh::update);
    }

    /** Loaded only when NEI is present. */
    private static final class NeiRefresh {

        static void update() {
            try {
                codechicken.nei.ItemList.updateFilter.restart();
            } catch (Throwable t) {
                GregGuns.LOG.debug("NEI refresh failed (harmless): {}", t.toString());
            }
        }
    }
}
