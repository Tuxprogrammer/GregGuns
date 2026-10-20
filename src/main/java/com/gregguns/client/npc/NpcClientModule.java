package com.gregguns.client.npc;

import com.gregguns.client.mob.MobRenderers;
import com.gregguns.client.utility.TurretRenderer;
import com.gregguns.core.DevEnvironment;
import com.gregguns.core.module.ClientModule;
import com.gregguns.core.module.ModuleId;
import com.gregguns.entity.npc.NpcEntity;
import com.gregguns.entity.npc.NpcTurret;

import cpw.mods.fml.client.registry.RenderingRegistry;

/**
 * Client half of the {@code modules.npcs} module. For now it registers only the dev-only test NPC with the shared
 * biped model; each mob batch adds its own renderers here. Client only, reached through {@code ClientProxy}.
 */
// Spec: docs/ROADMAP.md (M7), tools/asset-gen/entity_models.md
public final class NpcClientModule implements ClientModule {

    @Override
    public ModuleId id() {
        return ModuleId.NPCS;
    }

    @Override
    public void init() {
        // The hostile camp turret draws with the player turret's head model and the iron plate sheet.
        RenderingRegistry.registerEntityRenderingHandler(NpcTurret.class, new TurretRenderer());
        if (DevEnvironment.isActive()) {
            MobRenderers
                .biped(NpcEntity.class, "biped", "textures/entity/army_soldier.png", 1.0F, 0.5F, 0.0F, 0.0F, 0.0F);
        }
    }
}
