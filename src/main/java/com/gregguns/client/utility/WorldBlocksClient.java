package com.gregguns.client.utility;

import net.minecraft.item.Item;
import net.minecraftforge.client.MinecraftForgeClient;

import com.gregguns.utility.WorldBlockRegistration;
import com.gregguns.utility.camp.TileCampFlag;
import com.gregguns.utility.storage.TileGregChest;

import cpw.mods.fml.client.registry.ClientRegistry;

/**
 * Client registration of the placeholder renderers of the chests and the Camp Flag. The Bio Blob uses the plain block
 * renderer. Client only; {@code WorldBlockRegistration} calls it when the side is the client.
 */
// Spec: docs/techguns-spec/09-machines-utility/misc-tile-entities.md (Rendering); placeholders until the model pass
public final class WorldBlocksClient {

    private WorldBlocksClient() {}

    /** Binds the tile-entity renderers and the chest item renderers. */
    public static void register() {
        ClientRegistry.bindTileEntitySpecialRenderer(TileGregChest.class, new ChestRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(TileCampFlag.class, new CampFlagRenderer());
        MinecraftForgeClient.registerItemRenderer(
            Item.getItemFromBlock(WorldBlockRegistration.reinforcedChest()),
            new ChestRenderer.ItemForm(ChestRenderer.REINFORCED));
        MinecraftForgeClient.registerItemRenderer(
            Item.getItemFromBlock(WorldBlockRegistration.weaponChest()),
            new ChestRenderer.ItemForm(ChestRenderer.WEAPON));
    }
}
