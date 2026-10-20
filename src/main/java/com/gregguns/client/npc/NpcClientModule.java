package com.gregguns.client.npc;

import net.minecraft.entity.EntityLiving;
import net.minecraftforge.common.MinecraftForge;

import com.gregguns.client.mob.MobRenderers;
import com.gregguns.client.utility.TurretRenderer;
import com.gregguns.core.DevEnvironment;
import com.gregguns.core.module.ClientModule;
import com.gregguns.core.module.ModuleId;
import com.gregguns.entity.npc.AlienBug;
import com.gregguns.entity.npc.AttackHelicopter;
import com.gregguns.entity.npc.DictatorDave;
import com.gregguns.entity.npc.NpcEntity;
import com.gregguns.entity.npc.NpcTurret;
import com.gregguns.entity.npc.PsychoSteve;
import com.gregguns.entity.npc.SuperMutantBasic;
import com.gregguns.entity.npc.SuperMutantElite;
import com.gregguns.entity.npc.SuperMutantHeavy;
import com.gregguns.entity.npc.mob.CyberDemon;
import com.gregguns.entity.npc.mob.ZombieFarmer;
import com.gregguns.entity.npc.mob.ZombieMiner;
import com.gregguns.entity.npc.mob.ZombieSoldier;

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
        // Bosses (M7 B3): Steve wears the army skin under his miner armor, Dave has his own skin.
        MobRenderers
            .biped(PsychoSteve.class, "biped", MobRenderers.texture("army_soldier"), 1.0F, 0.5F, 0.0F, 0.0F, 0.0F);
        MobRenderers.biped(DictatorDave.class, "biped", MobRenderers.texture("dictator"), 1.0F, 0.5F, 0.0F, 0.0F, 0.0F);
        MinecraftForge.EVENT_BUS.register(new BossBarHandler());
        // Super Mutants: Basic, Heavy, Elite (M7-ASSETS: scale, shadow, gun offset, yOffset per tier).
        MobRenderers
            .biped(SuperMutantBasic.class, "super_mutant", "super_mutant_1", 1.35F, 0.6F, -0.10F, 0.30F, 0.0F, 0.55F);
        MobRenderers
            .biped(SuperMutantHeavy.class, "super_mutant", "super_mutant_2", 1.50F, 0.65F, -0.15F, 0.30F, 0.0F, 0.75F);
        MobRenderers
            .biped(SuperMutantElite.class, "super_mutant", "super_mutant_3", 1.65F, 0.7F, -0.20F, 0.40F, 0.0F, 0.95F);
        MobRenderers.biped(
            CyberDemon.class,
            "cyberdemon",
            "textures/entity/cyberdemon.png",
            1.0F,
            0.75F,
            -0.11F,
            0.15F,
            -0.18F);
        // Attack Helicopter: 18.3 blocks long, so a 5-block shadow (M7-ASSETS).
        MobRenderers.living(AttackHelicopter.class, "attack_helicopter", "attack_helicopter", 1.0F, 5.0F);
        HelicopterBossBar.register();
        MobRenderers.living(AlienBug.class, "alien_bug", "alien_bug", 1.0F, 1.0F);
        // The hostile camp turret draws with the player turret's head model and the iron plate sheet.
        RenderingRegistry.registerEntityRenderingHandler(NpcTurret.class, new TurretRenderer());
        // Zombies: the biped model with the zombie skin (M7-ASSETS), vanilla scale and shadow.
        zombie(ZombieSoldier.class);
        zombie(ZombieFarmer.class);
        zombie(ZombieMiner.class);
        if (DevEnvironment.isActive()) {
            MobRenderers
                .biped(NpcEntity.class, "biped", "textures/entity/army_soldier.png", 1.0F, 0.5F, 0.0F, 0.0F, 0.0F);
        }
    }

    private static void zombie(Class<? extends EntityLiving> clazz) {
        MobRenderers.biped(clazz, "biped", "textures/entity/zombie_soldier.png", 1.0F, 0.5F, 0.0F, 0.0F, 0.0F);
    }
}
