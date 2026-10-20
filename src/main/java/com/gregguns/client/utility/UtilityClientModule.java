package com.gregguns.client.utility;

import net.minecraft.item.Item;
import net.minecraftforge.client.MinecraftForgeClient;

import com.gregguns.core.module.ClientModule;
import com.gregguns.core.module.ModuleId;
import com.gregguns.utility.charge.BlockExplosiveCharge;
import com.gregguns.utility.charge.TileExplosiveCharge;
import com.gregguns.utility.turret.entity.TurretEntity;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.client.registry.RenderingRegistry;

/**
 * Client half of the {@code modules.utility} module: the placeholder renderers of the M6 content. Each M6 chunk adds
 * one line to {@link #init()} for its own renderers. Client only, reached through {@code ClientProxy}.
 */
// Spec: docs/ROADMAP.md (M6)
public final class UtilityClientModule implements ClientModule {

    @Override
    public ModuleId id() {
        return ModuleId.UTILITY;
    }

    @Override
    public void init() {
        RenderingRegistry.registerEntityRenderingHandler(TurretEntity.class, new TurretRenderer());
        initCharges();
    }

    /** Placeholder tile and item renderers of the explosive charges; skipped when the block is not registered. */
    private static void initCharges() {
        if (BlockExplosiveCharge.instance == null) {
            return;
        }
        ChargeRenderer renderer = new ChargeRenderer();
        ClientRegistry.bindTileEntitySpecialRenderer(TileExplosiveCharge.class, renderer);
        MinecraftForgeClient.registerItemRenderer(Item.getItemFromBlock(BlockExplosiveCharge.instance), renderer);
    }
}
