package com.gregguns.icbm.missile;

import net.minecraft.item.ItemBlock;

import com.gregguns.core.config.IcbmConfig;
import com.gregguns.core.registry.ModuleRegistrar;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.icbm.IcbmAvailability;

/**
 * Registration of the missile content: two body blocks and items, the filler, the tiles, the two missile entities, the
 * chunk-ticket callback, and (after the module gate) the Galacticraft hooks. {@code IcbmModule} calls the hooks. Common
 * side.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/missile-bodies.md, missile-entity.md
public final class MissileRegistration {

    /** Entity ids 120 and 121: GregGuns' other entities stop at 80. */
    private static final int ENTITY_ID_T1 = 120;
    private static final int ENTITY_ID_T2 = 121;
    /** Entity id 122: the T3 missile (D70). */
    private static final int ENTITY_ID_T3 = 122;

    /** Entity id 123: the silo door panels (D89). */
    private static final int ENTITY_ID_SILO_DOORS = 123;

    /** The countdown a dev server uses unless a scenario asks for another. */
    public static final int SCENARIO_COUNTDOWN_TICKS = 40;

    private MissileRegistration() {}

    /**
     * Config-independent registration.
     *
     * @param registrar the ICBM module's registrar
     */
    public static void preInit(ModuleRegistrar registrar) {
        BlockMissileBody[] bodies = new BlockMissileBody[MissileTier.values().length];
        for (MissileTier tier : MissileTier.values()) {
            BlockMissileBody block = registrar
                .registerBlock(tier.blockName(), new BlockMissileBody(tier), ItemBlockMissileBody.class);
            // Placeholder particle icon: the model texture lives in the atlas under another name (see the client).
            block.setBlockTextureName("gregguns:casings/gantry_casing");
            bodies[tier.ordinal()] = block;
            MissileBodies.set(tier, net.minecraft.item.Item.getItemFromBlock(block));
        }
        BlockMissileFiller filler = registrar
            .registerBlock(BlockMissileFiller.NAME, new BlockMissileFiller(), ItemBlock.class);
        filler.setBlockTextureName("gregguns:casings/gantry_casing");
        MissileBlocks.set(bodies, filler);
        registrar.registerTileEntity("missile_body", TileMissileBody.class);
        registrar.registerEntity("missile_t1", EntityMissileT1.class, ENTITY_ID_T1, 160, 1, true);
        registrar.registerEntity("missile_t2", EntityMissileT2.class, ENTITY_ID_T2, 160, 1, true);
        registrar.registerEntity("missile_t3", EntityMissileT3.class, ENTITY_ID_T3, 160, 1, true);
        registrar.registerEntity(
            "silo_doors",
            com.gregguns.icbm.silo.EntitySiloDoors.class,
            ENTITY_ID_SILO_DOORS,
            160,
            3,
            false);
    }

    /** Wires the chunk-ticket callback. Runs whatever the module flag says: a missile can exist in an old save. */
    public static void initContent() {
        MissileTickets.registerCallback();
    }

    /** Runs only for an enabled module (Galacticraft present): the fuel lists, the pad guard, recycling data. */
    public static void init() {
        if (IcbmAvailability.galacticraftPresent()) {
            MissileGc.init();
        }
    }

    /**
     * Dev-only scenarios.
     *
     * @param registry where to add them
     */
    public static void registerScenarios(ScenarioRegistry registry) {
        // The scenarios that only need a missile in the air run a short countdown (the old 40-tick warm-up); the launch
        // sequence scenarios set the real 200 ticks themselves and put this back (D88). Dev servers only.
        IcbmConfig.countdownTicks = SCENARIO_COUNTDOWN_TICKS;
        MissileScenarios.registerAll(registry);
    }
}
