package com.gregguns.client.npc;

import com.gregguns.client.mob.MobRenderers;
import com.gregguns.core.DevEnvironment;
import com.gregguns.core.module.ClientModule;
import com.gregguns.core.module.ModuleId;
import com.gregguns.entity.npc.Bandit;
import com.gregguns.entity.npc.NpcEntity;

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
        MobRenderers.biped(Bandit.class, "biped", "textures/entity/bandit.png", 1.0F, 0.5F, 0.0F, 0.0F, 0.0F);
        if (DevEnvironment.isActive()) {
            MobRenderers
                .biped(NpcEntity.class, "biped", "textures/entity/army_soldier.png", 1.0F, 0.5F, 0.0F, 0.0F, 0.0F);
        }
    }
}
