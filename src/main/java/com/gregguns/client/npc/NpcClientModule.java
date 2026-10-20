package com.gregguns.client.npc;

import net.minecraftforge.common.MinecraftForge;

import com.gregguns.client.mob.MobRenderers;
import com.gregguns.core.DevEnvironment;
import com.gregguns.core.module.ClientModule;
import com.gregguns.core.module.ModuleId;
import com.gregguns.entity.npc.DictatorDave;
import com.gregguns.entity.npc.NpcEntity;
import com.gregguns.entity.npc.PsychoSteve;

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
        // Bosses (M7 B3): Steve wears the army skin under his miner armor, Dave has his own skin.
        MobRenderers
            .biped(PsychoSteve.class, "biped", MobRenderers.texture("army_soldier"), 1.0F, 0.5F, 0.0F, 0.0F, 0.0F);
        MobRenderers.biped(DictatorDave.class, "biped", MobRenderers.texture("dictator"), 1.0F, 0.5F, 0.0F, 0.0F, 0.0F);
        MinecraftForge.EVENT_BUS.register(new BossBarHandler());
        if (DevEnvironment.isActive()) {
            MobRenderers
                .biped(NpcEntity.class, "biped", "textures/entity/army_soldier.png", 1.0F, 0.5F, 0.0F, 0.0F, 0.0F);
        }
    }
}
