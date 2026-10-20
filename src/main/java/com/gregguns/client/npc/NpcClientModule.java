package com.gregguns.client.npc;

import net.minecraft.entity.EntityLiving;
import net.minecraft.util.ResourceLocation;

import com.gregguns.client.mob.MobRenderers;
import com.gregguns.core.DevEnvironment;
import com.gregguns.core.module.ClientModule;
import com.gregguns.core.module.ModuleId;
import com.gregguns.entity.npc.NpcEntity;
import com.gregguns.entity.npc.mob.SkeletonSoldier;
import com.gregguns.entity.npc.mob.ZombieFarmer;
import com.gregguns.entity.npc.mob.ZombieMiner;
import com.gregguns.entity.npc.mob.ZombiePigmanSoldier;
import com.gregguns.entity.npc.mob.ZombieSoldier;

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
        // Zombies: the biped model with the zombie skin (M7-ASSETS), vanilla scale and shadow.
        zombie(ZombieSoldier.class);
        zombie(ZombieFarmer.class);
        zombie(ZombieMiner.class);
        MobRenderers.biped(
            SkeletonSoldier.class,
            "skeleton_soldier",
            new ResourceLocation("minecraft", "textures/entity/skeleton/skeleton.png"),
            1.0F,
            0.5F,
            0.1F,
            0.0F,
            0.05F);
        // Pigman Soldier has no dedicated skin: biped model, vanilla pigman skin.
        MobRenderers.biped(
            ZombiePigmanSoldier.class,
            "biped",
            new ResourceLocation("minecraft", "textures/entity/zombie_pigman.png"),
            1.0F,
            0.5F,
            0.0F,
            0.0F,
            0.0F);
        if (DevEnvironment.isActive()) {
            MobRenderers
                .biped(NpcEntity.class, "biped", "textures/entity/army_soldier.png", 1.0F, 0.5F, 0.0F, 0.0F, 0.0F);
        }
    }

    private static void zombie(Class<? extends EntityLiving> clazz) {
        MobRenderers.biped(clazz, "biped", "textures/entity/zombie_soldier.png", 1.0F, 0.5F, 0.0F, 0.0F, 0.0F);
    }
}
