package com.gregguns.client.utility;

import net.minecraft.item.Item;
import net.minecraftforge.client.MinecraftForgeClient;

import com.gregguns.core.module.ClientModule;
import com.gregguns.core.module.ModuleId;
import com.gregguns.utility.charge.BlockExplosiveCharge;
import com.gregguns.utility.charge.TileExplosiveCharge;

import cpw.mods.fml.client.registry.ClientRegistry;

/**
 * Client half of the explosive charges (chunk m6/charges): registers the placeholder tile and item renderers. The
 * utility module has no other client module yet; this one is separate so the parallel M6 chunks never edit one file.
 * Client only, reached through {@code ClientProxy}.
 */
// Spec: docs/techguns-spec/09-machines-utility/explosive-charges.md (World rendering)
public final class ChargeClientModule implements ClientModule {

    @Override
    public ModuleId id() {
        return ModuleId.UTILITY;
    }

    @Override
    public void init() {
        if (BlockExplosiveCharge.instance == null) {
            return;
        }
        ChargeRenderer renderer = new ChargeRenderer();
        ClientRegistry.bindTileEntitySpecialRenderer(TileExplosiveCharge.class, renderer);
        MinecraftForgeClient.registerItemRenderer(Item.getItemFromBlock(BlockExplosiveCharge.instance), renderer);
    }
}
