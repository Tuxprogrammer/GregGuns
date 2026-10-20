package com.gregguns.client.npc;

import com.gregguns.client.mob.MobRenderers;
import com.gregguns.core.DevEnvironment;
import com.gregguns.core.module.ClientModule;
import com.gregguns.core.module.ModuleId;
import com.gregguns.entity.npc.NpcEntity;
import com.gregguns.entity.npc.SuperMutantBasic;
import com.gregguns.entity.npc.SuperMutantElite;
import com.gregguns.entity.npc.SuperMutantHeavy;

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
        // Super Mutants: Basic, Heavy, Elite (M7-ASSETS: scale, shadow, gun offset, yOffset per tier).
        MobRenderers
            .biped(SuperMutantBasic.class, "super_mutant", "super_mutant_1", 1.35F, 0.6F, -0.10F, 0.30F, 0.0F, 0.55F);
        MobRenderers
            .biped(SuperMutantHeavy.class, "super_mutant", "super_mutant_2", 1.50F, 0.65F, -0.15F, 0.30F, 0.0F, 0.75F);
        MobRenderers
            .biped(SuperMutantElite.class, "super_mutant", "super_mutant_3", 1.65F, 0.7F, -0.20F, 0.40F, 0.0F, 0.95F);
        if (DevEnvironment.isActive()) {
            MobRenderers
                .biped(NpcEntity.class, "biped", "textures/entity/army_soldier.png", 1.0F, 0.5F, 0.0F, 0.0F, 0.0F);
        }
    }
}
