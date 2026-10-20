package com.gregguns.armor;

import net.minecraftforge.common.MinecraftForge;

import com.gregguns.GregGuns;
import com.gregguns.armor.flight.FlightEvents;
import com.gregguns.armor.slots.AmmoPouch;
import com.gregguns.armor.slots.EquipmentGuiHandler;
import com.gregguns.armor.slots.EquipmentKeybinds;
import com.gregguns.core.DevEnvironment;
import com.gregguns.core.module.GregGunsModule;
import com.gregguns.core.module.ModuleId;
import com.gregguns.core.registry.ModuleRegistrar;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.item.gun.AmmoSources;
import com.gregguns.recipe.ArmorRecipes;
import com.gregguns.recipe.EquipmentRecipes;
import com.gregguns.recipe.FlightRecipes;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkRegistry;

/**
 * The {@code modules.armor} module: the armor framework, set bonuses and the armor sets (M3). Each feature area adds
 * one delegating call per hook, so parallel work on different areas touches different lines. Needs the guns module (for
 * the damage system). Both sides; the client half is {@code com.gregguns.client.armor.ArmorClientModule}.
 *
 * <p>
 * Following waves add sets by appending to {@link com.gregguns.armor.logic.ArmorCatalog#ALL}; the slots-and-equipment
 * branch hangs its own registration calls here.
 */
// Spec: docs/ROADMAP.md (M3); docs/gregguns-spec/05-armor/index.md
public final class ArmorModule implements GregGunsModule {

    private static final ArmorEvents EVENTS = new ArmorEvents();

    /** @return the bonus handler instance registered on the event buses; used by scenarios */
    public static ArmorEvents events() {
        return EVENTS;
    }

    @Override
    public ModuleId id() {
        return ModuleId.ARMOR;
    }

    @Override
    public void preInit(FMLPreInitializationEvent event, ModuleRegistrar registrar) {
        ArmorItemRegistry.registerAll(registrar);
        EquipmentKeybinds.register();
    }

    @Override
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(EVENTS);
        MinecraftForge.EVENT_BUS.register(new FlightEvents());
        FMLCommonHandler.instance()
            .bus()
            .register(EVENTS);
        ArmorParts.addRecipes();
        AmmoSources.register(AmmoPouch.SOURCE);
        NetworkRegistry.INSTANCE.registerGuiHandler(GregGuns.instance, new EquipmentGuiHandler());
        EquipmentRecipes.register();
        ArmorRecipes.register();
        FlightRecipes.register();
        if (DevEnvironment.isActive()) {
            ArmorScenarios.registerAll(ScenarioRegistry.SCENARIOS);
            ArmorTierScenarios.registerAll(ScenarioRegistry.SCENARIOS);
            SlotScenarios.registerAll(ScenarioRegistry.SCENARIOS);
            SlotSyncScenarios.registerAll(ScenarioRegistry.SCENARIOS);
            FlightScenarios.registerAll(ScenarioRegistry.SCENARIOS);
            PowerArmorScenarios.registerAll(ScenarioRegistry.SCENARIOS);
            ArmorShowcaseScenario.register(ScenarioRegistry.SCENARIOS);
            AccessoryShowcaseScenario.registerAll(ScenarioRegistry.SCENARIOS);
        }
    }
}
